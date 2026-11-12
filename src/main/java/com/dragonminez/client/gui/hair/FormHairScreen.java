package com.dragonminez.client.gui.hair;

import com.dragonminez.api.forms.FormHair;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.screens.Screen;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Data-driven form entries; the existing editor still owns all strand editing tools. */
public final class FormHairScreen extends ScaledScreen {
    private final Screen previous;
    private final Character character;
    private final List<String> forms;
    private int page;
    public static Screen open(Screen previous,Character character){return FormHair.editable(character.getRaceName()).isEmpty()?new HairEditorScreen(previous,character):new FormHairScreen(previous,character);}
    private FormHairScreen(Screen previous,Character character){super(Component.translatable("gui.dragonminez.form_hair.title"));this.previous=previous;this.character=character;forms=new ArrayList<>(FormHair.editable(character.getRaceName()).keySet());}
    @Override protected void init() {
        super.init();
        int x = getUiWidth() / 2 - 110;
        addRenderableWidget(HairEditorUi.button(x, 48, 220, 20, HairEditorUi.tr("gui.dragonminez.form_hair.base"),
                button -> minecraft.setScreen(new HairEditorScreen(this, character))));
        int count = Math.max(1, (getUiHeight() - 130) / 24);
        int pages = Math.max(1, (forms.size() + count - 1) / count);
        page = Math.min(page, pages - 1);
        for (int i = page * count; i < Math.min(forms.size(), (page + 1) * count); i++) {
            String id = forms.get(i);
            var label = FormHair.name(character.getRaceName(), id);
            var button = HairEditorUi.button(x, 78 + (i - page * count) * 24, 220, 20,
                    HairEditorUi.txt(HairEditorUi.trimToWidth(font, label.getString(), 204)),
                    pressed -> minecraft.setScreen(new HairEditorScreen(this, character, id)));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
            addRenderableWidget(button);
        }
        int bottom = getUiHeight() - 36;
        var previous = HairEditorUi.button(x, bottom, 35, 20, HairEditorUi.txt("<"), button -> { page--; rebuildWidgets(); });
        previous.active = page > 0;
        addRenderableWidget(previous);
        addRenderableWidget(HairEditorUi.button(x + 40, bottom, 140, 20, HairEditorUi.tr("gui.back"), button -> onClose()));
        var next = HairEditorUi.button(x + 185, bottom, 35, 20, HairEditorUi.txt(">"), button -> { page++; rebuildWidgets(); });
        next.active = page + 1 < pages;
        addRenderableWidget(next);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0101810);
        beginUiScale(graphics);
        int x = getUiWidth() / 2 - 122;
        HairEditorUi.panel(graphics, x, 8, 244, getUiHeight() - 16);
        HairEditorUi.header(graphics, font, title.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(HairEditorUi.DMZ_FONT)), x, 8, 244);
        super.render(graphics, (int)toUiX(mouseX), (int)toUiY(mouseY), partialTick);
        endUiScale(graphics);
    }
    @Override public void onClose() { minecraft.setScreen(previous); }
}
