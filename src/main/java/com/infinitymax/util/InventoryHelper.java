package com.infinitymax.industry.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 小型インベントリヘルパー (非常に単純)
 * - スロットは ItemStack[] 管理（null -> ItemStack.EMPTY推奨）
 */
public final class InventoryHelper {
    private InventoryHelper() {}

    public static boolean canInsert(ItemStack slot, ItemStack toInsert, int maxStackSize) {
        if (toInsert == null || toInsert.isEmpty()) return false;
        if (slot == null || slot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(slot, toInsert)) return false;
        return slot.getCount() + toInsert.getCount() <= Math.min(maxStackSize, slot.getMaxStackSize());
    }

    public static int insert(ItemStack[] inv, int slotIndex, ItemStack toInsert, int maxStackSize) {
        if (toInsert == null || toInsert.isEmpty()) return 0;
        ItemStack slot = inv[slotIndex];
        if (slot == null || slot.isEmpty()) {
            int put = Math.min(toInsert.getCount(), Math.min(maxStackSize, toInsert.getMaxStackSize()));
            inv[slotIndex] = toInsert.copy();
            inv[slotIndex].setCount(put);
            return put;
        } else if (ItemStack.isSameItemSameTags(slot, toInsert)) {
            int can = Math.min(toInsert.getCount(), Math.min(maxStackSize, slot.getMaxStackSize()) - slot.getCount());
            slot.grow(can);
            return can;
        }
        return 0;
    }

    public static ItemStack extract(ItemStack[] inv, int slotIndex, int amount) {
        ItemStack slot = inv[slotIndex];
        if (slot == null || slot.isEmpty()) return ItemStack.EMPTY;
        int take = Math.min(amount, slot.getCount());
        ItemStack res = slot.copy();
        res.setCount(take);
        if (take >= slot.getCount()) inv[slotIndex] = ItemStack.EMPTY;
        else slot.shrink(take);
        return res;
    }
}