package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.BackpackGui;
import net.borisshoes.ancestralarchetypes.gui.BackpackSlot;
import net.borisshoes.ancestralarchetypes.gui.MountInventoryGui;
import net.borisshoes.arcananovum.items.GreavesOfGaialtus;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.borisshoes.borislib.utils.TextUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class BackpackItem extends AbilityItem{
   public BackpackItem(Settings settings){
      super(ArchetypeRegistry.BACKPACK, "\uD83D\uDCBC", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.SHULKER_SHELL;
      }else{
         return Items.MAGENTA_BUNDLE;
      }
   }
   
   @Override
   public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context){
      ItemStack superStack = super.getPolymerItemStack(itemStack, tooltipType, context);
      
      if(!(context.getPlayer() instanceof ServerPlayerEntity player)) return superStack;
      IArchetypeProfile profile = profile(player);
      if(!profile.hasAbility(this.ability)) return superStack;
      
      List<MutableText> lore = new ArrayList<>();
      lore.add(Text.translatable("text.ancestralarchetypes.backpack_description").formatted(Formatting.DARK_PURPLE,Formatting.ITALIC));
      lore.add(Text.literal(""));
      List<Pair<Item,Integer>> cargo = getCargoList(player);
      final int numDisplayed = 18;
      
      if(cargo.isEmpty()){
         lore.add(Text.translatable("text.ancestralarchetypes.backpack_contents_empty").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD));
      }else{
         lore.add(Text.translatable("text.ancestralarchetypes.backpack_contents").formatted(Formatting.LIGHT_PURPLE,Formatting.BOLD));
         int leftOverCount = 0;
         for(int i = 0; i < cargo.size(); i++){
            int count = cargo.get(i).getRight();
            if(i >= numDisplayed){
               leftOverCount += count;
               continue;
            }
            
            Item item = cargo.get(i).getLeft();
            int stacks = count / item.getMaxCount();
            int leftover = count % item.getMaxCount();
            
            if(count > item.getMaxCount()){
               lore.add(Text.translatable("text.ancestralarchetypes.backpack_contents_stack",
                     Text.literal(count+"").withColor(profile.getArchetype().getColor()),
                     Text.translatable("text.ancestralarchetypes.stacks",stacks).withColor(profile.getArchetype().getColor()),
                     Text.literal(leftover+"").withColor(profile.getArchetype().getColor()),
                     item.getName().copy().withColor(profile.getSubArchetype().getColor())
                     ).formatted(Formatting.DARK_PURPLE));
            }else{
               lore.add(Text.translatable("text.ancestralarchetypes.backpack_contents_item",
                     Text.literal(count+"").withColor(profile.getArchetype().getColor()),
                     item.getName().copy().withColor(profile.getSubArchetype().getColor())
               ).formatted(Formatting.DARK_PURPLE));
            }
         }
         
         if(leftOverCount > 0){
            lore.add(Text.translatable("text.ancestralarchetypes.backpack_contents_more",
                  Text.literal(leftOverCount+"").withColor(profile.getArchetype().getColor()),
                  Text.literal((cargo.size()-numDisplayed)+"").withColor(profile.getArchetype().getColor())
            ).formatted(Formatting.LIGHT_PURPLE));
         }
      }
      
      superStack.set(DataComponentTypes.LORE,new LoreComponent(lore.stream().map(TextUtils::removeItalics).collect(Collectors.toCollection(ArrayList::new))));
      return superStack;
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      BackpackGui gui = new BackpackGui(player, profile.getBackpackInventory());
      gui.open();
      profile(player).setAbilityCooldown(this.ability,10);
      return ActionResult.SUCCESS;
   }
   
   @Override
   public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity user) {
      if(!(user instanceof ServerPlayerEntity player)) return false;
      IArchetypeProfile profile = profile(player);
      if(!profile.hasAbility(this.ability)) return false;
      SimpleInventory backpackInventory = profile.getBackpackInventory();
      ItemStack backpackStack = slot.getStack();
      if (clickType == ClickType.LEFT && !backpackStack.isEmpty()) {
         ItemStack insertStack = slot.getStack();
         
         if (!BackpackSlot.isValidItem(insertStack)) {
            SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
         } else {
            int count = insertStack.getCount();
            ItemStack remainder = backpackInventory.addStack(insertStack);
            if(count == remainder.getCount()){
               SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
            }else{
               if (!remainder.isEmpty()) {
                  insertStack.setCount(remainder.getCount());
               }else{
                  insertStack.setCount(0);
               }
               SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + player.getWorld().getRandom().nextFloat() * 0.4F);
            }
         }
         
         this.onContentChanged(player, stack);
         return true;
      } else if (clickType == ClickType.RIGHT && backpackStack.isEmpty()) {
         for(int i = backpackInventory.size()-1; i >= 0; i--){
            ItemStack removeStack = backpackInventory.getStack(i).copy();
            if(!removeStack.isEmpty()){
               int count = removeStack.getCount();
               ItemStack remainderStack = slot.insertStack(removeStack);
               if (remainderStack.getCount() > 0) {
                  backpackInventory.removeStack(i,count-remainderStack.getCount());
               } else {
                  backpackInventory.removeStack(i);
                  SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getWorld().getRandom().nextFloat() * 0.4F);
               }
               break;
            }
         }
         this.onContentChanged(player, stack);
         return true;
      } else {
         return false;
      }
   }
   
   @Override
   public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity user, StackReference cursorStackReference) {
      if(!(user instanceof ServerPlayerEntity player)) return false;
      IArchetypeProfile profile = profile(player);
      if(!profile.hasAbility(this.ability)) return false;
      SimpleInventory backpackInventory = profile.getBackpackInventory();
      
      if (clickType == ClickType.LEFT && otherStack.isEmpty()) {
         return false;
      }
      
      if (clickType == ClickType.LEFT) {
         if(!BackpackSlot.isValidItem(otherStack)){
            SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
         }else{
            int count = otherStack.getCount();
            ItemStack remainder = backpackInventory.addStack(otherStack);
            if(count == remainder.getCount()){
               SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
            }else{
               if (!remainder.isEmpty()) {
                  otherStack.setCount(remainder.getCount());
               }else{
                  otherStack.setCount(0);
               }
               SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + player.getWorld().getRandom().nextFloat() * 0.4F);
            }
         }
         this.onContentChanged(player, stack);
         return true;
      } else if (clickType == ClickType.RIGHT && otherStack.isEmpty()) {
         for(int i = backpackInventory.size()-1; i >= 0; i--){
            ItemStack removeStack = backpackInventory.getStack(i);
            if(!removeStack.isEmpty()){
               SoundUtils.playSongToPlayer(player,SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getWorld().getRandom().nextFloat() * 0.4F);
               cursorStackReference.set(backpackInventory.removeStack(i));
               break;
            }
         }
         this.onContentChanged(player, stack);
         return true;
      } else {
         return false;
      }
   }
   
   private void onContentChanged(ServerPlayerEntity user, ItemStack stack) {
      ScreenHandler screenHandler = user.currentScreenHandler;
      if (screenHandler != null) {
         screenHandler.onContentChanged(user.getInventory());
      }
   }
   
   private static Pair<Inventory,ItemStack> tryAddStackToInventory(Inventory inventory, ItemStack stack){
      int size = inventory.size();
      List<ItemStack> invList = new ArrayList<>(size);
      for(int i = 0; i < size; i++){
         ItemStack invStack = inventory.getStack(i);
         invList.add(invStack);
      }
      
      // Fill up existing slots first
      for(ItemStack existingStack : invList){
         int curCount = stack.getCount();
         if(stack.isEmpty()) break;
         boolean canCombine = !existingStack.isEmpty()
               && ItemStack.areItemsAndComponentsEqual(existingStack, stack)
               && existingStack.isStackable()
               && existingStack.getCount() < existingStack.getMaxCount();
         if(!canCombine) continue;
         int toAdd = Math.min(existingStack.getMaxCount() - existingStack.getCount(),curCount);
         existingStack.increment(toAdd);
         stack.setCount(curCount - toAdd);
      }
      
      int nonEmpty = (int) invList.stream().filter(s -> !s.isEmpty()).count();
      
      if(!stack.isEmpty() && nonEmpty < size){
         if(nonEmpty == invList.size()){ // No middle empty slots, append new slot to end
            invList.add(stack.copyAndEmpty());
         }else{
            for(int i = 0; i < nonEmpty; i++){ // Find middle empty slot to fill
               if(invList.get(i).isEmpty()){
                  invList.set(i, stack.copyAndEmpty());
                  break;
               }
            }
         }
      }
      
      for(int i = 0; i < size; i++){
         inventory.setStack(i,invList.get(i));
      }
      
      return new Pair<>(inventory,stack);
   }
   
   public List<Pair<Item,Integer>> getCargoList(ServerPlayerEntity player){
      List<Pair<Item,Integer>> list = new ArrayList<>();
      IArchetypeProfile profile = profile(player);
      if(!profile.hasAbility(this.ability)) return list;
      Inventory backpackInventory = profile.getBackpackInventory();
      for(ItemStack stack : backpackInventory){
         if(stack.isEmpty()) continue;
         Item item = stack.getItem();
         boolean found = false;
         for(Pair<Item, Integer> pair : list){
            if(pair.getLeft() == item){
               pair.setRight(pair.getRight() + stack.getCount());
               found = true;
               break;
            }
         }
         if(!found){
            list.add(new Pair<>(item,stack.getCount()));
         }
      }
      list.sort((pair1, pair2) -> pair2.getRight().compareTo(pair1.getRight()));
      return list;
   }
}
