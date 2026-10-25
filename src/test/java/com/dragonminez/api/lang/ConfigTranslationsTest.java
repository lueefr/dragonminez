package com.dragonminez.api.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTranslationsTest {
    @Test void runtimeAuditDeduplicatesDynamicKeysAndClearsAfterReload()throws Exception{
        var t=new ConfigTranslations();
        t.recordMissing("pt_br","example:trial");t.recordMissing("pt_br","example:trial");
        t.recordMissing("pt_br","12.5");t.recordMissing("pt_br","/private/path");
        assertEquals(java.util.Set.of("example:trial"),t.missingSnapshot().get("pt_br"));
        t.replaceRemote(Map.of("pt_br",Map.of("example:trial","Teste")));
        assertTrue(t.missingSnapshot().isEmpty());
    }

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
    @Test void configEnglishOverridesBundledSelectedAndBundledEnglishRemainsAvailable()throws Exception{
        var translations=new ConfigTranslations();
        assertEquals("%s TP",translations.translate("pt_br","gui.dragonminez.cost.tp"));
        Files.writeString(dir.resolve("en_us.json"),"{\"gui.dragonminez.cost.tp\":\"Custom %s\"}");
        translations.reload(dir);
        assertEquals("Custom %s",translations.translate("pt_br","gui.dragonminez.cost.tp"));
        assertEquals("example.missing",translations.translate("pt_br","example.missing"));
    }
}
