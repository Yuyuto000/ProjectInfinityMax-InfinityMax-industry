package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蒸留塔：原油→ナフサ/軽油/重油を段階的に分離（簡易モデル）
 */
public class DistillationTowerBlockEntity extends MachineBlockEntity {

    public DistillationTowerBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, MachineBlock.Kind.DISTILLATION_TOWER);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        // Example:
        // - If raw oil present in adjacent tank, consume X mB and produce separated fluids to different tanks
        // - Use FluidNetwork.tick / direct neighbor flow for movement
    }
}