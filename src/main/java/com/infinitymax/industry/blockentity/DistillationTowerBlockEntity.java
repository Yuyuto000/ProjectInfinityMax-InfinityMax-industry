package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.fluid.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蒸留塔の簡易例:
 * - 原油を隣タンクから引いて、段階的に複数の隣接タンクへ分配する（簡易モデル）
 */
public class DistillationTowerBlockEntity extends MachineBlockEntity {

    private final int batchmB = 200;

    public DistillationTowerBlockEntity(BlockPos pos, BlockState st) {
        super(pos, st, MachineBlock.Kind.DISTILLATION_TOWER);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (level == null || level.isClientSide) return;

        // find any neighbor oil tank
        for (var npos : com.infinitymax.industry.fluid.FluidNetwork.neighbors(worldPosition)) {
            var be = level.getBlockEntity(npos);
            if (be instanceof FluidTankBlockEntity tank && "oil".equals(tank.getMedium().id) && tank.getAmountmB() >= batchmB) {
                // consume oil
                tank.flow(level, npos, -batchmB);
                // produce fractions into three neighboring tanks (if exist) in order
                var outs = new java.util.ArrayList<FluidTankBlockEntity>();
                for (var outPos : com.infinitymax.industry.fluid.FluidNetwork.neighbors(worldPosition)) {
                    var outBe = level.getBlockEntity(outPos);
                    if (outBe instanceof FluidTankBlockEntity outTank && outTank.getAmountmB() < outTank.getCapacitymB()) {
                        outs.add(outTank);
                    }
                }
                if (!outs.isEmpty()) {
                    int per = Math.max(1, batchmB / outs.size());
                    for (var o : outs) {
                        o.flow(level, o.getBlockPos(), +per);
                    }
                }
                return;
            }
        }
    }
}