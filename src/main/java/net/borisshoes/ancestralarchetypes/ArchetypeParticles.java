package net.borisshoes.ancestralarchetypes;

import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

public class ArchetypeParticles extends ParticleEffectUtils {
   public static void guardianRay(ServerLevel world, Vec3 p1, Vec3 p2, int tick){
      int windup = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_WINDUP);
      double length = p2.subtract(p1).length();
      Vec3 basisP1 = Vec3.ZERO;
      Vec3 basisP2 = new Vec3(0,length,0);
      
      Vec3 basis = basisP2.subtract(basisP1);
      Vec3 newBasis = p2.subtract(p1);
      Quaternionf transform = new Quaternionf().rotationTo(basis.toVector3f(), newBasis.toVector3f());
      
      int intervals = (int)(basisP1.subtract(basisP2).length()*(6+tick%2));
      double delta = 0.02;
      double radius = 0.2;
      double dy = (basisP2.y-basisP1.y)/intervals;
      for(int i = 0; i < intervals; i++){
         double y = basisP1.y + dy*i;
         double theta = i * Math.PI / 12.0 + tick * Math.PI / 12.0;
         Vec3 centerPos = new Vec3(0,y,0);
         Vec3 helixPos1 = new Vec3(radius*Math.cos(theta),y,radius*Math.sin(theta));
         Vec3 helixPos2 = new Vec3(radius*Math.cos(theta+Math.PI),y,radius*Math.sin(theta+Math.PI));
         Vec3 centerPosAdjust = new Vec3(transform.transform(centerPos.toVector3f())).add(p1);
         Vec3 helixPosAdjust1 = new Vec3(transform.transform(helixPos1.toVector3f())).add(p1);
         Vec3 helixPosAdjust2 = new Vec3(transform.transform(helixPos2.toVector3f())).add(p1);
         
         if(tick % 2 == 0){
            if(tick < windup){
               int rgb1 = 0x6116b8;
               int rgb2 = 0xe4f00e;
               
               int r1 = (rgb1 >> 16) & 0xFF;
               int g1 = (rgb1 >> 8) & 0xFF;
               int b1 = rgb1 & 0xFF;
               int r2 = (rgb2 >> 16) & 0xFF;
               int g2 = (rgb2 >> 8) & 0xFF;
               int b2 = rgb2 & 0xFF;
               float ratio = (float) tick / windup;
               int r = (int) (r1 + ratio * (r2 - r1));
               int g = (int) (g1 + ratio * (g2 - g1));
               int b = (int) (b1 + ratio * (b2 - b1));
               int rgb3 = (r << 16) | (g << 8) | b;
               
               DustParticleOptions dust = new DustParticleOptions(rgb3,0.25f+1.75f*((float) tick / windup));
               spawnLongParticle(world,dust,centerPosAdjust.x,centerPosAdjust.y,centerPosAdjust.z,delta,delta,delta,0,1);
            }else{
               int r = 20;
               int g = 190+(tick % 40);
               int b = 30+2*(tick+20 % 40);
               DustParticleOptions dust1 = new DustParticleOptions((r << 16) | (g << 8) | b,1);
               DustParticleOptions dust2 = new DustParticleOptions((r << 16) | (g-100 << 8) | b-15,0.6f);
               
               spawnLongParticle(world,dust1,centerPosAdjust.x,centerPosAdjust.y,centerPosAdjust.z,delta,delta,delta,0,1);
               spawnLongParticle(world,dust2,helixPosAdjust1.x,helixPosAdjust1.y,helixPosAdjust1.z,0,0,0,0,1);
               spawnLongParticle(world,dust2,helixPosAdjust2.x,helixPosAdjust2.y,helixPosAdjust2.z,0,0,0,0,1);
            }
         }
      }
   }
}
