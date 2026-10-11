package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.LegacyConfig;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class FluidTests {
    @net.minecraft.gametest.framework.GameTest(template="empty",timeoutTicks=100)
    public static void spreadingWubJuicePreservesOtherLiquids(GameTestHelper helper) {
        var source=new net.minecraft.core.BlockPos(3,3,3);
        helper.setBlock(source.below(),net.minecraft.world.level.block.Blocks.WATER);
        helper.setBlock(source.east().below(),net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(source.east(),net.minecraft.world.level.block.Blocks.LAVA);
        helper.setBlock(source,Content.block("vasholine"));
        helper.runAtTickTime(45,() -> {
            helper.assertTrue(helper.getBlockState(source.below()).getFluidState().is(net.minecraft.tags.FluidTags.WATER),"Wub juice must not displace water below it");
            helper.assertTrue(helper.getBlockState(source.east()).getFluidState().is(net.minecraft.tags.FluidTags.LAVA),"Wub juice must not displace another neighboring liquid");
            helper.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=50) public static void wubFluidUsesConfiguredHealingAndAppliesEffectsOncePerTick(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000004"),"TPFluidTest"));
        player.setGameMode(GameType.SURVIVAL); player.removeAllEffects(); player.setHealth(10);
        var block=Content.block("vasholine"); var state=block.defaultBlockState();
        var pos=helper.absolutePos(new BlockPos(3,2,3));
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getGameTime()%20==0,"Wait for the once-per-second healing tick");
            block.entityInside(state,helper.getLevel(),pos,player);
            block.entityInside(state,helper.getLevel(),pos.east(),player);
            float expected=10+LegacyConfig.decimal("ConfigHandler.vasholine_heal_amount");
            helper.assertTrue(Math.abs(player.getHealth()-expected)<.001F,"Overlapping fluid blocks must heal exactly the configured amount once");
            helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS)==LegacyConfig.enabled("ConfigHandler.wub_weakness"),"Weakness must follow config");
            helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS)==LegacyConfig.enabled("ConfigHandler.wub_blindness"),"Blindness must follow config");
            helper.assertTrue(player.hasEffect(MobEffects.DIG_SLOWDOWN)==LegacyConfig.enabled("ConfigHandler.wub_fatigue"),"Fatigue must follow config");
        });
    }
}
