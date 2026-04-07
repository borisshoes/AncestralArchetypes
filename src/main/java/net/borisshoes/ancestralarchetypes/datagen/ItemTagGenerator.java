package net.borisshoes.ancestralarchetypes.datagen;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator extends FabricTagsProvider.ItemTagsProvider {
   public ItemTagGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
      super(output, registriesFuture);
   }
   
   @Override
   protected void addTags(HolderLookup.Provider lookup) {
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
            .add(Items.RABBIT_STEW);
      
      valueLookupBuilder(ArchetypeRegistry.SLIME_GROW_ITEMS)
            .add(Items.SLIME_BALL);
      
      valueLookupBuilder(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)
            .add(Items.MAGMA_CREAM);
      
      valueLookupBuilder(ArchetypeRegistry.CHOCOLATE_ALLERGY_FOODS)
            .add(Items.COOKIE);
      
      TagAppender<Item, Item> abilityItemBuilder = valueLookupBuilder(ArchetypeRegistry.ABILITY_ITEMS);
      for(Item item : ArchetypeRegistry.ITEMS){
         if(item instanceof AbilityItem){
            abilityItemBuilder.add(item);
         }
      }
      
      valueLookupBuilder(ArchetypeRegistry.BACKPACK_DISALLOWED_ITEMS)
            .forceAddTag(ArchetypeRegistry.ABILITY_ITEMS)
            .forceAddTag(ItemTags.SHULKER_BOXES);
   }
}
