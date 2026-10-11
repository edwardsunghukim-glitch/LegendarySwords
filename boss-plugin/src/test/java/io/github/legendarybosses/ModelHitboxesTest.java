package io.github.legendarybosses;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.util.function.BonePredicate;
import kr.toxicity.model.api.data.blueprint.ModelBoundingBox;
class ModelHitboxesTest {
 @Test void includesGeometryButExcludesDummyOrEmptyBones(){RenderedBone bone=mock(RenderedBone.class,RETURNS_DEEP_STUBS);when(bone.getGroup().getHitBox()).thenReturn(mock(ModelBoundingBox.class));assertTrue(ModelHitboxes.hittable(bone));when(bone.isDummyBone()).thenReturn(true);assertFalse(ModelHitboxes.hittable(bone));when(bone.isDummyBone()).thenReturn(false);when(bone.getGroup().getHitBox()).thenReturn(null);assertFalse(ModelHitboxes.hittable(bone));}
 @Test void usesNativeDelegationInsteadOfApplyingDamageTwice(){try(var engine=mockStatic(kr.toxicity.model.api.BetterModel.class)){var platform=mock(kr.toxicity.model.api.BetterModelPlatform.class,RETURNS_DEEP_STUBS);engine.when(kr.toxicity.model.api.BetterModel::platform).thenReturn(platform);EntityTracker tracker=mock(EntityTracker.class);when(tracker.createHitBox(isNull(),any(BonePredicate.class))).thenReturn(true);assertTrue(ModelHitboxes.install(tracker));verify(tracker).createHitBox(isNull(),any(BonePredicate.class));}}
}
