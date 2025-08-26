package com.infinitymax.industry.gui.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.Font;

/**
 * Machine の GUI スクリーン
 * - プログレスバー
 * - エネルギーバー
 * - 流体タンクバー (2 本)
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final ResourceLocation BG = new ResourceLocation("infinitymax_industry", "textures/gui/machine_bg.png");

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(PoseStack ps, float partialTicks, int mouseX, int mouseY) {
        this.getMinecraft().getTextureManager().bind(BG);
        blit(ps, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        // プログレスバー
        int prog = menu.data.get(0);
        int progMax = menu.data.get(1);
        int px = progMax == 0 ? 0 : (int) ((double) prog / progMax * 24.0);
        blit(ps, leftPos + 79, topPos + 34, 176, 0, px, 16, 256, 256);

        // エネルギーバー
        int cur = menu.data.get(2), cap = menu.data.get(3);
        int ey = cap == 0 ? 0 : (int) ((double) cur / cap * 50.0);
        blit(ps, leftPos + 152, topPos + 16 + (50 - ey), 176, 16 + (50 - ey), 6, ey, 256, 256);

        // 流体タンク 0
        int f0 = menu.data.get(4);
        int fy0 = (int)((double)f0 / 16000.0 * 50.0);
        blit(ps, leftPos + 162, topPos + 16 + (50 - fy0), 182, 16 + (50 - fy0), 6, fy0, 256, 256);

        // 流体タンク 1
        int f1 = menu.data.get(5);
        int fy1 = (int)((double)f1 / 16000.0 * 50.0);
        blit(ps, leftPos + 170, topPos + 16 + (50 - fy1), 188, 16 + (50 - fy1), 6, fy1, 256, 256);
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ps);
        super.render(ps, mouseX, mouseY, partialTicks);
        this.renderTooltip(ps, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack ps, int mouseX, int mouseY) {
        Font font = this.font;
        font.draw(ps, title, 8.0F, 6.0F, 4210752);
    }
}