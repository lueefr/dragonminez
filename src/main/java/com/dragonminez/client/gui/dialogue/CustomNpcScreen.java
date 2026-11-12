package com.dragonminez.client.gui.dialogue;

import com.dragonminez.api.npc.CustomNpcRegistry;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.quest.QuestRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.dragonminez.client.gui.hair.HairEditorUi;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class CustomNpcScreen extends ScaledScreen {
    private record Offer(Component label, Runnable action) {}
    private final CustomNpcEntity npc;
    private final List<Offer> offers=new ArrayList<>();
    private int page;
    private int pageSize;
    private CustomNpcScreen(CustomNpcEntity npc) { super(Component.translatable(npc.definition().name()));this.npc=npc; }
    public static void open(int entityId) {
        var mc=Minecraft.getInstance();
        if (mc.level!=null && mc.level.getEntity(entityId) instanceof CustomNpcEntity npc && npc.definition()!=null) mc.setScreen(new CustomNpcScreen(npc));
    }
    @Override protected void init() {
        super.init();
        offers.clear();
        var definition=npc.definition();
        if (definition==null) return;
        for (String skill:definition.offers().skills()) offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.skill",Component.translatable("skill.dragonminez."+skill)),
                () -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.PURCHASE,skill,0))));
        for (String id:definition.offers().quests()) {
            var quest=QuestRegistry.getClientQuest(id);
            Component name=quest==null?com.dragonminez.api.lang.ConfigTranslations.text(id):Component.translatable(quest.getTitle());
            offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.quest_start",name),
                    () -> NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START,id,definition.id()))));
            offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.quest_turn_in",name),
                    () -> NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.TURN_IN,id,definition.id()))));
        }
        for (int i=0;i<definition.offers().shop().size();i++) {
            int index=i;var trade=definition.offers().shop().get(i);
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(trade.item()));
            offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.trade",item.getDescription(),trade.count(),trade.price()),
                    () -> NetworkHandler.sendToServer(new CustomNpcShopC2S(npc.getId(),index))));
        }
        pageSize = Math.max(1, (getUiHeight() - 158) / 24);
        int pages = Math.max(1, (offers.size() + pageSize - 1) / pageSize);
        page = Math.min(page, pages - 1);
        int x = getUiWidth() / 2 - 145;
        for (int i = page * pageSize; i < Math.min(offers.size(), (page + 1) * pageSize); i++) {
            Offer offer = offers.get(i);
            var button = HairEditorUi.button(x, 110 + (i % pageSize) * 24, 290, 20,
                    HairEditorUi.txt(HairEditorUi.trimToWidth(font, offer.label().getString(), 274)), pressed -> offer.action().run());
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(offer.label()));
            addRenderableWidget(button);
        }
        int bottom = getUiHeight() - 36;
        var previous = HairEditorUi.button(x, bottom, 35, 20, HairEditorUi.txt("<"), button -> { page--; rebuildWidgets(); });
        previous.active = page > 0;
        addRenderableWidget(previous);
        addRenderableWidget(HairEditorUi.button(x + 40, bottom, 210, 20, HairEditorUi.tr("gui.done"), button -> onClose()));
        var next = HairEditorUi.button(x + 255, bottom, 35, 20, HairEditorUi.txt(">"), button -> { page++; rebuildWidgets(); });
        next.active = page + 1 < pages;
        addRenderableWidget(next);
    }

    @Override public void tick() {
        if (minecraft.player==null || !npc.isAlive() || minecraft.player.distanceToSqr(npc)>100) onClose();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0101810);
        beginUiScale(graphics);
        int x = getUiWidth() / 2 - 153;
        HairEditorUi.panel(graphics, x, 8, 306, getUiHeight() - 16);
        HairEditorUi.header(graphics, font, HairEditorUi.txt(HairEditorUi.trimToWidth(font, title.getString(), 270)), x, 8, 306);
        var definition = npc.definition();
        if (definition != null) {
            graphics.enableScissor(x + 12, 42, x + 294, 102);
            int y = 42;
            for (String dialogue : definition.dialogues()) {
                for (var line : font.split(HairEditorUi.tr(dialogue), 278)) {
                    if (y >= 102) break;
                    graphics.drawString(font, line, x + 14, y, HairEditorUi.MUTED, false);
                    y += 10;
                }
                if (y >= 102) break;
            }
            graphics.disableScissor();
        }
        super.render(graphics, (int)toUiX(mouseX), (int)toUiY(mouseY), partialTick);
        endUiScale(graphics);
    }
    @Override public boolean isPauseScreen() { return false; }
}
