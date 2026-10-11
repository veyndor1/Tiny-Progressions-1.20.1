package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.Content;
import com.kashdeya.tinyprogressions.item.PouchMenu;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.ItemStackHandler;

@GameTestHolder("tp")
@PrefixGameTestTemplate(false)
public final class PouchMenuTests {
    @GameTest(template="empty") public static void transfersAndOffhandSwapsCannotMoveOrNestTheOpenPouch(GameTestHelper helper) {
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.fromString("d9bdf701-1000-4000-8000-000000000008"),"TPPouchMenuTest"));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        var pouch=Content.item("pouch").getDefaultInstance();
        var storage=(ItemStackHandler)pouch.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(() -> new AssertionError("Missing pouch storage"));
        player.getInventory().setItem(0,pouch);
        var menu=new PouchMenu(1,player.getInventory(),pouch,0,storage);
        menu.clicked(81,0,ClickType.QUICK_MOVE,player);
        menu.clicked(81,1,ClickType.THROW,player);
        helper.assertTrue(player.getInventory().getItem(0)==pouch && menu.stillValid(player),"Open pouch must not move or drop through its menu");
        player.getInventory().setItem(1,new ItemStack(Items.DIAMOND,12));
        menu.clicked(82,0,ClickType.QUICK_MOVE,player);
        helper.assertTrue(player.getInventory().getItem(1).isEmpty() && storage.getStackInSlot(0).getCount()==12,"Shift-transfer must move the entire stack exactly once");
        var nested=Content.item("pouch").getDefaultInstance(); player.getInventory().setItem(1,nested);
        menu.clicked(82,0,ClickType.QUICK_MOVE,player);
        helper.assertTrue(player.getInventory().getItem(1)==nested && storage.getStackInSlot(1).isEmpty(),"Shift-transfer must reject nested pouches");
        menu.clicked(0,0,ClickType.SWAP,player);
        helper.assertTrue(player.getInventory().getItem(0)==pouch && storage.getStackInSlot(0).getCount()==12,"Number-key swapping must not swap the open pouch into itself");
        player.getInventory().removeItemNoUpdate(0);
        helper.assertTrue(!menu.stillValid(player),"Losing ownership of the pouch must invalidate the menu");
        player.getInventory().setItem(40,pouch);
        var offhand=new PouchMenu(2,player.getInventory(),pouch,40,storage);
        offhand.clicked(0,40,ClickType.SWAP,player);
        helper.assertTrue(player.getInventory().getItem(40)==pouch && storage.getStackInSlot(0).getCount()==12,"Offhand swap must preserve the open pouch and its contents");
        helper.succeed();
    }
}
