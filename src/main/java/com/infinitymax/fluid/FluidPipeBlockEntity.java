package com.infinitymax.industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FluidPipeBlockEntity extends BlockEntity implements IPressureNode {

    // 配管特性（例：鋼管・中径）
    private int capacitymB = 2000;
    private int amountmB = 0;
    private double pressureKPa = 101.3; // 大気圧近辺から
    private Medium medium = Medium.WATER;

    private int maxInPerTick  = 250;
    private int maxOutPerTick = 250;

    public static net.minecraft.world.level.block.entity.BlockEntityType<FluidPipeBlockEntity> TYPE;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    // ===== IPressureNode =====
    @Override public double getPressureKPa() { return pressureKPa; }
    @Override public int getCapacitymB() { return capacitymB; }
    @Override public int getAmountmB() { return amountmB; }
    @Override public Medium getMedium() { return medium; }
    @Override public int getMaxFlowIntakePerTick() { return maxInPerTick; }
    @Override public int getMaxFlowOutputPerTick() { return Math.min(maxOutPerTick, amountmB); }

    @Override
    public int flow(Level level, BlockPos pos, int requestedmB) {
        if (requestedmB == 0) return 0;
        if (requestedmB > 0) { // 流入
            int can = Math.min(requestedmB, Math.min(maxInPerTick, capacitymB - amountmB));
            amountmB += can;
            // 簡易圧力上昇（充填率に比例）
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            return can;
        } else { // 流出
            int want = -requestedmB;
            int can = Math.min(want, Math.min(maxOutPerTick, amountmB));
            amountmB -= can;
            pressureKPa = 101.3 + (amountmB / (double)capacitymB) * 400.0;
            return -can;
        }
    }

    @Override public void markDirtyGraph() {}

    // ===== Tick =====
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        FluidNetwork.tick(level, worldPosition, 512);
    }
}
