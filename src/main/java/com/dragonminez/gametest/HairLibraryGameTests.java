package com.dragonminez.gametest;

import com.dragonminez.common.hair.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.Files;
import java.util.EnumMap;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class HairLibraryGameTests {
    @GameTest(template="port_empty")
    public static void persistentPresets(GameTestHelper helper) throws Exception {
        var folder = Files.createTempDirectory("dmz-hair-library-");
        var library = new HairLibrary(folder);
        var styles = new EnumMap<HairStyleSlot, CustomHair>(HairStyleSlot.class);
        for (var slot : HairStyleSlot.values()) {
            var hair = new CustomHair(); hair.setGlobalColor("#AABBCC");
            var strand = new HairStrand(0); strand.setSegments(3); strand.setLength(4);
            strand.setJointStyle(HairJointStyle.CONNECTED);
            hair.setStrand(CustomHair.HairFace.FRONT, 0, strand);
            styles.put(slot, hair);
        }
        var entry = library.save("Across worlds", styles, false);
        var reopened = new HairLibrary(folder);
        helper.assertTrue(reopened.list().size() == 1, "Preset disappeared with a new library session");
        var restored = reopened.styles(entry.id());
        for (var slot : HairStyleSlot.values()) helper.assertTrue(styles.get(slot).save().equals(restored.get(slot).save()), "Hair data changed for " + slot);
        restored.get(HairStyleSlot.BASE).setGlobalColor("#FFFFFF");
        helper.assertTrue(reopened.styles(entry.id()).get(HairStyleSlot.BASE).getGlobalColor().equals("#AABBCC"), "Editing one character mutated the saved preset");
        helper.assertTrue(!reopened.read(entry.id()).getBoolean("HairBase"), "Hair base flag was lost");
        boolean rejected = false;
        try { reopened.read("../other"); } catch (IllegalArgumentException ex) { rejected = true; }
        helper.assertTrue(rejected, "Unsafe preset path accepted");
        reopened.delete(entry.id()); Files.delete(folder);
        helper.succeed();
    }
}
