import hashlib
import json
import re
from pathlib import Path


def read_gradle_properties(path):
    values = {}
    for raw_line in Path(path).read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def version_type(version):
    lowered = version.lower()
    if "alpha" in lowered:
        return "alpha"
    if "beta" in lowered:
        return "beta"
    return "release"


def ensure_stable_release(version):
    detected = version_type(version)
    if detected != "release":
        raise ValueError(
            f"Automatic publishing on main is limited to stable releases; "
            f"version '{version}' is classified as '{detected}'."
        )


def next_github_release(version, tags, release_tags):
    """Choose the next GitHub tag from all existing tags and release tags."""
    ensure_stable_release(version)
    base_tag = f"v{version}"
    known_tags = set(tags) | set(release_tags)

    hotfix_numbers = []
    has_release_for_version = base_tag in known_tags
    numbered_prefix = f"{base_tag}-hotfix-"
    legacy_tag = f"{base_tag}-hotfix"

    for tag in known_tags:
        if tag == legacy_tag:
            has_release_for_version = True
            hotfix_numbers.append(1)
        elif tag.startswith(numbered_prefix):
            suffix = tag[len(numbered_prefix):]
            if re.fullmatch(r"[1-9][0-9]*", suffix):
                has_release_for_version = True
                hotfix_numbers.append(int(suffix))

    if not has_release_for_version:
        return {"tag": base_tag, "kind": "base", "hotfix_number": None}

    hotfix_number = max(hotfix_numbers, default=0) + 1
    return {
        "tag": f"{base_tag}-hotfix-{hotfix_number}",
        "kind": "hotfix",
        "hotfix_number": hotfix_number,
    }


def sha256_file(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_manifest(
    *,
    gradle_properties,
    artifact_path,
    commit_sha,
    repository,
    run_id,
    server_url,
    changelog,
    require_stable,
):
    version = gradle_properties["mod_version"]
    if require_stable:
        ensure_stable_release(version)

    artifact = Path(artifact_path)
    if not artifact.is_file():
        raise FileNotFoundError(f"Release artifact does not exist: {artifact}")

    release_channel = version_type(version)
    return {
        "version": version,
        "release_type": release_channel,
        "minecraft_version": gradle_properties["minecraft_version"],
        "loader": "neoforge" if "neo_version" in gradle_properties else "forge",
        "forge_version": gradle_properties.get("forge_version"),
        "neo_version": gradle_properties.get("neo_version"),
        "commit_sha": commit_sha,
        "repository": repository,
        "workflow_run_url": f"{server_url}/{repository}/actions/runs/{run_id}",
        "targets": ["modrinth", "curseforge"],
        "artifact": {
            "name": artifact.name,
            "path": artifact.as_posix(),
            "sha256": sha256_file(artifact),
        },
        "changelog": changelog,
        "bot_payload": {
            "event_type": "dragonminez_release_candidate",
            "client_payload": {
                "version": version,
                "release_type": release_channel,
                "minecraft_version": gradle_properties["minecraft_version"],
                "loader": "neoforge" if "neo_version" in gradle_properties else "forge",
                "forge_version": gradle_properties.get("forge_version"),
                "neo_version": gradle_properties.get("neo_version"),
                "commit_sha": commit_sha,
                "artifact_name": artifact.name,
                "targets": ["modrinth", "curseforge"],
                "workflow_run_url": f"{server_url}/{repository}/actions/runs/{run_id}",
            },
        },
    }


def update_forge_update_json(data, *, minecraft_version, version, description):
    updated = dict(data)
    promos = dict(updated.get("promos", {}))
    promos[f"{minecraft_version}-latest"] = version
    promos[f"{minecraft_version}-recommended"] = version
    updated["promos"] = promos

    versions = dict(updated.get(minecraft_version, {}))
    versions[version] = description
    updated[minecraft_version] = versions
    return updated


def write_json(path, data):
    Path(path).write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
