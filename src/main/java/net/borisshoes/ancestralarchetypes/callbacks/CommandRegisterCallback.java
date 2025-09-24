package net.borisshoes.ancestralarchetypes.callbacks;

import com.mojang.brigadier.CommandDispatcher;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeCommands;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.DEV_MODE;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandRegisterCallback {
   public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment){
      dispatcher.register(literal("archetypes")
            .then(literal("setArchetype").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("targets", players()).then(argument("archetype_id", string()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.setSubArchetype(context,getPlayers(context,"targets"),getString(context,"archetype_id"))))))
            .then(literal("addChanges").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("targets", players()).then(argument("changes", integer())
                        .executes(context -> ArchetypeCommands.addChanges(context,getPlayers(context,"targets"),getInteger(context,"changes"))))))
            .then(literal("resetCooldowns").requires(source -> source.hasPermissionLevel(2)).executes(ArchetypeCommands::resetAbilityCooldowns)
                  .then(argument("targets", players()).executes(context -> ArchetypeCommands.resetAbilityCooldowns(context,getPlayers(context,"targets")))))
            .then(literal("setGliderColor")
                  .then(argument("color", string()).executes(context -> ArchetypeCommands.setGliderColor(context,getString(context,"color"),""))
                        .then(argument("trim_color", string()).suggests(ArchetypeCommands::getTrimSuggestions)
                              .executes(context -> ArchetypeCommands.setGliderColor(context,getString(context,"color"),getString(context,"trim_color"))))))
            .then(literal("setHelmetColor")
                  .then(argument("color", string()).executes(context -> ArchetypeCommands.setHelmetColor(context,getString(context,"color"),""))
                        .then(argument("trim_color", string()).suggests(ArchetypeCommands::getTrimSuggestions)
                              .executes(context -> ArchetypeCommands.setHelmetColor(context,getString(context,"color"),getString(context,"trim_color"))))))
            .then(literal("setMountName")
                  .executes(context -> ArchetypeCommands.setMountName(context,null))
                  .then(argument("name", greedyString()).executes(context -> ArchetypeCommands.setMountName(context,getString(context,"name")))))
            .then(literal("setHorseVariant")
                  .then(argument("color", string()).suggests((context, builder) -> ArchetypeCommands.getEnumSuggestions(context,builder, HorseColor.class))
                        .then(argument("markings", string()).suggests((context, builder) -> ArchetypeCommands.getEnumSuggestions(context,builder, HorseMarking.class))
                              .executes(context -> ArchetypeCommands.setHorseVariant(context,getString(context,"color"),getString(context,"markings"))))))
            .then(literal("items").executes(ArchetypeCommands::getItems))
            .then(literal("toggleReminders").executes(ArchetypeCommands::toggleReminders))
            .then(literal("changeArchetype").executes(ArchetypeCommands::changeArchetype))
            .then(literal("abilities")
                  .executes(context -> ArchetypeCommands.getAbilities(context, null))
                  .then(argument("archetype_id", string()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.getAbilities(context, getString(context,"archetype_id")))))
            .then(literal("list").executes(ArchetypeCommands::archetypeList))
            .then(literal("distribution").requires(source -> source.hasPermissionLevel(2)).executes(ArchetypeCommands::getDistribution))
            .then(literal("getPlayersOfArchetype").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("archetype_id", string()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.getPlayersOfArchetype(context, getString(context, "archetype_id")))))
            .then(literal("getAllPlayerArchetypes").requires(source -> source.hasPermissionLevel(2)).executes(ArchetypeCommands::getAllPlayerArchetypes))
            .then(literal("getArchetype").requires(source -> source.hasPermissionLevel(2))
                  .then(argument("target",string()).suggests(ArchetypeCommands::getPlayerSuggestions)
                        .executes(context -> ArchetypeCommands.getArchetype(context, getString(context, "target")))))
      );
      
      dispatcher.register(AncestralArchetypes.CONFIG.generateCommand("archetypes","config"));
      
      if(DEV_MODE){
         dispatcher.register(literal("archetypes")
               .then(literal("test").requires(source -> source.hasPermissionLevel(2)).executes(ArchetypeCommands::test))
         );
      }
   }
}
