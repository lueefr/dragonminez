import com.dragonminez.api.lang.ConfigTranslations;
import com.dragonminez.api.npc.CustomNpcRegistry;
import java.nio.file.*;
import java.util.*;

public final class CustomConfigTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "custom-config-");
        Path lang = Files.createDirectories(root.resolve("lang"));
        Files.writeString(lang.resolve("en_us.json"), "{\"custom.form\":\"Example form\",\"custom.fallback\":\"Fallback\"}");
        Files.writeString(lang.resolve("pt_br.json"), "{\"custom.form\":\"Forma de exemplo\"}");
        var translations = new ConfigTranslations();
        translations.reload(lang);
        check("Forma de exemplo".equals(translations.translate("pt_br","custom.form")));
        check("Fallback".equals(translations.translate("pt_br","custom.fallback")));
        check("custom.missing".equals(translations.translate("pt_br","custom.missing")));
        translations.replaceRemote(Map.of("pt_br",Map.of("custom.form","Nome do servidor")));
        check("Nome do servidor".equals(translations.translate("pt_br","custom.form")));
        translations.clearRemote();
        check("Forma de exemplo".equals(translations.translate("pt_br","custom.form")));
        Files.writeString(lang.resolve("pt_br.json"), "{\"custom.form\":42}");
        try { translations.reload(lang); throw new AssertionError("Invalid translation accepted"); }
        catch (IllegalArgumentException expected) { check(translations.translate("pt_br","custom.form").equals("Forma de exemplo")); }

        Path npcs = Files.createDirectories(root.resolve("custom_npcs"));
        Path placements = Files.createDirectories(root.resolve("custom_npc_placements"));
        String definition = "{\"id\":\"custom:guide\",\"baseTexture\":\"master_male\",\"name\":\"custom.npc\",\"alignment\":50}";
        Files.writeString(npcs.resolve("guide.json"),definition);
        Files.writeString(placements.resolve("guide.json"),"{\"npc\":\"custom:guide\",\"dimension\":\"minecraft:overworld\",\"x\":0,\"y\":70,\"z\":0}");
        var registry = new CustomNpcRegistry();
        registry.reload(root);
        check(registry.get("custom:guide").isPresent());
        check(registry.placements().size()==1);
        Files.writeString(npcs.resolve("duplicate.json"),definition);
        try { registry.reload(root); throw new AssertionError("Duplicate NPC accepted"); }
        catch (IllegalArgumentException expected) { check(registry.definitions().size()==1); }
        check(!Files.exists(root.resolve("masters")) && !Files.exists(root.resolve("enemies")));
        System.out.println("Config APIs: locale/English/raw fallback, server overlay, atomic reload, isolated NPC definitions, placements and duplicate validation passed.");
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
}
