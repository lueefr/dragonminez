package com.dragonminez.common.wish;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.diagnostics.JsonKeys;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.dragonminez.common.diagnostics.JsonSchema;
import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.common.util.gson.WishTypeAdapterFactory;
import com.dragonminez.common.wish.wishes.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WishManager {

	public static void init() {}

	public static void loadWishes(MinecraftServer server) {
		if (server == null) {
			LogUtil.warn(Env.COMMON, "Cannot load wishes: server is null");
			return;
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			LogUtil.warn(Env.COMMON, "Cannot load wishes: overworld is null");
			return;
		}

		JsonLoadReport.clear("wishes");
		Path worldFolder = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
		Path dragonminezFolder = worldFolder.resolve("dragonminez");
		Path wishDir = dragonminezFolder.resolve("wishes");

		Map<String, List<Wish>> merged = new LinkedHashMap<>(DragonWishRegistry.getServerWishes());
        Map<String,Integer> counts=new LinkedHashMap<>();
		try {
			if (!Files.exists(wishDir)) {
				Files.createDirectories(wishDir);
			}

			try (var stream = Files.list(wishDir)) {
				List<String> filenames = stream.map(path -> path.getFileName().toString()).toList();
				if (!filenames.contains("shenron.json")) {
					createDefaultShenronWishes(wishDir);
				}
				if (!filenames.contains("porunga.json")) {
					createDefaultPorungaWishes(wishDir);
				}
				if (!filenames.contains("porunga_daima.json")) {
					writeDefaultWishes(wishDir, "porunga_daima", DefaultWishes.porungaDaima());
				}
			}

			try (var stream = Files.list(wishDir)) {
				stream.filter(path -> path.getFileName().toString().endsWith(".json"))
						.sorted(Comparator.comparing(path -> path.getFileName().toString()))
						.forEach(path -> loadWishConfig(path, merged, counts));
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to load wishes", e);
		}

		DragonWishRegistry.setServerWishes(merged);
        com.dragonminez.common.dragonball.DragonBallDefinitions.wishCounts(counts);
		LogUtil.info(Env.COMMON, "Loaded {} dragon wish list(s) after merging datapacks and config files", merged.size());
	}

	private static void loadWishConfig(Path path, Map<String, List<Wish>> merged,Map<String,Integer> counts) {
		try {
            String original=Files.readString(path);
            JsonElement root=JsonParser.parseString(original);
            String dragonId=path.getFileName().toString().replace(".json", "");
            boolean legacy=root.isJsonArray();
            JsonArray rootArray=legacy?root.getAsJsonArray():root.getAsJsonObject().getAsJsonArray("wishes");
            int count=legacy?defaultCount(dragonId):root.getAsJsonObject().getAsJsonObject("summoning").get("wishCount").getAsInt();
            if(!legacy&&root.getAsJsonObject().getAsJsonObject("summoning").get("wishCount").getAsBigDecimal().compareTo(java.math.BigDecimal.valueOf(count))!=0)
                throw new IllegalArgumentException("Wish count must be an integer");
            if(count<1||count>com.dragonminez.common.network.C2S.GrantWishC2S.MAX_WISHES)throw new IllegalArgumentException("Wish count must be within 1.."+com.dragonminez.common.network.C2S.GrantWishC2S.MAX_WISHES);
			List<Wish> wishes = new ArrayList<>();
			for (JsonElement element : rootArray) {
				validateWish("wishes/" + path.getFileName(), element);
				Wish wish=GsonUtils.GSON.fromJson(element, Wish.class);
                wish.validate();wishes.add(wish);
			}
            if(legacy)saveConverted(path,original,rootArray,count);
            counts.put(dragonId,count);
			merged.put(dragonId, List.copyOf(wishes));
			LogUtil.info(Env.COMMON, "Loaded dragon wishes from config file {}", path.getFileName());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load dragon wish config '{}': {}", path.getFileName(), e.toString());
			JsonLoadReport.error("wishes", "wishes/" + path.getFileName(), "Malformed wish JSON, file skipped: " + JsonLoadReport.rootCause(e));
		}
	}

    private static int defaultCount(String id) {
        return com.dragonminez.common.dragonball.DragonBallDefinitions.defaultCount(id);
    }
    private static JsonObject envelope(JsonArray wishes,int count) {
        var root=new JsonObject();var summoning=new JsonObject();summoning.addProperty("wishCount",count);
        root.add("summoning",summoning);root.add("wishes",wishes);return root;
    }
    private static void saveConverted(Path file,String original,JsonArray wishes,int count)throws IOException {
        if(!Files.readString(file).equals(original))throw new IOException("Wish file changed while loading");
        Path folder=file.getParent().resolve("oldBackup");Files.createDirectories(folder);
        Path backup=folder.resolve(file.getFileName()+".legacy.bak");
        for(int index=1;Files.exists(backup);index++)backup=folder.resolve(file.getFileName()+".legacy."+index+".bak");
        Files.copy(file,backup);
        Path temporary=Files.createTempFile(file.getParent(),"wish-",".tmp");
        try {
            Files.writeString(temporary,GsonUtils.GSON.toJson(envelope(wishes,count)));
            if(!Files.readString(file).equals(original))throw new IOException("Wish file changed during conversion");
            try{Files.move(temporary,file,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
            catch(java.nio.file.AtomicMoveNotSupportedException ex){Files.move(temporary,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
        LogUtil.info(Env.COMMON,"Converted wishes {}; original backup: {}",file,backup);
    }

	private static void validateWish(String file, JsonElement element) {
		if (element == null || !element.isJsonObject()) return;
		JsonObject obj = element.getAsJsonObject();
		String type = obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : null;
		Class<? extends Wish> target = WishTypeAdapterFactory.classForType(type);
		if (target == null) {
			JsonKeys.reportBadType("wishes", file, "wish", type);
		} else {
			JsonSchema.check("wishes", file, "wish", obj, target);
		}
	}

	private static void createDefaultShenronWishes(Path wishDir) {
		writeDefaultWishes(wishDir, "shenron", DefaultWishes.shenron());
	}

	private static void createDefaultPorungaWishes(Path wishDir) {
		writeDefaultWishes(wishDir, "porunga", DefaultWishes.porunga());
	}

	private static void writeDefaultWishes(Path wishDir, String dragonId, List<Wish> defaultWishes) {
		Path wishFile = wishDir.resolve(dragonId + ".json");
		try (Writer writer = Files.newBufferedWriter(wishFile, StandardCharsets.UTF_8)) {
			Type listType = new TypeToken<ArrayList<Wish>>() {
			}.getType();
			JsonArray wishes=GsonUtils.GSON.toJsonTree(defaultWishes,listType).getAsJsonArray();
            GsonUtils.GSON.toJson(envelope(wishes,defaultCount(dragonId)),writer);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not create default wishes for " + dragonId, e);
		}
	}

	public static Map<String, List<Wish>> getAllWishes() {
		return DragonWishRegistry.getServerWishes();
	}

	public static List<Wish> getClientWishes(String dragonName) {
		return DragonWishRegistry.getClientWishes().getOrDefault(dragonName, new ArrayList<>());
	}

	public static void applySyncedWishes(Map<String, List<Wish>> wishes) {
		DragonWishRegistry.setClientWishes(wishes); LogUtil.info(Env.CLIENT, "Loaded {} wish list(s) from server", wishes.size());
	}
}
