package com.kashdeya.tinyprogressions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GrowthCrystalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);
    private final int radius;
    private final java.util.function.IntSupplier interval;

    public GrowthCrystalBlock(Properties properties, int radius, java.util.function.IntSupplier interval) {
        super(properties.noOcclusion());
        this.radius = radius;
        this.interval = interval;
    }
    @Override public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                        BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                   BlockState previous, boolean moving) {
        if (!level.isClientSide && !previous.is(this)) level.scheduleTick(pos, this, interval.getAsInt());
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius),
                                                     pos.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(target)) continue;
            BlockState crop = level.getBlockState(target);
            // Mature stems still need random ticks to produce fruit; bonemeal eligibility would exclude them.
            if (GrowthSupport.grows(crop)) {
                crop.randomTick(level, target.immutable(), random);
            }
        }
        level.scheduleTick(pos, this, interval.getAsInt());
    }
    @Override public boolean dropFromExplosion(net.minecraft.world.level.Explosion explosion) { return false; }
    @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,RandomSource random) {
        if(!com.kashdeya.tinyprogressions.LegacyConfig.enabled("ConfigHandler.ParticalTicks")) return;
        String name=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(this).getPath();
        String setting=switch(name) { case "growth_upgrade" -> "GrowthUpgradeParticalTicks"; case "growth_upgrade_two" -> "GrowthUpgradeTwoParticalTicks"; default -> "GrowthParticalTicks"; };
        if(random.nextInt(Math.max(1,com.kashdeya.tinyprogressions.LegacyConfig.integer("ConfigHandler."+setting)))!=0) return;
        boolean nearby=false;
        for(var target:BlockPos.betweenClosed(pos.offset(-radius,-radius,-radius),pos.offset(radius,radius,radius))) {
            if(level.hasChunkAt(target) && GrowthSupport.grows(level.getBlockState(target))) { nearby=true; break; }
        }
        if(nearby) for(int i=0;i<4;i++) level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,
                (random.nextDouble()-.5)*radius,random.nextDouble()-1,(random.nextDouble()-.5)*radius);
    }
}
