package com.kashdeya.tinyprogressions;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "tp");
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, "tp");
    public static final RegistryObject<FluidType> TYPE = TYPES.register("vasholine", () ->
            new FluidType(FluidType.Properties.create().density(3000).viscosity(1500).lightLevel(15)) {
                @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return ResourceLocation.tryBuild("tp", "fluids/wub_juice_still"); }
                        @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.tryBuild("tp", "fluids/wub_juice_flow"); }
                    });
                }
            });
    public static final RegistryObject<FlowingFluid> SOURCE = FLUIDS.register("vasholine", () -> new ForgeFlowingFluid.Source(properties()) {
        @Override protected boolean canSpreadTo(net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos from,net.minecraft.world.level.block.state.BlockState fromState,net.minecraft.core.Direction direction,net.minecraft.core.BlockPos to,net.minecraft.world.level.block.state.BlockState toState,net.minecraft.world.level.material.FluidState targetFluid,Fluid incoming) {
            return permits(toState) && super.canSpreadTo(level,from,fromState,direction,to,toState,targetFluid,incoming);
        }
        @Override protected void spreadTo(net.minecraft.world.level.LevelAccessor level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.Direction direction,net.minecraft.world.level.material.FluidState fluid) {
            if(permits(state)) super.spreadTo(level,pos,state,direction,fluid);
        }
    });
    public static final RegistryObject<FlowingFluid> FLOWING = FLUIDS.register("flowing_vasholine", () -> new ForgeFlowingFluid.Flowing(properties()) {
        @Override protected boolean canSpreadTo(net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos from,net.minecraft.world.level.block.state.BlockState fromState,net.minecraft.core.Direction direction,net.minecraft.core.BlockPos to,net.minecraft.world.level.block.state.BlockState toState,net.minecraft.world.level.material.FluidState targetFluid,Fluid incoming) {
            return permits(toState) && super.canSpreadTo(level,from,fromState,direction,to,toState,targetFluid,incoming);
        }
        @Override protected void spreadTo(net.minecraft.world.level.LevelAccessor level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.Direction direction,net.minecraft.world.level.material.FluidState fluid) {
            if(permits(state)) super.spreadTo(level,pos,state,direction,fluid);
        }
    });
    private static boolean permits(net.minecraft.world.level.block.state.BlockState state) {
        return state.getFluidState().isEmpty() || state.getFluidState().getType().getFluidType()==TYPE.get();
    }
    public static final RegistryObject<Item> BUCKET = Content.ITEMS.register("vasholine_bucket", () ->
            new BucketItem(SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(TYPE, SOURCE, FLOWING)
                .bucket(BUCKET).block(() -> (net.minecraft.world.level.block.LiquidBlock) Content.block("vasholine"))
                .tickRate(20).slopeFindDistance(3).levelDecreasePerBlock(1);
    }
    public static void initialize() { /* class initialization registers the fluid and bucket suppliers */ }
}
