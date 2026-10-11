package com.kashdeya.tinyprogressions.audit;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.network.NetworkConstants;

/** Developer companion only; never included in the player artifact. */
@Mod("tp_audit")
public final class ProductionAudit {
    public ProductionAudit(FMLJavaModLoadingContext context) {
        // The companion adds no gameplay or network channels and is only needed by the QA client.
        context.registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY,(version,server) -> true));
    }
}
