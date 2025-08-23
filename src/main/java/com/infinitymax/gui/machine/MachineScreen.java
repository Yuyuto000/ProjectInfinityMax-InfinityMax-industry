package com.infinitymax.industry.gui.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.Font;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final ResourceLocation BG = new ResourceLocation("infinitymax_industry", "textures/gui/machine_bg.png");

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        this.getMinecraft().getTextureManager().bind(BG);
        blit(pPoseStack, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        // draw progress bar reading data[0] (progress) and data[1] (progressMax)
        int prog = menu.data.get(0);
        int progMax = menu.data.get(1);
        int px = progMax == 0 ? 0 : (int) ((double) prog / progMax * 24.0);
        blit(pPoseStack, leftPos + 79, topPos + 34, 176, 0, px, 16, 256, 256);

        // energy bar (data[2] currentJ, data[3] capacity)
        int cur = menu.data.get(2), cap = menu.data.get(3);
        int ey = cap == 0 ? 0 : (int) ((double) cur / cap * 50.0);
        blit(pPoseStack, leftPos + 152, topPos + 16 + (50 - ey), 176, 16 + (50 - ey), 6, ey, 256, 256);
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ps);
        super.render(ps, mouseX, mouseY, partialTicks);
        this.renderTooltip(ps, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        Font font = this.font;
        font.draw(pPoseStack, title, 8.0F, 6.0F, 4210752);
    }
}