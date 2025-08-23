package com.infinitymax.industry.recipe;

import net.minecraft.world.item.ItemStack;

/** 2入力→1出力 + 稼働時間 + 消費エネルギー(J/t) の超簡易モデル */
public class MachineRecipe {
    public final ItemStack inputA, inputB, output;
    public final int ticks;
    public final double joulesPerTick;

    public MachineRecipe(ItemStack a, ItemStack b, ItemStack out, int ticks, double jpt) {
        this.inputA = a == null ? ItemStack.EMPTY : a.copy();
        this.inputB = b == null ? ItemStack.EMPTY : b.copy();
        this.output = out.copy();
        this.ticks = ticks;
        this.joulesPerTick = jpt;
    }
}