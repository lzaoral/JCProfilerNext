#!/usr/bin/env python3

from pathlib import Path
from shutil import copytree, rmtree
from subprocess import call
from tempfile import mkdtemp
from typing import Any, Dict, List

import argparse
import json
import os
import re
import sys


MODES = ['memory', 'time']

STAGES = ['instrumentation', 'compilation', 'installation', 'profiling',
          'visualisation', 'all']


BOLD_RED = '\033[1;31m'
BOLD_GREEN = '\033[1;32m'
BOLD_YELLOW = '\033[1;33m'
RESET = '\033[00m'


def print(*args, colour=BOLD_GREEN, **kwargs):
    from builtins import print
    print(colour, end='')
    print(*args, **kwargs)
    print(RESET, end='', flush=True)


def rebuild_jar() -> None:
    suffix = '.bat' if os.name == 'nt' else ''
    script_path = Path('../gradlew' + suffix).absolute()

    print('Rebuilding project')
    ret = call([script_path, '--project-dir=..', 'build'])
    if ret != 0:
        print('JAR rebuild failed with return code', ret, colour=BOLD_RED)
        sys.exit(1)


def clone_git_repo(repo: str, target: str, reclone: bool = True) -> None:
    if os.path.exists(target):
        if not reclone:
            return
        rmtree(target)

    ret = call(['git', 'clone', '--depth=1', repo, target])
    if ret != 0:
        print('Cloning failed with return code', ret, colour=BOLD_RED)
        sys.exit(1)

    print(target, 'cloned successfully')
    return


def modify_repo(test: Dict[str, Any]):
    for rm in test.get('remove', []):
        for file in Path(test['name']).glob(rm):
            print('Removing', file)
            os.unlink(file)

    for replace in test.get('fixup', []):
        for glb in replace['files']:
            for file in Path(test['name']).glob(glb):
                regex = re.compile(replace['regex'], re.MULTILINE)
                regex_str = replace['regex'].replace('\n', '\\n')
                print('Removing lines matching', regex_str, 'from', file)
                with open(file, 'r') as f:
                    lines = f.read()
                with open(file, 'w') as f:
                    f.write(regex.sub('', lines))


def execute_cmd(cmd: List[str]) -> None:
    stages = ['all'] if ARGS.card else STAGES
    for stage in stages:
        stage_cmd = cmd.copy()

        if stage != 'all':
            stage_cmd += ['--start-from', str(stage)]
            stage_cmd += ['--stop-after', str(stage)]

        print('Excecuting stage', stage)
        print('Command: ', end='')
        print(" ".join(stage_cmd), colour=BOLD_YELLOW, flush=True)
        ret = call(stage_cmd)
        if ret != 0:
            print('Command failed with return code', ret, colour=BOLD_RED)
            sys.exit(1)


def prepare_workdir(test: Dict[str, Any], subtest_name: str) -> Path:
    test_dir = Path(mkdtemp(prefix=f'{test["name"]}_{subtest_name}_',
                    dir='.')).absolute()
    print('Created temporary directory', test_dir)

    copytree(Path(test['name']) / test['path'], test_dir,
             dirs_exist_ok=True)
    return test_dir


def test_ctor(test: Dict[str, Any], cmd: List[str], dir_prefix: str) -> None:
    ctor_cmd = cmd.copy()
    test_dir = prepare_workdir(test, dir_prefix + 'ctor')

    ctor_cmd += ['--work-dir', str(test_dir)]
    ctor_cmd += ['--mode', 'memory']
    execute_cmd(ctor_cmd)


def test_applet(test: Dict[str, Any], cmd: List[str],
                entry_point: Dict[str, Any] = {}) -> None:
    dir_prefix: str = ''
    test_desc = test

    if entry_point:
        test_desc = entry_point
        cmd += ['--entry-point', entry_point['name']]
        dir_prefix = entry_point['name'] + '_'

    if 'resetInst' in test_desc:
        cmd += ['--reset-inst', test_desc['resetInst']]
    if 'cla' in test_desc:
        cmd += ['--cla', test_desc['cla']]

    # test memory measurement in constructor
    test_ctor(test, cmd, dir_prefix)

    for subtest in test_desc['subtests']:
        sub_cmd = cmd.copy()
        test_dir = prepare_workdir(test, dir_prefix + subtest["method"])

        sub_cmd += ['--work-dir', str(test_dir)]
        sub_cmd += ['--method', subtest['method']]

        if 'input' in subtest:
            sub_cmd += ['--data-regex', subtest['input']]
        else:
            input_file = Path(subtest['inputFile']).absolute()
            sub_cmd += ['--data-file', str(input_file)]

        if 'inst' in subtest:
            sub_cmd += ['--inst', subtest['inst']]
        if 'p1' in subtest:
            sub_cmd += ['--p1', subtest['p1']]
        if 'p2' in subtest:
            sub_cmd += ['--p2', subtest['p2']]

        print('Executing subtest:', subtest['name'])
        for mode in MODES:
            print('Excecuting mode', mode)
            mode_cmd = sub_cmd.copy()
            mode_cmd += ['--mode', mode]

            execute_cmd(mode_cmd)

        # TODO: check format and contents of generated profiling reports


def execute_test(test: Dict[str, Any]) -> None:
    print('Running test', test['name'])

    test['name'] = test['name'].replace(' ', '_')
    clone_git_repo(test['repo'], test['name'])
    modify_repo(test)

    jar = Path('../build/libs/JCProfilerNext-1.0-SNAPSHOT.jar').absolute()

    min_jckit = ARGS.min_jckit
    jckit_version = \
        min_jckit if min_jckit and min_jckit > test['jckit'] else test['jckit']
    jckit = Path(f'jcsdk/jc{jckit_version}_kit').absolute()

    cmd = ['java', '-jar', str(jar), '--jckit', str(jckit),
                                     '--repeat-count', str(ARGS.repeat_count)]

    if not ARGS.card:
        cmd.append('--simulator')

    if 'entryPoints' not in test:
        test_applet(test, cmd)
        return

    for entry_point in test['entryPoints']:
        print('Using', entry_point['name'], 'entry point.')
        test_applet(test, cmd.copy(), entry_point)


def main() -> None:
    root = Path(__file__).parent.resolve()
    print('Test root:', root)
    os.chdir(root)

    rebuild_jar()

    with open('test_data.json') as f:
        data = json.load(f)

    clone_git_repo(data['jcsdkRepo'], 'jcsdk', reclone=False)

    tests = data['tests']
    if ARGS.filter:
        tests = [x for x in tests if x['name'] in ARGS.filter]
        if not tests:
            raise ValueError(f'No tests match the {ARGS.filter} filter.')

    for t in tests:
        execute_test(t)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
            description='JCProfilerNext integration test suite')

    parser.add_argument('--min-jckit',
                        help='Minimal JCKit version used during testing')
    parser.add_argument('filter', nargs='*',
                        help='List of applet names (see ./test_data.json)')

    parser.add_argument('--card', action='store_true',
                        help='Use a real card instead of a simulator')
    parser.add_argument('--repeat-count', type=int, default=100,
                        help='Number profiling rounds (default 100)')

    ARGS = parser.parse_args()

    main()
