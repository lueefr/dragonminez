"""Validate resource JSON in the built mod, after Gradle resource processing."""
import json
import sys
import zipfile
from pathlib import Path

for name in sys.argv[1:]:
    with zipfile.ZipFile(name) as jar:
        entries = set(jar.namelist())
        assert "pack.mcmeta" in entries, f"{name}: missing pack metadata"
        for entry in entries:
            if entry == "pack.mcmeta" or (entry.startswith("assets/dragonminez/") and entry.endswith(".json")):
                tree = json.loads(jar.read(entry))
                if entry.startswith("assets/dragonminez/models/"):
                    parent = tree.get("parent", "")
                    if parent.startswith("dragonminez:"):
                        target = "assets/dragonminez/models/" + parent.split(":", 1)[1] + ".json"
                        assert target in entries, f"{entry}: missing parent {parent}"
                        assert target != entry, f"{entry}: self-referencing model"
    print(f"{Path(name).name}: packaged resource JSON and model parents valid")
