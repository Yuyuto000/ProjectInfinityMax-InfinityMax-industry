package com.infinitymax.industry.recipe;

import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 機械種別ごとのレシピをメモリ登録する簡易レジストリ。
 * - Json/DataPack化は後で拡張可（Serializer 追加）
 */
public final class RecipeRegistry {
    private RecipeRegistry(){}

    private static final Map<MachineBlock.Kind, List<MachineRecipe>> RECIPES = new EnumMap<>(MachineBlock.Kind.class);

    public static void register(MachineBlock.Kind kind, MachineRecipe recipe) {
        RECIPES.computeIfAbsent(kind, k -> new ArrayList<>()).add(recipe);
    }

    /** 入力Stackから一致するレシピを検索（数・アイテム一致） */
    public static MachineRecipe match(MachineBlock.Kind kind, ItemStack a, ItemStack b) {
        List<MachineRecipe> list = RECIPES.get(kind);
        if (list == null) return null;
        for (MachineRecipe r : list) {
            if (fits(a, r.inputA) && fits(b, r.inputB)) return r;
            if (fits(b, r.inputA) && fits(a, r.inputB)) return r; // 反転も許可
        }
        return null;
    }

    private static boolean fits(ItemStack have, ItemStack need) {
        if (need.isEmpty()) return true;
        if (have.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(have, need) && have.getCount() >= need.getCount();
    }
}