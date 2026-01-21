package com.github.breakfastpasta.expandedalchemy.interactions;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class TeleportHomeInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<TeleportHomeInteraction> CODEC;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        final CommandBuffer<EntityStore> buffer = context.getCommandBuffer();
        if (buffer != null) {
            final Ref<EntityStore> ref = context.getEntity();
            final TransformComponent transform = buffer.getComponent(ref, TransformComponent.getComponentType());
            final World world = buffer.getExternalData().getWorld();

            if (transform != null) {
                final HeadRotation headRotation = buffer.getComponent(ref, HeadRotation.getComponentType());
                if (headRotation != null) {
                    final Vector3d oldPos = transform.getPosition().clone();
                    final Vector3f oldRot = headRotation.getRotation().clone();
                    buffer.ensureAndGetComponent(ref, TeleportHistory.getComponentType()).append(world, oldPos, oldRot, "Home");
                    buffer.addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(null, Player.getRespawnPosition(ref, world.getName(), buffer)));
                }
            }
        }
    }

    static {
        CODEC = BuilderCodec.builder(TeleportHomeInteraction.class, TeleportHomeInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Teleports a player to their respawn point")
                .build();
    }
}
