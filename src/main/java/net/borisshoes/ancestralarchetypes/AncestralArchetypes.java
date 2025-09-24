package net.borisshoes.ancestralarchetypes;

import com.mojang.authlib.GameProfile;
import net.borisshoes.ancestralarchetypes.callbacks.*;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.misc.SpyglassRevealEvent;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.config.ConfigManager;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.borisshoes.borislib.utils.ItemModDataHandler;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.CONFIG_SETTINGS;
import static net.borisshoes.ancestralarchetypes.cca.PlayerComponentInitializer.PLAYER_DATA;

public class AncestralArchetypes implements ModInitializer, ClientModInitializer {
   
   private static final Logger LOGGER = LogManager.getLogger("Ancestral Archetypes");
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
      return ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID,tag.substring(lastDotIndex + 1)));
   }
   
   public static IArchetypeProfile profile(PlayerEntity player){
      if(player == null){
         return null;
      }
      try{
         return PLAYER_DATA.get(player);
      }catch(Exception e){
         log(3,"Failed to get Archetype Profile for "+player.getNameForScoreboard() + " ("+player.getUuidAsString()+")");
         log(3,e.toString());
      }
      return null;
   }
   
   public static IArchetypeProfile getPlayerOrOffline(UUID playerId){
      MinecraftServer server = BorisLib.SERVER;
      ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
      if(player != null){
         return PLAYER_DATA.get(player);
      }else{
         GameProfile profile = server.getUserCache().getByUuid(playerId).orElse(null);
         if(profile == null) return null;
         player = MinecraftUtils.getRequestedPlayer(server,profile);
         return PLAYER_DATA.get(player);
      }
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
