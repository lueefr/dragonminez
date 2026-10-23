package com.dragonminez.client.gui.hair;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.PanelSkin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class HairEditorTextures {
	public static final Identifier MENU_BIG = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	public static final Identifier MENU_SMALL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menusmall.png");
	public static final Identifier BUTTONS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	public static final PanelSkin PANEL = PanelSkin.PANEL;
	public static final PanelSkin SMALL_PANEL = PanelSkin.SMALL_PANEL;
	public static final PanelSkin BUTTON = PanelSkin.BUTTON;
	public static final PanelSkin BUTTON_HOVER = PanelSkin.BUTTON_HOVER;
	public static final PanelSkin BUTTON_ACTIVE = PanelSkin.BUTTON_ACTIVE;
	public static final PanelSkin BUTTON_ACCENT = PanelSkin.BUTTON_ACCENT;
	public static final PanelSkin FIELD = PanelSkin.FIELD;
	public static final PanelSkin FIELD_EDITING = PanelSkin.FIELD_EDITING;

	public static final Sprite TITLE_PLATE = new Sprite(MENU_BIG, 142, 22, 107, 21);
	public static final Sprite TITLE_PLATE_SHORT = new Sprite(MENU_BIG, 142, 0, 79, 21);
	public static final Sprite IMPORT = new Sprite(BUTTONS, 162, 0, 20, 20);
	public static final Sprite IMPORT_HOVER = new Sprite(BUTTONS, 162, 20, 20, 20);
	public static final Sprite EXPORT = new Sprite(BUTTONS, 182, 0, 20, 20);
	public static final Sprite EXPORT_HOVER = new Sprite(BUTTONS, 182, 20, 20, 20);

	public record Sprite(Identifier texture, int u, int v, int width, int height) {}

	private HairEditorTextures() {}

	public static void draw(GuiGraphicsExtractor graphics, PanelSkin skin, int x, int y, int width, int height) {
		skin.draw(graphics, x, y, width, height);
	}

	public static void draw(GuiGraphicsExtractor graphics, PanelSkin skin, int x, int y, int width, int height, float red, float green, float blue, float alpha) {
		skin.draw(graphics, x, y, width, height, red, green, blue, alpha);
	}

	public static void draw(GuiGraphicsExtractor graphics, Sprite sprite, int x, int y) {
		HudRender.blit(graphics, sprite.texture(), x, y, sprite.u(), sprite.v(), sprite.width(), sprite.height(), 256, 256);
	}
}
