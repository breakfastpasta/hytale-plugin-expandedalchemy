package com.github.breakfastpasta.expandedalchemy.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TeleportToPlayerPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final BuilderCodec<TeleportToPlayerPageSupplier> CODEC;

    public CustomUIPage tryCreate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, PlayerRef playerRef, InteractionContext interactionContext) {
        return new TeleportToPlayerPage(playerRef);
    }

    static {
        CODEC = BuilderCodec.builder(TeleportToPlayerPageSupplier.class, TeleportToPlayerPageSupplier::new).build();
    }
}
