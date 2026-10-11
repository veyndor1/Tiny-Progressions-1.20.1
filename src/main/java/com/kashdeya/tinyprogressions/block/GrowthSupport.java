package com.kashdeya.tinyprogressions.block;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class GrowthSupport {
    public static boolean grows(BlockState state) {
        Block block=state.getBlock();
        return state.isRandomlyTicking() && (block instanceof BonemealableBlock
                || block instanceof net.minecraftforge.common.IPlantable || block instanceof CactusBlock
                || block instanceof SugarCaneBlock || block instanceof ChorusFlowerBlock);
    }
}
