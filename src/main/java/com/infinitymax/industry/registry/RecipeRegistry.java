package com.infinitymax.industry.registry;

import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 簡易レシピ管理
 * - 入力アイテム1～2、出力アイテム1 をサポートする軽量レジストリ
 * - 値は数値の「処理時間(ticks)」「電力消費(J/tick)」を含む
 */
public final class RecipeRegistry {
    private RecipeRegistry() {}

    public static class Recipe {
        public final String id;
        public final ItemStack inputA;
        public final ItemStack inputB; // null可
        public final ItemStack output;
        public final int ticks;
        public final double joulesPerTick;

        public Recipe(String id, ItemStack inputA, ItemStack inputB, ItemStack output, int ticks, double joulesPerTick) {
            this.id = id;
            this.inputA = inputA.copy();
            this.inputB = (inputB == null ? null : inputB.copy());
            this.output = output.copy();
            this.ticks = ticks;
            this.joulesPerTick = joulesPerTick;
        }
    }

    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();

    public static void register(Recipe r) {
        RECIPES.put(r.id, r);
    }

    public static Recipe findMatching(ItemStack a, ItemStack b) {
        for (Recipe r : RECIPES.values()) {
            if (matches(r.inputA, a) && matches(r.inputB, b)) return r;
            if (matches(r.inputA, b) && matches(r.inputB, a)) return r; // orderless
        }
        return null;
    }

    private static boolean matches(ItemStack want, ItemStack have) {
        if (want == null) return have == null || have.isEmpty();
        if (have == null) return false;
        if (have.isEmpty()) return false;
        return ItemStack.isSame(want, have);
    }

    public static Collection<Recipe> getAll() { return Collections.unmodifiableCollection(RECIPES.values()); }
}