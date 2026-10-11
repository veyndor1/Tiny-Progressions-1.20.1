package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.*;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class SupporterTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper helper, String suffix) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(suffix.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "TPSupporter"));
        player.setGameMode(GameType.SURVIVAL);
        helper.getLevel().getServer().getPlayerList().op(player.getGameProfile());
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3,2,3))));
        return player;
    }
    @GameTest(template="empty") public static void birthdayCakePlacementCostsDurabilityAndHonorsCancellation(GameTestHelper helper) {
        var player = player(helper,"birthday");
        var stack = Content.item("birthday_pickaxe").getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos base = new BlockPos(3,1,4);
        helper.setBlock(base, Blocks.STONE);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(base)), Direction.UP, helper.absolutePos(base), false));
        Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> { if (event.getEntity() == player) event.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(cancel);
        try {
            stack.useOn(context);
            helper.assertBlockPresent(Blocks.AIR, base.above());
            helper.assertTrue(stack.getDamageValue() == 0, "Denied cake placement must not consume durability");
        } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        helper.assertTrue(stack.useOn(context).consumesAction(), "Birthday pickaxe must place cake");
        helper.assertBlockPresent(Blocks.CAKE, base.above());
        helper.assertTrue(stack.getMaxDamage() == 6521 && stack.getDamageValue() == 854 && stack.getCount() == 1,
                "Cake placement must preserve the pickaxe and original durability cost");
        helper.succeed();
    }
    @GameTest(template="empty") public static void angelBlockPlacesInAirAndDoesNotOverwriteBlocks(GameTestHelper helper) {
        var player = player(helper,"angel");
        player.setYRot(0); player.setXRot(0);
        var stack = Content.item("fmf_block").getDefaultInstance(); stack.setCount(3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos target = BlockPos.containing(player.getX(), player.getEyeY(), Math.floor(player.getBoundingBox().maxZ) + 1);
        var relative = target.subtract(helper.absolutePos(BlockPos.ZERO));
        helper.assertTrue(stack.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND).getResult().consumesAction(), "Angel block must place without an existing supporting block");
        helper.assertBlockPresent(Content.block("fmf_block"), relative);
        helper.assertTrue(stack.getCount() == 2, "Floating placement must consume exactly one block");
        helper.assertTrue(stack.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
                "Air placement must not overwrite occupied targets");
        helper.assertTrue(stack.getCount() == 2, "Failed placement must not consume a block");
        helper.succeed();
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void set(String name, Object value) { ((ForgeConfigSpec.ConfigValue)LegacyConfig.SPEC.getValues().get(java.util.Arrays.asList(name.split("\\.")))).set(value); }
    @GameTest(template="empty") public static void supporterConfigurationChangesMiningAndCreativeAvailability(GameTestHelper helper) {
        String[] names = {"ConfigHandler.kappa_damage", "ConfigHandler.kappa_level", "ConfigHandler.sea_pickaxe", "ConfigHandler.sea_axe", "ConfigHandler.c_axe", "DamageHandler.wooden_scythe"};
        Object[] saved = java.util.Arrays.stream(names).map(LegacyConfig::value).toArray();
        try {
            var kappa = Content.item("kappa_pick").getDefaultInstance();
            set(names[5],128);
            helper.assertTrue(Content.item("wooden_scythe").getDefaultInstance().getMaxDamage()==128,"Custom tool durability must use loaded settings");
            set(names[0], 942); set(names[1], 1);
            helper.assertTrue(kappa.getMaxDamage() == 942 && !kappa.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Kappa config must control durability and mining level");
            set(names[1], 2);
            helper.assertTrue(kappa.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Iron mining level must harvest diamond ore");
            var sea = Content.item("c_axe").getDefaultInstance();
            set(names[2], true); set(names[3], true); set(names[4], true);
            helper.assertTrue(sea.canPerformAction(ToolActions.PICKAXE_DIG) && !sea.canPerformAction(ToolActions.AXE_STRIP)
                    && sea.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()), "Sea pickaxe mode must take precedence and retain cobweb harvesting");
            helper.assertTrue(Content.enabled("c_axe"), "Sea tool must remain available when either mode is enabled");
            set(names[2], false);
            helper.assertTrue(sea.canPerformAction(ToolActions.AXE_STRIP) && !sea.canPerformAction(ToolActions.PICKAXE_DIG), "Sea axe mode must support normal axe actions");
            set(names[4], false);
            helper.assertTrue(!Content.enabled("c_axe"), "Disabled feature must be hidden from the creative tab");
        } finally { for (int i=0;i<names.length;i++) set(names[i],saved[i]); }
        helper.succeed();
    }
}
