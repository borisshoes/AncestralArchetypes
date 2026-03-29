package net.borisshoes.ancestralarchetypes.callbacks;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeCommands;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.DEV_MODE;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

public class CommandRegisterCallback {
   public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment){
      dispatcher.register(literal("archetypes")
            .then(literal("setArchetype").requires(Permissions.require(MOD_ID + ".setarchetype", PermissionLevel.GAMEMASTERS))
                  .then(argument("targets", players()).then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.setSubArchetype(context,getPlayers(context,"targets"),getString(context,"archetype_id"))))))
            .then(literal("addChanges").requires(Permissions.require(MOD_ID + ".addchanges", PermissionLevel.GAMEMASTERS))
                  .then(argument("targets", players()).then(argument("changes", integer())
                        .executes(context -> ArchetypeCommands.addChanges(context,getPlayers(context,"targets"),getInteger(context,"changes"))))))
            .then(literal("resetCooldowns").requires(Permissions.require(MOD_ID + ".resetcooldowns", PermissionLevel.GAMEMASTERS)).executes(ArchetypeCommands::resetAbilityCooldowns)
                  .then(argument("targets", players()).executes(context -> ArchetypeCommands.resetAbilityCooldowns(context,getPlayers(context,"targets")))))
            .then(literal("setGliderColor").requires(Permissions.require(MOD_ID + ".setglidercolor", PermissionLevel.ALL))
                  .then(argument("color", word()).executes(context -> ArchetypeCommands.setGliderColor(context,getString(context,"color"),""))
                        .then(argument("trim_color", word()).suggests(ArchetypeCommands::getTrimSuggestions)
                              .executes(context -> ArchetypeCommands.setGliderColor(context,getString(context,"color"),getString(context,"trim_color"))))))
            .then(literal("setHelmetColor").requires(Permissions.require(MOD_ID + ".sethelmetcolor", PermissionLevel.ALL))
                  .then(argument("color", word()).executes(context -> ArchetypeCommands.setHelmetColor(context,getString(context,"color"),""))
                        .then(argument("trim_color", word()).suggests(ArchetypeCommands::getTrimSuggestions)
                              .executes(context -> ArchetypeCommands.setHelmetColor(context,getString(context,"color"),getString(context,"trim_color"))))))
            .then(literal("setMountName").requires(Permissions.require(MOD_ID + ".setmountname", PermissionLevel.ALL))
                  .executes(context -> ArchetypeCommands.setMountName(context,null))
                  .then(argument("name", greedyString()).executes(context -> ArchetypeCommands.setMountName(context,getString(context,"name")))))
            .then(literal("setHorseVariant").requires(Permissions.require(MOD_ID + ".sethorsevariant", PermissionLevel.ALL))
                  .then(argument("color", word()).suggests((context, builder) -> ArchetypeCommands.getEnumSuggestions(context,builder, Variant.class))
                        .then(argument("markings", word()).suggests((context, builder) -> ArchetypeCommands.getEnumSuggestions(context,builder, Markings.class))
                              .executes(context -> ArchetypeCommands.setHorseVariant(context,getString(context,"color"),getString(context,"markings"))))))
            .then(literal("items").requires(Permissions.require(MOD_ID + ".items", PermissionLevel.ALL)).executes(ArchetypeCommands::getItems))
            .then(literal("toggleReminders").requires(Permissions.require(MOD_ID + ".togglereminders", PermissionLevel.ALL)).executes(ArchetypeCommands::toggleReminders))
            .then(literal("changeArchetype").requires(Permissions.require(MOD_ID + ".changearchetype", PermissionLevel.ALL)).executes(ArchetypeCommands::changeArchetype))
            .then(literal("abilities").requires(Permissions.require(MOD_ID + ".abilities", PermissionLevel.ALL))
                  .executes(context -> ArchetypeCommands.getAbilities(context, null))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.getAbilities(context, getString(context,"archetype_id")))))
            .then(literal("list").requires(Permissions.require(MOD_ID + ".list", PermissionLevel.ALL)).executes(ArchetypeCommands::archetypeList))
            .then(literal("distribution").requires(Permissions.require(MOD_ID + ".distribution", PermissionLevel.GAMEMASTERS)).executes(ArchetypeCommands::getDistribution))
            .then(literal("getPlayersOfArchetype").requires(Permissions.require(MOD_ID + ".getplayersofarchetype", PermissionLevel.GAMEMASTERS))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.getPlayersOfArchetype(context, getString(context, "archetype_id")))))
            .then(literal("getAllPlayerArchetypes").requires(Permissions.require(MOD_ID + ".getallplayerarchetypes", PermissionLevel.GAMEMASTERS)).executes(ArchetypeCommands::getAllPlayerArchetypes))
            .then(literal("getArchetype").requires(Permissions.require(MOD_ID + ".getarchetype", PermissionLevel.GAMEMASTERS))
                  .then(argument("target",word()).suggests(MinecraftUtils::getPlayerSuggestions)
                        .executes(context -> ArchetypeCommands.getArchetype(context, getString(context, "target")))))
            .then(literal("resetArchetypeAbilities").requires(Permissions.require(MOD_ID + ".resetarchetypeabilities", PermissionLevel.GAMEMASTERS))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .executes(context -> ArchetypeCommands.resetAbilities(context, getString(context,"archetype_id")))))
            .then(literal("addArchetypeAbility").requires(Permissions.require(MOD_ID + ".addarchetypeability", PermissionLevel.GAMEMASTERS))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .then(argument("ability_id",word()).suggests((context, builder) -> ArchetypeCommands.getAbilitySuggestions(context,builder,getString(context,"archetype_id"),true,true))
                              .executes(context -> ArchetypeCommands.addAbility(context, getString(context,"archetype_id"), getString(context,"ability_id"))))))
            .then(literal("removeArchetypeAbility").requires(Permissions.require(MOD_ID + ".removearchetypeability", PermissionLevel.GAMEMASTERS))
                  .then(argument("archetype_id", word()).suggests(ArchetypeCommands::getSubArchetypeSuggestions)
                        .then(argument("ability_id",word()).suggests((context, builder) -> ArchetypeCommands.getAbilitySuggestions(context,builder,getString(context,"archetype_id"),true,false))
                              .executes(context -> ArchetypeCommands.removeAbility(context, getString(context,"archetype_id"), getString(context,"ability_id"))))))
      );
      
      dispatcher.register(AncestralArchetypes.CONFIG.generateCommand("archetypes","config"));
      
      if(DEV_MODE){
         dispatcher.register(literal("archetypes")
               .then(literal("test").requires(Permissions.require(MOD_ID + ".test", PermissionLevel.GAMEMASTERS)).executes(ArchetypeCommands::test))
         );
      }
   }
}
