package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.gui.PotionSelectionGui;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.concurrent.atomic.AtomicInteger;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class PortableCauldronItem extends AbilityItem{
   
   public PortableCauldronItem(Properties settings){
      super(ArchetypeRegistry.POTION_BREWER, "\uD83E\uDDEA", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.POTION;
      }else{
         return Items.CAULDRON;
      }
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
      
      if(player.isShiftKeyDown()){
         profile.setPotionType(null);
         PotionSelectionGui gui = new PotionSelectionGui(player);
         gui.open();
      }else{
         ItemStack potionStack = profile.getPotionStack();
         if(potionStack.isEmpty() || (potionStack.has(DataComponents.POTION_CONTENTS) && !PotionSelectionGui.isUnlocked(player, potionStack.get(DataComponents.POTION_CONTENTS).potion().orElse(null)))){
            profile.setPotionType(null);
            PotionSelectionGui gui = new PotionSelectionGui(player);
            gui.open();
            return InteractionResult.SUCCESS;
         }
         
         ItemStack stack = user.getItemInHand(hand);
         boolean hasConsumable = stack.has(DataComponents.CONSUMABLE);
         if(hasConsumable){
            player.startUsingItem(hand);
         }else{
            if(potionStack.is(Items.SPLASH_POTION)){
               Projectile.spawnProjectileFromRotation(ThrownSplashPotion::new, player.level(), potionStack, user, -20.0f, 0.7f, 0.25f);
            }else if(potionStack.is(Items.LINGERING_POTION)){
               Projectile.spawnProjectileFromRotation(ThrownLingeringPotion::new, player.level(), potionStack, user, -20.0f, 0.7f, 0.25f);
            }
            PotionContents potionComp = potionStack.get(DataComponents.POTION_CONTENTS);
            AtomicInteger totalDuration = new AtomicInteger();
            potionComp.forEachEffect(effect -> {
               totalDuration.addAndGet(effect.getDuration());
            }, 1);
            profile.setAbilityCooldown(this.ability, (int) Math.max(CONFIG.getInt(ArchetypeRegistry.CAULDRON_INSTANT_EFFECT_COOLDOWN),totalDuration.get()*CONFIG.getDouble(ArchetypeRegistry.CAULDRON_THROWABLE_COOLDOWN_MODIFIER)));
         }
      }
      
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user){
      if(!(user instanceof ServerPlayer player)) return stack;
      PlayerArchetypeData profile = profile(player);
      ItemStack potionStack = profile.getPotionStack();
      PotionContents potionComp = potionStack.get(DataComponents.POTION_CONTENTS);
      if (potionStack.is(Items.POTION) && potionComp != null) {
         AtomicInteger totalDuration = new AtomicInteger();
         potionComp.forEachEffect(effect -> {
            totalDuration.addAndGet(effect.getDuration());
            player.addEffect(effect);
         }, 1);
         profile.setAbilityCooldown(this.ability, (int) Math.max(CONFIG.getInt(ArchetypeRegistry.CAULDRON_INSTANT_EFFECT_COOLDOWN),totalDuration.get()*CONFIG.getDouble(ArchetypeRegistry.CAULDRON_DRINKABLE_COOLDOWN_MODIFIER)));
      }
      
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return stack;
   }
   
   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack){
      if(stack.has(DataComponents.CONSUMABLE)){
         return ItemUseAnimation.DRINK;
      }
      return ItemUseAnimation.NONE;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      ItemStack potionStack = profile.getPotionStack();
      boolean hasConsumable = stack.has(DataComponents.CONSUMABLE);
      boolean hasPotion = stack.has(DataComponents.POTION_CONTENTS);
      
      try{
         if(potionStack.isEmpty()){
            if(hasConsumable || hasPotion){
               stack.remove(DataComponents.CONSUMABLE);
               stack.remove(DataComponents.POTION_CONTENTS);
            }
         }else{
            boolean shouldHaveConsumable = potionStack.is(Items.POTION);
            if(shouldHaveConsumable && !hasConsumable){
               stack.set(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK);
            }else if(!shouldHaveConsumable && hasConsumable){
               stack.remove(DataComponents.CONSUMABLE);
            }
            
            if(!hasPotion){
               stack.set(DataComponents.POTION_CONTENTS,potionStack.get(DataComponents.POTION_CONTENTS));
            }else{
               Holder<Potion> profilePotion = potionStack.get(DataComponents.POTION_CONTENTS).potion().get();
               Holder<Potion> stackPotion = stack.get(DataComponents.POTION_CONTENTS).potion().get();
               if(profilePotion.value() != stackPotion.value()){
                  stack.set(DataComponents.POTION_CONTENTS,potionStack.get(DataComponents.POTION_CONTENTS));
               }
            }
         }
      }catch(Exception e){
         log(2, e.toString());
      }
   }
}
