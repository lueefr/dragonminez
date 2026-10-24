#!/usr/bin/env python3
"""Extract non-terrain blocks from bundled Otherworld MCA files as vanilla structure templates."""

from __future__ import annotations

import argparse
import gzip
import math
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REGIONS = ROOT / "src/main/resources/data/dragonminez/regions/otherworld"
TEMPLATES = ROOT / "src/main/resources/data/dragonminez/structure/otherworld_chunks"
INDEX = ROOT / "src/main/resources/data/dragonminez/otherworld/fixed_structure_chunks.txt"
WORLD_BOTTOM = -64
WORLD_HEIGHT = 448
DATA_VERSION = 3955  # Minecraft 1.21.1
LANDMARKS = (
    (-63, 146, -51, 63, 180, 84),
    (-13, 158, 976, 131, 326, 1120),
    (-12, 147, 68, 12, 163, 129),
    (-36, 124, 126, 6, 166, 193),
    (-36, 120, 190, -24, 148, 257),
    (-66, 135, 254, -24, 166, 321),
    (-66, 151, 318, -54, 166, 385),
    (-66, 151, 382, -54, 169, 449),
    (-66, 156, 446, -19, 197, 513),
    (-36, 185, 510, 6, 203, 577),
    (-36, 191, 574, -24, 207, 641),
    (-36, 154, 638, -24, 209, 705),
    (-36, 132, 702, -24, 167, 769),
    (-36, 132, 766, -24, 175, 833),
    (-36, 160, 830, -24, 175, 897),
    (-36, 157, 894, -7, 172, 961),
    (-19, 159, 958, 15, 180, 998),
)


class NbtReader:
    def __init__(self, data: bytes):
        self.data = data
        self.offset = 0

    def take(self, count: int) -> bytes:
        value = self.data[self.offset : self.offset + count]
        if len(value) != count:
            raise ValueError(f"Unexpected end of NBT data at offset {self.offset} requesting {count} of {len(self.data)} bytes")
        self.offset += count
        return value

    def unpack(self, fmt: str):
        return struct.unpack(">" + fmt, self.take(struct.calcsize(">" + fmt)))[0]

    def string(self) -> str:
        return self.take(self.unpack("H")).decode("utf-8")

    def payload(self, tag_type: int):
        if tag_type == 1:
            return self.unpack("b")
        if tag_type == 2:
            return self.unpack("h")
        if tag_type == 3:
            return self.unpack("i")
        if tag_type == 4:
            return self.unpack("q")
        if tag_type == 5:
            return self.unpack("f")
        if tag_type == 6:
            return self.unpack("d")
        if tag_type in (7, 11, 12):
            length = self.unpack("i")
            formats = {7: "b", 11: "i", 12: "q"}
            size = struct.calcsize(">" + formats[tag_type])
            if length < 0 or length > 16_000_000:
                raise ValueError(f"Invalid NBT array length {length}")
            return list(struct.unpack(">" + formats[tag_type] * length, self.take(length * size))) if length else []
        if tag_type == 8:
            return self.string()
        if tag_type == 9:
            element_type = self.unpack("B")
            length = self.unpack("i")
            if length < 0 or length > 16_000_000:
                raise ValueError(f"Invalid NBT list length {length}")
            return (element_type, [self.payload(element_type) for _ in range(length)])
        if tag_type == 10:
            result = {}
            while (child_type := self.unpack("B")) != 0:
                child_name = self.string()
                result[child_name] = (child_type, self.payload(child_type))
            return result
        raise ValueError(f"Unsupported NBT tag type {tag_type}")

    def root(self):
        tag_type = self.unpack("B")
        if tag_type != 10:
            raise ValueError("Expected NBT compound root")
        name = self.string()
        return name, self.payload(tag_type)


def tag_value(compound, name, expected_type=None, default=None):
    value = compound.get(name)
    if value is None:
        return default
    tag_type, payload = value
    if expected_type is not None and tag_type != expected_type:
        raise ValueError(f"Expected NBT type {expected_type} for {name}, got {tag_type}")
    return payload


def nbt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def nbt_tag(tag_type: int, name: str, payload: bytes) -> bytes:
    return struct.pack(">B", tag_type) + nbt_string(name) + payload


def nbt_int(value: int) -> bytes:
    return struct.pack(">i", value)


def nbt_int_list(values) -> bytes:
    return struct.pack(">Bi", 3, len(values)) + b"".join(nbt_int(value) for value in values)


def nbt_compound_payload(children) -> bytes:
    return b"".join(children) + b"\x00"


def write_string_compound(name: str, value: str) -> bytes:
    return nbt_tag(8, name, nbt_string(value))


def encode_palette_entry(state) -> bytes:
    raw_name = state.get("Name")
    name = raw_name[1] if isinstance(raw_name, tuple) else raw_name
    children = [write_string_compound("Name", name)]
    raw_properties = state.get("Properties", {})
    properties = raw_properties[1] if isinstance(raw_properties, tuple) else raw_properties
    if properties:
        entries = [write_string_compound(key, value[1] if isinstance(value, tuple) else value) for key, value in sorted(properties.items())]
        children.append(nbt_tag(10, "Properties", nbt_compound_payload(entries)))
    return nbt_compound_payload(children)


def encode_block(position, state_index, block_entity=None) -> bytes:
    children = [
        nbt_tag(9, "pos", struct.pack(">Bi", 3, 3) + b"".join(nbt_int(v) for v in position)),
        nbt_tag(3, "state", nbt_int(state_index)),
    ]
    if block_entity:
        children.append(nbt_tag(10, "nbt", encode_compound(block_entity)))
    return nbt_compound_payload(children)


def encode_compound(compound) -> bytes:
    encoded = []
    for name, (tag_type, payload) in compound.items():
        if tag_type == 1:
            data = struct.pack(">b", payload)
        elif tag_type == 2:
            data = struct.pack(">h", payload)
        elif tag_type == 3:
            data = nbt_int(payload)
        elif tag_type == 4:
            data = struct.pack(">q", payload)
        elif tag_type == 5:
            data = struct.pack(">f", payload)
        elif tag_type == 6:
            data = struct.pack(">d", payload)
        elif tag_type == 7:
            data = nbt_int(len(payload)) + bytes(payload)
        elif tag_type == 8:
            data = nbt_string(payload)
        elif tag_type == 9:
            element_type, elements = payload
            data = struct.pack(">Bi", element_type, len(elements)) + b"".join(encode_payload(element_type, item) for item in elements)
        elif tag_type == 10:
            data = encode_compound(payload)
        elif tag_type in (11, 12):
            code = "i" if tag_type == 11 else "q"
            data = nbt_int(len(payload)) + (struct.pack(">" + code * len(payload), *payload) if payload else b"")
        else:
            raise ValueError(f"Unsupported NBT tag type {tag_type}")
        encoded.append(nbt_tag(tag_type, name, data))
    return nbt_compound_payload(encoded)


def encode_payload(tag_type: int, payload) -> bytes:
    if tag_type == 10:
        return encode_compound(payload)
    if tag_type == 9:
        element_type, elements = payload
        return struct.pack(">Bi", element_type, len(elements)) + b"".join(
            encode_payload(element_type, item) for item in elements
        )
    if tag_type == 8:
        return nbt_string(payload)
    if tag_type == 7:
        return nbt_int(len(payload)) + bytes(payload)
    if tag_type in (11, 12):
        code = "i" if tag_type == 11 else "q"
        return nbt_int(len(payload)) + (struct.pack(">" + code * len(payload), *payload) if payload else b"")
    formats = {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}
    return struct.pack(">" + formats[tag_type], payload)


def encode_template(min_y, max_y, palette, blocks) -> bytes:
    children = [
        nbt_tag(3, "DataVersion", nbt_int(DATA_VERSION)),
        # Vanilla StructureTemplate.load reads size as a TAG_List of TAG_Int, not TAG_Int_Array.
        nbt_tag(9, "size", nbt_int_list((16, max_y - min_y + 1, 16))),
        nbt_tag(9, "palette", struct.pack(">Bi", 10, len(palette)) + b"".join(encode_palette_entry(state) for state in palette)),
        nbt_tag(9, "blocks", struct.pack(">Bi", 10, len(blocks)) + b"".join(
            encode_block((x, y, z), state_index, block_entity)
            for x, y, z, state_index, block_entity in blocks
        )),
        nbt_tag(9, "entities", struct.pack(">Bi", 10, 0)),
    ]
    return gzip.compress(struct.pack(">B", 10) + nbt_string("") + nbt_compound_payload(children), mtime=0)


def chunk_nbt(region: bytes, local_x: int, local_z: int):
    location_offset = 4 * (local_x + local_z * 32)
    location = struct.unpack_from(">I", region, location_offset)[0]
    sector_offset, _sector_count = location >> 8, location & 0xFF
    if sector_offset == 0:
        return None
    byte_offset = sector_offset * 4096
    length = struct.unpack_from(">I", region, byte_offset)[0]
    compression = region[byte_offset + 4]
    compressed = region[byte_offset + 5 : byte_offset + 4 + length]
    if compression == 2:
        raw = zlib.decompress(compressed)
    elif compression == 1:
        raw = gzip.decompress(compressed)
    elif compression == 3:
        raw = compressed
    else:
        raise ValueError(f"Unknown MCA compression type {compression}")
    return NbtReader(raw).root()[1]


def state_key(state):
    raw_name = state["Name"]
    name = raw_name[1] if isinstance(raw_name, tuple) else raw_name
    raw_properties = state.get("Properties", {})
    properties = raw_properties[1] if isinstance(raw_properties, tuple) else raw_properties
    if not properties:
        return name
    return name + "[" + ",".join(
        f"{key}={value[1] if isinstance(value, tuple) else value}" for key, value in sorted(properties.items())
    ) + "]"


def section_blocks(chunk, chunk_x, chunk_z):
    columns = {}
    for local_z in range(16):
        for local_x in range(16):
            world_x = chunk_x * 16 + local_x
            world_z = chunk_z * 16 + local_z
            intervals = [
                (min_y, max_y)
                for min_x, min_y, min_z, max_x, max_y, max_z in LANDMARKS
                if min_x <= world_x <= max_x and min_z <= world_z <= max_z
            ]
            if intervals:
                columns[(local_x, local_z)] = intervals
    if not columns:
        return None

    sections = tag_value(chunk, "sections", 9, (10, []))[1]
    block_entities = {}
    for block_entity in tag_value(chunk, "block_entities", 9, (10, []))[1]:
        x = tag_value(block_entity, "x", 3)
        y = tag_value(block_entity, "y", 3)
        z = tag_value(block_entity, "z", 3)
        if x is not None and y is not None and z is not None:
            block_entities[(x & 15, y, z & 15)] = block_entity

    state_by_position = {}
    for section in sections:
        section_y = tag_value(section, "Y", 1)
        if section_y is None:
            continue
        block_states = tag_value(section, "block_states", 10, {})
        palette = tag_value(block_states, "palette", 9, (10, []))[1]
        if not palette:
            continue
        state_ids = [state_key(state) for state in palette]
        data = tag_value(block_states, "data", 12)
        if data is None or len(palette) == 1:
            indexes = None
        else:
            bits = max(4, math.ceil(math.log2(len(palette))))
            values_per_long = 64 // bits
            mask = (1 << bits) - 1
            indexes = []
            for block_index in range(4096):
                long_index = block_index // values_per_long
                bit_offset = (block_index % values_per_long) * bits
                indexes.append((data[long_index] >> bit_offset) & mask)

        for block_index in range(4096):
            world_y = (section_y << 4) + (block_index >> 8)
            local_z = (block_index >> 4) & 15
            local_x = block_index & 15
            intervals = columns.get((local_x, local_z))
            if intervals is None or not any(min_y <= world_y <= max_y for min_y, max_y in intervals):
                continue
            palette_index = 0 if indexes is None else indexes[block_index]
            state_by_position[(local_x, world_y, local_z)] = (state_ids[palette_index], palette[palette_index])

    min_y = min(start for intervals in columns.values() for start, _ in intervals)
    max_y = max(end for intervals in columns.values() for _, end in intervals)
    air = {"Name": "minecraft:air"}
    palette_by_key = {"minecraft:air": air}
    blocks_by_position = {}
    for (local_x, local_z), intervals in columns.items():
        for start, end in intervals:
            for world_y in range(start, end + 1):
                position = (local_x, world_y, local_z)
                key, state = state_by_position.get(position, ("minecraft:air", air))
                palette_by_key.setdefault(key, state)
                blocks_by_position[position] = key

    # Include every masked position, including air, so both the surface and interior match the MCA.
    palette_keys = sorted(palette_by_key)
    palette_indexes = {key: index for index, key in enumerate(palette_keys)}
    palette = [palette_by_key[key] for key in palette_keys]
    blocks = []
    for (local_x, world_y, local_z), key in sorted(blocks_by_position.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        block_entity = block_entities.get((local_x, world_y, local_z))
        if block_entity:
            block_entity = dict(block_entity)
            block_entity.pop("x", None)
            block_entity.pop("y", None)
            block_entity.pop("z", None)
        blocks.append((local_x, world_y - min_y, local_z, palette_indexes[key], block_entity))
    return min_y, max_y, palette, blocks


def region_coordinates(path: Path):
    parts = path.stem.split(".")
    if len(parts) != 3 or parts[0] != "r":
        raise ValueError(f"Invalid region file name: {path.name}")
    return int(parts[1]), int(parts[2])


def chunk_intersects_landmark(chunk_x, chunk_z):
    min_x = chunk_x * 16
    min_z = chunk_z * 16
    max_x = min_x + 15
    max_z = min_z + 15
    return any(
        min_x <= box_max_x and max_x >= box_min_x and min_z <= box_max_z and max_z >= box_min_z
        for box_min_x, _box_min_y, box_min_z, box_max_x, _box_max_y, box_max_z in LANDMARKS
    )


def verify_template(path: Path, chunk_x, chunk_z, min_y, max_y, palette, blocks):
    _name, template = NbtReader(gzip.decompress(path.read_bytes())).root()
    if tag_value(template, "DataVersion", 3) != DATA_VERSION:
        raise AssertionError(f"Wrong DataVersion for chunk {chunk_x},{chunk_z}")

    def engine_list(name: str, expected_element_type: int):
        element_type, values = tag_value(template, name, 9)
        if element_type != expected_element_type:
            raise AssertionError(
                f"Wrong engine NBT list type for {name} in chunk {chunk_x},{chunk_z}: "
                f"root must be TAG_List, element={element_type}, expected element={expected_element_type}"
            )
        return values

    size_tag_type, size = tag_value(template, "size", 9)
    if size_tag_type != 3 or len(size) != 3:
        raise AssertionError(
            f"StructureTemplate.load expects a three-item TAG_List<TAG_Int> size in chunk {chunk_x},{chunk_z}; "
            f"got element={size_tag_type}, values={size}"
        )
    if size != [16, max_y - min_y + 1, 16]:
        raise AssertionError(f"Wrong template size for chunk {chunk_x},{chunk_z}: {size}")
    actual_palette = engine_list("palette", 10)
    engine_list("entities", 10)
    actual_palette_keys = [state_key(state) for state in actual_palette]
    actual_blocks = {}
    for block in engine_list("blocks", 10):
        position_tag_type, position = tag_value(block, "pos", 9)
        if position_tag_type != 3 or len(position) != 3:
            raise AssertionError(
                f"StructureTemplate.load expects block pos as a three-item TAG_List<TAG_Int> "
                f"in chunk {chunk_x},{chunk_z}: {position}"
            )
        position = tuple(position)
        state_index = tag_value(block, "state", 3)
        block_entity = tag_value(block, "nbt", 10)
        actual_blocks[position] = (actual_palette_keys[state_index], block_entity)

    expected_blocks = {}
    expected_palette_keys = [state_key(state) for state in palette]
    for local_x, local_y, local_z, state_index, block_entity in blocks:
        expected_blocks[(local_x, local_y, local_z)] = (expected_palette_keys[state_index], block_entity)
    if actual_blocks != expected_blocks:
        expected_positions = set(expected_blocks)
        actual_positions = set(actual_blocks)
        missing = expected_positions - actual_positions
        extra = actual_positions - expected_positions
        mismatch = next((pos for pos in expected_positions & actual_positions if expected_blocks[pos] != actual_blocks[pos]), None)
        raise AssertionError(
            f"Template differs from source MCA at chunk {chunk_x},{chunk_z}: "
            f"missing={len(missing)} extra={len(extra)} first_mismatch={mismatch}"
        )
    air_blocks = sum(1 for state, _nbt in actual_blocks.values() if state == "minecraft:air")
    block_entities = sum(1 for _state, nbt in actual_blocks.values() if nbt is not None)
    return air_blocks, len(actual_blocks) - air_blocks, block_entities


def extract(dry_run: bool, verify: bool):
    entries = []
    total_blocks = 0
    verified_chunks = 0
    verified_air_blocks = 0
    verified_non_air_blocks = 0
    verified_block_entities = 0
    if not dry_run:
        TEMPLATES.mkdir(parents=True, exist_ok=True)
        INDEX.parent.mkdir(parents=True, exist_ok=True)
    for region_path in sorted(REGIONS.glob("r.*.*.mca")):
        region_x, region_z = region_coordinates(region_path)
        region = region_path.read_bytes()
        if len(region) < 8192:
            raise ValueError(f"Invalid region file: {region_path}")
        for local_z in range(32):
            for local_x in range(32):
                chunk_x = region_x * 32 + local_x
                chunk_z = region_z * 32 + local_z
                if not chunk_intersects_landmark(chunk_x, chunk_z):
                    continue
                source_chunk = chunk_nbt(region, local_x, local_z)
                if source_chunk is None:
                    continue
                extracted = section_blocks(source_chunk, chunk_x, chunk_z)
                if extracted is None:
                    continue
                min_y, max_y, palette, blocks = extracted
                entries.append((chunk_x, chunk_z, min_y, max_y))
                total_blocks += len(blocks)
                if not dry_run:
                    path = TEMPLATES / f"chunk_{chunk_x}_{chunk_z}.nbt"
                    path.write_bytes(encode_template(min_y, max_y, palette, blocks))
                    if verify:
                        air_count, non_air_count, block_entity_count = verify_template(
                            path, chunk_x, chunk_z, min_y, max_y, palette, blocks
                        )
                        verified_chunks += 1
                        verified_air_blocks += air_count
                        verified_non_air_blocks += non_air_count
                        verified_block_entities += block_entity_count

    print(f"templates={len(entries)} blocks={total_blocks}")
    if not dry_run:
        INDEX.write_text(
            "# chunkX,chunkZ,minY,maxY; generated from OtherworldLandmarks and bundled 1.21.1 MCA regions\n"
            + "\n".join(",".join(map(str, row)) for row in sorted(entries))
            + "\n",
            encoding="utf-8",
        )
    if verify:
        if not any(chunk_x < 0 or chunk_z < 0 for chunk_x, chunk_z, _min_y, _max_y in entries):
            raise AssertionError("No negative-coordinate template was generated")
        if not verified_air_blocks or not verified_non_air_blocks:
            raise AssertionError("Template check did not cover both air and non-air states")
        print(
            f"verified={verified_chunks} air={verified_air_blocks} non_air={verified_non_air_blocks} "
            f"block_entities={verified_block_entities} negative_chunks=present"
        )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Count output templates without writing resources")
    parser.add_argument("--verify", action="store_true", help="Round-trip compare every generated template to its MCA mask")
    args = parser.parse_args()
    if args.verify and args.dry_run:
        parser.error("--verify requires writing templates; it cannot be combined with --dry-run")
    extract(args.dry_run, args.verify)


if __name__ == "__main__":
    main()
