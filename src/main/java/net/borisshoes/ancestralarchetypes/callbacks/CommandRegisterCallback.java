package net.borisshoes.ancestralarchetypes.callbacks;

import com.mojang.brigadier.CommandDispatcher;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeCommands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.DEV_MODE;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

public class CommandRegisterCallback {
   public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment){
      dispatcher.register(literal("archetypes")
            .then(literal("setArchetype").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                  .then(argument("targets", players()).then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.setSubArchetype(context,getPlayers(context,"targets"),getString(context,"archetype_id"))))))
            .then(literal("addChanges").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                  .then(argument("targets", players()).then(argument("changes", integer())
                        .executes(context -> ArchetypeCommands.addChanges(context,getPlayers(context,"targets"),getInteger(context,"changes"))))))
            .then(literal("resetCooldowns").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(ArchetypeCommands::resetAbilityCooldowns)
                  .then(argument("targets", players()).executes(context -> ArchetypeCommands.resetAbilityCooldowns(context,getPlayers(context,"targets")))))
            .then(literal("setGliderColor")
                  .then(argument("color", word()).executes(context -> ArchetypeCommands.setGliderColor(context,getString(context,"color"),""))
                        .then(argument("trim_color", word()).suggests(ArchetypeCommands::getTrimSuggestions)
                              .executes(context -> ArchetypeCommands.setGliderColor(context,getString(context,"color"),getString(context,"trim_color"))))))
            .then(literal("setHelmetColor")
                  .then(argument("color", word()).executes(context -> ArchetypeCommands.setHelmetColor(context,getString(context,"color"),""))
                        .then(argument("trim_color", word()).suggests(ArchetypeCommands::getTrimSuggestions)
                              .executes(context -> ArchetypeCommands.setHelmetColor(context,getString(context,"color"),getString(context,"trim_color"))))))
            .then(literal("setMountName")
                  .executes(context -> ArchetypeCommands.setMountName(context,null))
                  .then(argument("name", greedyString()).executes(context -> ArchetypeCommands.setMountName(context,getString(context,"name")))))
            .then(literal("setHorseVariant")
                  .then(argument("color", word()).suggests((context, builder) -> ArchetypeCommands.getEnumSuggestions(context,builder, Variant.class))
                        .then(argument("markings", word()).suggests((context, builder) -> ArchetypeCommands.getEnumSuggestions(context,builder, Markings.class))
                              .executes(context -> ArchetypeCommands.setHorseVariant(context,getString(context,"color"),getString(context,"markings"))))))
            .then(literal("items").executes(ArchetypeCommands::getItems))
            .then(literal("toggleReminders").executes(ArchetypeCommands::toggleReminders))
            .then(literal("changeArchetype").executes(ArchetypeCommands::changeArchetype))
            .then(literal("abilities")
                  .executes(context -> ArchetypeCommands.getAbilities(context, null))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.getAbilities(context, getString(context,"archetype_id")))))
            .then(literal("list").executes(ArchetypeCommands::archetypeList))
            .then(literal("distribution").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(ArchetypeCommands::getDistribution))
            .then(literal("getPlayersOfArchetype").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.getPlayersOfArchetype(context, getString(context, "archetype_id")))))
            .then(literal("getAllPlayerArchetypes").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(ArchetypeCommands::getAllPlayerArchetypes))
            .then(literal("getArchetype").requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                  .then(argument("target",word()).suggests(ArchetypeCommands::getPlayerSuggestions)
                        .executes(context -> ArchetypeCommands.getArchetype(context, getString(context, "target")))))
      );
      
      dispatcher.register(AncestralArchetypes.CONFIG.generateCommand("archetypes","config"));
      
      if(DEV_MODE){
         dispatcher.register(literal("archetypes")
               .then(literal("test").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)).executes(ArchetypeCommands::test))
         );
      }
   }
}
