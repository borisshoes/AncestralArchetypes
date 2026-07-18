package net.borisshoes.ancestralarchetypes.datagen;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.concurrent.CompletableFuture;

public class DamageTagGenerator extends FabricTagsProvider<DamageType> {
   public DamageTagGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture){
      super(output, Registries.DAMAGE_TYPE, registriesFuture);
   }
   
   @Override
   protected void addTags(HolderLookup.Provider lookup){
      builder(ArchetypeRegistry.NO_STARTLE)
            .add(DamageTypes.IN_FIRE)
            .add(DamageTypes.LAVA)
            .add(DamageTypes.DRAGON_BREATH)
            .add(DamageTypes.DROWN)
            .add(DamageTypes.DRY_OUT)
            .add(DamageTypes.DRAGON_BREATH)
            .add(DamageTypes.WITHER)
            .add(DamageTypes.CAMPFIRE)
            .add(DamageTypes.HOT_FLOOR)
            .add(DamageTypes.ON_FIRE)
            .add(DamageTypes.ENDER_PEARL)
            .add(DamageTypes.FREEZE)
            .add(DamageTypes.STARVE)
            .add(DamageTypes.INDIRECT_MAGIC)
      ;
   }
}
