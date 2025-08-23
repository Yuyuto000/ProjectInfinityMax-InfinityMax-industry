package com.infinitymax.industry.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Objects;

/**
 * 簡易 FluidStack 実装（NBT / FriendlyByteBuf 入出力をサポート）
 * - Fluid は Registry から lookup する想定
 */
public class FluidStack {
    public final Fluid fluid;
    public int amount;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid == null ? Fluids.EMPTY : fluid;
        this.amount = Math.max(0, amount);
    }

    public static FluidStack of(ResourceLocation id, int amount) {
        Fluid f = Registry.FLUID.get(id);
        if (f == null) f = Fluids.EMPTY;
        return new FluidStack(f, amount);
    }

    public CompoundTag writeToNbt() {
        CompoundTag tag = new CompoundTag();
        ResourceLocation id = Registry.FLUID.getKey(fluid);
        tag.putString("fluid", id == null ? "minecraft:empty" : id.toString());
        tag.putInt("amount", amount);
        return tag;
    }

    public static FluidStack readFromNbt(CompoundTag tag) {
        try {
            ResourceLocation id = new ResourceLocation(tag.getString("fluid"));
            Fluid f = Registry.FLUID.get(id);
            int a = tag.getInt("amount");
            return new FluidStack(f, a);
        } catch (Throwable t) {
            return new FluidStack(Fluids.EMPTY, 0);
        }
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        ResourceLocation id = Registry.FLUID.getKey(fluid);
        buf.writeUtf(id == null ? "minecraft:empty" : id.toString());
        buf.writeInt(amount);
    }

    public static FluidStack readFromBuffer(FriendlyByteBuf buf) {
        String idstr = buf.readUtf();
        ResourceLocation id = new ResourceLocation(idstr);
        Fluid f = Registry.FLUID.get(id);
        int a = buf.readInt();
        return new FluidStack(f, a);
    }

    public boolean isEmpty() { return fluid == null || fluid == Fluids.EMPTY || amount <= 0; }

    @Override
    public String toString() {
        ResourceLocation id = Registry.FLUID.getKey(fluid);
        return (id == null ? "empty" : id.toString()) + ":" + amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FluidStack)) return false;
        FluidStack other = (FluidStack) o;
        return Objects.equals(Registry.FLUID.getKey(fluid), Registry.FLUID.getKey(other.fluid)) && amount == other.amount;
    }
}