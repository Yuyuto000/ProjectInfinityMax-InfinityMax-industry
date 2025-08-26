package com.infinitymax.industry.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class MachineRecipeTypes {
    public static final RecipeType<MachineRecipe> MACHINE = RecipeType.register("infinitymax:machine_recipe");
    public static final RecipeSerializer<MachineRecipe> SERIALIZER = MachineRecipe.Serializer.INSTANCE;
}