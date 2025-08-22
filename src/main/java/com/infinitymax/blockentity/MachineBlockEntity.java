package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MachineBlockEntity extends BlockEntity {
    public static BlockEntityType<MachineBlockEntity> TYPE; // RegistryManagerで注入
    public final MachineBlock.Kind kind;
    private int progress;

    public MachineBlockEntity(BlockPos pos, BlockState state, MachineBlock.Kind kind) {
        super(TYPE, pos, state);
        this.kind = kind;
        this.progress = 0;
    }

    /** 超軽量サンプル処理（後でエネルギー/流体APIに繋ぐ） */
    public void serverTick() {
        // ここで kind によって処理を分岐可能
        // 例: CRUSHERなら input -> output に進行、必要電力チェック等
        progress++;
        if (progress >= 20) progress = 0;
    }
}
