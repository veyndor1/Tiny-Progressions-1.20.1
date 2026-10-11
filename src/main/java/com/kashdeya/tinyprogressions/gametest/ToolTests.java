package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class ToolTests {
    @GameTest(template = "empty")
    public static void paxelSupportsAllThreeToolFamilies(GameTestHelper helper) {
        var stack = Content.item("emerald_multi").getDefaultInstance();
        helper.assertTrue(stack.canPerformAction(ToolActions.PICKAXE_DIG),"Paxel must mine stone");
        helper.assertTrue(stack.canPerformAction(ToolActions.AXE_DIG),"Paxel must chop wood");
        helper.assertTrue(stack.canPerformAction(ToolActions.SHOVEL_DIG),"Paxel must dig soil");
        helper.assertTrue(stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1,"Paxel must have tool speed on wood");
        helper.assertTrue(stack.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1,"Paxel must have tool speed on soil");
        helper.assertTrue(stack.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) > 1 && stack.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()),"Paxel must retain its legacy fast web harvesting");
        helper.succeed();
    }
    @GameTest(template="empty") public static void hoesKeepOriginalCombatAttributes(GameTestHelper helper) {
        for(String name:new String[]{"bone_hoe","flint_hoe","emerald_hoe","wub_hoe","nether_iron_hoe"}) {
            var stack=Content.item(name).getDefaultInstance();
            var hoe=(net.minecraft.world.item.HoeItem)stack.getItem();
            var attributes=stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            double damage=attributes.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
            double speed=attributes.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
            helper.assertTrue(damage==0 && Math.abs(speed-(hoe.getTier().getAttackDamageBonus()-3))<.0001,"Hoe combat must match 1.12.2 ItemHoe, including fractional materials: "+name);
        }
        helper.succeed();
    }
    @GameTest(template="empty") public static void battleAxesKeepSwordAndWebBehavior(GameTestHelper helper) {
        for(String name:new String[]{"wooden_battle","stone_battle","iron_battle","diamond_battle","emerald_battle"}) {
            var stack=Content.item(name).getDefaultInstance();
            var axe=(net.minecraft.world.item.AxeItem)stack.getItem();
            helper.assertTrue(stack.canPerformAction(ToolActions.AXE_DIG) && stack.canPerformAction(ToolActions.SWORD_DIG),"Battle axe must support its two source tool families");
            helper.assertTrue(stack.isCorrectToolForDrops(Blocks.COBWEB.defaultBlockState()) && stack.getDestroySpeed(Blocks.COBWEB.defaultBlockState())==axe.getTier().getSpeed(),"Battle axe must harvest web at material speed");
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void spearProvidesServerReachAttribute(GameTestHelper helper) {
        var stack = Content.item("emerald_spear").getDefaultInstance();
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(ForgeMod.ENTITY_REACH.get());
        helper.assertTrue(!modifiers.isEmpty(),"Spear must have an authoritative reach modifier");
        helper.assertTrue(stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(ForgeMod.ENTITY_REACH.get()).isEmpty(),
                "Spear reach must be tied to the attacking hand");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void scytheHarvestsAndReplantsWheatAndBeetroot(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        helper.getLevel().getServer().getPlayerList().op(player.getGameProfile());
        var stack = Content.item("emerald_scythe").getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        BlockPos wheat = new BlockPos(3,2,3), beet = new BlockPos(4,2,3);
        helper.setBlock(wheat.above(2),Blocks.GLOWSTONE);
        helper.setBlock(beet.above(2),Blocks.GLOWSTONE);
        helper.setBlock(wheat.below(),Blocks.FARMLAND);
        helper.setBlock(beet.below(),Blocks.FARMLAND);
        helper.runAtTickTime(5,() -> {
        helper.setBlock(wheat,((CropBlock)Blocks.WHEAT).getStateForAge(7));
        helper.setBlock(beet,((CropBlock)Blocks.BEETROOTS).getStateForAge(3));
        BlockPos absolute = helper.absolutePos(wheat);
        player.setPos(absolute.getX()+.5,absolute.getY(),absolute.getZ()+.5);
        helper.assertTrue(helper.getLevel().mayInteract(player,absolute),"Test player must have permission at the crop");
        helper.assertTrue(player.mayUseItemAt(absolute,Direction.UP,stack),"Test player must be able to use a tool here");
        helper.assertTrue(((CropBlock)helper.getBlockState(wheat).getBlock()).isMaxAge(helper.getBlockState(wheat)),"Test wheat must start mature");
        var context = new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute),Direction.UP,absolute,false));
        InteractionResult result = stack.useOn(context);
        helper.assertTrue(result.consumesAction(),"Scythe must harvest mature crops");
        helper.assertTrue(((CropBlock)helper.getBlockState(wheat).getBlock()).getAge(helper.getBlockState(wheat)) == 0,
                "Wheat must be replanted");
        helper.assertTrue(((CropBlock)helper.getBlockState(beet).getBlock()).getAge(helper.getBlockState(beet)) == 0,
                "Beetroot must use its own age property");
        helper.succeed();
        });
    }
}
