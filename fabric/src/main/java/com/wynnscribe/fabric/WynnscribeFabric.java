package com.wynnscribe.fabric;

import com.wynnscribe.*;
import com.wynnscribe.schemas.TranslationRepository;
import com.wynnscribe.wynntils.EventHandler;
import com.wynntils.core.WynntilsMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.wynnscribe.Wynnscribe.MOD_ID;

public class WynnscribeFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Wynnscribe.INSTANCE.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if(FabricLoader.getInstance().isModLoaded("wynntils")) {
                WynntilsMod.registerEventListener(new EventHandler());
            }
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleResourceReloadListener<TranslationRepository>() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.tryBuild(MOD_ID, "translations");
            }

            @Override
            public CompletableFuture<TranslationRepository> load(ResourceManager manager, Executor executor) {
                return CompletableFuture.supplyAsync(()->{
                    try {
                        return TranslationRepository.read(Minecraft.getInstance().getLanguageManager().getSelected());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
            }

            @Override
            public CompletableFuture<Void> apply(TranslationRepository data, ResourceManager manager, Executor executor) {
                return CompletableFuture.supplyAsync(()->{
                    if(data != null) {
                        TranslationRepository.load(data);
                    }
                    return null;
                });
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (KeyMappings.RegisteredKeyMapping mapping : KeyMappings.INSTANCE.getDefaultMappings()) {
                if(mapping == null) continue;
                if(mapping.consumeClick()) {
                    mapping.run();
                }
            }
        });
    }
}
