package com.infinitymax.industry.blockentity;

import com.infinitymax.industry.block.MachineBlock;
import com.infinitymax.industry.tick.TickDispatcher;
import com.infinitymax.industry.registry.RecipeRegistry;
import com.infinitymax.industry.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 粉砕機：粉化（例：ore -> dust）
 */
public class CrusherBlockEntity extends MachineBlockEntity {

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, MachineBlock.Kind.CRUSHER);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        // Crusher-specific behavior could be added (e.g. higher wear, chance for byproduct)
        // Example: small chance to produce extra dust when recipe finishes
        // Handled in parent output step in this template
    }
}