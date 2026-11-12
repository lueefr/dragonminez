package com.dragonminez.client.gui.hair;

import com.dragonminez.common.hair.HairLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import java.util.List;

/** Local library, reusable when editing any permitted character or form. */
final class HairLibraryScreen extends Screen {
    private final HairEditorScreen parent;
    private final HairEditorState state;
    private final HairLibrary library = new HairLibrary(FMLPaths.CONFIGDIR.get().resolve("dragonminez/hair_presets"));
    private List<HairLibrary.Entry> entries = List.of();
    private EditBox name;
    private int page;
    private Component status = Component.empty();

    HairLibraryScreen(HairEditorScreen parent, HairEditorState state) {
        super(text("library")); this.parent = parent; this.state = state;
    }

    private static Component text(String key) { return Component.translatable("gui.dragonminez.hair_editor." + key); }

    protected void init() {
        clearWidgets();
        try { entries = library.list(); } catch (Exception ex) { failure(ex); }
        page = Math.min(page, Math.max(0, (entries.size() - 1) / 6));
        int x = width / 2 - 145;
        name = new EditBox(font, x, 38, 200, 20, text("preset_name"));
        name.setMaxLength(64); name.setHint(text("preset_name"));
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(text("save_preset"), button -> {
            try {
                library.save(name.getValue(), state.styles(), state.isHairBase());
                status = text("preset_saved"); init();
            } catch (Exception ex) { failure(ex); }
        }).bounds(x + 204, 38, 86, 20).build());
        for (int row = 0; row < 6; row++) {
            int index = page * 6 + row;
            if (index >= entries.size()) break;
            var entry = entries.get(index);
            int y = 68 + row * 24;
            addRenderableWidget(Button.builder(HairEditorUi.txt(entry.name()), button -> {
                try {
                    state.loadPreset(library.styles(entry.id()), library.read(entry.id()).getBoolean("HairBase"));
                    minecraft.setScreen(parent);
                } catch (Exception ex) { failure(ex); }
            }).bounds(x, y, 242, 20).build());
            addRenderableWidget(Button.builder(text("delete_preset"), button -> {
                minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed -> {
                    if (confirmed) try { library.delete(entry.id()); } catch (Exception ex) { failure(ex); }
                    minecraft.setScreen(this);
                }, text("delete_preset"), HairEditorUi.txt(entry.name())));
            }).bounds(x + 246, y, 44, 20).build());
        }
        int bottom = Math.min(height - 30, 218);
        var prev = Button.builder(HairEditorUi.txt("<"), button -> { page--; init(); }).bounds(x, bottom, 40, 20).build();
        prev.active = page > 0; addRenderableWidget(prev);
        var next = Button.builder(HairEditorUi.txt(">"), button -> { page++; init(); }).bounds(x + 44, bottom, 40, 20).build();
        next.active = (page + 1) * 6 < entries.size(); addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(x + 190, bottom, 100, 20).build());
    }

    private void failure(Exception error) {
        status = text("preset_error");
        com.dragonminez.LogUtil.warn(com.dragonminez.Env.CLIENT, "Hair preset operation failed: {}", error.getMessage());
    }

    public void onClose() { minecraft.setScreen(parent); }
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(font, status, width / 2, 27, 0xFFFF77);
    }
}
