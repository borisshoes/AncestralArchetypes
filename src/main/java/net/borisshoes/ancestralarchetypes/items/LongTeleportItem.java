package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

public class LongTeleportItem extends DirectionalTeleportItem {
   
   public LongTeleportItem(Properties settings){
      super(ArchetypeRegistry.LONG_TELEPORT, "\uD83D\uDC41", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.CLAY_BALL;
      }else{
         return Items.ENDER_PEARL;
      }
   }
   
   @Override
   protected double getMaxRange(){
      return CONFIG.getDouble(ArchetypeRegistry.LONG_TELEPORT_DISTANCE);
   }
   
   @Override
   protected int getCooldownTicks(){
      return CONFIG.getInt(ArchetypeRegistry.LONG_TELEPORT_COOLDOWN);
   }
   
   @Override
   protected int getGlowColor(){
      return 0xAA00AA;
   }
}
