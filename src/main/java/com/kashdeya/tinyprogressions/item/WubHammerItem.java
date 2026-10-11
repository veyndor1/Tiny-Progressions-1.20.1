package com.kashdeya.tinyprogressions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.ChatFormatting;
import java.util.List;

public final class WubHammerItem extends PickaxeItem implements ConfiguredTools.Durable {
    private static final ThreadLocal<Boolean> AREA_MINING=ThreadLocal.withInitial(() -> false);
    public WubHammerItem(Tier tier,Properties properties) { super(tier,1,-2.8F,properties); }
    public static boolean areaMining() { return AREA_MINING.get(); }
    public static void recordFace(ItemStack stack,BlockPos pos,Direction face) {
        var tag=stack.getOrCreateTag(); tag.putLong("tp:hammer_origin",pos.asLong()); tag.putInt("tp:hammer_face",face.ordinal());
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity miner) {
        boolean success=super.mineBlock(stack,level,state,pos,miner);
        if(level.isClientSide || AREA_MINING.get() || !(miner instanceof ServerPlayer player) || player.isShiftKeyDown() || stack.isEmpty()) return success;
        Direction face=Direction.getNearest(player.getLookAngle().x,player.getLookAngle().y,player.getLookAngle().z).getOpposite();
        var tag=stack.getTag();
        if(tag!=null && tag.contains("tp:hammer_origin") && tag.getLong("tp:hammer_origin")==pos.asLong()) {
            face=Direction.from3DDataValue(tag.getInt("tp:hammer_face")); tag.remove("tp:hammer_origin"); tag.remove("tp:hammer_face");
        }
        int dx=face.getAxis()==Direction.Axis.X ? 0 : 1;
        int dy=face.getAxis()==Direction.Axis.Y ? 0 : 1;
        int dz=face.getAxis()==Direction.Axis.Z ? 0 : 1;
        AREA_MINING.set(true);
        try {
            for(BlockPos target:BlockPos.betweenClosed(pos.offset(-dx,-dy,-dz),pos.offset(dx,dy,dz))) {
                if(stack.isEmpty()) break;
                if(target.equals(pos) || !level.hasChunkAt(target) || !level.mayInteract(player,target) || !player.mayUseItemAt(target,face,stack)) continue;
                BlockState neighbor=level.getBlockState(target);
                if(neighbor.isAir() || neighbor.getDestroySpeed(level,target)<0 || !neighbor.is(BlockTags.MINEABLE_WITH_PICKAXE) || !stack.isCorrectToolForDrops(neighbor) || neighbor.hasBlockEntity()) continue;
                // Vanilla server mining preserves permission events, item durability, tool-aware loot, Silk Touch and XP.
                player.gameMode.destroyBlock(target.immutable());
            }
        } finally { AREA_MINING.remove(); }
        return success;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,level,tooltip,flag);
        tooltip.add(Component.translatable("tooltip.wubhammer_1").withStyle(ChatFormatting.YELLOW));
    }
}
