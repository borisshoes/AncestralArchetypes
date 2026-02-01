package net.borisshoes.ancestralarchetypes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.borisshoes.ancestralarchetypes.gui.ArchetypeSelectionGui;
import net.borisshoes.borislib.config.ConfigValue;
import net.borisshoes.borislib.config.IConfigSetting;
import net.borisshoes.borislib.datastorage.DataAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class ArchetypeCommands {
   
   public static CompletableFuture<Suggestions> getAbilitySuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, String archetypeId, boolean curAbilities, boolean invert){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
      if(subArchetype != null && curAbilities){
         if(invert){
            ArchetypeRegistry.ABILITIES.stream().filter(key -> !subArchetype.getRawAbilities().contains(key)).forEach(key -> items.add(key.id().toLowerCase(Locale.ROOT)));
         }else{
            subArchetype.getRawAbilities().forEach(key -> items.add(key.id().toLowerCase(Locale.ROOT)));
         }
      }else{
         ArchetypeRegistry.ABILITIES.registryKeySet().forEach(key -> items.add(key.identifier().getPath().toLowerCase(Locale.ROOT)));
      }
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static CompletableFuture<Suggestions> getSubArchetypeSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      ArchetypeRegistry.SUBARCHETYPES.registryKeySet().forEach(key -> items.add(key.identifier().getPath().toLowerCase(Locale.ROOT)));
      items.add("none");
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static CompletableFuture<Suggestions> getTrimSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      context.getSource().getServer().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).listElements().forEach(entry -> items.add(entry.getRegisteredName().replaceAll("^minecraft:", "").toLowerCase(Locale.ROOT)));
      items.add("");
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static <E extends Enum<E>> CompletableFuture<Suggestions> getEnumSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, Class<E> enumClass){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      for(E value : enumClass.getEnumConstants()){
         items.add(value.name().toLowerCase(Locale.ROOT));
      }
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static int setSubArchetype(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, String archetypeId){
      try{
         CommandSourceStack source = context.getSource();
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
         if(subArchetype == null && !archetypeId.equals("none")){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         
         for(ServerPlayer target : targets){
            PlayerArchetypeData profile = profile(target);
            if(archetypeId.equals("none")){
               profile.changeArchetype(target,null,true);
            }else{
               profile.changeArchetype(target,subArchetype,true);
            }
         }
         
         source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.changed_archetypes",targets.size(), archetypeId).withStyle(ChatFormatting.AQUA), true);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int addChanges(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, int changes){
      try{
         CommandSourceStack source = context.getSource();
         for(ServerPlayer target : targets){
            PlayerArchetypeData profile = profile(target);
            profile.increaseAllowedChanges(changes);
         }
         source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.added_changes",changes,targets.size()).withStyle(ChatFormatting.AQUA), true);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getAbilities(CommandContext<CommandSourceStack> context, String archetypeId){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         SubArchetype subArchetype;
         boolean personal = false;
         if(archetypeId == null || archetypeId.isEmpty()){
            subArchetype = profile.getSubArchetype();
            personal = true;
            if(subArchetype == null){
               source.sendFailure(Component.literal("You have no archetype"));
               return 0;
            }
         }else{
            subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
            if(subArchetype == null){
               source.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
               return 0;
            }
         }
         
         MutableComponent feedback = Component.empty();
         if(personal){
            feedback.append(Component.translatable("command.ancestralarchetypes.abilities_get_personal",
                  subArchetype.getName().withColor(subArchetype.getColor()).withStyle(ChatFormatting.BOLD),
                  subArchetype.getArchetype().getName().withColor(subArchetype.getArchetype().color()).withStyle(ChatFormatting.BOLD)).withStyle(ChatFormatting.AQUA)).append(Component.literal("\n"));
         }else{
            feedback.append(Component.translatable("command.ancestralarchetypes.archetype_get",
                  subArchetype.getName().withColor(subArchetype.getColor()).withStyle(ChatFormatting.BOLD),
                  subArchetype.getArchetype().getName().withColor(subArchetype.getArchetype().color()).withStyle(ChatFormatting.BOLD)).withStyle(ChatFormatting.AQUA)).append(Component.literal("\n"));
         }
         for(ArchetypeAbility ability : subArchetype.getActualAbilities()){
            feedback.append(ability.getName().withStyle(ChatFormatting.DARK_AQUA)).append(Component.literal("\n"));
            for(IConfigSetting<?> config : ability.reliantConfigs()){
               MutableComponent text = CONFIG.values.stream()
                     .filter(confVal -> confVal.getName().equals(config.getName()))
                     .map(confVal -> MutableComponent.create(
                           new TranslatableContents(ConfigValue.getTranslation(confVal.getName(),MOD_ID,"getter_setter"), null, new String[] {String.valueOf(confVal.getValueString())}
                           )))
                     .findFirst().orElse(Component.empty());
               feedback.append(Component.translatable("text.ancestralarchetypes.list_item",text).withStyle(ChatFormatting.DARK_GREEN)).append(Component.literal("\n"));
            }
         }
         source.sendSuccess(() -> feedback,false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int resetAbilityCooldowns(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         return resetAbilityCooldowns(context, List.of(source.getPlayer()));
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int resetAbilityCooldowns(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets){
      try{
         CommandSourceStack source = context.getSource();
         
         for(ServerPlayer target : targets){
            PlayerArchetypeData profile = profile(target);
            profile.resetAbilityCooldowns();
         }
         
         source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.reset_cooldowns",targets.size()).withStyle(ChatFormatting.AQUA), false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int test(CommandContext<CommandSourceStack> context){
      if(!DEV_MODE)
         return 0;
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         //ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null);
         //selectionGui.open();
         
         ItemStack stack = player.getMainHandItem();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setGliderColor(CommandContext<CommandSourceStack> context, String color, String trimColor){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         try{
            if(color.startsWith("0x")){
               color = color.substring(2);
            }else if(color.startsWith("#")){
               color = color.substring(1);
            }
            
            int parsedColor;
            if (color.matches("[0-9A-Fa-f]{6}")) {
               parsedColor = Integer.parseInt(color, 16);
            } else {
               parsedColor = Integer.parseInt(color);
            }
            
            if(trimColor.isEmpty()){
               profile.setGliderTrimMaterial(null);
            }else{
               Optional<Holder.Reference<TrimMaterial>> material = context.getSource().getServer().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).get(Identifier.parse(trimColor));
               if(material.isEmpty()){
                  source.sendFailure(Component.translatable("command.ancestralarchetypes.trim_error"));
                  return -1;
               }else{
                  profile.setGliderTrimMaterial(material.get());
               }
            }
            
            profile.setGliderColor(parsedColor);
            source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.glider_success", String.format("%06X", parsedColor), trimColor.isEmpty() ? "none" : trimColor), false);
            return 1;
         }catch(Exception e){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.glider_error"));
            return -1;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setHelmetColor(CommandContext<CommandSourceStack> context, String color, String trimColor){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         try{
            if(color.startsWith("0x")){
               color = color.substring(2);
            }else if(color.startsWith("#")){
               color = color.substring(1);
            }
            
            int parsedColor;
            if (color.matches("[0-9A-Fa-f]{6}")) {
               parsedColor = Integer.parseInt(color, 16);
            } else {
               parsedColor = Integer.parseInt(color);
            }
            
            if(trimColor.isEmpty()){
               profile.setHelmetTrimMaterial(null);
            }else{
               Optional<Holder.Reference<TrimMaterial>> material = context.getSource().getServer().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).get(Identifier.parse(trimColor));
               if(material.isEmpty()){
                  source.sendFailure(Component.translatable("command.ancestralarchetypes.trim_error"));
                  return -1;
               }else{
                  profile.setHelmetTrimMaterial(material.get());
               }
            }
            
            profile.setHelmetColor(parsedColor);
            source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.helmet_success", String.format("%06X", parsedColor), trimColor.isEmpty() ? "none" : trimColor), false);
            return 1;
         }catch(Exception e){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.helmet_error"));
            return -1;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setHorseVariant(CommandContext<CommandSourceStack> context, String color, String markings){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         Variant horseColor = Arrays.stream(Variant.values()).filter(value -> value.name().equalsIgnoreCase(color)).findFirst().orElse(null);
         Markings marking = Arrays.stream(Markings.values()).filter(value -> value.name().equalsIgnoreCase(markings)).findFirst().orElse(null);
         
         if(horseColor == null || marking == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.horse_error"));
            return -1;
         }
         
         profile.setHorseVariant(horseColor,marking);
         source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.horse_success",(horseColor.name()+" "+marking.name())), false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setMountName(CommandContext<CommandSourceStack> context, String name){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         if(name != null){
            String sanitized = StringUtil.filterText(name);
            if(sanitized.length() > 50){
               source.sendFailure(Component.translatable("command.ancestralarchetypes.mount_name_error"));
               return -1;
            }
            name = sanitized;
         }
         
         profile.setMountName(name);
         String finalName = name;
         if(name != null){
            source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.mount_name_success", finalName), false);
         }else{
            source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.mount_name_reset"), false);
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getItems(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         if(!profile.giveAbilityItems(player,false)){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.ability_items_error"));
            return -1;
         }else{
            source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.ability_items_success"), false);
            return 1;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int toggleReminders(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         boolean giveReminders = !profile.giveReminders();
         profile.setReminders(giveReminders);
         source.sendSuccess(()-> Component.translatable("command.ancestralarchetypes.reminders",giveReminders ? "true": "false"), false);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int changeArchetype(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         long tickDiff = profile.getTicksSinceArchetypeChange() - CONFIG.getInt(ArchetypeRegistry.ARCHETYPE_CHANGE_COOLDOWN);
         if(tickDiff < 0){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.change_archetype_cooldown",-tickDiff/20));
            return -1;
         }
         
         if(!profile.canChangeArchetype()){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.change_archetype_error"));
            return -1;
         }
         
         ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null, false);
         selectionGui.open();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int archetypeList(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack source = context.getSource();
         if(!source.isPlayer() || source.getPlayer() == null){
            source.sendFailure(Component.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayer player = context.getSource().getPlayerOrException();
         PlayerArchetypeData profile = profile(player);
         
         ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null, true);
         selectionGui.open();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getDistribution(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack src = context.getSource();
         
         HashMap<SubArchetype,Integer> archetypeCounter = new HashMap<>();
         for(SubArchetype subarchetype : ArchetypeRegistry.SUBARCHETYPES){
            archetypeCounter.put(subarchetype,0);
         }
         archetypeCounter.put(null,0);
         
         for(PlayerArchetypeData profile : DataAccess.allPlayerDataFor(PlayerArchetypeData.KEY).values()){
            if(profile == null){
               log(1,"An error occurred loading a null profile");
            }else{
               SubArchetype subArchetype = profile.getSubArchetype();
               archetypeCounter.compute(subArchetype, (k, count) -> count + 1);
            }
         }
         
         StringBuilder masterString = new StringBuilder(Component.translatable("text.ancestralarchetypes.distribution_header").getString());
         src.sendSuccess(() -> Component.translatable("text.ancestralarchetypes.distribution_header"),false);
         archetypeCounter.forEach((subArchetype, integer) -> {
            int color = subArchetype == null ? 0xFFFFFF : subArchetype.getColor();
            MutableComponent name = subArchetype == null ? Component.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
            Component text = Component.empty().withColor(color).append(name).append(Component.literal(" - ").append(Component.literal(String.format("%,d",integer))));
            src.sendSuccess(() -> text, false);
            masterString.append("\n").append(text.getString());
         });
         src.sendSuccess(() -> Component.translatable("text.ancestralarchetypes.dump_copy").withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(masterString.toString()))),false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getAllPlayerArchetypes(CommandContext<CommandSourceStack> context){
      try{
         CommandSourceStack src = context.getSource();
         
         log(0, Component.translatable("text.ancestralarchetypes.dump_init").getString());
         StringBuilder masterString = new StringBuilder(Component.translatable("text.ancestralarchetypes.dump_header").getString());
         
         DataAccess.allPlayerDataFor(PlayerArchetypeData.KEY).forEach((player, profile) -> {
            SubArchetype subArchetype = profile.getSubArchetype();
            Archetype archetype = profile.getArchetype();
            MutableComponent subName = subArchetype == null ? Component.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
            MutableComponent name = archetype == null ? Component.translatable("text.ancestralarchetypes.none") : archetype.getName();
            String str = "\n"+ Component.translatable("text.ancestralarchetypes.archetype_player_get",profile.getUsername(),subName,name).getString();
            masterString.append(str);
         });
         
         src.sendSuccess(() -> Component.translatable("text.ancestralarchetypes.dump_copy").withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(masterString.toString()))),false);
         log(0,masterString.toString());
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getPlayersOfArchetype(CommandContext<CommandSourceStack> context, String archetypeId){
      try{
         CommandSourceStack src = context.getSource();
         
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
         if(subArchetype == null && !archetypeId.equals("none")){
            src.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         MutableComponent subName = subArchetype == null ? Component.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
         
         log(0, Component.translatable("text.ancestralarchetypes.type_dump_init",subName).getString());
         StringBuilder masterString = new StringBuilder(Component.translatable("text.ancestralarchetypes.dump_type_header",subName).getString());
         
         DataAccess.allPlayerDataFor(PlayerArchetypeData.KEY).forEach((player, profile) -> {
            SubArchetype playerArchetype = profile.getSubArchetype();
            if(playerArchetype == subArchetype){
               Archetype archetype = profile.getArchetype();
               MutableComponent name = archetype == null ? Component.translatable("text.ancestralarchetypes.none") : archetype.getName();
               String str = "\n"+ Component.translatable("text.ancestralarchetypes.archetype_player_get",profile.getUsername(),subName,name).getString();
               masterString.append(str);
            }
         });
         
         src.sendSuccess(() -> Component.translatable("text.ancestralarchetypes.dump_copy").withStyle(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(masterString.toString()))),false);
         log(0,masterString.toString());
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getArchetype(CommandContext<CommandSourceStack> context, String target){
      try{
         CommandSourceStack source = context.getSource();
         MinecraftServer server = source.getServer();
         PlayerList manger = server.getPlayerList();
         ServerPlayer player = manger.getPlayerByName(target);
         
         PlayerArchetypeData profile = DataAccess.allPlayerDataFor(PlayerArchetypeData.KEY).values().stream().filter(data ->
            data.getUsername().toLowerCase(Locale.ROOT).equals(target.toLowerCase(Locale.ROOT)) || data.getPlayerID().toString().equalsIgnoreCase(target)
         ).findAny().orElse(null);
         
         if(profile == null){
            source.sendFailure(Component.translatable("text.ancestralarchetypes.no_player_found"));
            return 0;
         }
         
         SubArchetype subArchetype = profile.getSubArchetype();
         Archetype archetype = profile.getArchetype();
         MutableComponent subName = subArchetype == null ? Component.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
         MutableComponent name = archetype == null ? Component.translatable("text.ancestralarchetypes.none") : archetype.getName();
         int color = subArchetype == null ? 0xFFFFFF : subArchetype.getColor();
         source.sendSuccess(() -> Component.translatable("text.ancestralarchetypes.archetype_player_get", profile.getUsername(),subName,name).withColor(color),false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int resetAbilities(CommandContext<CommandSourceStack> context, String archetypeId){
      try{
         CommandSourceStack src = context.getSource();
         
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
         if(subArchetype == null){
            src.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         MutableComponent subName = subArchetype.getName();
         ArchetypeAbilityStorage.resetAbilities(src.getServer(),subArchetype);
         src.sendSuccess(() -> Component.translatable("command.ancestralarchetypes.reset_abilities",subName),true);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int addAbility(CommandContext<CommandSourceStack> context, String archetypeId, String abilityId){
      try{
         CommandSourceStack src = context.getSource();
         
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
         if(subArchetype == null){
            src.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,abilityId));
         if(ability == null){
            src.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_ability",abilityId));
            return 0;
         }
         MutableComponent subName = subArchetype.getName();
         MutableComponent abilityName = ability.getName();
         boolean succ = ArchetypeAbilityStorage.addAbility(src.getServer(),subArchetype,ability);
         if(succ){
            src.sendSuccess(() -> Component.translatable("command.ancestralarchetypes.add_ability_succ",abilityName,subName),true);
            return 1;
         }else{
            src.sendFailure(Component.translatable("command.ancestralarchetypes.add_ability_fail",abilityName,subName));
            return 0;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int removeAbility(CommandContext<CommandSourceStack> context, String archetypeId, String abilityId){
      try{
         CommandSourceStack src = context.getSource();
         
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,archetypeId));
         if(subArchetype == null){
            src.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,abilityId));
         if(ability == null){
            src.sendFailure(Component.translatable("command.ancestralarchetypes.invalid_ability",abilityId));
            return 0;
         }
         MutableComponent subName = subArchetype.getName();
         MutableComponent abilityName = ability.getName();
         boolean succ = ArchetypeAbilityStorage.removeAbility(src.getServer(),subArchetype,ability);
         if(succ){
            src.sendSuccess(() -> Component.translatable("command.ancestralarchetypes.remove_ability_succ",abilityName,subName),true);
            return 1;
         }else{
            src.sendFailure(Component.translatable("command.ancestralarchetypes.remove_ability_fail",abilityName,subName));
            return 0;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
}
