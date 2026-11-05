"""Serialize publication in Actions; derive notes from exact immutable Git ranges."""
import argparse
import json
import os
import re
import subprocess
from pathlib import Path
from scripts.release.release_common import read_gradle_properties, sha256_file

ARTIFACT_FILES = {"build.gradle.kts", "settings.gradle", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "packsquash.toml"}


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def artifact_change(paths):
    return any(path in ARTIFACT_FILES or path.startswith(("src/main/", "src/generated/resources/", "gradle/")) for path in paths)


def version_key(tag):
    match = re.fullmatch(r"v?(\d+)\.(\d+)(?:\.(\d+))?(?:-hotfix(?:-(\d+))?)?", tag)
    if not match:
        return None
    major, minor, patch, hotfix = match.groups()
    return int(major), int(minor), int(patch or 0), int(hotfix or (1 if "hotfix" in tag else 0))


def next_tag(version, tags, numbering="version"):
    if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?", version):
        raise ValueError("GitHub stable releases require a numeric source version")
    base = f"v{version}"
    if base not in tags:
        return base
    numbers = [0]
    for tag in tags:
        key = version_key(tag)
        if key and (numbering == "global" or tag.startswith(base + "-hotfix")):
            numbers.append(key[3])
    return f"{base}-hotfix-{max(numbers) + 1}"


def ancestor(first, second):
    return subprocess.run(["git", "merge-base", "--is-ancestor", first, second], check=False).returncode == 0


def resolve(tag):
    result = subprocess.run(["git", "rev-parse", "--verify", f"{tag}^{{commit}}"], capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else None


def predecessors(releases, source):
    result = []
    for release in releases:
        if release.get("draft") or release.get("prerelease") or not version_key(release["tag_name"]):
            continue
        sha = resolve(release["tag_name"])
        if sha and sha != source and ancestor(sha, source):
            result.append((version_key(release["tag_name"]), release["tag_name"], sha))
    return sorted(result, reverse=True)


def notes(tag, source, previous, repository, directory=Path("release-notes")):
    exact = directory / f"{tag.removeprefix('v')}.md"
    if exact.is_file():
        text = exact.read_text(encoding="utf-8").strip()
        if not text:
            raise ValueError(f"Empty release notes: {exact}")
    else:
        revision = f"{previous}..{source}" if previous else source
        commits = run("git", "log", "--first-parent", "--reverse", "--format=%H", revision).splitlines()
        changes = []
        for sha in commits:
            parent = resolve(sha + "^")
            paths = run("git", "diff", "--name-only", parent, sha).splitlines() if parent else run("git", "show", "--format=", "--name-only", sha).splitlines()
            if artifact_change(paths):
                subject = run("git", "show", "-s", "--format=%s", sha)
                subject = re.sub(r"([\\`*_[\]<>])", r"\\\1", subject)
                changes.append(f"- {subject} ([{sha[:8]}](https://github.com/{repository}/commit/{sha})).")
        if not changes:
            raise ValueError("No mod changes in this release range; refusing recycled or empty notes")
        text = "## Changes\n\n" + "\n".join(changes)
    if previous:
        text += f"\n\n[Compare changes](https://github.com/{repository}/compare/{previous}...{tag})"
    return text + f"\n\n<!-- source-commit: {source} -->\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["publish", "notes"])
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--source", default=os.environ.get("GITHUB_SHA", "HEAD"))
    parser.add_argument("--tag")
    parser.add_argument("--output", default="release-changelog.md")
    parser.add_argument("--numbering", choices=["version", "global"], default="version")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if not args.repository or not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", args.repository):
        parser.error("An explicit owner/repository is required")
    source = resolve(args.source)
    if not source:
        parser.error("Source commit does not exist")
    releases = json.loads(run("gh", "api", f"repos/{args.repository}/releases?per_page=100", "--paginate", "--slurp"))
    releases = [release for page in releases for release in page]
    properties = read_gradle_properties("gradle.properties")
    version = properties["mod_version"]
    if version_key(version) is None:
        print(f"Skipping non-stable version {version}"); return
    tags = set(run("git", "tag", "--list").splitlines()) | {r["tag_name"] for r in releases}
    tag = args.tag or next_tag(version, tags, args.numbering)
    earlier = predecessors(releases, source)
    previous = earlier[0][1] if earlier else None
    if args.mode == "publish":
        if not ancestor(source, "origin/main"):
            raise ValueError("Release source is not on origin/main")
        # Never promote an older build, even when queued builds complete out of order.
        for release in releases:
            if release.get("draft") or release.get("prerelease") or not version_key(release["tag_name"]):
                continue
            sha = resolve(release["tag_name"])
            if sha == source:
                print(f"Already published as {release['tag_name']}"); return
            if sha and ancestor(source, sha):
                print(f"Skipping obsolete source; {release['tag_name']} already contains it"); return
            if version_key(release["tag_name"])[:3] > version_key(version)[:3]:
                print("Skipping older version series"); return
        if artifact_change(run("git", "diff", "--name-only", source, "origin/main").splitlines()):
            print("Skipping obsolete build: newer mod changes are on main"); return
        if earlier and not artifact_change(run("git", "diff", "--name-only", earlier[0][2], source).splitlines()):
            print("No mod changes since the last release"); return
    body = notes(tag, source, previous, args.repository)
    Path(args.output).write_text(body, encoding="utf-8")
    if args.mode == "notes":
        return
    minecraft = properties["minecraft_version"]
    artifact = Path(f"build/libs/dragonminez-{minecraft}-{version}.jar")
    digest = sha256_file(artifact)
    body += f"\nJAR SHA-256: `{digest}`\n"
    Path(args.output).write_text(body, encoding="utf-8")
    hotfix = version_key(tag)[3]
    title = f"DragonMineZ {version}" + (f" - Hotfix {hotfix}" if hotfix else "") + f" (Minecraft {minecraft} NeoForge)"
    if args.dry_run:
        print(json.dumps({"tag": tag, "source": source, "previous": previous, "name": title, "sha256": digest}, indent=2)); return
    run("gh", "release", "create", tag, str(artifact), "--repo", args.repository, "--target", source, "--title", title, "--notes-file", args.output, "--latest")
    print(f"Published {tag} from {source}")


if __name__ == "__main__":
    main()
