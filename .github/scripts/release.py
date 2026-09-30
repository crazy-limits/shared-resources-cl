#!/usr/bin/env python3
"""
Works out the next version from Conventional Commits (https://www.conventionalcommits.org).

  release.py next [--bump auto|patch|minor|major]
      Prints the next version and writes release notes to CHANGELOG.md.
      `feat` bumps the minor version, `fix` and `perf` bump the patch version, and a `!` after the type
      or a `BREAKING CHANGE:` footer bumps the major version. Other types (chore, docs, ci, ...) don't
      make a release on their own.

  release.py lint <revision range>
      Fails if a commit in the range doesn't follow Conventional Commits.

Releases are tagged with the bare version (`1.10.0`), like the tags before this script. Tags with build
metadata from older releases (`1.9.2+1.21.4`) count as their base version.
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROPERTIES = ROOT / "stonecutter.properties.toml"
CHANGELOG = ROOT / "CHANGELOG.md"

TYPES = ("feat", "fix", "perf", "refactor", "revert", "docs", "style", "test", "build", "ci", "chore")
HEADER = re.compile(r"^(?P<type>[a-z]+)(?:\((?P<scope>[^()\s]+)\))?(?P<breaking>!)?: (?P<subject>\S.*)$")
BREAKING_FOOTER = re.compile(r"^BREAKING[ -]CHANGE: (?P<text>.+)$", re.MULTILINE)
TAG = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:\+.*)?$")
MOD_VERSION = re.compile(r'^(mod\.version\s*=\s*")([^"]*)(")', re.MULTILINE)

BUMPS = ("patch", "minor", "major")
SECTIONS = (("breaking", "Breaking changes"), ("feat", "Features"), ("fix", "Fixes"), ("perf", "Performance"))


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, check=True, capture_output=True, text=True).stdout


def parse_version(text):
    match = TAG.match(text)
    return tuple(int(part) for part in match.groups()) if match else None


def format_version(version):
    return ".".join(str(part) for part in version)


def bump(version, level):
    major, minor, patch = version
    if level == "major":
        return major + 1, 0, 0
    if level == "minor":
        return major, minor + 1, 0
    return major, minor, patch + 1


def last_release():
    """The highest released version reachable from HEAD, and every tag naming it."""
    tags = {}
    for tag in git("tag", "--merged", "HEAD").split():
        version = parse_version(tag)
        if version:
            tags.setdefault(version, []).append(tag)
    if not tags:
        return None, []
    version = max(tags)
    return version, tags[version]


def commits(*revisions):
    """(hash, subject, body) of each commit, oldest first, merge commits left out."""
    log = git("log", "--no-merges", "--reverse", "--format=%H%x1f%s%x1f%b%x1e", *revisions)
    for record in log.split("\x1e"):
        record = record.strip("\n")
        if record:
            sha, subject, body = record.split("\x1f")
            yield sha, subject, body


def parse(subject, body):
    match = HEADER.match(subject)
    if not match or match["type"] not in TYPES:
        return None
    footer = BREAKING_FOOTER.search(body)
    return {
        "type": match["type"],
        "scope": match["scope"],
        "subject": match["subject"],
        "breaking": bool(match["breaking"] or footer),
        "breaking_note": footer["text"] if footer else None,
    }


def read_mod_version():
    match = MOD_VERSION.search(PROPERTIES.read_text())
    if not match:
        sys.exit(f"no mod.version in {PROPERTIES.name}")
    return parse_version(match[2])


def write_mod_version(version):
    text = PROPERTIES.read_text()
    PROPERTIES.write_text(MOD_VERSION.sub(lambda m: m[1] + format_version(version) + m[3], text, count=1))


def release_notes(changes):
    lines = []
    for key, title in SECTIONS:
        if key == "breaking":
            entries = [c["breaking_note"] or c["subject"] for c in changes if c["breaking"]]
        else:
            entries = [c["subject"] for c in changes if c["type"] == key and not c["breaking"]]
        entries = [f"- {entry[0].upper()}{entry[1:]}" for entry in entries]
        if entries:
            lines += [f"### {title}", *entries, ""]
    return "\n".join(lines)


def next_version(args):
    last, last_tags = last_release()
    revisions = ["HEAD", *(f"^{tag}" for tag in last_tags)]
    changes = [change for change in (parse(s, b) for _, s, b in commits(*revisions)) if change]

    level = args.bump
    if level == "auto":
        if any(c["breaking"] for c in changes):
            level = "major"
        elif any(c["type"] == "feat" for c in changes):
            level = "minor"
        elif any(c["type"] in ("fix", "perf") for c in changes):
            level = "patch"
        else:
            level = None

    stored = read_mod_version()
    base = last or (0, 0, 0)
    version = bump(base, level) if level else None
    # mod.version may already have been raised by hand ahead of the release; never go below it
    if stored > base and (version is None or stored > version):
        version = stored
    if version is None:
        sys.exit(f"nothing to release since {format_version(base)}: no feat, fix, perf or breaking commits")

    notes = release_notes(changes)
    if not notes:
        # Only a hand-raised mod.version: keep the notes someone already wrote in CHANGELOG.md
        notes = CHANGELOG.read_text().strip() + "\n"
    if args.write:
        write_mod_version(version)
        CHANGELOG.write_text(notes)
    else:
        print(notes, file=sys.stderr)
    print(format_version(version))


def lint(args):
    bad = [(sha, subject) for sha, subject, body in commits(args.range) if not parse(subject, body)]
    for sha, subject in bad:
        print(f"::error::{sha[:8]} is not a conventional commit: {subject}")
    if bad:
        print(f"\nCommit subjects must look like `type(scope): subject`, type one of {', '.join(TYPES)}.")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)

    next_parser = commands.add_parser("next", help="print the next version")
    next_parser.add_argument("--bump", choices=("auto", *BUMPS), default="auto")
    next_parser.add_argument("--write", action="store_true", help="update mod.version and CHANGELOG.md")
    next_parser.set_defaults(run=next_version)

    lint_parser = commands.add_parser("lint", help="check commit messages")
    lint_parser.add_argument("range", help="revision range, like origin/master..HEAD")
    lint_parser.set_defaults(run=lint)

    args = parser.parse_args()
    args.run(args)


if __name__ == "__main__":
    main()
