package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.util.InventoryHelper;
import com.infinitymax.industry.registry.RecipeRegistry;
import com.infinitymax.industry.energy.IElectricNode;
import com.infinitymax.industry.energy.IElectricPort;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 汎用マシン BE:
 * - 2 input スロット (0/1)、1 output スロット (2)
 * - RecipeRegistry からマッチするレシピを処理
 * - 電力（Joule）を消費して進行する（joulesPerTick）
 */
public class MachineBlockEntity extends BlockEntity implements IElectricNode, IElectricPort {

    public static BlockEntityType<MachineBlockEntity> TYPE; // 注入される

    public final MachineBlock.Kind kind;

    private final ItemStack[] items = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private int progress = 0;
    private int progressRequired = 0;
    private RecipeRegistry.Recipe currentRecipe = null;

    // Joules storage
    private double storedJ = 0.0;
    private double capacityJ = 200000.0;

    // electrical params
    private double ratedV = 240.0;
    private double ratedA = 40.0;
    private double internalR = 0.5;
    private double terminalV = 0.0;

    public MachineBlockEntity(BlockPos pos, BlockState state, MachineBlock.Kind kind) {
        super(TYPE, pos, state);
        this.kind = kind;
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
    }

    // ---------- core tick ----------
    public void serverTick() {
        if (level == null || level.isClientSide) return;

        // if no recipe, try to find one
        if (currentRecipe == null) {
            currentRecipe = RecipeRegistry.findMatching(items[0], items[1]);
            if (currentRecipe != null) {
                progressRequired = currentRecipe.ticks;
                progress = 0;
            }
        }

        if (currentRecipe != null) {
            double needJ = currentRecipe.joulesPerTick;
            // consume storedJ
            if (storedJ >= needJ) {
                storedJ -= needJ;
                progress++;
                if (progress >= progressRequired) {
                    // try to output
                    ItemStack out = currentRecipe.output.copy();
                    if (InventoryHelper.canInsert(items[2], out, out.getMaxStackSize())) {
                        InventoryHelper.insert(items, 2, out, out.getMaxStackSize());
                        // consume inputs
                        if (currentRecipe.inputA != null && !currentRecipe.inputA.isEmpty()) {
                            InventoryHelper.extract(items, 0, currentRecipe.inputA.getCount());
                        }
                        if (currentRecipe.inputB != null && !currentRecipe.inputB.isEmpty()) {
                            InventoryHelper.extract(items, 1, currentRecipe.inputB.getCount());
                        }
                    }
                    // reset
                    currentRecipe = null;
                    progress = 0;
                    progressRequired = 0;
                }
            } else {
                // insufficient energy: wait, maybe request from network (ElectricNetwork will push)
            }
        }

        // small passive loss
        storedJ = Math.max(0.0, storedJ - 0.01);
    }

    // ========= IElectricNode =========
    @Override public double getVoltageV() { return terminalV; }
    @Override public double getInternalResistanceOhm() { return internalR; }
    @Override public double getMaxIntakeA() { return ratedA; }
    @Override public double getMaxOutputA() { return 0.0; }

    @Override
    public double pushPullCurrent(net.minecraft.world.level.Level lvl, BlockPos pos, double requestedVoltageV, double requestedCurrentA) {
        // accept positive current; return actual A accepted
        if (requestedCurrentA <= 0) return 0.0;
        double allow = Math.min(requestedCurrentA, ratedA);
        double deliveredJ = requestedVoltageV * allow / 20.0; // Joules per tick
        storedJ = Math.min(capacityJ, storedJ + deliveredJ);
        terminalV += (requestedVoltageV - terminalV) * 0.3;
        return allow;
    }

    @Override public void markDirtyGraph() { /* no-op */ }

    // ========= IElectricPort =========
    @Override public double requiredWorkJPerTick() {
        return currentRecipe == null ? 0.0 : currentRecipe.joulesPerTick;
    }

    @Override
    public double acceptPowerVA(double voltageV, double maxCurrentA) {
        double acceptedA = pushPullCurrent(level, worldPosition, voltageV, maxCurrentA);
        return acceptedA;
    }

    @Override public double ratedVoltageV() { return ratedV; }
    @Override public double ratedCurrentA() { return ratedA; }

    // ===== persistence (very simple) =====
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // store minimal state for demo (not storing inventory here for brevity)
        this.storedJ = tag.getDouble("storedJ");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("storedJ", storedJ);
    }

    // Inventory accessors for GUI / automation (simplified)
    public ItemStack getSlot(int i) { return items[i]; }
    public void setSlot(int i, ItemStack s) { items[i] = s; }
}