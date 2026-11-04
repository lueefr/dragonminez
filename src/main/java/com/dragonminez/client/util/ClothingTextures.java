package com.dragonminez.client.util;

import com.dragonminez.client.render.compat.CosmeticArmorCompat;
import com.dragonminez.common.config.ConfigManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import java.util.*;
import java.io.IOException;

public final class ClothingTextures {
    private static final Map<ResourceLocation, ResourceLocation> cache = new HashMap<>();
    private ClothingTextures() {}

    public static boolean hidesTop(Player player) {
        if (!ConfigManager.clothingGate()) return false;
        var stack = CosmeticArmorCompat.getVisibleStack(player, EquipmentSlot.CHEST);
        if (stack.isEmpty()) return false;
        Set<String> tags = new HashSet<>();
        stack.getTags().forEach(tag -> tags.add(tag.location().toString()));
        double damage = stack.getMaxDamage() > 0 ? (double) stack.getDamageValue() / stack.getMaxDamage() : 0;
        return ConfigManager.getClothing().hidesTop(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), tags, damage);
    }

    public static ResourceLocation resolve(ResourceLocation texture, boolean hidden) {
        if (!hidden || !texture.getNamespace().equals("dragonminez")) return texture;
        String path = texture.getPath();
        if (!(path.startsWith("textures/entity/races/humansaiyan/bodytype_female_") || path.startsWith("textures/entity/races/majin/bodytype_female_"))) return texture;
        if (!path.endsWith("layer1.png") && !path.endsWith("layer1_shadow.png")) return texture;
        return cache.computeIfAbsent(texture, ClothingTextures::create);
    }

    private static ResourceLocation create(ResourceLocation texture) {
        var mc = Minecraft.getInstance();
        boolean shadow = texture.getPath().endsWith("_shadow.png");
        var base = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), texture.getPath().replace("_shadow.png", ".png"));
        try (var input = mc.getResourceManager().open(texture); var maskInput = mc.getResourceManager().open(base);
             var mask = NativeImage.read(maskInput)) {
            NativeImage image = NativeImage.read(input);
            try {
                for (int y = 16; y < Math.min(30, image.getHeight()); y++) {
                    for (int x = 16; x < Math.min(40, image.getWidth()); x++) {
                        if (x >= mask.getWidth() || y >= mask.getHeight()) continue;
                        int pixel = mask.getPixelRGBA(x,y);
                        if ((pixel >>> 24) != 0 && (pixel & 0xFFFFFF) == 0)
                            image.setPixelRGBA(x,y,shadow ? 0 : 0xFFFFFFFF);
                    }
                }
                return mc.getTextureManager().register("dmz_clothing", new DynamicTexture(image));
            } catch (RuntimeException ex) { image.close(); throw ex; }
        } catch (IOException | RuntimeException ex) {
            com.dragonminez.LogUtil.error(com.dragonminez.Env.CLIENT,"Cannot prepare clothing texture {}: {}",texture,ex.getMessage());
            return texture;
        }
    }

    public static void clearCache() {
        var manager = Minecraft.getInstance().getTextureManager();
        cache.forEach((source, generated) -> { if (!source.equals(generated)) manager.release(generated); });
        cache.clear();
    }
}
