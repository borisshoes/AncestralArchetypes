package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.borislib.sequences.CameraPath;
import net.borisshoes.borislib.sequences.CutsceneSequence;

import java.util.UUID;

/**
 * Marker subclass of {@link CutsceneSequence} used exclusively for the Creaking Heart anchored-respawn flow.
 *
 * <p>It behaves identically to a normal spectator cutscene (no mannequin stand-in) but exists as a distinct type so
 * {@link net.borisshoes.borislib.sequences.SequenceManager#isInSequence(UUID, Class)} can reliably tell whether a
 * player is currently inside <em>our</em> respawn cutscene versus any other cutscene that might be running.
 *
 * @see CreakingRespawnManager
 */
public class CreakingRespawnCutscene extends CutsceneSequence {
   
   public CreakingRespawnCutscene(UUID playerUUID, CameraPath path, int durationTicks){
      super(playerUUID, path, durationTicks, false);
   }
}
