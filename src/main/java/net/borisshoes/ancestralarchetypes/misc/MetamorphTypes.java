package net.borisshoes.ancestralarchetypes.misc;

import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum MetamorphTypes{
   ICE("ice", Blocks.PACKED_ICE, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/packed_ice"))),
   WOOL("wool", Blocks.WHITE_WOOL, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/white_wool"))),
   IRON("iron", Blocks.IRON_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/iron_block"))),
   NETHERITE("netherite", Blocks.NETHERITE_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/netherite_block"))),
   TNT("tnt", Blocks.TNT, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/tnt_side"))),
   GOLD("gold", Blocks.GOLD_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/gold_block"))),
   MAGMA("magma", Blocks.MAGMA_BLOCK, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/magma"))),
   BOOKSHELF("bookshelf", Blocks.BOOKSHELF, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/bookshelf"))),
   SCULK("sculk", Blocks.SCULK_SENSOR, new AtlasSprite(AtlasIds.BLOCKS, Identifier.parse("block/sculk_shrieker_can_summon_inner_top")));
   
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
