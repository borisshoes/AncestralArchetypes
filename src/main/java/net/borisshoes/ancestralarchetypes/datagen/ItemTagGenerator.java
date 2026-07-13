package net.borisshoes.ancestralarchetypes.datagen;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.misc.MetamorphTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator extends FabricTagsProvider.ItemTagsProvider {
   public ItemTagGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
      super(output, registriesFuture);
   }
   
   @Override
   protected void addTags(HolderLookup.Provider lookup) {
      builder(ArchetypeRegistry.CARNIVORE_FOODS)
            .add(ItemIds.BEEF)
            .add(ItemIds.COOKED_BEEF)
            .add(ItemIds.PORKCHOP)
            .add(ItemIds.COOKED_PORKCHOP)
            .add(ItemIds.MUTTON)
            .add(ItemIds.COOKED_MUTTON)
            .add(ItemIds.CHICKEN)
            .add(ItemIds.COOKED_CHICKEN)
            .add(ItemIds.RABBIT)
            .add(ItemIds.COOKED_RABBIT)
            .add(ItemIds.COD)
            .add(ItemIds.COOKED_COD)
            .add(ItemIds.SALMON)
            .add(ItemIds.COOKED_SALMON)
            .add(ItemIds.TROPICAL_FISH)
            .add(ItemIds.PUFFERFISH)
            .add(ItemIds.ROTTEN_FLESH)
            .add(ItemIds.SPIDER_EYE)
            .add(ItemIds.RABBIT_STEW);
      
      builder(ArchetypeRegistry.SLIME_GROW_ITEMS)
            .add(ItemIds.SLIME_BALL);
      
      builder(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)
            .add(ItemIds.MAGMA_CREAM);
      
      builder(ArchetypeRegistry.SULFUR_GROW_ITEMS)
            .add(BlockItemIds.SULFUR_SPIKE);
      
      builder(ArchetypeRegistry.CHOCOLATE_ALLERGY_FOODS)
            .add(ItemIds.COOKIE);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.ICE))
            .add(BlockItemIds.ICE)
            .add(BlockItemIds.BLUE_ICE)
            .add(BlockItemIds.PACKED_ICE);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.WOOL))
            .forceAddTag(ItemTags.WOOL);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.IRON))
            .add(BlockItemIds.IRON_BLOCK)
            .add(BlockItemIds.RAW_IRON_BLOCK);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.NETHERITE))
            .add(BlockItemIds.NETHERITE_BLOCK);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.TNT))
            .add(BlockItemIds.TNT);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.GOLD))
            .add(BlockItemIds.GOLD_BLOCK)
            .add(BlockItemIds.RAW_GOLD_BLOCK);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.MAGMA))
            .add(BlockItemIds.MAGMA_BLOCK);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.BOOKSHELF))
            .add(BlockItemIds.BOOKSHELF)
            .add(BlockItemIds.CHISELED_BOOKSHELF);
      
      builder(ArchetypeRegistry.METAMORPH_ITEMS.get(MetamorphTypes.SCULK))
            .add(BlockItemIds.SCULK)
            .add(BlockItemIds.SCULK_CATALYST)
            .add(BlockItemIds.SCULK_SENSOR)
            .add(BlockItemIds.SCULK_SHRIEKER);
      
      TagAppender<Item> abilityItemBuilder = builder(ArchetypeRegistry.ABILITY_ITEMS);
      ArchetypeRegistry.ABILITY_ITEM_KEY_MAP.forEach((item, key) -> abilityItemBuilder.add(key));
      
      builder(ArchetypeRegistry.BACKPACK_DISALLOWED_ITEMS)
            .forceAddTag(ArchetypeRegistry.ABILITY_ITEMS)
            .forceAddTag(ItemTags.SHULKER_BOXES);
   }
}
