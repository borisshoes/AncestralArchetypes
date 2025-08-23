package net.borisshoes.ancestralarchetypes;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.ArchetypeSelectionGui;
import net.borisshoes.ancestralarchetypes.utils.ConfigUtils;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.UserCache;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class ArchetypeCommands {
   
   public static CompletableFuture<Suggestions> getPlayerSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);
      Set<String> items = new HashSet<>();
      context.getSource().getPlayerNames().forEach(name -> items.add(name.toLowerCase(Locale.ROOT)));
      items.stream().filter(s -> s.startsWith(start)).forEach(builder::suggest);
      return builder.buildFuture();
   }
   
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
   
   public static int getAbilities(CommandContext<ServerCommandSource> context, String archetypeId){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         SubArchetype subArchetype;
         boolean personal = false;
         if(archetypeId == null || archetypeId.isEmpty()){
            subArchetype = profile.getSubArchetype();
            personal = true;
            if(subArchetype == null){
               source.sendError(Text.literal("You have no archetype"));
               return 0;
            }
         }else{
            subArchetype = ArchetypeRegistry.SUBARCHETYPES.get(Identifier.of(MOD_ID,archetypeId));
            if(subArchetype == null){
               source.sendError(Text.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
               return 0;
            }
         }
         
         MutableText feedback = Text.empty();
         if(personal){
            feedback.append(Text.translatable("command.ancestralarchetypes.abilities_get_personal",
                  subArchetype.getName().formatted(MiscUtils.getClosestFormatting(subArchetype.getColor()),Formatting.BOLD),
                  subArchetype.getArchetype().getName().formatted(MiscUtils.getClosestFormatting(subArchetype.getArchetype().getColor()),Formatting.BOLD)).formatted(Formatting.AQUA)).append(Text.literal("\n"));
         }else{
            feedback.append(Text.translatable("command.ancestralarchetypes.archetype_get",
                  subArchetype.getName().formatted(MiscUtils.getClosestFormatting(subArchetype.getColor()),Formatting.BOLD),
                  subArchetype.getArchetype().getName().formatted(MiscUtils.getClosestFormatting(subArchetype.getArchetype().getColor()),Formatting.BOLD)).formatted(Formatting.AQUA)).append(Text.literal("\n"));
         }
         for(ArchetypeAbility ability : subArchetype.getActualAbilities()){
            feedback.append(ability.getName().formatted(Formatting.DARK_AQUA)).append(Text.literal("\n"));
            for(ArchetypeConfig.ConfigSetting<?> config : ability.getReliantConfigs()){
               feedback.append(Text.translatable("text.ancestralarchetypes.list_item",CONFIG.getGetter(config.getName())).formatted(Formatting.DARK_GREEN)).append(Text.literal("\n"));
            }
         }
         source.sendFeedback(() -> feedback,false);
         
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
         
         ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null, false);
         selectionGui.open();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int archetypeList(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource source = context.getSource();
         if(!source.isExecutedByPlayer() || source.getPlayer() == null){
            source.sendError(Text.translatable("command.ancestralarchetypes.not_player_error"));
            return -1;
         }
         
         ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
         IArchetypeProfile profile = profile(player);
         
         ArchetypeSelectionGui selectionGui = new ArchetypeSelectionGui(player, null, true);
         selectionGui.open();
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   private static HashMap<ServerPlayerEntity,IArchetypeProfile> getAllPlayerData(MinecraftServer server){
      HashMap<ServerPlayerEntity,IArchetypeProfile> data = new HashMap<>();
      try{
         UserCache userCache = server.getUserCache();
         List<ServerPlayerEntity> allPlayers = new ArrayList<>();
         List<UserCache.Entry> cacheEntries = userCache.load();
         
         for(UserCache.Entry cacheEntry : cacheEntries){
            GameProfile reqProfile = cacheEntry.getProfile();
            ServerPlayerEntity reqPlayer = MiscUtils.getRequestedPlayer(server,reqProfile);
            allPlayers.add(reqPlayer);
         }
         
         for(ServerPlayerEntity player : allPlayers){
            IArchetypeProfile profile = AncestralArchetypes.profile(player);
            data.put(player,profile);
         }
         
         return data;
      }catch(Exception e){
         log(2,e.toString());
         return data;
      }
   }
   
   public static int getDistribution(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource src = context.getSource();
         
         HashMap<SubArchetype,Integer> archetypeCounter = new HashMap<>();
         for(SubArchetype subarchetype : ArchetypeRegistry.SUBARCHETYPES){
            archetypeCounter.put(subarchetype,0);
         }
         archetypeCounter.put(null,0);
         
         for(IArchetypeProfile profile : getAllPlayerData(src.getServer()).values()){
            if(profile == null){
               log(1,"An error occurred loading a null profile");
            }else{
               SubArchetype subArchetype = profile.getSubArchetype();
               archetypeCounter.compute(subArchetype, (k, count) -> count + 1);
            }
         }
         
         StringBuilder masterString = new StringBuilder(Text.translatable("text.ancestralarchetypes.distribution_header").getString());
         src.sendFeedback(() -> Text.translatable("text.ancestralarchetypes.distribution_header"),false);
         archetypeCounter.forEach((subArchetype, integer) -> {
            int color = subArchetype == null ? 0xFFFFFF : subArchetype.getColor();
            MutableText name = subArchetype == null ? Text.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
            Text text = Text.empty().formatted(MiscUtils.getClosestFormatting(color)).append(name).append(Text.literal(" - ").append(Text.literal(String.format("%,d",integer))));
            src.sendFeedback(() -> text, false);
            masterString.append("\n").append(text.getString());
         });
         src.sendFeedback(() -> Text.translatable("text.ancestralarchetypes.dump_copy").styled(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(masterString.toString()))),false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getAllPlayerArchetypes(CommandContext<ServerCommandSource> context){
      try{
         ServerCommandSource src = context.getSource();
         
         log(0,Text.translatable("text.ancestralarchetypes.dump_init").getString());
         StringBuilder masterString = new StringBuilder(Text.translatable("text.ancestralarchetypes.dump_header").getString());
         
         getAllPlayerData(src.getServer()).forEach((player, profile) -> {
            SubArchetype subArchetype = profile.getSubArchetype();
            Archetype archetype = profile.getArchetype();
            MutableText subName = subArchetype == null ? Text.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
            MutableText name = archetype == null ? Text.translatable("text.ancestralarchetypes.none") : archetype.getName();
            String str = "\n"+Text.translatable("text.ancestralarchetypes.archetype_player_get",player.getStyledDisplayName(),subName,name).getString();
            masterString.append(str);
         });
         
         src.sendFeedback(() -> Text.translatable("text.ancestralarchetypes.dump_copy").styled(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(masterString.toString()))),false);
         log(0,masterString.toString());
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getPlayersOfArchetype(CommandContext<ServerCommandSource> context, String archetypeId){
      try{
         ServerCommandSource src = context.getSource();
         
         SubArchetype subArchetype = ArchetypeRegistry.SUBARCHETYPES.get(Identifier.of(MOD_ID,archetypeId));
         if(subArchetype == null && !archetypeId.equals("none")){
            src.sendError(Text.translatable("command.ancestralarchetypes.invalid_archetype",archetypeId));
            return 0;
         }
         MutableText subName = subArchetype == null ? Text.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
         
         log(0,Text.translatable("text.ancestralarchetypes.type_dump_init",subName).getString());
         StringBuilder masterString = new StringBuilder(Text.translatable("text.ancestralarchetypes.dump_type_header",subName).getString());
         
         getAllPlayerData(src.getServer()).forEach((player, profile) -> {
            SubArchetype playerArchetype = profile.getSubArchetype();
            if(playerArchetype == subArchetype){
               Archetype archetype = profile.getArchetype();
               MutableText name = archetype == null ? Text.translatable("text.ancestralarchetypes.none") : archetype.getName();
               String str = "\n"+Text.translatable("text.ancestralarchetypes.archetype_player_get",player.getStyledDisplayName(),subName,name).getString();
               masterString.append(str);
            }
         });
         
         src.sendFeedback(() -> Text.translatable("text.ancestralarchetypes.dump_copy").styled(s -> s.withClickEvent(new ClickEvent.CopyToClipboard(masterString.toString()))),false);
         log(0,masterString.toString());
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
   
   public static int getArchetype(CommandContext<ServerCommandSource> context, String target){
      try{
         ServerCommandSource source = context.getSource();
         MinecraftServer server = source.getServer();
         PlayerManager manger = server.getPlayerManager();
         ServerPlayerEntity player = manger.getPlayer(target);
         
         IArchetypeProfile profile = null;
         if(player != null){
            profile = AncestralArchetypes.profile(player);
         }else{
            UserCache userCache = server.getUserCache();
            List<UserCache.Entry> cacheEntries = userCache.load();
            
            for(UserCache.Entry cacheEntry : cacheEntries){
               GameProfile reqProfile = cacheEntry.getProfile();
               if(reqProfile.getName().equalsIgnoreCase(target)){
                  player = MiscUtils.getRequestedPlayer(server,reqProfile);
                  profile = AncestralArchetypes.profile(player);
                  break;
               }
            }
            if(profile == null){
               source.sendError(Text.translatable("text.ancestralarchetypes.no_player_found"));
               return 0;
            }
         }
         
         SubArchetype subArchetype = profile.getSubArchetype();
         Archetype archetype = profile.getArchetype();
         MutableText subName = subArchetype == null ? Text.translatable("text.ancestralarchetypes.none") : subArchetype.getName();
         MutableText name = archetype == null ? Text.translatable("text.ancestralarchetypes.none") : archetype.getName();
         int color = subArchetype == null ? 0xFFFFFF : subArchetype.getColor();
         ServerPlayerEntity finalPlayer = player;
         source.sendFeedback(() -> Text.translatable("text.ancestralarchetypes.archetype_player_get", finalPlayer.getStyledDisplayName(),subName,name).formatted(MiscUtils.getClosestFormatting(color)),false);
         
         return 1;
      }catch(Exception e){
         log(2,e.toString());
         return -1;
      }
   }
}
