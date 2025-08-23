package net.borisshoes.ancestralarchetypes.datagen;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator extends FabricTagProvider.ItemTagProvider {
   public ItemTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
      super(output, registriesFuture);
   }
   
   @Override
   protected void configure(RegistryWrapper.WrapperLookup lookup) {
      valueLookupBuilder(ArchetypeRegistry.CARNIVORE_FOODS)
            .add(Items.BEEF)
            .add(Items.COOKED_BEEF)
            .add(Items.PORKCHOP)
            .add(Items.COOKED_PORKCHOP)
            .add(Items.MUTTON)
            .add(Items.COOKED_MUTTON)
            .add(Items.CHICKEN)
            .add(Items.COOKED_CHICKEN)
            .add(Items.RABBIT)
            .add(Items.COOKED_RABBIT)
            .add(Items.COD)
            .add(Items.COOKED_COD)
            .add(Items.SALMON)
            .add(Items.COOKED_SALMON)
            .add(Items.TROPICAL_FISH)
            .add(Items.PUFFERFISH)
            .add(Items.ROTTEN_FLESH)
            .add(Items.SPIDER_EYE)
            .add(Items.RABBIT_STEW)
            ;
      valueLookupBuilder(ArchetypeRegistry.SLIME_GROW_ITEMS)
            .add(Items.SLIME_BALL)
            ;
      valueLookupBuilder(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)
            .add(Items.MAGMA_CREAM)
      ;
   }
}
