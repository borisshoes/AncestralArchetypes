package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xyz.nucleoid.packettweaker.PacketContext;

public class EndGliderItem extends GliderItem{
   public EndGliderItem(Settings settings){
      super(ArchetypeRegistry.ENDER_GLIDER, ArchetypeRegistry.ENDER_DRAGON.getColor(), settings, ArchetypeRegistry.END_GLIDER_TRIM_PATTERN);
   }
}
