package net.borisshoes.ancestralarchetypes;

import net.borisshoes.ancestralarchetypes.callbacks.*;
import net.borisshoes.ancestralarchetypes.misc.SpyglassRevealEvent;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.config.ConfigManager;
import net.borisshoes.borislib.datastorage.DataAccess;
import net.borisshoes.borislib.utils.AlgoUtils;
import net.borisshoes.borislib.utils.ItemModDataHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.CONFIG_SETTINGS;

public class AncestralArchetypes implements ModInitializer, ClientModInitializer {
   
   static final Logger LOGGER = LogManager.getLogger("Ancestral Archetypes");
   private static final String CONFIG_NAME = "AncestralArchetypes.properties";
   public static final String MOD_ID = "ancestralarchetypes";
   
   public static ConfigManager CONFIG;
   public static final ArrayList<SpyglassRevealEvent> SPYGLASS_REVEAL_EVENTS = new ArrayList<>();
   public static boolean hasArcana = false;
   
   public static final boolean DEV_MODE = false;
   
   public static final ItemModDataHandler archetypes$ITEM_DATA = new ItemModDataHandler(MOD_ID);
   
   @Override
   public void onInitialize(){
      CONFIG = new ConfigManager(MOD_ID,"Ancestral Archetypes",CONFIG_NAME,CONFIG_SETTINGS);

      hasArcana = FabricLoader.getInstance().isModLoaded("arcananovum");
      
      ServerTickEvents.END_SERVER_TICK.register(TickCallback::onTick);
      CommandRegistrationCallback.EVENT.register(CommandRegisterCallback::registerCommands);
      ServerEntityEvents.ENTITY_UNLOAD.register(EntityLoadCallbacks::unloadEntity);
      UseEntityCallback.EVENT.register(EntityUseCallback::useEntity);
      ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionCallback::onPlayerLeave);
      ServerPlayConnectionEvents.JOIN.register(PlayerConnectionCallback::onPlayerJoin);
      ServerPlayerEvents.AFTER_RESPAWN.register(PlayerRespawnCallback::onRespawn);
      AttackEntityCallback.EVENT.register(EntityAttackedCallback::attackedEntity);
      
      ArchetypeRegistry.initialize();
      
      LOGGER.info("Evolving Ancestral Archetypes Into Your World!");
   }
   
   @Override
   public void onInitializeClient(){
      LOGGER.info("Evolving Ancestral Archetypes Into Your Client!");
   }
   
   public static ArchetypeAbility abilityFromTag(String tag){
      int lastDotIndex = tag.lastIndexOf(".");
      if (lastDotIndex == -1) {
         return null;
      }
      return ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID,tag.substring(lastDotIndex + 1)));
   }
   
   public static PlayerArchetypeData profile(ServerPlayer player){
      try{
         return profile(player.getUUID());
      }catch(Exception e){
         log(3,"Failed to get Archetype Profile for "+player.getScoreboardName() + " ("+player.getStringUUID()+")");
         log(3,e.toString());
      }
      return new PlayerArchetypeData(AlgoUtils.getUUID(BorisLib.BLANK_UUID));
   }
   
   public static PlayerArchetypeData profile(UUID playerId){
      try{
         if(playerId == null){
            log(3,"Tried to get Archetype Profile for null UUID, returning default entry");
            return new PlayerArchetypeData(AlgoUtils.getUUID(BorisLib.BLANK_UUID));
         }
         PlayerArchetypeData profile = DataAccess.getPlayer(playerId, PlayerArchetypeData.KEY);
         if(profile == null){
            log(3,"Returned Archetype Profile was somehow null for playerId: "+playerId+" returning default entry");
            return new PlayerArchetypeData(AlgoUtils.getUUID(BorisLib.BLANK_UUID));
         }
         return profile;
      }catch(Exception e){
         log(3,"Failed to get Archetype Profile for ("+playerId+")");
         log(3,e.toString());
      }
      return new PlayerArchetypeData(AlgoUtils.getUUID(BorisLib.BLANK_UUID));
   }
   
   /**
    * Uses built in logger to log a message
    * @param level 0 - Info | 1 - Warn | 2 - Error | 3 - Fatal | Else - Debug
    * @param msg  The {@code String} to be printed.
    */
   public static void log(int level, String msg){
      switch(level){
         case 0 -> LOGGER.info(msg);
         case 1 -> LOGGER.warn(msg);
         case 2 -> LOGGER.error(msg);
         case 3 -> LOGGER.fatal(msg);
         default -> LOGGER.debug(msg);
      }
   }
}
