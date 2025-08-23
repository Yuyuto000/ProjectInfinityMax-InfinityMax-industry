package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.recipe.MachineRecipe;
import com.infinitymax.industry.recipe.RecipeHelper;
import com.infinitymax.industry.util.InventoryHelper;
import com.infinitymax.industry.energy.IElectricNode;
import com.infinitymax.industry.energy.IElectricPort;
import com.infinitymax.industry.fluid.FluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MachineBlockEntity extends BlockEntity implements IElectricNode, IElectricPort {

    public static BlockEntityType<MachineBlockEntity> TYPE;

    public final MachineBlock.Kind kind;

    protected final ItemStack[] items = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    protected int progress = 0;
    protected int progressRequired = 0;
    protected MachineRecipe currentRecipe = null;

    protected double storedJ = 0.0;
    protected double capacityJ = 200000.0;

    protected double ratedV = 240.0;
    protected double ratedA = 40.0;
    protected double internalR = 0.5;
    protected double terminalV = 0.0;

    protected FluidTank[] tanks; // 派生機械で使用

    public MachineBlockEntity(BlockPos pos, BlockState state, MachineBlock.Kind kind) {
        super(TYPE, pos, state);
        this.kind = kind;
        this.tanks = new FluidTank[]{ new FluidTank(1000), new FluidTank(1000) }; // デフォルト2タンク
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        // JSON レシピ取得
        if (currentRecipe == null) {
            currentRecipe = RecipeHelper.findMatching(level, kind, items[0], items[1]);
            if (currentRecipe != null) {
                progressRequired = currentRecipe.getTicks();
                progress = 0;
            }
        }

        if (currentRecipe != null) {
            double needJ = currentRecipe.getJoulesPerTick();
            if (storedJ >= needJ) {
                storedJ -= needJ;
                progress++;

                if (progress >= progressRequired) {
                    // 出力アイテム処理
                    ItemStack out = currentRecipe.getOutput().copy();
                    if (InventoryHelper.canInsert(items[2], out, out.getMaxStackSize())) {
                        InventoryHelper.insert(items, 2, out, out.getMaxStackSize());
                        // 入力消費
                        if (!currentRecipe.getInputA().isEmpty()) InventoryHelper.extract(items, 0, currentRecipe.getInputA().getCount());
                        if (!currentRecipe.getInputB().isEmpty()) InventoryHelper.extract(items, 1, currentRecipe.getInputB().getCount());
                    }

                    // 流体出力処理
                    if (currentRecipe.getFluidOutputs() != null) {
                        for (int i = 0; i < currentRecipe.getFluidOutputs().length; i++) {
                            tanks[i].fill(currentRecipe.getFluidOutputs()[i]);
                        }
                    }

                    currentRecipe = null;
                    progress = 0;
                    progressRequired = 0;
                }
            }
        }

        // 微小な自然放電
        storedJ = Math.max(0.0, storedJ - 0.01);
    }

    // ====== 電力関連 ======
    @Override public double getVoltageV() { return terminalV; }
    @Override public double getInternalResistanceOhm() { return internalR; }
    @Override public double getMaxIntakeA() { return ratedA; }
    @Override public double getMaxOutputA() { return 0.0; }

    @Override
    public double pushPullCurrent(net.minecraft.world.level.Level lvl, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        if (requestedCurrentA <= 0) return 0.0;
        double allow = Math.min(requestedCurrentA, ratedA);
        double deliveredJ = requestedVoltageV * allow / 20.0;
        storedJ = Math.min(capacityJ, storedJ + deliveredJ);
        terminalV += (requestedVoltageV - terminalV) * 0.3;
        return allow;
    }

    @Override public void markDirtyGraph() {}

    @Override public double requiredWorkJPerTick() { return currentRecipe == null ? 0.0 : currentRecipe.getJoulesPerTick(); }
    @Override public double acceptPowerVA(double voltageV, double maxCurrentA) { return pushPullCurrent(level, worldPosition, voltageV, maxCurrentA); }
    @Override public double ratedVoltageV() { return ratedV; }
    @Override public double ratedCurrentA() { return ratedA; }

    // ====== persistence ======
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.storedJ = tag.getDouble("storedJ");
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(tag.getCompound("tank" + i));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("storedJ", storedJ);
        for (int i = 0; i < tanks.length; i++) tag.put("tank" + i, tanks[i].writeToNBT());
    }

    // ====== Inventory & GUI accessors ======
    public ItemStack getSlot(int i) { return items[i]; }
    public void setSlot(int i, ItemStack stack) { items[i] = stack; }
    public int getProgress() { return progress; }
    public int getProgressRequired() { return progressRequired; }
    public double getEnergyStored() { return storedJ; }
    public FluidTank getTank(int i) { return tanks[i]; }
}