package com.github.breakfastpasta.expandedalchemy.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class TeleportToSpawnInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<TeleportToSpawnInteraction> CODEC;

    @Override
    protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> ref = context.getEntity();
        Player playerComponent = (Player) commandBuffer.getComponent(ref, Player.getComponentType());

        if (playerComponent != null) {
            World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();

            Transform respawn = Player.getRespawnPosition(ref, world.getName(), commandBuffer);
            commandBuffer.addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(world, respawn));
        }
    }

    static {
        CODEC = BuilderCodec.builder(TeleportToSpawnInteraction.class, TeleportToSpawnInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Teleports a player to their respawn point")
                .build();
    }
}
