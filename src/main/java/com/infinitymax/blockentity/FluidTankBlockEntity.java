package com.infinitymax.industry. blockentity;

import com.infinitymax.industry.tick.TickDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * タンク (流体貯蔵のノード)
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
    }

    @Override public double getPressureKPa() { return pressureKPa; }
    @Override public int getCapacitymB() { return capacitymB; }
    @Override public int getAmountmB() { return amountmB; }
    @Override public Medium getMedium() { return medium; }
    @Override public int getMaxFlowIntakePerTick() { return maxIn; }
    @Override public int getMaxFlowOutputPerTick() { return Math.min(maxOut, amountmB); }

    @Override
    public int flow(net.minecraft.world.level.Level level, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;
        if (requestedmB > 0) {
            int can = Math.min(requestedmB, Math.min(maxIn, capacitymB - amountmB));
            amountmB += can;
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            return can;
        } else {
            int want = -requestedmB;
            int can = Math.min(want, Math.min(maxOut, amountmB));
            amountmB -= can;
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            return -can;
        }
    }

    @Override public void markDirtyGraph() {}

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        FluidNetwork.tick(level, worldPosition, 512);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        amountmB = tag.getInt("amountmB");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("amountmB", amountmB);
    }
}