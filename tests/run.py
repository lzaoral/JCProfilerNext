#!/usr/bin/env python3

from pathlib import Path
from shutil import copytree
from subprocess import call
from tempfile import mkdtemp
from typing import Any, Dict

import json
import os
import re
import sys


STAGES = ['instrumentation', 'compilation', 'installation', 'profiling',
          'visualisation', 'all']


def rebuild_jar() -> None:
    suffix = '.bat' if os.name == 'nt' else ''
    script_path = Path('../gradlew' + suffix)

    print('Rebuilding project')
    ret = call([script_path.absolute(), '--project-dir=..', 'build'])
    if ret != 0:
        print('JAR rebuild failed with return code', ret)
        sys.exit(1)


def clone_git_repo(repo: str, target: str) -> bool:
    if os.path.exists(target):
        print(repo, 'seems to be already cloned')
        return False

    print(target, 'is not cloned yet', flush=True)

    ret = call(['git', 'clone', '--depth=1', repo, target])
    if ret != 0:
        print('Cloning failed with return code', ret)
        sys.exit(1)

    print(target, 'cloned successfully')
    return True


def modify_repo(test: Dict[str, Any]):
    for rm in test['remove']:
        for file in Path(test['name']).glob(rm):
            print('Removing', file)
            os.unlink(file)

    for replace in test['fixup']:
        for glb in replace['files']:
            for file in Path(test['name']).glob(glb):
                regex = re.compile(replace['regex'], re.MULTILINE)
                regex_str = replace['regex'].replace('\n', '\\n')
                print('Removing lines matching', regex_str, 'from', file)
                with open(file, 'r') as f:
                    lines = f.read()
                with open(file, 'w') as f:
                    f.write(regex.sub('', lines))


def execute_test(test: Dict[str, Any]):
    name = test['name']
    print('Running test', name)

    if clone_git_repo(test['repo'], test['name']):
        modify_repo(test)

    jar = Path('../build/libs/JCProfilerNext-1.0-SNAPSHOT.jar').absolute()
    jckit = Path(f'jcsdk/jc{test["jckit"]}_kit').absolute()

    cmd = ['java', '-jar', str(jar), '--jckit', str(jckit), '--simulator',
                                     '--repeat-count', '1000']

    if 'entryPoint' in test:
        cmd += ['--entry-point', test['entryPoint']]
    if 'resetInst' in test:
        cmd += ['--reset-inst', test['resetInst']]
    if 'cla' in test:
        cmd += ['--cla', test['cla']]

    for subtest in test['subtests']:
        sub_cmd = cmd.copy()

        test_dir = Path(mkdtemp(prefix=f'{test["name"]}_{subtest["method"]}_',
                        dir='.')).absolute()
        print('Created temporary directory', test_dir)

        copytree(Path(test['name']) / test['path'], test_dir,
                 dirs_exist_ok=True)

        sub_cmd += ['--work-dir', str(test_dir)]
        sub_cmd += ['--method', subtest['method']]
        sub_cmd += ['--inst', subtest['inst']]

        if 'input' in subtest:
            sub_cmd += ['--data-regex', subtest['input']]
        else:
            input_file = Path(subtest['inputFile']).absolute()
            sub_cmd += ['--data-file', str(input_file)]

        if 'p1' in subtest:
            sub_cmd += ['--p1', subtest['p1']]
        if 'p2' in subtest:
            sub_cmd += ['--p2', subtest['p2']]

        print('Executing subtest:', subtest['name'])

        for stage in STAGES:
            stage_cmd = sub_cmd.copy()

            if stage != "all":
                stage_cmd += ['--start-from', str(stage)]
                stage_cmd += ['--stop-after', str(stage)]

            print('Excecuting stage', stage)
            print('Command:', " ".join(stage_cmd), flush=True)
            ret = call(stage_cmd)
            if ret != 0:
                print('Command failed with return code', ret)
                sys.exit(1)

        # TODO: check format and contents of generated profiling reports


def main():
    root = Path(__file__).parent.resolve()
    print('Test root:', root)
    os.chdir(root)

    rebuild_jar()

    with open('test_data.json') as f:
        data = json.load(f)

    clone_git_repo(data['jcsdkRepo'], 'jcsdk')
    for t in data['tests']:
        execute_test(t)


if __name__ == '__main__':
    main()
