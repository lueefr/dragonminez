import argparse
import json
from pathlib import Path

from scripts.release.release_common import next_github_release


def read_tags(path):
    return Path(path).read_text(encoding="utf-8").splitlines()


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Choose the next stable GitHub release tag for DragonMineZ."
    )
    parser.add_argument("--version", required=True)
    parser.add_argument("--tags-file", required=True)
    parser.add_argument("--release-tags-file", required=True)
    args = parser.parse_args(argv)

    result = next_github_release(
        args.version,
        read_tags(args.tags_file),
        read_tags(args.release_tags_file),
    )
    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
