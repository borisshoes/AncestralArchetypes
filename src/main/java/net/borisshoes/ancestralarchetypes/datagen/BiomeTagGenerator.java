package net.borisshoes.ancestralarchetypes.datagen;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.concurrent.CompletableFuture;

public class BiomeTagGenerator extends FabricTagProvider<Biome> {
   public BiomeTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
      super(output, Registries.BIOME, registriesFuture);
   }
   
   @Override
   protected void addTags(HolderLookup.Provider lookup) {
      builder(ArchetypeRegistry.COLD_DAMAGE_EXCEPTION_BIOMES)
            .add(Biomes.THE_VOID)
      ;
      builder(ArchetypeRegistry.COLD_DAMAGE_INCLUDE_BIOMES)
            .add(Biomes.DEEP_FROZEN_OCEAN)
      ;
      builder(ArchetypeRegistry.DRY_OUT_EXCEPTION_BIOMES)
            .add(Biomes.THE_VOID)
            .add(Biomes.THE_END)
            .add(Biomes.END_BARRENS)
            .add(Biomes.END_HIGHLANDS)
            .add(Biomes.END_MIDLANDS)
            .add(Biomes.SMALL_END_ISLANDS)
      ;
      builder(ArchetypeRegistry.DRY_OUT_INCLUDE_BIOMES);
   }
}
