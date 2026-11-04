package com.dragonminez.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ClothingConfigTest {
    @TempDir Path dir;

    @Test void matchesRules() {
        var config = ClothingConfig.parse("""
                {"rules":[
                  {"tags":["example:open_robes"],"hideTop":true},
                  {"items":["example:robe"],"minDamage":0.5,"hideTop":false}
                ]}
                """);
        assertTrue(config.hidesTop("example:robe",Set.of("example:open_robes"),0));
        assertFalse(config.hidesTop("example:robe",Set.of("example:open_robes"),0.5));
        assertFalse(config.hidesTop("example:other",Set.of(),0));
    }

    @Test void preservesFiles() throws Exception {
        Path file = dir.resolve("appearance/clothing.json");
        assertTrue(ClothingConfig.load(file).hidesTop("dragonminez:gogeta_armor_chestplate",Set.of(),0));
        String invalid = "{\"rules\":[{\"items\":[\"invalid\"]}]}";
        Files.writeString(file,invalid);
        assertThrows(IllegalArgumentException.class,()->ClothingConfig.load(file));
        assertEquals(invalid,Files.readString(file));
        assertThrows(IllegalArgumentException.class,()->ClothingConfig.parse("{\"rules\":[{\"items\":[\"example:robe\"],\"minDamage\":2}]}"));
    }
}
