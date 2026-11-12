package com.dragonminez.client.gui.hair;

import com.dragonminez.common.hair.HairLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import java.util.List;

/** Local library, reusable when editing any permitted character or form. */
final class HairLibraryScreen extends ScaledScreen {
    private final HairEditorScreen parent;
    private final HairEditorState state;
    private final HairLibrary library = new HairLibrary(FMLPaths.CONFIGDIR.get().resolve("dragonminez/hair_presets"));
    private List<HairLibrary.Entry> entries = List.of();
    private EditBox name;
    private int page;
    private HairLibrary.Entry deleting;
    private Component status = Component.empty();

    HairLibraryScreen(HairEditorScreen parent, HairEditorState state) {
        super(text("library")); this.parent = parent; this.state = state;
    }

    private static Component text(String key) { return HairEditorUi.tr("gui.dragonminez.hair_editor." + key); }

    @Override protected int getMinGuiHeight() { return 252; }

    protected void init() {
        super.init();
        clearWidgets();
        try { entries = library.list(); } catch (Exception ex) { failure(ex); }
        page = Math.min(page, Math.max(0, (entries.size() - 1) / 6));
        int x = getUiWidth() / 2 - 145;
        if (deleting != null) {
            addRenderableWidget(HairEditorUi.button(x + 15, 113, 125, 20, HairEditorUi.tr("gui.yes"), button -> {
                try { library.delete(deleting.id()); status = Component.empty(); } catch (Exception ex) { failure(ex); }
                deleting = null; init();
            }));
            addRenderableWidget(HairEditorUi.button(x + 150, 113, 125, 20, HairEditorUi.tr("gui.cancel"), button -> { deleting = null; init(); }));
            return;
        }
        name = new EditBox(font, x + 5, 42, 190, 12, text("preset_name"));
        name.setBordered(false); name.setTextColor(HairEditorUi.TEXT);
        name.setMaxLength(64); name.setHint(text("preset_name"));
        addRenderableWidget(name);
        addRenderableWidget(HairEditorUi.button(x + 204, 38, 86, 20, text("save_preset"), button -> {
            try {
                library.save(name.getValue(), state.styles(), state.isHairBase());
                status = text("preset_saved"); init();
            } catch (Exception ex) { failure(ex); }
        }));
        for (int row = 0; row < 6; row++) {
            int index = page * 6 + row;
            if (index >= entries.size()) break;
            var entry = entries.get(index);
            int y = 68 + row * 24;
            addRenderableWidget(HairEditorUi.button(x, y, 242, 20, HairEditorUi.txt(HairEditorUi.trimToWidth(font, entry.name(), 230)), button -> {
                try {
                    state.loadPreset(library.styles(entry.id()), library.read(entry.id()).getBoolean("HairBase"));
                    minecraft.setScreen(parent);
                } catch (Exception ex) { failure(ex); }
            }));
            addRenderableWidget(HairEditorUi.button(x + 246, y, 44, 20, text("delete_preset"), button -> {
                deleting = entry; init();
            }));
        }
        int bottom = Math.min(getUiHeight() - 30, 218);
        var prev = HairEditorUi.button(x, bottom, 40, 20, HairEditorUi.txt("<"), button -> { page--; init(); });
        prev.active = page > 0; addRenderableWidget(prev);
        var next = HairEditorUi.button(x + 44, bottom, 40, 20, HairEditorUi.txt(">"), button -> { page++; init(); });
        next.active = (page + 1) * 6 < entries.size(); addRenderableWidget(next);
        addRenderableWidget(HairEditorUi.button(x + 190, bottom, 100, 20, HairEditorUi.tr("gui.back"), button -> onClose()));
    }

    private void failure(Exception error) {
        status = text("preset_error");
        com.dragonminez.LogUtil.warn(com.dragonminez.Env.CLIENT, "Hair preset operation failed: {}", error.getMessage());
    }

    public void onClose() {
        if (deleting != null) { deleting = null; init(); }
        else minecraft.setScreen(parent);
    }
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0101810);
        beginUiScale(graphics);
        int x = getUiWidth() / 2 - 153;
        HairEditorUi.panel(graphics, x, 6, 306, 240);
        HairEditorUi.header(graphics, font, deleting == null ? title : text("delete_preset"), x, 6, 306);
        if (deleting == null) HairEditorUi.field(graphics, x + 8, 38, 200, 20, name.isFocused());
        else graphics.drawCenteredString(font, HairEditorUi.txt(HairEditorUi.trimToWidth(font, deleting.name(), 270)), getUiWidth() / 2, 81, HairEditorUi.TEXT);
        super.render(graphics, (int)toUiX(mouseX), (int)toUiY(mouseY), partialTick);
        graphics.drawCenteredString(font, status, getUiWidth() / 2, 27, HairEditorUi.TITLE);
        endUiScale(graphics);
    }
}
