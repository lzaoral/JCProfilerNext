#!/usr/bin/env python3

from collections import Counter
from pathlib import Path
from run import main, parse_args
from typing import Optional

import json
import os
import platform
import re
import shutil
import tempfile

PWD = Path.cwd()
STATS_FILE = Path('stats.txt').absolute()
STATS_DIR = Path('stats_out').absolute()

INCOMPLETE_HEADER_RE = re.compile(r'CSV generated in permissive mode!')

print('Gathering API usage statistics')

if os.path.exists(STATS_DIR):
    old_dir = tempfile.mkdtemp(prefix=str(STATS_DIR))
    print('Renaming previous stats dir to', old_dir)
    shutil.move(STATS_DIR, old_dir)

# gather stats
parse_args(['--mode', 'stats', '--output-dir', str(STATS_DIR)])
main()

# generate STATS_FILE
with open('test-data.json', 'r') as j, open(STATS_FILE, 'w') as f:
    tests = json.load(j)['tests']
    incomplete = 0

    for test in tests:
        print('# Applet:', test['name'], file=f)
        print('# Repo:', test['repo'], file=f)
        print('# Path:', test['path'], file=f)
        print(file=f)

        # skip if disabled
        osName = platform.system()
        if osName.lower() in test and not test[osName.lower()]:
            print('disabled on', osName, file=f)
            print(file=f)
            continue

        # append the CSV
        csv_path = next(STATS_DIR.glob(
                test['name'].replace(' ', '_') + '_stats_*'))
        with open(csv_path / 'APIstatistics.csv', 'r') as csv:
            contents = csv.read()
            if INCOMPLETE_HEADER_RE.search(contents) is not None:
                incomplete += 1
            f.write(contents)
        print('\n', file=f)

    print('Analysed', len(tests), 'projects.')
    print(incomplete, 'did not have all dependencies on class path!')
    print(STATS_FILE.name, 'generated successfully.')


# count API symbol usages across projects
with open(STATS_FILE, 'r') as f:
    # count applet usages
    counter: Counter[str] = Counter()
    for elem in f:
        # skip empty lines and comments
        if elem.isspace() or elem.startswith('#'):
            continue
        counter[elem.rpartition(',')[0]] += 1


def generate_summary(filename: str, members: bool) -> None:
    with open(f'{filename}-in.txt') as cin, \
         open(PWD / f'{filename}.txt', 'w') as cout:
        pkg: Optional[str] = None
        cls: Optional[str] = None

        print('# Number of projects that use the given symbol\n', file=cout)

        for line in cin:
            # strip whitespace
            line = line.strip()

            # skip empty lines
            if not line:
                print(file=cout)
                continue

            # handle section start
            if line.startswith('#'):
                print(line, file=cout)
                pkg = line.removeprefix('# ')
                if members:
                    pkg, _, cls = pkg.rpartition('.')
                continue

            assert pkg is not None, "Should never happen"

            # package
            key = f'{pkg},'

            # add class for members
            if members:
                key += f'{cls},'

            # escape methods with multiple arguments
            key += f'"{line}"' if ',' in line else line

            # add empty member for classes
            if not members:
                key += ','

            # remove the element so that we are left with stuff that's
            # not from JavaCard API
            print(line, '-', counter.pop(key, 0), file=cout)

    print(f'{filename}.txt generated successfully.')


generate_summary('constants', members=True)
generate_summary('types', members=False)
generate_summary('methods', members=True)


# store remaining symbols
with open(PWD / 'thirdparty.txt', 'w') as t:
    print('# Number of projects that use the given symbol', file=t)
    print('# package,type[,member] - count\n', file=t)
    for symbol, count in sorted(counter.items()):
        print(symbol.removesuffix(','), '-', count, file=t)
print('thirdparty.txt generated successfully.')
