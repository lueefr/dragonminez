package com.dragonminez.api.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTranslationsTest {
    @TempDir Path dir;
    @Test void configNamesFallBackToEnglishThenRawAndReloadAtomically() throws Exception {
        Files.writeString(dir.resolve("en_us.json"),"{\"example.name\":\"Example\",\"example.only_english\":\"Fallback\"}");
        Files.writeString(dir.resolve("pt_br.json"),"{\"example.name\":\"Exemplo\"}");
        var t=new ConfigTranslations();t.reload(dir);
        assertEquals("Exemplo",t.translate("pt_br","example.name"));
        assertEquals("Fallback",t.translate("pt_br","example.only_english"));
        assertEquals("example.unknown",t.translate("pt_br","example.unknown"));
        t.replaceRemote(Map.of("pt_br",Map.of("example.name","Servidor")));
        assertEquals("Servidor",t.translate("pt_br","example.name"));
        t.clearRemote();assertEquals("Exemplo",t.translate("pt_br","example.name"));
        Files.writeString(dir.resolve("pt_br.json"),"{\"example.name\":42}");
        assertThrows(IllegalArgumentException.class,()->t.reload(dir));
        assertEquals("Exemplo",t.translate("pt_br","example.name"));
    }
}
