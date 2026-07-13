package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.misc.MetamorphTypes;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypes$ITEM_DATA;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypesId;

public class MetamorphHeadItem extends Item implements PolymerItem {
   
   public static final String METAMORPH_VISIBLE_MODEL = "metamorph_visible";
   public static final String METAMORPH_HIDDEN_MODEL = "metamorph_hidden";
   public static final String METAMORPH_HIDDEN_ASSET = "metamorph_asset";
   public static final String METAMORPH_HELMET_TYPE = "metamorph_type";
   
   public MetamorphHeadItem(Properties properties){
      super(properties.setId(ResourceKey.create(Registries.ITEM, archetypesId("metamorph_helmet"))));
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      if(entity instanceof ServerPlayer player){
         if(player.inventoryMenu.getCarried().is(ArchetypeRegistry.METAMORPH_HELMET_ITEM)){
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            return;
         }
         if(slot != EquipmentSlot.HEAD){
            stack.copyAndClear();
            return;
         }
         PlayerArchetypeData profile = AncestralArchetypes.profile(player);
         if(!profile.isMetamorphed()){
            stack.copyAndClear();
            return;
         }else{
            MetamorphTypes type = profile.getMetamorph();
            Identifier modelId = type.getBlock().asItem().getDefaultInstance().get(DataComponents.ITEM_MODEL);
            stack.set(DataComponents.ITEM_MODEL, modelId);
            archetypes$ITEM_DATA.putProperty(stack, METAMORPH_VISIBLE_MODEL, modelId.toString());
            archetypes$ITEM_DATA.putProperty(stack, METAMORPH_HELMET_TYPE, type.toString());
         }
      }
      stack.copyAndClear();
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext context){
      return Items.GLASS;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup){
      String id = archetypes$ITEM_DATA.getStringProperty(stack, METAMORPH_VISIBLE_MODEL);
      if(id.isEmpty()){
         return BuiltInRegistries.ITEM.getResourceKey(getPolymerItem(stack,context)).get().identifier();
      }else{
         return Identifier.parse(id);
      }
   }
}
