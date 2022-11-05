#!/bin/sh -eu

echo "Gathering API usage statistics"
rm stats.txt || true
rm -r stats_out || true
./run.py --stats --output-dir stats_out

set -- stats_out/*
for f in "$@"; do
    {
        APPLET_DIR="$(basename "$f" | sed -e 's/_stats_.*$//')"
        REPO_URL="$(git -C "$APPLET_DIR" remote get-url origin)"

        # print header
        echo "# Applet: $APPLET_DIR"
        echo "# Repo: $REPO_URL"
        echo

        # append stats
        cat "$f/APIstatistics.csv"
        printf "\n\n"
    } >> stats.txt
done

echo "Analysed $# projects."
INCOMPLETE_COUNT="$(grep -c 'CSV generated in permissive mode!' stats.txt)"
echo "$INCOMPLETE_COUNT did not have all dependencies on class path!"

echo "stats.txt generated successfully."
