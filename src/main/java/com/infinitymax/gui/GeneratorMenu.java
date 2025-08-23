package com.infinitymax.industry.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 発電機用の Menu（3スロット + データ4）
 */
public class GeneratorMenu extends AbstractContainerMenu {
    public final Container inv;
    public final ContainerData data;
    public final BlockPos pos;

    public GeneratorMenu(int id, Inventory playerInv, Container inv, ContainerData data, BlockPos pos) {
        super(MenuTypeBuilder.GENERATOR_MENU.get(), id);
        this.inv = inv;
        this.data = data;
        this.pos = pos;

        // slots: fuel (0), maybe input (1), output(2)
        this.addSlot(new Slot(inv, 0, 56, 36));
        this.addSlot(new Slot(inv, 1, 80, 36));
        this.addSlot(new Slot(inv, 2, 116, 36));

        // player inv
        for (int row=0; row<3; row++)
            for (int col=0; col<9; col++)
                this.addSlot(new Slot(playerInv, col + row*9 + 9, 8 + col*18, 84 + row*18));
        for (int col=0; col<9; col++) this.addSlot(new Slot(playerInv, col, 8 + col*18, 142));

        addDataSlots(data);
    }

    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) { return net.minecraft.world.item.ItemStack.EMPTY; }
}