package com.infinitymax.industry.recipe;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * MachineRecipe の検索ヘルパ
 * - アイテムだけでなく FluidStack もマッチ可能
 * - machineId ⇔ MachineBlock.Kind を統一
 */
public final class RecipeHelper {
    private RecipeHelper() {}

    private static final Map<String, MachineBlock.Kind> ID_TO_KIND = new HashMap<>();
    static {
        // ここで JSON の "machine" を Kind にマッピング
        ID_TO_KIND.put("crusher", MachineBlock.Kind.CRUSHER);
        ID_TO_KIND.put("electrolyzer", MachineBlock.Kind.ELECTROLYZER);
        ID_TO_KIND.put("mixer", MachineBlock.Kind.MIXER);
        ID_TO_KIND.put("furnace", MachineBlock.Kind.FURNACE);
        // 必要に応じて追加
    }

    public static MachineBlock.Kind getKindFromId(String machineId) {
        if (machineId == null) return null;
        return ID_TO_KIND.get(machineId.toLowerCase());
    }

    /**
     * レシピ検索
     * - items: a, b
     * - fluids: list of FluidStack in tanks or input slots
     */
    public static MachineRecipe findMatching(Level level, String machineId,
                                             ItemStack a, ItemStack b,
                                             List<FluidStack> fluidInputs) {
        if (level == null || machineId == null) return null;
        MachineBlock.Kind kind = getKindFromId(machineId);
        if (kind == null) return null;

        List<MachineRecipe> all = level.getRecipeManager().getAllRecipesFor(MachineRecipe.Type.INSTANCE);
        for (MachineRecipe r : all) {
            if (!kind.equals(getKindFromId(r.getMachine()))) continue;
            if (!matchesItems(r.getInputs(), a, b)) continue;
            if (!matchesFluids(r.getFluidInputs(), fluidInputs)) continue;
            return r;
        }
        return null;
    }

    // ------------------- ヘルパ -------------------
    private static boolean matchesItems(List<ItemStack> need, ItemStack a, ItemStack b) {
        if (need == null || need.isEmpty()) return true;
        if (need.size() == 1) return itemMatches(need.get(0), a) || itemMatches(need.get(0), b);
        if (need.size() == 2) {
            boolean m1 = itemMatches(need.get(0), a) && itemMatches(need.get(1), b);
            boolean m2 = itemMatches(need.get(0), b) && itemMatches(need.get(1), a);
            return m1 || m2;
        }
        for (ItemStack n : need) {
            if (!(itemMatches(n, a) || itemMatches(n, b))) return false;
        }
        return true;
    }

    private static boolean itemMatches(ItemStack need, ItemStack have) {
        if (need == null || need.isEmpty()) return false;
        if (have == null || have.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(need, have) && have.getCount() >= need.getCount();
    }

    private static boolean matchesFluids(List<FluidStack> need, List<FluidStack> have) {
        if (need == null || need.isEmpty()) return true;
        if (have == null || have.isEmpty()) return false;

        for (FluidStack n : need) {
            boolean found = false;
            for (FluidStack h : have) {
                if (fluidMatches(n, h)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean fluidMatches(FluidStack need, FluidStack have) {
        if (need == null || have == null) return false;
        return need.getFluid().equals(have.getFluid()) && have.getAmount() >= need.getAmount();
    }
}