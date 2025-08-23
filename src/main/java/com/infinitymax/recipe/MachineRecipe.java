package com.infinitymax.industry.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * データ駆動型マシンレシピ
 * - items 入出力
 * - fluids 入出力
 * - ticks, energy
 */
public class MachineRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final String machine;
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;
    private final int ticks;
    private final double joulesPerTick;
    private final List<FluidStack> fluidInputs;
    private final List<FluidStack> fluidOutputs;

    public MachineRecipe(ResourceLocation id,
                         String machine,
                         List<ItemStack> inputs,
                         List<ItemStack> outputs,
                         int ticks,
                         double joulesPerTick,
                         List<FluidStack> fluidInputs,
                         List<FluidStack> fluidOutputs) {
        this.id = id;
        this.machine = machine;
        this.inputs = inputs;
        this.outputs = outputs;
        this.ticks = ticks;
        this.joulesPerTick = joulesPerTick;
        this.fluidInputs = fluidInputs;
        this.fluidOutputs = fluidOutputs;
    }

    public String getMachine() { return machine; }
    public List<ItemStack> getInputs() { return inputs; }
    public List<ItemStack> getOutputs() { return outputs; }
    public List<FluidStack> getFluidInputs() { return fluidInputs; }
    public List<FluidStack> getFluidOutputs() { return fluidOutputs; }
    public int getTicks() { return ticks; }
    public double getJoulesPerTick() { return joulesPerTick; }

    @Override public boolean matches(SimpleContainer inv, Level level) { return true; }
    @Override public ItemStack assemble(SimpleContainer inv) { return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem() { return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<MachineRecipe> {
        private Type() {}
        public static final Type INSTANCE = new Type();
        public static final String ID = "machine_recipe";
    }

    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation("infinitymax", "machine_recipe");

        @Override
        public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
            String machine = json.get("machine").getAsString();

            List<ItemStack> inputs = new ArrayList<>();
            JsonArray inArr = json.getAsJsonArray("inputs");
            for (JsonElement e : inArr) {
                JsonObject o = e.getAsJsonObject();
                inputs.add(new ItemStack(
                        BuiltInRegistries.ITEM.get(new ResourceLocation(o.get("item").getAsString())),
                        o.has("count") ? o.get("count").getAsInt() : 1
                ));
            }

            List<ItemStack> outputs = new ArrayList<>();
            if (json.has("outputs")) {
                for (JsonElement e : json.getAsJsonArray("outputs")) {
                    JsonObject o = e.getAsJsonObject();
                    outputs.add(new ItemStack(
                            BuiltInRegistries.ITEM.get(new ResourceLocation(o.get("item").getAsString())),
                            o.has("count") ? o.get("count").getAsInt() : 1
                    ));
                }
            }

            int ticks = json.get("ticks").getAsInt();
            double jpt = json.get("joules_per_tick").getAsDouble();

            List<FluidStack> fin = new ArrayList<>();
            if (json.has("fluid_input")) {
                for (JsonElement e : json.getAsJsonArray("fluid_input")) {
                    JsonObject o = e.getAsJsonObject();
                    fin.add(new FluidStack(
                            BuiltInRegistries.FLUID.get(new ResourceLocation(o.get("fluid").getAsString())),
                            o.get("amount").getAsInt()
                    ));
                }
            }

            List<FluidStack> fout = new ArrayList<>();
            if (json.has("fluid_output")) {
                for (JsonElement e : json.getAsJsonArray("fluid_output")) {
                    JsonObject o = e.getAsJsonObject();
                    fout.add(new FluidStack(
                            BuiltInRegistries.FLUID.get(new ResourceLocation(o.get("fluid").getAsString())),
                            o.get("amount").getAsInt()
                    ));
                }
            }

            return new MachineRecipe(id, machine, inputs, outputs, ticks, jpt, fin, fout);
        }

        @Override
        public MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String machine = buf.readUtf();
            int inSize = buf.readInt();
            List<ItemStack> ins = new ArrayList<>();
            for (int i = 0; i < inSize; i++) ins.add(buf.readItem());

            int outSize = buf.readInt();
            List<ItemStack> outs = new ArrayList<>();
            for (int i = 0; i < outSize; i++) outs.add(buf.readItem());

            int ticks = buf.readInt();
            double jpt = buf.readDouble();

            int fInSize = buf.readInt();
            List<FluidStack> fin = new ArrayList<>();
            for (int i = 0; i < fInSize; i++) fin.add(FluidStack.readFromBuffer(buf));

            int fOutSize = buf.readInt();
            List<FluidStack> fout = new ArrayList<>();
            for (int i = 0; i < fOutSize; i++) fout.add(FluidStack.readFromBuffer(buf));

            return new MachineRecipe(id, machine, ins, outs, ticks, jpt, fin, fout);
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