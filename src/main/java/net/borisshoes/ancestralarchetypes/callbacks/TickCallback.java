package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.*;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.misc.SpyglassRevealEvent;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.borisshoes.ancestralarchetypes.utils.PlayerMovementEntry;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;


public class TickCallback {
   
   public static void onTick(MinecraftServer server){
      for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
         IArchetypeProfile profile = profile(player);
         ServerWorld world = player.getServerWorld();
         
         if(profile.getHealthUpdate() != 0){
            double scale = -(1 - Math.pow(0.5,profile.getDeathReductionSizeLevel()));
            MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
            MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
            if(player.getMaxHealth() >= profile.getHealthUpdate()){
               player.setHealth(profile.getHealthUpdate());
               profile.setHealthUpdate(0);
            }
         }
         
         PlayerInventory inv = player.getInventory();
         ItemStack mainhand = player.getMainHandStack();
         ItemStack offhand = player.getOffHandStack();
         for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getStack(i);
            HashMap<Item, Pair<Float,Integer>> map = null;
            boolean unusualFood = ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())
                  || ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())
                  || ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())
                  || stack.isIn(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)
                  || stack.isIn(ArchetypeRegistry.SLIME_GROW_ITEMS);
            float durationMod = 1.0f;
            
            if(profile.hasAbility(ArchetypeRegistry.TUFF_EATER) && ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())){
               map = ArchetypeRegistry.TUFF_FOODS;
               durationMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.TUFF_FOOD_DURATION_MODIFIER);
            }
            if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) && ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())){
               map = ArchetypeRegistry.COPPER_FOODS;
               durationMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.COPPER_FOOD_DURATION_MODIFIER);
            }
            if(profile.hasAbility(ArchetypeRegistry.IRON_EATER) && ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())){
               map = ArchetypeRegistry.IRON_FOODS;
               durationMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.IRON_FOOD_DURATION_MODIFIER);
            }
            
            boolean inHand = stack.equals(mainhand) || stack.equals(offhand);
            boolean shouldHaveGolemEatComponent = (inHand && map != null && player.getHealth() < player.getMaxHealth());
            boolean shouldHaveGelatianEatComponent = inHand && profile.getDeathReductionSizeLevel() != 0
                  && ((profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && stack.isIn(ArchetypeRegistry.SLIME_GROW_ITEMS))
                     || (profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && stack.isIn(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)));
            if(shouldHaveGolemEatComponent){ // Add component
               if(!stack.contains(DataComponentTypes.CONSUMABLE)){
                  Pair<Float,Integer> pair = map.get(stack.getItem());
                  ConsumableComponent comp = ConsumableComponent.builder().sound(SoundEvents.ENTITY_GENERIC_EAT).useAction(UseAction.EAT).consumeSeconds(pair.getRight()/20.0f * durationMod).consumeParticles(true).build();
                  stack.set(DataComponentTypes.CONSUMABLE,comp);
               }
            }else if(shouldHaveGelatianEatComponent){
               if(!stack.contains(DataComponentTypes.CONSUMABLE)){
                  ConsumableComponent comp = ConsumableComponent.builder().sound(SoundEvents.ENTITY_GENERIC_EAT).useAction(UseAction.EAT).consumeSeconds(ArchetypeConfig.getInt(ArchetypeRegistry.GELATIAN_GROW_ITEM_EAT_DURATION)/20.0f).consumeParticles(true).build();
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
            if(profile.hasAbility(ArchetypeRegistry.GOOD_SWIMMER)){
               if(player.isSubmergedInWater()){
                  StatusEffectInstance conduitPower = new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 110, 0, false, false, true);
                  player.addStatusEffect(conduitPower);
               }
               MiscUtils.attributeEffect(player, EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1f, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"swim_buff"),false);
            }else{
               MiscUtils.attributeEffect(player, EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1f, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"swim_buff"),true);
            }
            
            if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isTouchingWaterOrRain()){
               MiscUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, ArchetypeConfig.getDouble(ArchetypeRegistry.GREAT_SWIMMER_MOVE_SPEED_MODIFIER), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"great_swimmer"),false);
            }else{
               MiscUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, ArchetypeConfig.getDouble(ArchetypeRegistry.GREAT_SWIMMER_MOVE_SPEED_MODIFIER), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"great_swimmer"),true);
            }
            
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE, -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
            MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
            
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE, 0.25, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"tall_sized"),!profile.hasAbility(ArchetypeRegistry.TALL_SIZED));
            
            MiscUtils.attributeEffect(player, EntityAttributes.SCALE, 0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"giant_sized"),!profile.hasAbility(ArchetypeRegistry.GIANT_SIZED));
            MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"giant_sized"),!profile.hasAbility(ArchetypeRegistry.GIANT_SIZED));
            
            MiscUtils.attributeEffect(player, EntityAttributes.ENTITY_INTERACTION_RANGE, ArchetypeConfig.getDouble(ArchetypeRegistry.LONG_ARMS_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"long_arms"),!profile.hasAbility(ArchetypeRegistry.LONG_ARMS));
            MiscUtils.attributeEffect(player, EntityAttributes.BLOCK_INTERACTION_RANGE, ArchetypeConfig.getDouble(ArchetypeRegistry.LONG_ARMS_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"long_arms"),!profile.hasAbility(ArchetypeRegistry.LONG_ARMS));
            
            MiscUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, ArchetypeConfig.getDouble(ArchetypeRegistry.SPEEDY_SPEED_BOOST), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"speedy"),!profile.hasAbility(ArchetypeRegistry.SPEEDY));
            
            MiscUtils.attributeEffect(player, EntityAttributes.SNEAKING_SPEED, ArchetypeConfig.getDouble(ArchetypeRegistry.SNEAKY_SPEED_BOOST), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"sneaky"),!profile.hasAbility(ArchetypeRegistry.SNEAKY));
            
            MiscUtils.attributeEffect(player, EntityAttributes.JUMP_STRENGTH, ArchetypeConfig.getDouble(ArchetypeRegistry.JUMPY_JUMP_BOOST), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"jumpy"),!profile.hasAbility(ArchetypeRegistry.JUMPY));
            MiscUtils.attributeEffect(player, EntityAttributes.SAFE_FALL_DISTANCE, ArchetypeConfig.getDouble(ArchetypeRegistry.JUMPY_JUMP_BOOST)*10, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"jumpy"),!profile.hasAbility(ArchetypeRegistry.JUMPY));
            
            MiscUtils.attributeEffect(player, EntityAttributes.ATTACK_KNOCKBACK, ArchetypeConfig.getDouble(ArchetypeRegistry.HARD_HITTER_KNOCKBACK_INCREASE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"hard_hitter"),!profile.hasAbility(ArchetypeRegistry.HARD_HITTER));
            
            MiscUtils.attributeEffect(player, EntityAttributes.ATTACK_SPEED, ArchetypeConfig.getDouble(ArchetypeRegistry.HASTY_ATTACK_SPEED_INCREASE), EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"hasty"),!profile.hasAbility(ArchetypeRegistry.HASTY));
            
            Entity vehicle = player.getVehicle();
            boolean gaveReach = false;
            if(vehicle != null){
               if(!vehicle.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
                  MiscUtils.attributeEffect(player, EntityAttributes.BLOCK_INTERACTION_RANGE, ArchetypeConfig.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),false);
                  MiscUtils.attributeEffect(player, EntityAttributes.ENTITY_INTERACTION_RANGE, ArchetypeConfig.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),false);
                  gaveReach = true;
               }
            }
            if(!gaveReach){
               MiscUtils.attributeEffect(player, EntityAttributes.BLOCK_INTERACTION_RANGE, ArchetypeConfig.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),true);
               MiscUtils.attributeEffect(player, EntityAttributes.ENTITY_INTERACTION_RANGE, ArchetypeConfig.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),true);
            }
            
            long timeOfDay = world.getTimeOfDay();
            int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
            int curPhase = day % 8;
            int moonLevel = Math.abs(-curPhase+4); // 0 - new moon, 4 - full moon
            for(int i = 0; i <= 4; i++){
               Identifier id = Identifier.of(MOD_ID,"slime_moonlit_"+i);
               if(moonLevel == i && profile.hasAbility(ArchetypeRegistry.MOONLIT) && profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM)){
                  MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, ArchetypeConfig.getDouble(ArchetypeRegistry.MOONLIT_SLIME_HEALTH_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, id,false);
                  MiscUtils.attributeEffect(player, EntityAttributes.SCALE, ArchetypeConfig.getDouble(ArchetypeRegistry.MOONLIT_SLIME_SIZE_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, id,false);
               }else{
                  MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, ArchetypeConfig.getDouble(ArchetypeRegistry.MOONLIT_SLIME_HEALTH_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, id,true);
                  MiscUtils.attributeEffect(player, EntityAttributes.SCALE, ArchetypeConfig.getDouble(ArchetypeRegistry.MOONLIT_SLIME_SIZE_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, id,true);
               }
            }
            
            if(profile.hasAbility(ArchetypeRegistry.FIRE_IMMUNE)){
               StatusEffectInstance fireRes = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 110, 0, false, false, true);
               player.addStatusEffect(fireRes);
               if(player.isOnFire()) player.extinguish();
            }
            
            if(profile.hasAbility(ArchetypeRegistry.INSATIABLE)){
               HungerManager hungerManager = player.getHungerManager();
               hungerManager.addExhaustion((float) ArchetypeConfig.getDouble(ArchetypeRegistry.INSATIATBLE_HUNGER_RATE));
            }
         }
         
         if(server.getTicks() % 40 == 0){
            RegistryEntry<Biome> biome = player.getServerWorld().getBiome(player.getBlockPos());
            float temp = biome.value().getTemperature();
            
            boolean shouldFreeze = (biome.isIn(ArchetypeRegistry.COLD_DAMAGE_INCLUDE_BIOMES) || (temp < 0.1 && !biome.isIn(ArchetypeRegistry.COLD_DAMAGE_EXCEPTION_BIOMES))) &&
                  profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD) && !player.hasStatusEffect(StatusEffects.WATER_BREATHING) && !(player.isCreative() || player.isSpectator());
            if(shouldFreeze){
               player.damage(world, world.getDamageSources().freeze(), (float) ArchetypeConfig.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
               player.sendMessage(Text.translatable("text.ancestralarchetypes.freeze_warning").formatted(Formatting.AQUA,Formatting.ITALIC),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_PLAYER_HURT_FREEZE,1,1f);
            }
            
            boolean shouldDryOut = (biome.isIn(ArchetypeRegistry.DRY_OUT_INCLUDE_BIOMES) || (!biome.value().hasPrecipitation() && !biome.isIn(ArchetypeRegistry.DRY_OUT_EXCEPTION_BIOMES))) &&
                  profile.hasAbility(ArchetypeRegistry.DRIES_OUT) && !player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) &&
                  !(player.isCreative() || player.isSpectator()) && !player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.TURTLE_HELMET);
            if(shouldDryOut){
               player.damage(world, world.getDamageSources().dryOut(), (float) ArchetypeConfig.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
               player.sendMessage(Text.translatable("text.ancestralarchetypes.dry_out_warning").formatted(Formatting.RED,Formatting.ITALIC),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.ITEM_FIRECHARGE_USE,1,1f);
            }
         }
         
         if(profile.hasAbility(ArchetypeRegistry.ANTIVENOM) && player.hasStatusEffect(StatusEffects.POISON)){
            player.removeStatusEffect(StatusEffects.POISON);
         }
         if(profile.hasAbility(ArchetypeRegistry.WITHERING) && player.hasStatusEffect(StatusEffects.WITHER)){
            player.removeStatusEffect(StatusEffects.WITHER);
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
         
         if(profile.hasAbility(ArchetypeRegistry.SLOW_FALLER)){
            int predictedFallDist = 0;
            for(int y = player.getBlockY(); y >= player.getBlockY()-player.getServerWorld().getHeight(); y--){
               BlockPos blockPos = new BlockPos(player.getBlockX(),y,player.getBlockZ());
               BlockState state = player.getServerWorld().getBlockState(blockPos);
               if(state.isAir() || state.getCollisionShape(player.getServerWorld(),blockPos).isEmpty()){
                  predictedFallDist++;
               }else{
                  break;
               }
            }
            
            double yMov = PLAYER_MOVEMENT_TRACKER.getOrDefault(player,PlayerMovementEntry.blankEntry(player)).velocity().getY();
            boolean shouldTriggerSlowFall = (yMov < -ArchetypeConfig.getDouble(ArchetypeRegistry.SLOW_FALLER_TRIGGER_SPEED))
                  && !player.isGliding() && !player.getAbilities().flying && predictedFallDist > player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE) && !player.isSwimming();
            if(shouldTriggerSlowFall){
               if(!player.isSneaking()){
                  if(!player.hasStatusEffect(StatusEffects.SLOW_FALLING)){
                     SoundUtils.playSongToPlayer(player,SoundEvents.ENTITY_ENDER_DRAGON_FLAP,0.3f,1);
                  }else{
                     if(yMov < -1.25*ArchetypeConfig.getDouble(ArchetypeRegistry.SLOW_FALLER_TRIGGER_SPEED)){
                        player.addVelocity(0,0.2,0);
                        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                     }
                  }
                  StatusEffectInstance slowFall = new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false, true);
                  player.addStatusEffect(slowFall);
               }
               player.networkHandler.floatingTicks = 0;
            }
         }
         
         
         if(server.getTicks() % 18000 == 0 && profile.giveReminders()){ // Ability and Archetype check
            if(profile.getSubArchetype() == null && profile.canChangeArchetype()){
               player.sendMessage(Text.translatable("text.ancestralarchetypes.archetype_reminder").styled(s ->
                     s.withClickEvent(new ClickEvent.RunCommand("/archetypes changeArchetype"))
                           .withHoverEvent(new HoverEvent.ShowText(Text.translatable("text.ancestralarchetypes.change_hover")))
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
                     s.withClickEvent(new ClickEvent.RunCommand("/archetypes items"))
                           .withHoverEvent(new HoverEvent.ShowText(Text.translatable("text.ancestralarchetypes.items_hover")))
                           .withColor(Formatting.AQUA)));
            }
         }
         
         
         boolean spyglass = ArchetypeConfig.getBoolean(ArchetypeRegistry.SPYGLASS_REVEALS_ARCHETYPE);
         if(spyglass){
            int warmup = ArchetypeConfig.getInt(ArchetypeRegistry.SPYGLASS_INVESTIGATE_DURATION);
            boolean alert = ArchetypeConfig.getBoolean(ArchetypeRegistry.SPYGLASS_REVEAL_ALERTS_PLAYER);
            ItemStack activeStack = player.getActiveItem();
            if(activeStack != null && activeStack.isOf(Items.SPYGLASS)){
               MiscUtils.LasercastResult lasercast = MiscUtils.lasercast(world, player.getEyePos(), player.getRotationVecClient(), server.getPlayerManager().getViewDistance() * 16, true, player);
               for(Entity entity : lasercast.sortedHits()){
                  if(entity instanceof ServerPlayerEntity target){
                     if(SPYGLASS_REVEAL_EVENTS.stream().anyMatch(event -> event.isReset() && event.getTarget().equals(target) && event.getInspector().equals(player))){
                        continue;
                     }
                     SPYGLASS_REVEAL_EVENTS.add(new SpyglassRevealEvent(player,target, (int) (warmup*1.125),false));
                     break;
                  }
               }
            }
            
            HashMap<ServerPlayerEntity, Integer> revealers = new HashMap<>();
            for(SpyglassRevealEvent event : SPYGLASS_REVEAL_EVENTS){
               if(!event.getTarget().equals(player) || event.isReset()) continue;
               revealers.compute(event.getInspector(), (k, count) -> count == null ? 1 : count + 1);
            }
            AtomicInteger highestRevealCount = new AtomicInteger();
            revealers.forEach((inspector, count) -> {
               if(count > highestRevealCount.get()){
                  highestRevealCount.set(count);
               }
               double percentage = (double) count / warmup;
               StringBuilder message = new StringBuilder("\uD83D\uDD0D ");
               for (int i = 0; i < 20; i++) {
                  if(percentage*20 > i){
                     message.append("|");
                  }else{
                     message.append("¦");
                  }
               }
               message.append(" \uD83D\uDD0D");
               if(inspector.getActiveItem() != null && inspector.getActiveItem().isOf(Items.SPYGLASS))
                  inspector.sendMessage(Text.literal(message.toString()).formatted(Formatting.GOLD),true);
               if(percentage > 1.0){
                  SubArchetype subArchetype = profile.getSubArchetype();
                  if(subArchetype == null){
                     inspector.sendMessage(Text.translatable("text.ancestralarchetypes.inspect_no_archetype",player.getStyledDisplayName()).formatted(Formatting.AQUA),false);
                  }else{
                     inspector.sendMessage(Text.translatable("text.ancestralarchetypes.inspect_results",player.getStyledDisplayName(),subArchetype.getName().formatted(MiscUtils.getClosestFormatting(subArchetype.getColor()))).formatted(Formatting.AQUA),false);
                  }
                  if(alert) player.sendMessage(Text.translatable("text.ancestralarchetypes.inspected").formatted(Formatting.RED));
                  
                  SPYGLASS_REVEAL_EVENTS.removeIf(event -> event.getInspector().equals(inspector) && event.getTarget().equals(player));
                  SPYGLASS_REVEAL_EVENTS.add(new SpyglassRevealEvent(inspector, player,1200,true));
               }
            });
            
            if(alert && highestRevealCount.get() > 0){
               double percentage = (double) highestRevealCount.get() / warmup;
               StringBuilder message = new StringBuilder();
               for (int i = 0; i < 20; i++) {
                  if(percentage*20 > i){
                     message.append("|");
                  }else{
                     message.append("¦");
                  }
               }
               player.sendMessage(Text.translatable("text.ancestralarchetypes.inspection_in_progress",message.toString()).formatted(Formatting.GOLD),true);
            }
         }
      }
      
      SPYGLASS_REVEAL_EVENTS.forEach(SpyglassRevealEvent::tickCooldown);
      SPYGLASS_REVEAL_EVENTS.removeIf(event -> event.getCooldown() < 0);
   }
}
