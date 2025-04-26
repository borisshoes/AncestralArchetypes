package net.borisshoes.ancestralarchetypes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.ArchetypeSelectionGui;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class ArchetypeCommands {
   
   public static CompletableFuture<Suggestions> getSubArchetypeSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      ArchetypeRegistry.SUBARCHETYPES.getKeys().forEach(key -> items.add(key.getValue().getPath().toLowerCase(Locale.ROOT)));
      items.add("none");
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static <E extends Enum<E>> CompletableFuture<Suggestions> getEnumSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder, Class<E> enumClass){
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      for(E value : enumClass.getEnumConstants()){
         items.add(value.name().toLowerCase(Locale.ROOT));
      }
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
   public static int setSubArchetype(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets, String archetypeId){
      try{
         ServerCommandSource source = context.getSource();
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.get(Identifier.of(MOD_ID,archetypeId));
         if(subArchetype == null && !archetypeId.equals("none")){
            source.sendError(Text.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         
         for(ServerPlayerEntity target : targets){
            IArchetypeProfile profile = profile(target);
            if(archetypeId.equals("none")){
               profile.changeArchetype(null);
            }else{
               profile.changeArchetype(subArchetype);
            }
         }
         
         source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.changed_archetypes",targets.size(), archetypeId).formatted(Formatting.AQUA), true);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int addChanges(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets, int changes){
      try{
         ServerCommandSource source = context.getSource();
         for(ServerPlayerEntity target : targets){
            IArchetypeProfile profile = profile(target);
            profile.increaseAllowedChanges(changes);
         }
         source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.added_changes",changes,targets.size()).formatted(Formatting.AQUA), true);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getAbilities(CommandContext<ServerCommandSource> context){
      if(!DEV_MODE)
         return 0;
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         if(profile.getSubArchetype() != null){
            source.sendFeedback(() -> Text.literal("You are a " + profile.getSubArchetype().getId() + ", a subarchetype of " + profile.getArchetype().getId()).formatted(Formatting.AQUA), false);
            source.sendFeedback(() -> Text.literal("\nYour abilities are: ").formatted(Formatting.AQUA), false);
            for(ArchetypeAbility ability : profile.getAbilities()){
               source.sendFeedback(() -> ability.getName().formatted(Formatting.DARK_AQUA), false);
            }
         }else{
            source.sendError(Text.literal("You have no archetype"));
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int resetAbilityCooldowns(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         return resetAbilityCooldowns(context, List.of(source.getPlayer()));
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int resetAbilityCooldowns(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets){
      try{
         ServerCommandSource source = context.getSource();
         
         for(ServerPlayerEntity target : targets){
            IArchetypeProfile profile = profile(target);
            profile.resetAbilityCooldowns();
         }
         
         source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.reset_cooldowns",targets.size()).formatted(Formatting.AQUA), false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int test(CommandContext<ServerCommandSource> context){
      if(!DEV_MODE)
         return 0;
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         //ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null);
         //selectionGui.open();
         
         ItemStack stack = player.getMainHandStack();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setGliderColor(CommandContext<ServerCommandSource> context, String color){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
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
            
            profile.setGliderColor(parsedColor);
            source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.glider_success", String.format("%06X", parsedColor)), false);
            return 1;
         }catch(Exception e){
            source.sendError(Text.translatable("command.ancestralarchetypes.glider_error"));
            return -1;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setHorseVariant(CommandContext<ServerCommandSource> context, String color, String markings){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         HorseColor horseColor = Arrays.stream(HorseColor.values()).filter(value -> value.name().equalsIgnoreCase(color)).findFirst().orElse(null);
         HorseMarking marking = Arrays.stream(HorseMarking.values()).filter(value -> value.name().equalsIgnoreCase(markings)).findFirst().orElse(null);
         
         if(horseColor == null || marking == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.horse_error"));
            return -1;
         }
         
         profile.setHorseVariant(horseColor,marking);
         source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.horse_success",(horseColor.name()+" "+marking.name())), false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int setMountName(CommandContext<ServerCommandSource> context, String name){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         if(name != null){
            String sanitized = StringHelper.stripInvalidChars(name);
            if(sanitized.length() > 50){
               source.sendError(Text.translatable("command.ancestralarchetypes.mount_name_error"));
               return -1;
            }
            name = sanitized;
         }
         
         profile.setMountName(name);
         String finalName = name;
         if(name != null){
            source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.mount_name_success", finalName), false);
         }else{
            source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.mount_name_reset"), false);
         }
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getItems(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         if(!profile.giveAbilityItems(false)){
            source.sendError(Text.translatable("command.ancestralarchetypes.ability_items_error"));
            return -1;
         }else{
            source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.ability_items_success"), false);
            return 1;
         }
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int toggleReminders(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         boolean giveReminders = !profile.giveReminders();
         profile.setReminders(giveReminders);
         source.sendFeedback(()->Text.translatable("command.ancestralarchetypes.reminders",giveReminders ? "true": "false"), false);
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int changeArchetype(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         if(!profile.canChangeArchetype()){
            source.sendError(Text.translatable("command.ancestralarchetypes.change_archetype_error"));
            return -1;
         }
         
         ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null);
         selectionGui.open();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
}
