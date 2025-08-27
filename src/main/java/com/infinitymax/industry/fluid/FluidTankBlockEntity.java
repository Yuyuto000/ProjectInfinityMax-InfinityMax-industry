package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.fluid.IPressureNode;
import com.infinitymax.industry.fluid.Medium;
import com.infinitymax.industry.network.SmartNetworkManager;
import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * タンク (流体貯蔵のノード)
 *
 * - SmartNetworkManager を使って局所デバウンス再構築を要求する
 * - serverTick は軽量処理 + SmartNetworkManager.serverTick(level)
 */
public class FluidTankBlockEntity extends BlockEntity implements IPressureNode {

    public static BlockEntityType<FluidTankBlockEntity> TYPE;

    private int capacitymB = 16000;
    private int amountmB = 0;
    private Medium medium = Medium.WATER;
    private double pressureKPa = 101.3;
    private int maxIn = 1000;
    private int maxOut = 1000;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        TickDispatcher.register(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TickDispatcher.unregister(this);
        // 削除時に起点座標を渡してデバウンス再構築を要求
        if (level != null && !level.isClientSide) {
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    // ===== IPressureNode 実装 =====
    @Override public double getPressureKPa() { return pressureKPa; }
    @Override public int getCapacitymB() { return capacitymB; }
    @Override public int getAmountmB() { return amountmB; }
    @Override public Medium getMedium() { return medium; }
    @Override public int getMaxFlowIntakePerTick() { return maxIn; }
    @Override public int getMaxFlowOutputPerTick() { return Math.min(maxOut, amountmB); }

    @Override
    public int flow(Level lvl, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;

        if (requestedmB > 0) {
            int can = Math.min(requestedmB, Math.min(maxIn, capacitymB - amountmB));
            if (can > 0) {
                amountmB += can;
                pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            }
            return can;
        } else {
            int want = -requestedmB;
            int can = Math.min(want, Math.min(maxOut, amountmB));
            if (can > 0) {
                amountmB -= can;
                pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            }
            return -can;
        }
    }

    @Override
    public void markDirtyGraph() {
        if (level != null && !level.isClientSide) {
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        // SmartNetworkManager に処理を委譲（デバウンス再構築・ネットワーク tick）
        SmartNetworkManager.get().serverTick(level);

        // ローカルな緩和処理（例）
        pressureKPa = Math.max(101.3, pressureKPa - 0.02);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    public void onNeighborsChanged() {
        if (level != null && !level.isClientSide) {
            SmartNetworkManager.get().markFluidDirty(level, worldPosition);
        }
    }

    // 永続化
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        amountmB = tag.getInt("amountmB");
        capacitymB = tag.contains("capacitymB") ? tag.getInt("capacitymB") : capacitymB;
        pressureKPa = tag.contains("pressureKPa") ? tag.getDouble("pressureKPa") : pressureKPa;
        if (tag.contains("medium")) {
            try { medium = Medium.valueOf(tag.getString("medium")); } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("amountmB", amountmB);
        tag.putInt("capacitymB", capacitymB);
        tag.putDouble("pressureKPa", pressureKPa);
        tag.putString("medium", medium.name());
    }
}