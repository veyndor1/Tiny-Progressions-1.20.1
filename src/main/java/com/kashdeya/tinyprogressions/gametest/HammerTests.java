package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.item.WubHammerItem;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class HammerTests {
    @GameTest(template="empty") public static void hammerMinesOnePlaneAndPreservesUnbreakableBlocks(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000005"),"TPHammerTest"));
        player.setGameMode(GameType.SURVIVAL); helper.getLevel().getServer().getPlayerList().op(player.getGameProfile());
        var stack=Content.item("wub_hammer").getDefaultInstance(); stack.enchant(Enchantments.SILK_TOUCH,1);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        BlockPos center=new BlockPos(3,3,3);
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-1,-1,0),center.offset(1,1,0))) helper.setBlock(pos,Blocks.STONE);
        helper.setBlock(center.offset(1,0,0),Blocks.BEDROCK);
        helper.setBlock(center.offset(0,0,1),Blocks.STONE);
        WubHammerItem.recordFace(stack,helper.absolutePos(center),Direction.NORTH);
        helper.getLevel().removeBlock(helper.absolutePos(center),false);
        stack.mineBlock(helper.getLevel(),Blocks.STONE.defaultBlockState(),helper.absolutePos(center),player);
        helper.assertBlockPresent(Blocks.BEDROCK,center.offset(1,0,0));
        helper.assertBlockPresent(Blocks.STONE,center.offset(0,0,1));
        helper.assertBlockPresent(Blocks.AIR,center.offset(-1,1,0));
        helper.assertTrue(stack.getDamageValue()==8,"Hammer must consume durability only for eight actually mined blocks");
        helper.succeed();
    }
}
