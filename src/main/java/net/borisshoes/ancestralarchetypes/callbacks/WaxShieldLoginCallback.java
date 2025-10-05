package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.borislib.callbacks.LoginCallback;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import static net.borisshoes.arcananovum.ArcanaNovum.MOD_ID;

public class WaxShieldLoginCallback extends LoginCallback {
   
   private float hearts;
   
   public WaxShieldLoginCallback(){
      super(Identifier.of(MOD_ID, ArchetypeRegistry.WAX_SHIELD.getId()));
   }
   
   public WaxShieldLoginCallback(ServerPlayerEntity player, float hearts){
      this();
      this.world = player.getServer().getWorld(ServerWorld.OVERWORLD);
      this.playerUUID = player.getUuidAsString();
      this.hearts = hearts;
   }
   
   @Override
   public void onLogin(ServerPlayNetworkHandler netHandler, MinecraftServer server){
      // Double check that this is the correct player before running timer
      ServerPlayerEntity player = netHandler.player;
      if(player.getUuidAsString().equals(playerUUID)){
         float removed = Math.max(0,player.getAbsorptionAmount()-hearts);
         if(player.getAbsorptionAmount() != 0){
            SoundUtils.playSongToPlayer(player, SoundEvents.ITEM_HONEYCOMB_WAX_ON, 1.0f, .3f);
         }
         MinecraftUtils.removeMaxAbsorption(player,Identifier.of(AncestralArchetypes.MOD_ID, ArchetypeRegistry.WAX_SHIELD.getId()),hearts);
         player.setAbsorptionAmount(removed);
      }
   }
   
   @Override
   public void setData(NbtCompound data){
      //Data tag just has single float for hearts
      this.data = data;
      hearts = data.getFloat("hearts", 0.0f);
   }
   
   @Override
   public NbtCompound getData(){
      NbtCompound data = new NbtCompound();
      data.putFloat("hearts",hearts);
      this.data = data;
      return this.data;
   }
   
   @Override
   public boolean canCombine(LoginCallback loginCallback){
      return true;
   }
   
   @Override
   public boolean combineCallbacks(LoginCallback callback){
      if(callback instanceof WaxShieldLoginCallback newCallback){
         this.hearts += newCallback.hearts;
         return true;
      }
      return false;
   }
   
   @Override
   public LoginCallback makeNew(){
      return new WaxShieldLoginCallback();
   }
}
