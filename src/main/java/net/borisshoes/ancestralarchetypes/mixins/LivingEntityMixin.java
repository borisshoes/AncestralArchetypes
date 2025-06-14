package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.MountInventoryGui;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.items.GraphicalItem;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageRecord;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin {
   
   @Shadow public abstract void playSound(@Nullable SoundEvent sound);
   
   @Inject(method="onEquipStack", at=@At("HEAD"), cancellable = true)
   private void archetypes_equipSoundSpam(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo ci){
      if(oldStack.getItem() instanceof AbilityItem && newStack.getItem() instanceof AbilityItem){
         ci.cancel();
      }
   }
   
   @ModifyReturnValue(method = "shouldDropExperience", at = @At("RETURN"))
   private boolean archetypes_mountDropsXP(boolean original){
      if(!original) return false;
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) return false;
      return original;
   }
   
   @ModifyReturnValue(method = "shouldDropLoot", at = @At("RETURN"))
   private boolean archetypes_mountDropsLoot(boolean original){
      if(!original) return false;
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) return false;
      return original;
   }
   
   @Inject(method = "remove", at = @At("HEAD"))
   private void archetypes_removeMount(Entity.RemovalReason reason, CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(tags.isEmpty()) return;
      
      if(entity instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity player && entity.getWorld() instanceof ServerWorld entityWorld){
         IArchetypeProfile profile = profile(player);
         ArchetypeAbility ability = MiscUtils.abilityFromTag(tags.getFirst());
         
         if(ability != null){
            UUID mountId = profile.getMountEntity(ability);
            if(mountId != null && entity.getUuid().equals(mountId)){
               if(reason == Entity.RemovalReason.KILLED){
                  profile.setMountHealth(ability, 0);
                  profile.setAbilityCooldown(ability, ArchetypeConfig.getInt(ArchetypeRegistry.SPIRIT_MOUNT_KILL_COOLDOWN));
               }
               player.sendMessage(Text.translatable("text.ancestralarchetypes.spirit_mount_unsummon").formatted(Formatting.RED,Formatting.ITALIC),true);
            }
         }
      }
   }
   
   @Inject(method = "tick", at = @At("HEAD"))
   private void archetypes_tickMount(CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(tags.isEmpty()) return;
      if(entity instanceof AnimalEntity animal){
         if(animal.getLoveTicks() > 0){
            animal.setLoveTicks(0);
         }
      }
      
      boolean remove = false;
      block: {
         if(!(entity instanceof Tameable tameable) || !(tameable.getOwner() instanceof ServerPlayerEntity player) || !(entity.getWorld() instanceof ServerWorld entityWorld)){
            remove = true;
            break block;
         }
         
         remove = !player.getServerWorld().getRegistryKey().getValue().equals(entityWorld.getRegistryKey().getValue());
         IArchetypeProfile profile = profile(player);
         ArchetypeAbility ability = MiscUtils.abilityFromTag(tags.getFirst());
         if(entity.getControllingPassenger() != null && !entity.getControllingPassenger().equals(tameable.getOwner())) entity.removeAllPassengers();
         List<Entity> passengers = entity.getPassengerList();
         for(Entity passenger : passengers){
            if(passenger instanceof ServerPlayerEntity otherPlayer){
               if(otherPlayer.currentScreenHandler instanceof HorseScreenHandler){
                  otherPlayer.closeHandledScreen();
                  if(tags.getFirst().contains(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT.getId())){
                     MountInventoryGui gui = new MountInventoryGui(player, profile.getMountInventory());
                     gui.open();
                  }else{
                     SimpleGui gui = new SimpleGui(ScreenHandlerType.HOPPER,player,false);
                     for(int i = 0; i < gui.getSize(); i++){
                        gui.setSlot(i, GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.GraphicItems.BLACK)).hideTooltip());
                     }
                     gui.setTitle(Text.translatable("container.inventory"));
                     gui.open();
                  }
                  
               }
            }
         }
         
         if(ability == null || player.distanceTo(entity) > 64){
            remove = true;
            break block;
         }
         UUID mountId = profile.getMountEntity(ability);
         if(mountId == null || !mountId.equals(entity.getUuid())){
            remove = true;
         }
      }
      
      if(remove){
         entity.discard();
      }else if(entity.age % 100 == 0){
         entity.heal((float) ArchetypeConfig.getDouble(ArchetypeRegistry.SPIRIT_MOUNT_REGENERATION_RATE));
      }
   }
   
   @ModifyReturnValue(method = "tryUseDeathProtector", at = @At("RETURN"))
   private boolean archetypes_deathProtector(boolean original){
      LivingEntity entity = (LivingEntity) (Object) this;
      if(original) return true;
      
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && profile.getDeathReductionSizeLevel() <= 1){
            profile.changeDeathReductionSizeLevel(false);
            player.getServerWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,player.getX(), player.getY()+player.getHeight()/2, player.getZ(), 100, 0.15, 0.15, 0.15, 0.3);
            playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE);
            return true;
         }else if(profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && profile.getDeathReductionSizeLevel() <= 1){
            profile.changeDeathReductionSizeLevel(false);
            player.getServerWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,player.getX(), player.getY()+player.getHeight()/2, player.getZ(), 100, 0.15, 0.15, 0.15, 0.3);
            playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE);
            return true;
         }
      }
      return original;
   }
   
   @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private double archetypes_knockback(double strength){
      LivingEntity entity = (LivingEntity) (Object) this;
      
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.INCREASED_KNOCKBACK)){
            return strength * (float) ArchetypeConfig.getDouble(ArchetypeRegistry.KNOCKBACK_INCREASE);
         }
         if(profile.hasAbility(ArchetypeRegistry.REDUCED_KNOCKBACK)){
            return strength * (float) ArchetypeConfig.getDouble(ArchetypeRegistry.KNOCKBACK_DECREASE);
         }
      }
      return strength;
   }
   
   @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
   private void archetypes_damage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
      LivingEntity entity = (LivingEntity) (Object) this;
      
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.NO_FALL_DAMAGE) && source.isIn(DamageTypeTags.IS_FALL)){
            cir.setReturnValue(false);
         }
      }
   }
   
   
   @ModifyReturnValue(method = "modifyAppliedDamage", at = @At("RETURN"))
   private float archetypes_modifyDamage(float original, DamageSource source, float amount){
      float newReturn = original;
      LivingEntity entity = (LivingEntity) (Object) this;
      Entity attacker = source.getAttacker();
      
      if(attacker instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         ItemStack weapon = source.getWeaponStack();
         
         if(profile.hasAbility(ArchetypeRegistry.SOFT_HITTER) && source.isDirect()){
            newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.SOFT_HITTER_DAMAGE_REDUCTION);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HARD_HITTER) && source.isDirect()){
            newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.HARD_HITTER_DAMAGE_INCREASE);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.SNEAK_ATTACK)){
            if(entity instanceof ServerPlayerEntity target){
               Vec3d targetEyePos = target.getEyePos();
               Vec3d targetRot = target.getRotationVecClient();
               Vec3d attackerEyePos = player.getEyePos();
               Vec3d eyeDiff = attackerEyePos.subtract(targetEyePos).normalize();
               double thresh = Math.cos(Math.toRadians(60));
               double dp = eyeDiff.dotProduct(targetRot.normalize().negate());
               boolean behind = dp >= thresh;
               if(behind){
                  newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.PLAYER_SNEAK_ATTACK_MODIFIER);
                  DustColorTransitionParticleEffect particle = new DustColorTransitionParticleEffect(0xee1c1c,0x621313,1.0f);
                  player.getServerWorld().spawnParticles(particle,entity.getX(), entity.getY()+entity.getHeight()/2, entity.getZ(), 25, 0.25, 0.25, 0.25, 0.5);
               }
            }else{
               boolean foundAttacker = false;
               for(DamageRecord damageRecord : entity.getDamageTracker().recentDamage){
                  if(player.equals(damageRecord.damageSource().getAttacker()) || player.equals(damageRecord.damageSource().getSource())){
                     foundAttacker = true;
                     break;
                  }
               }
               if(!foundAttacker){
                  newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.MOB_SNEAK_ATTACK_MODIFIER);
                  DustColorTransitionParticleEffect particle = new DustColorTransitionParticleEffect(0xee1c1c,0x621313,1.0f);
                  player.getServerWorld().spawnParticles(particle,entity.getX(), entity.getY()+entity.getHeight()/2, entity.getZ(), 25, 0.25, 0.25, 0.25, 0.5);
               }
            }
         }
      }
      
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         ItemStack weapon = source.getWeaponStack();
         
         if(profile.hasAbility(ArchetypeRegistry.IMPALE_VULNERABLE) && weapon != null){
            int impaleLvl = EnchantmentHelper.getLevel(MiscUtils.getEnchantment(Enchantments.IMPALING), weapon);
            newReturn += (float) ArchetypeConfig.getDouble(ArchetypeRegistry.IMPALE_VULNERABLE_MODIFIER) * impaleLvl;
         }
         if(profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD) && source.isIn(DamageTypeTags.IS_FREEZING)){
            newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.COLD_DAMAGE_MODIFIER);
         }
         if(profile.hasAbility(ArchetypeRegistry.HALVED_FALL_DAMAGE) && source.isIn(DamageTypeTags.IS_FALL)){
            newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.FALL_DAMAGE_REDUCTION);
         }
         if(profile.hasAbility(ArchetypeRegistry.PROJECTILE_RESISTANT) && source.isIn(DamageTypeTags.IS_PROJECTILE)){
            newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.PROJECTILE_RESISTANT_REDUCTION);
         }
         if(profile.hasAbility(ArchetypeRegistry.SLIPPERY) && player.isTouchingWaterOrRain()){
            if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER)){
               newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.GREAT_SWIMMER_SLIPPERY_DAMAGE_MODIFIER);
            }else{
               newReturn *= (float) ArchetypeConfig.getDouble(ArchetypeRegistry.SLIPPERY_DAMAGE_MODIFIER);
            }
         }
         if(profile.hasAbility(ArchetypeRegistry.INSATIABLE) && source.isOf(DamageTypes.STARVE)){
            newReturn += (float) ArchetypeConfig.getDouble(ArchetypeRegistry.ADDED_STARVE_DAMAGE);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.STUNNED_BY_DAMAGE) && amount > ArchetypeConfig.getDouble(ArchetypeRegistry.STARTLE_MIN_DAMAGE) && !source.isIn(ArchetypeRegistry.NO_STARTLE)){
            StatusEffectInstance slowness = new StatusEffectInstance(StatusEffects.SLOWNESS, ArchetypeConfig.getInt(ArchetypeRegistry.DAMAGE_STUN_DURATION), 9, false, false, true);
            player.addStatusEffect(slowness);
         }
      }
      
      return newReturn;
   }
}
