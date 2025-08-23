package com.infinitymax.industry.recipe;

import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;

import java.util.List;
import java.util.Optional;

/**
 * Level の RecipeManager から MachineRecipe を取り出し、指定の機械種・インベントリで一致するレシピを返す。
 * - データパック変更は RecipeManager が自動で反映する（ワールド再ロード不要）
 */
public final class RecipeHelper {
    private RecipeHelper() {}

    public static MachineRecipe findMatching(Level level, MachineBlock.Kind kind, ItemStack a, ItemStack b) {
        if (level == null || kind == null) return null;
        List<MachineRecipe> list = level.getRecipeManager().getAllRecipesFor(MachineRecipeTypes.MACHINE);
        for (MachineRecipe r : list) {
            if (r.machineKind != kind) continue;
            if (r.matchesStacks(a, b) || r.matchesStacks(b, a)) return r;
        }
        return null;
    }
}