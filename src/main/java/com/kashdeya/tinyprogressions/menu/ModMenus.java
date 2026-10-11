package com.kashdeya.tinyprogressions.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"tp");
    public static final RegistryObject<MenuType<IronFurnaceMenu>> IRON_FURNACE=MENUS.register("iron_furnace",() -> IForgeMenuType.create(IronFurnaceMenu::new));
}
