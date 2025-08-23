package com.infinitymax.industry.gui.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 汎用 Machine 用コンテナ(menu)
 * - 2 input slots (0,1) and 1 output (2)
 * - player inventory & hotbar appended
 */
public class MachineMenu extends AbstractContainerMenu {

    public final Container machineInv;
    public final ContainerData data; // progress, energy etc
    public final BlockPos pos;
    private final Level level;
    private final Player player;

    public MachineMenu(int id, Inventory playerInv, Container machineInv, ContainerData data, BlockPos pos) {
        super(MenuTypeBuilder.MACHINE_MENU.get(), id); // MenuTypeBuilder is user to register MenuType; if not present, use a static constant
        this.machineInv = machineInv;
        this.data = data;
        this.pos = pos;
        this.level = playerInv.player.level;
        this.player = playerInv.player;

        // machine slots
        this.addSlot(new Slot(machineInv, 0, 44, 20)); // input A
        this.addSlot(new Slot(machineInv, 1, 44, 50)); // input B
        this.addSlot(new Slot(machineInv, 2, 116, 35) { // output (disable insert)
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public ItemStack remove(int amount) { return super.remove(amount); }
        });

        // player inventory (3 rows)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        this.data = data;
        addDataSlots(this.data);
    }

    // utility to transfer stack on shift-click
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack copy = slot.getItem();
            result = copy.copy();
            final int machineInvSize = 3;
            if (index < machineInvSize) {
                // move from machine to player
                if (!this.moveItemStackTo(copy, machineInvSize, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // move from player to machine inputs
                if (!this.moveItemStackTo(copy, 0, 2, false)) return ItemStack.EMPTY;
            }

            if (copy.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // optionally check player distance to pos
    }
}