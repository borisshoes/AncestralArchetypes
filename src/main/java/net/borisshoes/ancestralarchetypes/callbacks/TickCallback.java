package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;


public class TickCallback {
   public static void onTick(MinecraftServer server){
      
      for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
         IArchetypeProfile profile = profile(player);
         ServerWorld world = player.getServerWorld();
         
         if(PLAYER_MOVEMENT_TRACKER.containsKey(player) && !player.isDead()){
            Pair<Vec3d,Vec3d> tracker = PLAYER_MOVEMENT_TRACKER.get(player);
            Vec3d oldPos = tracker.getLeft();
            Vec3d newPos = player.getPos();
            PLAYER_MOVEMENT_TRACKER.put(player,new Pair<>(newPos, newPos.subtract(oldPos)));
         }else{
            PLAYER_MOVEMENT_TRACKER.put(player,new Pair<>(player.getPos(), new Vec3d(0,0,0)));
         }
         
         PlayerInventory inv = player.getInventory();
         ItemStack mainhand = player.getMainHandStack();
         ItemStack offhand = player.getOffHandStack();
         for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getStack(i);
            HashMap<Item, Pair<Float,Integer>> map = null;
            boolean unusualFood = ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem()) || ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem()) || ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem());
            if(profile.hasAbility(ArchetypeRegistry.TUFF_EATER) && ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())){
               map = ArchetypeRegistry.TUFF_FOODS;
            }
            if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) && ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())){
               map = ArchetypeRegistry.COPPER_FOODS;
            }
            if(profile.hasAbility(ArchetypeRegistry.IRON_EATER) && ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())){
               map = ArchetypeRegistry.IRON_FOODS;
            }
            
            boolean shouldHaveEatComponent = (stack.equals(mainhand) || stack.equals(offhand)) && map != null && player.getHealth() < player.getMaxHealth();
            if(shouldHaveEatComponent){ // Add component
               if(!stack.contains(DataComponentTypes.CONSUMABLE)){
                  Pair<Float,Integer> pair = map.get(stack.getItem());
                  ConsumableComponent comp = ConsumableComponent.builder().sound(SoundEvents.ENTITY_GENERIC_EAT).useAction(UseAction.EAT).consumeSeconds(pair.getRight()/20.0f).consumeParticles(true).build();
                  stack.set(DataComponentTypes.CONSUMABLE,comp);
               }
            }else if(unusualFood && stack.contains(DataComponentTypes.CONSUMABLE)){ // Remove component
               stack.remove(DataComponentTypes.CONSUMABLE);
            }
         }
         
         profile.tick();
         
         if(player.isUsingItem()){
            if(player.getActiveItem().contains(DataComponentTypes.FOOD)){
               boolean stopUsing = false;
               if(profile.hasAbility(ArchetypeRegistry.CARNIVORE) && !player.getActiveItem().isIn(ArchetypeRegistry.CARNIVORE_FOODS)){
                  stopUsing = true;
               }else if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) || profile.hasAbility(ArchetypeRegistry.IRON_EATER) || profile.hasAbility(ArchetypeRegistry.TUFF_EATER)){
                  stopUsing = true;
               }
               
               if(stopUsing){
                  player.getItemCooldownManager().set(player.getActiveItem(),20);
                  player.stopUsingItem();
                  player.sendMessage(Text.translatable("text.ancestralarchetypes.cannot_eat").formatted(Formatting.RED,Formatting.ITALIC),true);
                  SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_PLAYER_BURP,1,0.5f);
               }
            }
         }
         
         
         if(server.getTicks() % 10 == 0){
            if(profile.hasAbility(ArchetypeRegistry.GOOD_SWIMMER) || profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER)){
               if(player.isSubmergedInWater()){
                  StatusEffectInstance conduitPower = new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 110, 0, false, false, true);
                  player.addStatusEffect(conduitPower);
               }
               float level = profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) ? 1.5f : 1f;
               MiscUtils.attributeEffect(player, EntityAttributes.WATER_MOVEMENT_EFFICIENCY, level, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"swim_buff"),false);
            }else{
               MiscUtils.attributeEffect(player, EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1f, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"swim_buff"),true);
            }
            
            if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER)){
               if(player.isSubmergedInWater()){
                  StatusEffectInstance grace = new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 110, 0, false, false, true);
                  player.addStatusEffect(grace);
               }
            }
            
            
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE, -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
            MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
            
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE, 0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"tall_sized"),!profile.hasAbility(ArchetypeRegistry.TALL_SIZED));
            MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"tall_sized"),!profile.hasAbility(ArchetypeRegistry.TALL_SIZED));
            
            if(profile.hasAbility(ArchetypeRegistry.FIRE_IMMUNE)){
               StatusEffectInstance fireRes = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 110, 0, false, false, true);
               player.addStatusEffect(fireRes);
               if(player.isOnFire()) player.extinguish();
            }
            
            if(profile.hasAbility(ArchetypeRegistry.SPEEDY)){
               StatusEffectInstance speed = new StatusEffectInstance(StatusEffects.SPEED, 110, 0, false, false, true);
               player.addStatusEffect(speed);
            }
            
            if(profile.hasAbility(ArchetypeRegistry.JUMPY)){
               StatusEffectInstance jumpBoost = new StatusEffectInstance(StatusEffects.JUMP_BOOST, 110, 2, false, false, true);
               player.addStatusEffect(jumpBoost);
            }
            
            if(profile.hasAbility(ArchetypeRegistry.HASTY)){
               StatusEffectInstance haste = new StatusEffectInstance(StatusEffects.HASTE, 110, 1, false, false, true);
               player.addStatusEffect(haste);
            }
            
            if(profile.hasAbility(ArchetypeRegistry.INSATIABLE)){
               HungerManager hungerManager = player.getHungerManager();
               hungerManager.addExhaustion((float) ArchetypeConfig.getDouble(ArchetypeRegistry.INSATIATBLE_HUNGER_RATE));
            }
         }
         
         if(server.getTicks() % 40 == 0){
            RegistryEntry<Biome> biome = player.getServerWorld().getBiome(player.getBlockPos());
            float temp = biome.value().getTemperature();
            
            if(temp < 0.1 && profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD) && !player.hasStatusEffect(StatusEffects.WATER_BREATHING) && !(player.isCreative() || player.isSpectator())){
               player.damage(world, world.getDamageSources().freeze(), (float) ArchetypeConfig.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
               player.sendMessage(Text.translatable("text.ancestralarchetypes.freeze_warning").formatted(Formatting.AQUA,Formatting.ITALIC),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_PLAYER_HURT_FREEZE,1,1f);
            }
            
            if(!biome.value().hasPrecipitation() && !player.isTouchingWater() && profile.hasAbility(ArchetypeRegistry.DRIES_OUT) && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && !(player.isCreative() || player.isSpectator())){
               player.damage(world, world.getDamageSources().drown(), (float) ArchetypeConfig.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
               player.sendMessage(Text.translatable("text.ancestralarchetypes.dry_out_warning").formatted(Formatting.RED,Formatting.ITALIC),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.ITEM_FIRECHARGE_USE,1,1f);
            }
         }
         
         
         if(profile.hasAbility(ArchetypeRegistry.REGEN_WHEN_LOW) && player.getHealth() < player.getMaxHealth()/2.0 && !player.isDead()){
            player.heal((float) ArchetypeConfig.getDouble(ArchetypeRegistry.REGENERATION_RATE));
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HEALTH_BASED_SPRINT)){
            HungerManager hungerManager = player.getHungerManager();
            boolean canSprint = player.getHealth()/player.getMaxHealth() > (float) ArchetypeConfig.getDouble(ArchetypeRegistry.HEALTH_SPRINT_CUTOFF);
            if(canSprint){
               hungerManager.setFoodLevel(20);
            }else{
               hungerManager.setFoodLevel(2);
            }
         }
         
         if(profile.hasAbility(ArchetypeRegistry.SLOW_FALLER) && PLAYER_MOVEMENT_TRACKER.get(player).getRight().getY() < -0.6){
            if(!player.hasStatusEffect(StatusEffects.SLOW_FALLING)){
               SoundUtils.playSongToPlayer(player,SoundEvents.ENTITY_ENDER_DRAGON_FLAP,0.3f,1);
            }
            StatusEffectInstance slowFall = new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0, false, false, true);
            player.addStatusEffect(slowFall);
         }
         
         
         if(server.getTicks() % 18000 == 0 && profile.giveReminders()){ // Ability and Archetype check
            if(profile.getSubArchetype() == null && profile.canChangeArchetype()){
               player.sendMessage(Text.translatable("text.ancestralarchetypes.archetype_reminder").styled(s ->
                     s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/archetypes changeArchetype"))
                           .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("text.ancestralarchetypes.change_hover")))
                           .withColor(Formatting.AQUA)));
            }
            
            List<ArchetypeAbility> abilities = profile.getAbilities();
            boolean giveWarning = false;
            block: {
               for(ArchetypeAbility ability : abilities){
                  if(!ability.isActive()) continue;
                  for(Item item : ITEMS){
                     if(item instanceof AbilityItem abilityItem && ability.equals(abilityItem.ability)){
                        boolean found = false;
                        for(int i = 0; i < inv.size(); i++){
                           ItemStack stack = inv.getStack(i);
                           if(stack.isOf(abilityItem)){
                              found = true;
                              break;
                           }
                        }
                        if(!found){
                           giveWarning = true;
                           break block;
                        }
                     }
                  }
               }
            }
            if(giveWarning){
               player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_reminder").styled(s ->
                     s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/archetypes items"))
                           .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("text.ancestralarchetypes.items_hover")))
                           .withColor(Formatting.AQUA)));
            }
         }
      }
   }
}
