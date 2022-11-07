#!/usr/bin/env python3

from pathlib import Path
from run import main, parse_args

import json
import os
import re
import tempfile

STATS_FILE = Path('stats.txt').absolute()
STATS_DIR = Path('stats_out').absolute()

INCOMPLETE_HEADER_RE = re.compile(r'CSV generated in permissive mode!')

print('Gathering API usage statistics')

if os.path.exists(STATS_DIR):
    old_dir = tempfile.mkdtemp(prefix=str(STATS_DIR))
    print('Renaming previous stats dir to', old_dir)
    os.rename(STATS_DIR, old_dir)

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

        # append the CSV
        csv_path = next(STATS_DIR.glob(
                test['name'].replace(' ', '_') + '_stats_*/APIstatistics.csv'))
        with open(csv_path, 'r') as csv:
            contents = csv.read()
            if INCOMPLETE_HEADER_RE.search(contents) is not None:
                incomplete += 1
            f.write(contents)
        print('\n', file=f)

    print('Analysed', len(tests), 'projects.')
    print(incomplete, 'did not have all dependencies on class path!')
    print(STATS_FILE.name, 'generated successfully.')
