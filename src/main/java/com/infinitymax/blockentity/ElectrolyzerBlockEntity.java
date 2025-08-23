package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 電解：液体(例えば水) を分解して水素/酸素を作る
 * - ここでは FluidTankBlockEntity と連携して水を消費し、Gas(=Medium) を生成して隣接タンクへ供給する想定
 */
public class ElectrolyzerBlockEntity extends MachineBlockEntity {

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, MachineBlock.Kind.ELECTROLYZER);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        // Example behavior:
        // - If has required joules and adjacent tank with water, consume water mB and produce hydrogen mB to adjacent tank
        // This reference code intentionally leaves plumbing calls generic; integrate with FluidNetwork.flow() or directly manipulate neighbor FluidTankBlockEntity
    }
}