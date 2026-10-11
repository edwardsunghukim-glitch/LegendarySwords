package io.github.legendarybosses;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.function.BonePredicate;
final class ModelHitboxes {
 static boolean hittable(RenderedBone bone){return !bone.isDummyBone()&&bone.getGroup().getHitBox()!=null;}
 static boolean install(EntityTracker tracker){
  // Native hitboxes follow bone animations/scaling and delegate damage to the source mob.
  return tracker.createHitBox(null,BonePredicate.from(ModelHitboxes::hittable));
 }
 private ModelHitboxes(){}
}
