package io.github.legendaryswords.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelConfig;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.animation.AnimationProgress;
import kr.toxicity.model.api.animation.AnimationStateHandler;
import kr.toxicity.model.api.bone.BoneMovement;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.platform.PlatformAdapter;
import kr.toxicity.model.api.platform.PlatformItemStack;
import org.junit.jupiter.api.Test;

/** Executes imported limb frames through BetterModel rather than inspecting raw JSON only. */
class BetterModelMotionPlaybackTest {
    @Test void importedWalkingAndRunningClipsAdvanceLegRotationsAcrossMultipleCycles() throws Exception {
        // Model import compiles bones on worker threads; static mocks are thread-local.
        var singleton = BetterModel.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        Object previousPlatform = singleton.get(null);
        try {
            BetterModelConfig config = mock(BetterModelConfig.class);
            when(config.lerpFrameTime()).thenReturn(1);
            BetterModelPlatform platform = mock(BetterModelPlatform.class);
            PlatformAdapter adapter = mock(PlatformAdapter.class);
            when(platform.adapter()).thenReturn(adapter);
            when(platform.config()).thenReturn(config);
            when(adapter.air()).thenReturn(mock(PlatformItemStack.class));
            BetterModel.register(platform);
            for (String sword : List.of("master_sword", "flameblade", "fierce_deity_sword", "frostblade",
                    "thunderblade", "royal_claymore", "eightfold_blade", "biggoron_sword")) {
                String name = "legendaryswords_" + sword;
                try (var stream = getClass().getResourceAsStream("/bettermodel/players/" + name + ".bbmodel")) {
                    assertNotNull(stream, name);
                    ModelData raw = ModelData.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), ModelData.class);
                    var imported = raw.loadBlueprint(name, true);
                    assertTrue(imported.errors().isEmpty(), () -> name + ": " + imported.errors());
                    for (String clip : List.of("walk", "run")) {
                        var animation = imported.blueprint().animations().get(clip);
                        assertNotNull(animation, name + ": " + clip);
                        for (String leg : List.of("right_leg", "left_leg", "right_foreleg", "left_foreleg")) {
                            var animator = animation.animator().entrySet().stream()
                                    .filter(entry -> entry.getKey().name().equals(leg)).findFirst().orElseThrow();
                            Set<Integer> firstCycle = new HashSet<>(), secondCycle = new HashSet<>();
                            BoneMovement identity = new BoneMovement(), animated = new BoneMovement();
                            var state = new AnimationStateHandler<AnimationProgress>(AnimationProgress.EMPTY, (before, after) -> {});
                            state.addAnimation(clip, animator.getValue().iterator(AnimationIterator.Type.LOOP),
                                    AnimationModifier.builder().start(3).priority(10).type(AnimationIterator.Type.LOOP).build(), () -> {});
                            int cycleTicks = (int) Math.ceil(animation.length() * 40D);
                            for (int tick = 0; tick < cycleTicks * 3; tick++) {
                                state.tick();
                                AnimationProgress progress = state.afterKeyframe();
                                if (progress == null) continue;
                                progress.animate(identity, animated);
                                int angle = Math.round(animated.rawRotation().x() * 1000F);
                                if (tick < cycleTicks) firstCycle.add(angle);
                                else if (tick >= cycleTicks * 2) secondCycle.add(angle);
                            }
                            String label = name + " " + clip + " " + leg;
                            assertTrue(firstCycle.size() > 4, label + " is static in its first cycle");
                            assertTrue(secondCycle.size() > 4, label + " stops after its first cycle");
                            assertEquals(clip, state.runningAnimation().name(), label);
                        }
                    }
                }
            }
        } finally { singleton.set(null, previousPlatform); }
    }
}
