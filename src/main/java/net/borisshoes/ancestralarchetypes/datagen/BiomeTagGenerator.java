package net.borisshoes.ancestralarchetypes.datagen;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.concurrent.CompletableFuture;

public class BiomeTagGenerator extends FabricTagProvider<Biome> {
   public BiomeTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
      super(output, RegistryKeys.BIOME, registriesFuture);
   }
   
   @Override
   protected void configure(RegistryWrapper.WrapperLookup lookup) {
      builder(ArchetypeRegistry.COLD_DAMAGE_EXCEPTION_BIOMES)
            .add(BiomeKeys.THE_VOID)
      ;
      builder(ArchetypeRegistry.COLD_DAMAGE_INCLUDE_BIOMES)
            .add(BiomeKeys.DEEP_FROZEN_OCEAN)
      ;
      builder(ArchetypeRegistry.DRY_OUT_EXCEPTION_BIOMES)
            .add(BiomeKeys.THE_VOID)
            .add(BiomeKeys.THE_END)
            .add(BiomeKeys.END_BARRENS)
            .add(BiomeKeys.END_HIGHLANDS)
            .add(BiomeKeys.END_MIDLANDS)
            .add(BiomeKeys.SMALL_END_ISLANDS)
      ;
      builder(ArchetypeRegistry.DRY_OUT_INCLUDE_BIOMES);
   }
}
