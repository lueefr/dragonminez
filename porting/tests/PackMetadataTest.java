import com.dragonminez.platform.resource.PackMetadata;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;

public final class PackMetadataTest {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        for (PackType type : PackType.values()) {
            JsonObject metadata = new JsonObject(); metadata.addProperty("description", "test");
            PackMetadata.addCurrentFormat(metadata, type);
            var parsed = PackMetadataSection.forPackType(type).codec().parse(JsonOps.INSTANCE, metadata).getOrThrow();
            var expected = SharedConstants.getCurrentVersion().packVersion(type);
            if (!parsed.supportedFormats().minInclusive().equals(expected) || !parsed.supportedFormats().maxInclusive().equals(expected)) throw new AssertionError("Wrong runtime pack format for " + type);
        }
        System.out.println("Runtime pack metadata: client and server format codecs match the exact target Minecraft version.");
    }
}
