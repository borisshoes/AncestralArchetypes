package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.gui.BackpackGui;
import net.borisshoes.ancestralarchetypes.gui.BackpackSlot;
import net.borisshoes.borislib.utils.SoundUtils;
import net.borisshoes.borislib.utils.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class BackpackItem extends AbilityItem{
   public BackpackItem(Properties settings){
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
   public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context){
      ItemStack superStack = super.getPolymerItemStack(itemStack, tooltipType, context);
      
      if(!(context.getPlayer() instanceof ServerPlayer player)) return superStack;
      PlayerArchetypeData profile = profile(player);
      if(!profile.hasAbility(this.ability)) return superStack;
      
      List<MutableComponent> lore = new ArrayList<>();
      lore.add(Component.translatable("text.ancestralarchetypes.backpack_description").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
      lore.add(Component.literal(""));
      List<Tuple<Item,Integer>> cargo = getCargoList(player);
      final int numDisplayed = 18;
      
      if(cargo.isEmpty()){
         lore.add(Component.translatable("text.ancestralarchetypes.backpack_contents_empty").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
      }else{
         lore.add(Component.translatable("text.ancestralarchetypes.backpack_contents").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
         int leftOverCount = 0;
         for(int i = 0; i < cargo.size(); i++){
            int count = cargo.get(i).getB();
            if(i >= numDisplayed){
               leftOverCount += count;
               continue;
            }
            
            Item item = cargo.get(i).getA();
            int stacks = count / item.getDefaultMaxStackSize();
            int leftover = count % item.getDefaultMaxStackSize();
            
            if(count > item.getDefaultMaxStackSize()){
               lore.add(Component.translatable("text.ancestralarchetypes.backpack_contents_stack",
                     Component.literal(count+"").withColor(profile.getArchetype().color()),
                     Component.translatable("text.ancestralarchetypes.stacks",stacks).withColor(profile.getArchetype().color()),
                     Component.literal(leftover+"").withColor(profile.getArchetype().color()),
                     item.getName().copy().withColor(profile.getSubArchetype().getColor())
                     ).withStyle(ChatFormatting.DARK_PURPLE));
            }else{
               lore.add(Component.translatable("text.ancestralarchetypes.backpack_contents_item",
                     Component.literal(count+"").withColor(profile.getArchetype().color()),
                     item.getName().copy().withColor(profile.getSubArchetype().getColor())
               ).withStyle(ChatFormatting.DARK_PURPLE));
            }
         }
         
         if(leftOverCount > 0){
            lore.add(Component.translatable("text.ancestralarchetypes.backpack_contents_more",
                  Component.literal(leftOverCount+"").withColor(profile.getArchetype().color()),
                  Component.literal((cargo.size()-numDisplayed)+"").withColor(profile.getArchetype().color())
            ).withStyle(ChatFormatting.LIGHT_PURPLE));
         }
      }
      
      superStack.set(DataComponents.LORE,new ItemLore(lore.stream().map(TextUtils::removeItalics).collect(Collectors.toCollection(ArrayList::new))));
      return superStack;
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
         return InteractionResult.PASS;
      }
      BackpackGui gui = new BackpackGui(player, profile.getBackpackInventory());
      gui.open();
      profile(player).setAbilityCooldown(this.ability,10);
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, Player user) {
      if(!(user instanceof ServerPlayer player)) return false;
      PlayerArchetypeData profile = profile(player);
      if(!profile.hasAbility(this.ability)) return false;
      SimpleContainer backpackInventory = profile.getBackpackInventory();
      ItemStack backpackStack = slot.getItem();
      if (clickType == ClickAction.PRIMARY && !backpackStack.isEmpty()) {
         ItemStack insertStack = slot.getItem();
         
         if (!BackpackSlot.isValidItem(insertStack)) {
            SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
         } else {
            int count = insertStack.getCount();
            ItemStack remainder = backpackInventory.addItem(insertStack);
            if(count == remainder.getCount()){
               SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
            }else{
               if (!remainder.isEmpty()) {
                  insertStack.setCount(remainder.getCount());
               }else{
                  insertStack.setCount(0);
               }
               SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
            }
         }
         
         this.onContentChanged(player, stack);
         return true;
      } else if (clickType == ClickAction.SECONDARY && backpackStack.isEmpty()) {
         for(int i = backpackInventory.getContainerSize()-1; i >= 0; i--){
            ItemStack removeStack = backpackInventory.getItem(i).copy();
            if(!removeStack.isEmpty()){
               int count = removeStack.getCount();
               ItemStack remainderStack = slot.safeInsert(removeStack);
               if (remainderStack.getCount() > 0) {
                  backpackInventory.removeItem(i,count-remainderStack.getCount());
               } else {
                  backpackInventory.removeItemNoUpdate(i);
                  SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
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
   public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player user, SlotAccess cursorStackReference) {
      if(!(user instanceof ServerPlayer player)) return false;
      PlayerArchetypeData profile = profile(player);
      if(!profile.hasAbility(this.ability)) return false;
      SimpleContainer backpackInventory = profile.getBackpackInventory();
      
      if (clickType == ClickAction.PRIMARY && otherStack.isEmpty()) {
         return false;
      }
      
      if (clickType == ClickAction.PRIMARY) {
         if(!BackpackSlot.isValidItem(otherStack)){
            SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
         }else{
            int count = otherStack.getCount();
            ItemStack remainder = backpackInventory.addItem(otherStack);
            if(count == remainder.getCount()){
               SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
            }else{
               if (!remainder.isEmpty()) {
                  otherStack.setCount(remainder.getCount());
               }else{
                  otherStack.setCount(0);
               }
               SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
            }
         }
         this.onContentChanged(player, stack);
         return true;
      } else if (clickType == ClickAction.SECONDARY && otherStack.isEmpty()) {
         for(int i = backpackInventory.getContainerSize()-1; i >= 0; i--){
            ItemStack removeStack = backpackInventory.getItem(i);
            if(!removeStack.isEmpty()){
               SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
               cursorStackReference.set(backpackInventory.removeItemNoUpdate(i));
               break;
            }
         }
         this.onContentChanged(player, stack);
         return true;
      } else {
         return false;
      }
   }
   
   private void onContentChanged(ServerPlayer user, ItemStack stack) {
      AbstractContainerMenu screenHandler = user.containerMenu;
      if (screenHandler != null) {
         screenHandler.slotsChanged(user.getInventory());
      }
   }
   
   private static Tuple<Container, ItemStack> tryAddStackToInventory(Container inventory, ItemStack stack){
      int size = inventory.getContainerSize();
      List<ItemStack> invList = new ArrayList<>(size);
      for(int i = 0; i < size; i++){
         ItemStack invStack = inventory.getItem(i);
         invList.add(invStack);
      }
      
      // Fill up existing slots first
      for(ItemStack existingStack : invList){
         int curCount = stack.getCount();
         if(stack.isEmpty()) break;
         boolean canCombine = !existingStack.isEmpty()
               && ItemStack.isSameItemSameComponents(existingStack, stack)
               && existingStack.isStackable()
               && existingStack.getCount() < existingStack.getMaxStackSize();
         if(!canCombine) continue;
         int toAdd = Math.min(existingStack.getMaxStackSize() - existingStack.getCount(),curCount);
         existingStack.grow(toAdd);
         stack.setCount(curCount - toAdd);
      }
      
      int nonEmpty = (int) invList.stream().filter(s -> !s.isEmpty()).count();
      
      if(!stack.isEmpty() && nonEmpty < size){
         if(nonEmpty == invList.size()){ // No middle empty slots, append new slot to end
            invList.add(stack.copyAndClear());
         }else{
            for(int i = 0; i < nonEmpty; i++){ // Find middle empty slot to fill
               if(invList.get(i).isEmpty()){
                  invList.set(i, stack.copyAndClear());
                  break;
               }
            }
         }
      }
      
      for(int i = 0; i < size; i++){
         inventory.setItem(i,invList.get(i));
      }
      
      return new Tuple<>(inventory,stack);
   }
   
   public List<Tuple<Item,Integer>> getCargoList(ServerPlayer player){
      List<Tuple<Item,Integer>> list = new ArrayList<>();
      PlayerArchetypeData profile = profile(player);
      if(!profile.hasAbility(this.ability)) return list;
      Container backpackInventory = profile.getBackpackInventory();
      for(ItemStack stack : backpackInventory){
         if(stack.isEmpty()) continue;
         Item item = stack.getItem();
         boolean found = false;
         for(Tuple<Item, Integer> pair : list){
            if(pair.getA() == item){
               pair.setB(pair.getB() + stack.getCount());
               found = true;
               break;
            }
         }
         if(!found){
            list.add(new Tuple<>(item,stack.getCount()));
         }
      }
      list.sort((pair1, pair2) -> pair2.getB().compareTo(pair1.getB()));
      return list;
   }
}
