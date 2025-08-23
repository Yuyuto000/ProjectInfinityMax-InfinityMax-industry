package com.infinitymax.industry.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;

public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private static final ResourceLocation BG = new ResourceLocation("infinitymax", "textures/gui/generator_bg.png");

    public GeneratorScreen(GeneratorMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176; this.imageHeight = 166;
    }

    @Override
    protected void renderBg(PoseStack ps, float pt, int mx, int my) {
        this.getMinecraft().getTextureManager().bind(BG);
        blit(ps, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // energy bar: data[0]=curJ, data[1]=capJ
        int cur = menu.data.get(0);
        int cap = menu.data.get(1);
        int h = cap==0?0:(int)((double)cur/cap*50);
        blit(ps, leftPos+150, topPos+16+(50-h), 176, 0+(50-h), 6, h);
    }

    @Override
    public void render(PoseStack ps, int mx, int my, float pt) {
        this.renderBackground(ps);
        super.render(ps, mx, my, pt);
        this.renderTooltip(ps, mx, my);
    }

    @Override
    protected void renderLabels(PoseStack ps, int mx, int my) {
        Font f = this.font;
        f.draw(ps, title, 8F, 6F, 4210752);
    }
}