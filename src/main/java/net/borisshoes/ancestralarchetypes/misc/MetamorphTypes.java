package net.borisshoes.ancestralarchetypes.misc;

import net.minecraft.core.Vec3i;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.ReplaceDisk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

public enum MetamorphTypes {
   ICE("ice", Blocks.PACKED_ICE, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/packed_ice"))),
   WOOL("wool", Blocks.WOOL.white(), new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/white_wool"))),
   IRON("iron", Blocks.IRON_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/iron_block"))),
   NETHERITE("netherite", Blocks.NETHERITE_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/netherite_block"))),
   TNT("tnt", Blocks.TNT, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/tnt_side"))),
   GOLD("gold", Blocks.GOLD_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/gold_block"))),
   MAGMA("magma", Blocks.MAGMA_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/magma"))),
   BOOKSHELF("bookshelf", Blocks.BOOKSHELF, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/bookshelf"))),
   SCULK("sculk", Blocks.SCULK_SHRIEKER, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/sculk_shrieker_can_summon_inner_top")));
   
   public static final ReplaceDisk ICE_WALKER = new ReplaceDisk(
         new LevelBasedValue.Clamped(LevelBasedValue.perLevel(3.0F, 1.0F), 0.0F, 16.0F),
         LevelBasedValue.constant(1.0F),
         new Vec3i(0, -1, 0),
         Optional.of(
               BlockPredicate.allOf(
                     BlockPredicate.matchesTag(new Vec3i(0, 1, 0), BlockTags.AIR),
                     BlockPredicate.matchesBlocks(Blocks.WATER),
                     BlockPredicate.matchesFluids(Fluids.WATER),
                     BlockPredicate.unobstructed()
               )
         ),
         BlockStateProvider.simple(Blocks.FROSTED_ICE),
         Optional.of(GameEvent.BLOCK_PLACE)
   );
   
   private final String id;
   private final Block block;
   private final AtlasSprite sprite;
   
   MetamorphTypes(String id, Block block, AtlasSprite sprite){
      this.id = id;
      this.sprite = sprite;
      this.block = block;
   }
   
   public Block getBlock(){
      return block;
   }
   
   public AtlasSprite getSprite(){
      return sprite;
   }
   
   public static MetamorphTypes fromString(String str){
      for(MetamorphTypes type : MetamorphTypes.values()){
         if(type.id.equalsIgnoreCase(str) || type.name().equalsIgnoreCase(str)){
            return type;
         }
      }
      return null;
   }
   
   @Override
   public String toString(){
      return this.id;
   }
}
