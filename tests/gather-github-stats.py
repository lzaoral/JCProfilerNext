#!/usr/bin/env python3

# SPDX-FileCopyrightText: 2023 Lukáš Zaoral <lzaoral@outlook.com>
# SPDX-License-Identifier: GPL-3.0-only

from pathlib import Path

import csv
import json
import requests
import sys

GH_API_URL = "https://api.github.com/repos/"
STATS_FILE = Path('gh_stats.csv').absolute()

HEADERS = {
    'Accept': 'application/vnd.github+json',
    'Authorization': 'token ' + sys.argv[1],
    'X-GitHub-Api-Version': '2022-11-28'
}

print('Gathering GitHub repo statistics')


def get(path, params={}):
    res = requests.get(GH_API_URL + path, headers=HEADERS, params=params)
    if not res.ok:
        print(f'ERROR: Url: {path}, Code {res.status_code}, Body: {res.json()}')
    return res.ok, res.json()


def get_date(repo_name, branch):
    def get_commit(commit):
        if 'commit' in commit:
            return commit
        _, commit = get(f'{repo_name}/commits/{commit["sha"]}')
        return commit

    return get_commit(branch['commit'])['commit']['committer']['date']


def get_repo_information(repo, parent_fullname=''):
    if isinstance(repo, str):
        # get repo information
        ok, repo = get(repo)
        if not ok:
            return

    full_name = repo['full_name']

    # get license
    license = repo['license']
    if repo['license']:
        license = license['spdx_id']

    default_commit_date = 'N/A'

    # get last commit date in default branch
    ok, branch = get(f'{full_name}/branches/{repo["default_branch"]}')
    if ok:
        default_commit_date = get_date(full_name, branch)

    # append the CSV
    writer.writerow([repo['full_name'],
                     repo['forks_count'],
                     repo['stargazers_count'],
                     repo['fork'],
                     parent_fullname,
                     repo['archived'],
                     repo['created_at'],
                     license,
                     repo['pushed_at'],
                     default_commit_date])

    # process forks (TODO: support repos with more than 100 forks)
    ok, forks = get(f'{full_name}/forks', params={'per_page': 100})
    if not ok:
        return

    for fork in forks:
        fork_url = fork['html_url']
        print(f'{" " * (3 + 2 * len(str(size)))} Querying fork {fork_url}')
        get_repo_information(fork, full_name)


# generate STATS_FILE
with open('test-data.json', 'r') as j, open(STATS_FILE, 'w') as f:
    tests = json.load(j)['tests']
    repos = {t['repo'] for t in tests}

    size = len(repos)
    processed = 0

    print(f'test-data.json contains {len(tests)} applets from {size} repositories')

    # write CSV header
    writer = csv.writer(f)
    writer.writerow(['repo', 'forks', 'stars', 'is_fork', 'parent',
                     'is_archived', 'creation', 'license',
                     'last push in any branch',
                     'last commit in default branch'])

    # write repo metadata
    for repo in repos:
        processed += 1
        print(f'[{processed: >{len(str(size))}}/{size}] Querying {repo}')

        if 'github.com' not in repo:
            print(f'SKIP: {repo} is not on GitHub!')
            continue

        get_repo_information(repo.split('/', 3)[3])

    print(STATS_FILE.name, 'generated successfully.')
