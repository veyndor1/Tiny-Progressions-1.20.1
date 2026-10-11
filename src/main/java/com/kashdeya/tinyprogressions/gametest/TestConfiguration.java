package com.kashdeya.tinyprogressions.gametest;

import com.kashdeya.tinyprogressions.LegacyConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** GameTests mutate the real spec in memory; autosaved world files are unsuitable test fixtures. */
@Mod.EventBusSubscriber(modid="tp")
public final class TestConfiguration {
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)) return;
        var config=CommentedConfig.inMemory();
        defaults(LegacyConfig.SPEC.getValues(),new java.util.ArrayList<>(),config);
        LegacyConfig.SPEC.setConfig(config);
    }
    private static void defaults(UnmodifiableConfig values,java.util.List<String> prefix,CommentedConfig config) {
        for(var entry:values.valueMap().entrySet()) {
            var path=new java.util.ArrayList<>(prefix); path.add(entry.getKey());
            if(entry.getValue() instanceof ForgeConfigSpec.ConfigValue<?> value) config.set(path,value.getDefault());
            else if(entry.getValue() instanceof UnmodifiableConfig nested) defaults(nested,path,config);
        }
    }
}
