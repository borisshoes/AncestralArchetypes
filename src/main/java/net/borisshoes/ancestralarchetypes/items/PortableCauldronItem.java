package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.PotionSelectionGui;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.concurrent.atomic.AtomicInteger;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class PortableCauldronItem extends AbilityItem{
   
   public PortableCauldronItem(Settings settings){
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
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      if(player.isSneaking()){
         profile.setPotionType(null);
         PotionSelectionGui gui = new PotionSelectionGui(player);
         gui.open();
      }else{
         ItemStack potionStack = profile.getPotionStack();
         if(potionStack.isEmpty() || (potionStack.contains(DataComponentTypes.POTION_CONTENTS) && !PotionSelectionGui.isUnlocked(player, potionStack.get(DataComponentTypes.POTION_CONTENTS).potion().orElse(null)))){
            profile.setPotionType(null);
            PotionSelectionGui gui = new PotionSelectionGui(player);
            gui.open();
            return ActionResult.SUCCESS;
         }
         
         ItemStack stack = user.getStackInHand(hand);
         boolean hasConsumable = stack.contains(DataComponentTypes.CONSUMABLE);
         if(hasConsumable){
            player.setCurrentHand(hand);
         }else{
            if(potionStack.isOf(Items.SPLASH_POTION)){
               ProjectileEntity.spawnWithVelocity(SplashPotionEntity::new, player.getEntityWorld(), potionStack, user, -20.0f, 0.7f, 0.25f);
            }else if(potionStack.isOf(Items.LINGERING_POTION)){
               ProjectileEntity.spawnWithVelocity(LingeringPotionEntity::new, player.getEntityWorld(), potionStack, user, -20.0f, 0.7f, 0.25f);
            }
            PotionContentsComponent potionComp = potionStack.get(DataComponentTypes.POTION_CONTENTS);
            AtomicInteger totalDuration = new AtomicInteger();
            potionComp.forEachEffect(effect -> {
               totalDuration.addAndGet(effect.getDuration());
            }, 1);
            profile.setAbilityCooldown(this.ability, (int) Math.max(CONFIG.getInt(ArchetypeRegistry.CAULDRON_INSTANT_EFFECT_COOLDOWN),totalDuration.get()*CONFIG.getDouble(ArchetypeRegistry.CAULDRON_THROWABLE_COOLDOWN_MODIFIER)));
         }
      }
      
      return ActionResult.SUCCESS;
   }
   
   @Override
   public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user){
      if(!(user instanceof ServerPlayerEntity player)) return stack;
      IArchetypeProfile profile = profile(player);
      ItemStack potionStack = profile.getPotionStack();
      PotionContentsComponent potionComp = potionStack.get(DataComponentTypes.POTION_CONTENTS);
      if (potionStack.isOf(Items.POTION) && potionComp != null) {
         AtomicInteger totalDuration = new AtomicInteger();
         potionComp.forEachEffect(effect -> {
            totalDuration.addAndGet(effect.getDuration());
            player.addStatusEffect(effect);
         }, 1);
         profile.setAbilityCooldown(this.ability, (int) Math.max(CONFIG.getInt(ArchetypeRegistry.CAULDRON_INSTANT_EFFECT_COOLDOWN),totalDuration.get()*CONFIG.getDouble(ArchetypeRegistry.CAULDRON_DRINKABLE_COOLDOWN_MODIFIER)));
      }
      
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return stack;
   }
   
   @Override
   public UseAction getUseAction(ItemStack stack){
      if(stack.contains(DataComponentTypes.CONSUMABLE)){
         return UseAction.DRINK;
      }
      return UseAction.NONE;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
      ItemStack potionStack = profile.getPotionStack();
      boolean hasConsumable = stack.contains(DataComponentTypes.CONSUMABLE);
      boolean hasPotion = stack.contains(DataComponentTypes.POTION_CONTENTS);
      
      try{
         if(potionStack.isEmpty()){
            if(hasConsumable || hasPotion){
               stack.remove(DataComponentTypes.CONSUMABLE);
               stack.remove(DataComponentTypes.POTION_CONTENTS);
            }
         }else{
            boolean shouldHaveConsumable = potionStack.isOf(Items.POTION);
            if(shouldHaveConsumable && !hasConsumable){
               stack.set(DataComponentTypes.CONSUMABLE, ConsumableComponents.DRINK);
            }else if(!shouldHaveConsumable && hasConsumable){
               stack.remove(DataComponentTypes.CONSUMABLE);
            }
            
            if(!hasPotion){
               stack.set(DataComponentTypes.POTION_CONTENTS,potionStack.get(DataComponentTypes.POTION_CONTENTS));
            }else{
               RegistryEntry<Potion> profilePotion = potionStack.get(DataComponentTypes.POTION_CONTENTS).potion().get();
               RegistryEntry<Potion> stackPotion = stack.get(DataComponentTypes.POTION_CONTENTS).potion().get();
               if(profilePotion.value() != stackPotion.value()){
                  stack.set(DataComponentTypes.POTION_CONTENTS,potionStack.get(DataComponentTypes.POTION_CONTENTS));
               }
            }
         }
      }catch(Exception e){
         log(2, e.toString());
      }
   }
}
