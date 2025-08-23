package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.fluid.FluidStack;
import com.infinitymax.industry.fluid.FluidTankBlockEntity;
import com.infinitymax.industry.recipe.MachineRecipe;
import com.infinitymax.industry.recipe.RecipeHelper;
import com.infinitymax.industry.util.InventoryHelper;
import com.infinitymax.industry.gui.machine.MachineMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 汎用 MachineBlockEntity（JSON Recipe 対応、流体入出力サポート、GUI 同期）
 *
 * ストレージ：
 * - items[0] / items[1] = input slots
 * - items[2] = output slot
 * - energyJ, energyCapJ で電力管理
 * - tanks[]: 内部小型タンク（派生機に合わせて数/容量を調整）
 *
 * Recipe の流体要求があれば、隣接の FluidTankBlockEntity から自動で吸い上げる（短時間）実装。
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider {

    public static BlockEntityType<MachineBlockEntity> TYPE; // RegistryManager で注入

    public final MachineBlock.Kind kind;

    protected final ItemStack[] items = new ItemStack[]{ ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    protected int progress = 0;
    protected int progressRequired = 0;
    protected MachineRecipe currentRecipe = null;

    // energy
    protected double energyJ = 0.0;
    protected double energyCapJ = 200_000.0;

    // internal tanks (default: 2 small tanks, 派生で変更可能)
    protected final FluidStack[] tanks;

    // GUI sync data (0: prog,1: progMax,2: energyCur,3: energyCap,4: tank0 amount,5: tank1 amount)
    protected final SimpleContainerData data = new SimpleContainerData(6);

    public MachineBlockEntity(BlockPos pos, BlockState state, MachineBlock.Kind kind) {
        super(TYPE, pos, state);
        this.kind = kind;
        this.tanks = new FluidStack[] { new FluidStack(net.minecraft.world.level.material.Fluids.EMPTY, 0),
                                        new FluidStack(net.minecraft.world.level.material.Fluids.EMPTY, 0) };
        markData();
    }

    // ----- server tick -----
    public void serverTick() {
        if (level == null || level.isClientSide) return;

        // 1) recipe lookup if none
        if (currentRecipe == null) {
            currentRecipe = RecipeHelper.findMatching(level, kind.name().toLowerCase(),
                    items[0], items[1]);
            if (currentRecipe != null) {
                progressRequired = currentRecipe.getTicks();
                progress = 0;
            }
        }

        if (currentRecipe != null) {
            // ensure fluid inputs are available (try to draw from neighbors into internal tanks)
            if (!ensureFluidInputs(currentRecipe.getFluidInputs())) {
                // missing fluids: do not progress
                markData();
                setChanged();
                return;
            }

            double needJ = currentRecipe.getJoulesPerTick();
            // if insufficient energy, we wait. Energy is expected to be injected by network via IElectricNode
            if (energyJ >= needJ) {
                energyJ -= needJ;
                progress++;

                if (progress >= progressRequired) {
                    // finalization: produce item outputs (try to insert), produce fluid outputs
                    boolean outputOk = true;
                    // items
                    for (ItemStack out : currentRecipe.getOutputs()) {
                        if (!InventoryHelper.canInsert(items[2], out, out.getMaxStackSize())) {
                            outputOk = false;
                            break;
                        }
                    }

                    if (outputOk) {
                        // consume items input counts
                        for (ItemStack need : currentRecipe.getInputs()) {
                            if (need != null && !need.isEmpty()) {
                                // try to consume from slot0 or slot1
                                if (InventoryHelper.itemMatches(items[0], need)) InventoryHelper.extract(items, 0, need.getCount());
                                else if (InventoryHelper.itemMatches(items[1], need)) InventoryHelper.extract(items, 1, need.getCount());
                            }
                        }

                        // insert outputs (simple: outputs go into slot2 stacked)
                        for (ItemStack out : currentRecipe.getOutputs()) {
                            InventoryHelper.insert(items, 2, out.copy(), out.getMaxStackSize());
                        }

                        // produce fluid outputs into internal tanks; if tank full, push to neighbors
                        produceFluidOutputs(currentRecipe.getFluidOutputs());

                        // reset
                        currentRecipe = null;
                        progress = 0;
                        progressRequired = 0;
                    } else {
                        // cannot place outputs -> stall
                        progress = progressRequired; // keep at finished until space
                    }
                }
            } else {
                // not enough energy: optionally request from network (left to ElectricNetwork to push)
            }
        }

        // small passive discharge / safety clamp
        energyJ = Math.max(0.0, Math.min(energyCapJ, energyJ - 0.0));
        markData();
        setChanged();
    }

    // ----- helper: ensure fluids available by pulling from neighbors into internal tanks when needed -----
    protected boolean ensureFluidInputs(List<FluidStack> required) {
        if (required == null || required.isEmpty()) return true;
        for (FluidStack need : required) {
            if (need == null || need.isEmpty()) continue;
            int remain = need.amount;
            // try internal tanks first
            for (int i = 0; i < tanks.length && remain > 0; i++) {
                if (RegistryFluidEquals(tanks[i], need)) {
                    int avail = tanks[i].amount;
                    int used = Math.min(avail, remain);
                    tanks[i].amount -= used;
                    remain -= used;
                }
            }
            if (remain <= 0) continue;
            // try neighbors by calling their flow to extract
            for (Direction d : Direction.values()) {
                BlockPos npos = worldPosition.relative(d);
                var be = level.getBlockEntity(npos);
                if (be instanceof FluidTankBlockEntity tank) {
                    // ask tank to give (flow with negative amount)
                    int gave = -tank.flow(level, npos, -remain); // flow expects negative to extract
                    if (gave > 0) remain -= gave;
                    if (remain <= 0) break;
                }
            }
            if (remain > 0) {
                // not enough fluid present anywhere
                return false;
            }
        }
        return true;
    }

    // ----- helper: produce fluid outputs into internal tanks or push to neighbor tanks -----
    protected void produceFluidOutputs(List<FluidStack> outputs) {
        if (outputs == null || outputs.isEmpty()) return;
        for (FluidStack out : outputs) {
            if (out == null || out.isEmpty()) continue;
            int remain = out.amount;
            // try internal tanks same fluid or empty
            for (int i = 0; i < tanks.length && remain > 0; i++) {
                if (tanks[i].isEmpty() || RegistryFluidEquals(tanks[i], out)) {
                    // choose capacity = 16000 per internal tank for example
                    int cap = 16000;
                    int can = Math.min(cap - tanks[i].amount, remain);
                    if (can > 0) {
                        // if tank empty, set fluid type
                        if (tanks[i].isEmpty()) tanks[i] = new FluidStack(out.fluid, can);
                        else tanks[i].amount += can;
                        remain -= can;
                    }
                }
            }
            if (remain <= 0) continue;
            // push to neighbors
            for (Direction d : Direction.values()) {
                BlockPos npos = worldPosition.relative(d);
                var be = level.getBlockEntity(npos);
                if (be instanceof FluidTankBlockEntity tank) {
                    int pushed = tank.flow(level, npos, remain);
                    // flow returns positive if accepted (?) follow your FluidTank implementation
                    // assume positive returned amount accepted
                    if (pushed > 0) remain -= pushed;
                }
                if (remain <= 0) break;
            }
            // if remain > 0, drop into world? (we skip dropping)
        }
    }

    // small helper to compare fluid types (registry keys)
    protected boolean RegistryFluidEquals(FluidStack a, FluidStack b) {
        if (a == null || b == null) return false;
        if (a.isEmpty() || b.isEmpty()) return false;
        return Registry.FLUID.getKey(a.fluid).equals(Registry.FLUID.getKey(b.fluid));
    }

    // ========== MenuProvider for GUI ==========
    @Override public Component getDisplayName() { return Component.literal(kind.name()); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        // use SimpleContainer + data to back the menu; MachineMenu expects machineInv and data
        SimpleContainer inv = new SimpleContainer(3);
        // copy current items into inv for the container view (server-side stays authoritative)
        for (int i = 0; i < items.length; i++) inv.setItem(i, items[i]);
        return new MachineMenu(id, playerInventory, inv, data, worldPosition);
    }

    // ========== persistence ==========
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyJ = tag.getDouble("energyJ");
        for (int i = 0; i < items.length; i++) {
            String k = "slot" + i;
            if (tag.contains(k)) items[i] = ItemStack.of(tag.getCompound(k));
            else items[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < tanks.length; i++) {
            String tk = "tank" + i;
            if (tag.contains(tk)) tanks[i] = FluidStack.readFromNbt(tag.getCompound(tk));
            else tanks[i] = new FluidStack(net.minecraft.world.level.material.Fluids.EMPTY, 0);
        }
        progress = tag.getInt("progress");
        progressRequired = tag.getInt("progressReq");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energyJ", energyJ);
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && !items[i].isEmpty()) tag.put("slot" + i, items[i].save(new CompoundTag()));
        }
        for (int i = 0; i < tanks.length; i++) tag.put("tank" + i, tanks[i] == null ? (new FluidStack(net.minecraft.world.level.material.Fluids.EMPTY,0)).writeToNbt() : tanks[i].writeToNbt());
        tag.putInt("progress", progress);
        tag.putInt("progressReq", progressRequired);
    }

    // ========== GUI data sync helper ==========
    protected void markData() {
        data.set(0, progress);
        data.set(1, progressRequired);
        data.set(2, (int)Math.min(Integer.MAX_VALUE, Math.round(energyJ)));
        data.set(3, (int)Math.min(Integer.MAX_VALUE, Math.round(energyCapJ)));
        data.set(4, tanks[0] == null ? 0 : Math.min(Integer.MAX_VALUE, tanks[0].amount));
        data.set(5, tanks.length > 1 && tanks[1] != null ? Math.min(Integer.MAX_VALUE, tanks[1].amount) : 0);
    }

    // ========== Accessors used by GUI etc ==========
    public ItemStack getSlot(int i) { return items[i]; }
    public void setSlot(int i, ItemStack s) { items[i] = s; }
    public int getProgress() { return progress; }
    public int getProgressRequired() { return progressRequired; }
    public double getEnergyJ() { return energyJ; }
    public double getEnergyCapJ() { return energyCapJ; }
    public FluidStack getTankStack(int idx) { return idx >= 0 && idx < tanks.length ? tanks[idx] : new FluidStack(net.minecraft.world.level.material.Fluids.EMPTY, 0); }
}