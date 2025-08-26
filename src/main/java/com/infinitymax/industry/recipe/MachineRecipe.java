package com.infinitymax.industry.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.infinitymax.industry.fluid.FluidStack;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * データ駆動のマシンレシピ:
 * - "machine" (小文字推奨): 機械識別子（例 "crusher", "electrolyzer"）
 * - "inputs": [{item, count}, ...]
 * - "outputs": [{item, count}, ...]
 * - "ticks": int
 * - "joules_per_tick": double
 * - "fluid_input": [{fluid, amount}, ...]
 * - "fluid_output": [{fluid, amount}, ...]
 */
public class MachineRecipe implements Recipe<SimpleContainer> {

    public static final ResourceLocation SERIALIZER_ID = new ResourceLocation("infinitymax", "machine_recipe");

    private final ResourceLocation id;
    private final String machine; // 小文字 or Enum 文字列
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;
    private final int ticks;
    private final double joulesPerTick;
    private final List<FluidStack> fluidInputs;
    private final List<FluidStack> fluidOutputs;

    public MachineRecipe(ResourceLocation id, String machine,
                         List<ItemStack> inputs, List<ItemStack> outputs,
                         int ticks, double joulesPerTick,
                         List<FluidStack> fluidInputs, List<FluidStack> fluidOutputs) {
        this.id = id;
        this.machine = machine.toLowerCase();
        this.inputs = inputs == null ? Collections.emptyList() : inputs;
        this.outputs = outputs == null ? Collections.emptyList() : outputs;
        this.ticks = Math.max(1, ticks);
        this.joulesPerTick = Math.max(0.0, joulesPerTick);
        this.fluidInputs = fluidInputs == null ? Collections.emptyList() : fluidInputs;
        this.fluidOutputs = fluidOutputs == null ? Collections.emptyList() : fluidOutputs;
    }

    public String getMachine() { return machine; }
    public List<ItemStack> getInputs() { return inputs; }
    public List<ItemStack> getOutputs() { return outputs; }
    public int getTicks() { return ticks; }
    public double getJoulesPerTick() { return joulesPerTick; }
    public List<FluidStack> getFluidInputs() { return fluidInputs; }
    public List<FluidStack> getFluidOutputs() { return fluidOutputs; }

    // Recipe interface (we won't use matches from RecipeManager — we use helper)
    @Override public boolean matches(SimpleContainer inv, Level level) { return false; }
    @Override public ItemStack assemble(SimpleContainer inv) { return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem() { return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    // ----- RecipeType holder -----
    public static final class Type implements RecipeType<MachineRecipe> {
        private Type() {}
        public static final Type INSTANCE = new Type();
    }

    // ----- Serializer -----
    public static final class Serializer implements RecipeSerializer<MachineRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
            String machine = json.has("machine") ? json.get("machine").getAsString().toLowerCase() : "crusher";

            // items inputs
            List<ItemStack> inputs = new ArrayList<>();
            if (json.has("inputs")) {
                JsonArray a = json.getAsJsonArray("inputs");
                for (JsonElement e : a) {
                    JsonObject o = e.getAsJsonObject();
                    ResourceLocation itemId = new ResourceLocation(o.get("item").getAsString());
                    int count = o.has("count") ? o.get("count").getAsInt() : 1;
                    inputs.add(new ItemStack(Registry.ITEM.get(itemId), Math.max(1, count)));
                }
            }

            // outputs
            List<ItemStack> outputs = new ArrayList<>();
            if (json.has("outputs")) {
                JsonArray a = json.getAsJsonArray("outputs");
                for (JsonElement e : a) {
                    JsonObject o = e.getAsJsonObject();
                    ResourceLocation itemId = new ResourceLocation(o.get("item").getAsString());
                    int count = o.has("count") ? o.get("count").getAsInt() : 1;
                    outputs.add(new ItemStack(Registry.ITEM.get(itemId), Math.max(1, count)));
                }
            } else if (json.has("output")) { // 互換性のため単数出力も許す
                JsonObject o = json.getAsJsonObject("output");
                ResourceLocation itemId = new ResourceLocation(o.get("item").getAsString());
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                outputs.add(new ItemStack(Registry.ITEM.get(itemId), Math.max(1, count)));
            }

            int ticks = json.has("ticks") ? json.get("ticks").getAsInt() : 200;
            double jpt = json.has("joules_per_tick") ? json.get("joules_per_tick").getAsDouble() : 100.0;

            List<FluidStack> fin = new ArrayList<>();
            if (json.has("fluid_input")) {
                JsonArray a = json.getAsJsonArray("fluid_input");
                for (JsonElement e : a) {
                    JsonObject o = e.getAsJsonObject();
                    ResourceLocation fid = new ResourceLocation(o.get("fluid").getAsString());
                    FluidStack fs = new FluidStack(Registry.FLUID.get(fid), o.get("amount").getAsInt());
                    fin.add(fs);
                }
            }

            List<FluidStack> fout = new ArrayList<>();
            if (json.has("fluid_output")) {
                JsonArray a = json.getAsJsonArray("fluid_output");
                for (JsonElement e : a) {
                    JsonObject o = e.getAsJsonObject();
                    ResourceLocation fid = new ResourceLocation(o.get("fluid").getAsString());
                    FluidStack fs = new FluidStack(Registry.FLUID.get(fid), o.get("amount").getAsInt());
                    fout.add(fs);
                }
            }

            return new MachineRecipe(id, machine, inputs, outputs, ticks, jpt, fin, fout);
        }

        @Override
        public MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String machine = buf.readUtf();
            int in = buf.readInt();
            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < in; i++) inputs.add(buf.readItem());

            int out = buf.readInt();
            List<ItemStack> outputs = new ArrayList<>();
            for (int i = 0; i < out; i++) outputs.add(buf.readItem());

            int ticks = buf.readInt();
            double jpt = buf.readDouble();

            int fin = buf.readInt();
            List<FluidStack> fIns = new ArrayList<>();
            for (int i = 0; i < fin; i++) fIns.add(FluidStack.readFromBuffer(buf));

            int fout = buf.readInt();
            List<FluidStack> fOuts = new ArrayList<>();
            for (int i = 0; i < fout; i++) fOuts.add(FluidStack.readFromBuffer(buf));

            return new MachineRecipe(id, machine, inputs, outputs, ticks, jpt, fIns, fOuts);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MachineRecipe recipe) {
            buf.writeUtf(recipe.machine);
            buf.writeInt(recipe.inputs.size());
            for (ItemStack s : recipe.inputs) buf.writeItem(s);
            buf.writeInt(recipe.outputs.size());
            for (ItemStack s : recipe.outputs) buf.writeItem(s);
            buf.writeInt(recipe.ticks);
            buf.writeDouble(recipe.joulesPerTick);
            buf.writeInt(recipe.fluidInputs.size());
            for (FluidStack f : recipe.fluidInputs) f.writeToBuffer(buf);
            buf.writeInt(recipe.fluidOutputs.size());
            for (FluidStack f : recipe.fluidOutputs) f.writeToBuffer(buf);
        }
    }
}