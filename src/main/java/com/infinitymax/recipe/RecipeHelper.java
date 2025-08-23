package com.infinitymax.industry.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * RecipeManager を検索して MachineRecipe を見つけるヘルパ
 */
public final class RecipeHelper {
    private RecipeHelper() {}

    public static MachineRecipe findMatching(Level level, String machineId, ItemStack a, ItemStack b) {
        if (level == null || machineId == null) return null;
        List<MachineRecipe> all = level.getRecipeManager().getAllRecipesFor(MachineRecipe.Type.INSTANCE);
        machineId = machineId.toLowerCase();
        for (MachineRecipe r : all) {
            if (!machineId.equals(r.getMachine())) continue;
            // items matching: require each required input present (order insensitive)
            if (matchesStacks(r.getInputs(), a, b)) return r;
        }
        return null;
    }

    private static boolean matchesStacks(java.util.List<ItemStack> need, ItemStack a, ItemStack b) {
        if (need == null || need.isEmpty()) return true;
        // simple support: up to 2 inputs
        if (need.size() == 1) {
            return itemMatches(need.get(0), a) || itemMatches(need.get(0), b);
        } else if (need.size() == 2) {
            boolean m1 = itemMatches(need.get(0), a) && itemMatches(need.get(1), b);
            boolean m2 = itemMatches(need.get(0), b) && itemMatches(need.get(1), a);
            return m1 || m2;
        }
        // fallback: all must be present in either slot (naive)
        for (ItemStack needSt : need) {
            if (!(itemMatches(needSt, a) || itemMatches(needSt, b))) return false;
        }
        return true;
    }

    private static boolean itemMatches(ItemStack need, ItemStack have) {
        if (need == null || need.isEmpty()) return false;
        if (have == null || have.isEmpty()) return false;
        return net.minecraft.world.item.ItemStack.isSameItemSameTags(need, have) && have.getCount() >= need.getCount();
    }
}