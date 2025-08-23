package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.fluid.FluidTankBlockEntity;
import com.infinitymax.industry.fluid.Medium;
import com.infinitymax.industry.recipe.MachineRecipe;
import com.infinitymax.industry.recipe.RecipeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 電解槽の例:
 * - 接続された隣接タンクから水(WATER)を消費し、隣接タンクへ水素(HYDROGEN)を生成して流す。
 * - 消費電力は MachineRecipe または定義値を使う（ここでは固定値の例）
 */
public class ElectrolyzerBlockEntity extends MachineBlockEntity {

    private final int consumePerOperationmB = 100; // 水を100mB消費して水素を生成する等（例）

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState st) {
        super(pos, st, MachineBlock.Kind.ELECTROLYZER);
    }

    @Override
    public void serverTick() {
        super.serverTick(); // 基本の recipe 処理を使う場合はここで動く

        // 追加: 電解固有の流体変換（優先度はrecipeよりこちらを使う場合）
        if (level == null || level.isClientSide) return;

        // find neighbor tank with water (simple 6方向チェック)
        for (var npos : com.infinitymax.industry.fluid.FluidNetwork.neighbors(worldPosition)) {
            var be = level.getBlockEntity(npos);
            if (be instanceof FluidTankBlockEntity tank && tank.getMedium() == Medium.WATER && tank.getAmountmB() >= consumePerOperationmB) {
                // check an output neighbor to push hydrogen
                for (var outPos : com.infinitymax.industry.fluid.FluidNetwork.neighbors(worldPosition)) {
                    var outBe = level.getBlockEntity(outPos);
                    if (outBe instanceof FluidTankBlockEntity outTank && outTank.getMedium() == Medium.HYDROGEN || outTank.getAmountmB() == 0) {
                        // consume water
                        tank.flow(level, npos, -consumePerOperationmB);
                        // produce hydrogen into outTank
                        outTank.flow(level, outPos, +consumePerOperationmB);
                        // consume energy
                        double needJ = 500.0; // example energy per op
                        if (energyJ >= needJ) {
                            energyJ -= needJ;
                        } else {
                            // insufficient energy: reverse and push back fluid (simplified: skip)
                        }
                        return;
                    }
                }
            }
        }
    }
}