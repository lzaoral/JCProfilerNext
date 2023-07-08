#!/usr/bin/env python3

from pathlib import Path

import csv
import sys

if len(sys.argv) != 2:
    raise RuntimeError(f'USAGE: {sys.argv[0]} [DIR]')

input_dir = Path(sys.argv[1]).absolute()

# print CSV header
print('applet,deselect,reset,persistent')

# sort for reproducibility
for file in sorted(input_dir.glob('*_ctor_*')):
    measurements = file / 'measurements.csv'
    applet = file.name.split('_ctor_')[0]

    try:
        with open(measurements, 'r') as c:
            reader = csv.reader(c, delimiter=',', quotechar="'")

            # skip CSV header
            while reader.line_num < 5:
                next(reader)

            # get first measurement (it's always reachable)
            _, *max_mem = _, *min_mem = next(reader)

            for line in reader:
                # skip empty measurements
                if not line[1]:
                    continue

                _, *min_mem = line

            # print the difference for each memory type
            print(applet, *map(lambda a, b: int(a) - int(b), max_mem, min_mem),
                  sep=',')
    except FileNotFoundError:
        pass
