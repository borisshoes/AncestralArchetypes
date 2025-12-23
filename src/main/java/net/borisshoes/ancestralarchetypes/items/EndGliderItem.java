package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;

public class EndGliderItem extends GliderItem{
   public EndGliderItem(Properties settings){
      super(ArchetypeRegistry.ENDER_GLIDER, ArchetypeRegistry.ENDER_DRAGON.getColor(), settings, ArchetypeRegistry.END_GLIDER_TRIM_PATTERN);
   }
}
