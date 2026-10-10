package io.github.legendaryswords.integration;

import com.google.gson.*;
import io.github.legendaryswords.sword.SwordType;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import kr.toxicity.model.api.*;
import kr.toxicity.model.api.data.blueprint.*;
import kr.toxicity.model.api.data.raw.ModelData;
import kr.toxicity.model.api.pack.PackObfuscator;
import kr.toxicity.model.api.platform.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Export official blueprint geometry for the reported snapshot's named item aliases. */
class BetterModelPackExportTest {
    @Test void newSignatureEffectsExportAllNamedItemsAndTexturesUsingOfficialCompiler() throws Exception {
        var singleton = BetterModel.class.getDeclaredField("instance"); singleton.setAccessible(true);
        Object previous = singleton.get(null);
        try {
            BetterModelConfig config = mock(BetterModelConfig.class);
            when(config.namespace()).thenReturn("bettermodel"); when(config.lerpFrameTime()).thenReturn(1);
            BetterModelPlatform platform = mock(BetterModelPlatform.class);
            PlatformAdapter adapter = mock(PlatformAdapter.class);
            when(platform.adapter()).thenReturn(adapter); when(platform.config()).thenReturn(config);
            when(adapter.air()).thenReturn(mock(PlatformItemStack.class)); BetterModel.register(platform);
            Path root = Path.of("target/bettermodel-expansion-pack");
            AtomicInteger aliases = new AtomicInteger();
            for (SwordType sword : SwordType.values()) {
                if (sword.ordinal() < 8) continue;
                String name = "legendaryswords_" + sword.id() + "_slash";
                try (var input = getClass().getResourceAsStream("/bettermodel/models/" + name + ".bbmodel")) {
                    assertNotNull(input);
                    ModelData raw = ModelData.GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), ModelData.class);
                    var loaded = raw.loadBlueprint(name, false);
                    assertTrue(loaded.errors().isEmpty(), loaded.errors().toString());
                    var blueprint = loaded.blueprint(); var context = blueprint.context();
                    for (BlueprintElement element : blueprint.elements()) {
                        if (!(element instanceof BlueprintElement.Group group)) continue;
                        var models = group.buildModernJson(PackObfuscator.pair(PackObfuscator.NONE, PackObfuscator.NONE), context);
                        assertNotNull(models); assertFalse(models.isEmpty());
                        JsonArray components = new JsonArray();
                        for (var model : models) {
                            write(root.resolve("assets/bettermodel/models/legendaryswords_expansion/" + model.name() + ".json"), model.buildJson().toString().getBytes(StandardCharsets.UTF_8));
                            JsonObject renderer = new JsonObject(); renderer.addProperty("type", "minecraft:model");
                            renderer.addProperty("model", "bettermodel:legendaryswords_expansion/" + model.name());
                            JsonArray tints = new JsonArray(); JsonObject tint = new JsonObject();
                            tint.addProperty("type", "minecraft:custom_model_data"); tint.addProperty("default", 0xFFFFFF); tints.add(tint); renderer.add("tints", tints); components.add(renderer);
                        }
                        JsonObject item = new JsonObject(), renderer = new JsonObject();
                        renderer.addProperty("type", "minecraft:composite"); renderer.add("models", components); item.add("model", renderer);
                        write(root.resolve("assets/bettermodel/items/c/" + name + "_" + group.name().rawName() + ".json"), item.toString().getBytes(StandardCharsets.UTF_8));
                        aliases.incrementAndGet();
                    }
                    for (var image : context.buildImage(PackObfuscator.NONE).toList()) {
                        write(root.resolve("assets/bettermodel/textures/item/" + image.pngName()), image.image());
                        if (image.mcmeta() != null) write(root.resolve("assets/bettermodel/textures/item/" + image.mcmetaName()), image.mcmeta().toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            assertEquals((SwordType.values().length - 8) * 3, aliases.get());
        } finally { singleton.set(null, previous); }
    }
    private static void write(Path file, byte[] bytes) throws Exception {
        Files.createDirectories(file.getParent()); Files.write(file, bytes);
    }
}
