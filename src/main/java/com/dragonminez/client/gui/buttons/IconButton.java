package com.dragonminez.client.gui.buttons;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IconButton extends Button {
	private final Identifier icon;
	private final int iconSize;
	private final int textureSize;

	public IconButton(int x, int y, int width, int height, Identifier icon, int iconSize, int textureSize,
						Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.icon = icon;
		this.iconSize = iconSize;
		this.textureSize = textureSize;
	}

	@Override
	protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		int iconX = this.getX() + (this.getWidth() - iconSize) / 2;
		int iconY = this.getY() + (this.getHeight() - iconSize) / 2;
		graphics.blit(icon, iconX, iconY, iconSize, iconSize, 0.0F, 0.0F, textureSize, textureSize, textureSize, textureSize);
	}

	@Override
	public void renderString(GuiGraphicsExtractor graphics, Font font, int color) {
		// Icon-only button; skip text rendering to avoid overlap.
	}
}
