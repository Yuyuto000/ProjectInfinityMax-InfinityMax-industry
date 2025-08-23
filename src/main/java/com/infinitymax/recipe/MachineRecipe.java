package com.infinitymax.industry.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/**
 * データパック読み込み可能な機械レシピ。
 * JSON フォーマット（例は下に記載）
 */
public class MachineRecipe implements Recipe<Container> {

    public static final ResourceLocation TYPE_ID = new ResourceLocation("infinitymax", "machine_recipe");

    public final ResourceLocation id;
    public final MachineBlock.Kind machineKind;
    public final ItemStack inputA, inputB;
    public final ItemStack output;
    public final int ticks;
    public final double joulesPerTick;

    public MachineRecipe(ResourceLocation id, MachineBlock.Kind kind, ItemStack inA, ItemStack inB, ItemStack out, int ticks, double jpt) {
        this.id = id;
        this.machineKind = kind;
        this.inputA = (inA == null) ? ItemStack.EMPTY : inA.copy();
        this.inputB = (inB == null) ? ItemStack.EMPTY : inB.copy();
        this.output = (out == null) ? ItemStack.EMPTY : out.copy();
        this.ticks = ticks;
        this.joulesPerTick = jpt;
    }

    @Override public boolean matches(Container inv, Level level) { return false; } // データパックは直接検索して使う（match logic below）

    @Override public ItemStack assemble(Container inv) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int pWidth, int pHeight) { return true; }
    @Override public ItemStack getResultItem() { return output.copy(); }
    @Override public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return MachineRecipeTypes.MACHINE; }

    // ------------ 実利用用ヘルパ ---------------
    public boolean matchesStacks(ItemStack a, ItemStack b) {
        if (!fits(a, inputA)) return false;
        if (!fits(b, inputB)) return false;
        return true;
    }
    private boolean fits(ItemStack have, ItemStack need) {
        if (need == null || need.isEmpty()) return true;
        if (have == null || have.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(have, need) && have.getCount() >= need.getCount();
    }

    // ---------------- JSON Serializer -----------------
    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
            String mk = json.has("machine") ? json.get("machine").getAsString() : "CRUSHER";
            MachineBlock.Kind kind = MachineBlock.Kind.valueOf(mk);

            // inputs: array of up to 2
            ItemStack inA = ItemStack.EMPTY;
            ItemStack inB = ItemStack.EMPTY;
            if (json.has("inputs")) {
                JsonArray arr = json.getAsJsonArray("inputs");
                if (arr.size() > 0) inA = parseStack(arr.get(0).getAsJsonObject());
                if (arr.size() > 1) inB = parseStack(arr.get(1).getAsJsonObject());
            }

            ItemStack out = ItemStack.EMPTY;
            if (json.has("output")) out = parseStack(json.getAsJsonObject("output"));

            int ticks = json.has("ticks") ? json.get("ticks").getAsInt() : 200;
            double jpt = json.has("joules_per_tick") ? json.get("joules_per_tick").getAsDouble() : 100.0;

            return new MachineRecipe(id, kind, inA, inB, out, ticks, jpt);
        }

        private static ItemStack parseStack(JsonObject obj) {
            String item = obj.has("item") ? obj.get("item").getAsString() : "minecraft:air";
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            ItemStack st = new ItemStack(Registry.ITEM.get(new ResourceLocation(item)));
            st.setCount(count);
            return st;
        }

        @Nullable
        @Override
        public MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String mk = buf.readUtf();
            MachineBlock.Kind kind = MachineBlock.Kind.valueOf(mk);
            ItemStack inA = buf.readItem();
            ItemStack inB = buf.readItem();
            ItemStack out = buf.readItem();
            int ticks = buf.readInt();
            double jpt = buf.readDouble();
            return new MachineRecipe(id, kind, inA, inB, out, ticks, jpt);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MachineRecipe r) {
            buf.writeUtf(r.machineKind.name());
            buf.writeItem(r.inputA);
            buf.writeItem(r.inputB);
            buf.writeItem(r.output);
            buf.writeInt(r.ticks);
            buf.writeDouble(r.joulesPerTick);
        }
    }
}