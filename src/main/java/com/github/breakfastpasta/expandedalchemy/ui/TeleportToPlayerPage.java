package com.github.breakfastpasta.expandedalchemy.ui;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportToPlayerPage extends InteractiveCustomUIPage<TeleportToPlayerPage.TeleportToPlayerPageEventData> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Nonnull
    private static final String PAGE_UI_FILE = "Pages/PlayerListEntryButton.ui";
    private final ConcurrentHashMap<String, Ref<EntityStore>> players = new ConcurrentHashMap<>();
    @Nonnull
    private String searchQuery = "";

    public TeleportToPlayerPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, TeleportToPlayerPageEventData.CODEC);
    }

    private CompletableFuture<Void> buildPlayerMap(@Nonnull Ref<EntityStore> ref) {
        Map<String, World> worlds = Universe.get().getWorlds();
        ObjectArrayList<CompletableFuture<Void>> futures = new ObjectArrayList<>();

        for (Map.Entry<String, World> entry : worlds.entrySet()) {
            World world = entry.getValue();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();
                Collection<PlayerRef> playerRefs = world.getPlayerRefs();

                for (PlayerRef playerRef : playerRefs) {
                    Ref<EntityStore> curRef = playerRef.getReference();
                    if (curRef != null && curRef.isValid() && curRef != ref) {
                        Player playerComponent = store.getComponent(curRef, Player.getComponentType());
                        if (playerComponent != null) {
                            DisplayNameComponent displayNameComponent = store.getComponent(curRef, DisplayNameComponent.getComponentType());

                            assert displayNameComponent != null;

                            Message displayName = displayNameComponent.getDisplayName();
                            if (displayName != null) {
                                this.players.put(displayName.getAnsiMessage(), curRef);
                            } else {
                                this.players.put(playerRef.getUsername(), curRef);
                            }
                        }
                    }
                }
            }, world);
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void updatePlayerList(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#PlayerList");
        ObjectArrayList<String> playerNames = new ObjectArrayList<>(players.keySet());
        if (playerNames.isEmpty()) {
            commandBuilder.appendInline("#PlayerList", "Label { Text: %server.customUI.teleportToPlayerListPage.noPlayers; Style: (Alignment: Center); }");
        } else {
            if (!searchQuery.isEmpty()) {
                playerNames.removeIf((p) -> !p.toLowerCase().contains(this.searchQuery));
            }

            Collections.sort(playerNames);
            int i = 0;

            for (int bound = playerNames.size(); i < bound; ++i) {
                String selector = "#PlayerList[" + i + "]";
                String playerName = playerNames.get(i);
                commandBuilder.append("#PlayerList", "Pages/PlayerListEntryButton.ui");
                commandBuilder.set(selector + " #Name.Text", playerName);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Player", playerName), false);
            }
        }
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/TeleportToPlayerPage.ui");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"));
        buildPlayerMap(ref)
                .thenRun(() -> {
                    UICommandBuilder updateCommandBuilder = new UICommandBuilder();
                    UIEventBuilder updateEventBuilder = new UIEventBuilder();
                    updatePlayerList(updateCommandBuilder, updateEventBuilder);
                    sendUpdate(updateCommandBuilder, updateEventBuilder, false);
                });
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull TeleportToPlayerPageEventData eventData) {
        if (eventData.getPlayer() != null) {
            World world = store.getExternalData().getWorld();
            Ref<EntityStore> targetRef = players.get(eventData.getPlayer());
            // check validity again (maybe they disconnected?)
            if (targetRef.isValid()) {
                close();

                Store<EntityStore> targetStore = targetRef.getStore();
                World targetWorld = targetStore.getExternalData().getWorld();
                TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());

                assert transformComponent != null;

                HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());

                assert headRotationComponent != null;

                Vector3d oldPos = transformComponent.getPosition().clone();
                Vector3f oldRot = headRotationComponent.getRotation().clone();
                targetWorld.execute(() -> {
                    TransformComponent targetTransformComponent = targetStore.getComponent(targetRef, TransformComponent.getComponentType());

                    assert targetTransformComponent != null;

                    HeadRotation targetHeadRotationComponent = targetStore.getComponent(targetRef, HeadRotation.getComponentType());

                    assert targetHeadRotationComponent != null;

                    Vector3d targetPosition = targetTransformComponent.getPosition().clone();
                    Vector3f targetRotation = targetHeadRotationComponent.getRotation().clone();
                    Transform targetTransform = new Transform(targetPosition, targetRotation);
                    world.execute(() -> {
                        Teleport teleportComponent = Teleport.createForPlayer(targetWorld, targetTransform);
                        store.addComponent(ref, Teleport.getComponentType(), teleportComponent);
                        PlayerRef targetPlayerRefComponent = targetStore.getComponent(targetRef, PlayerRef.getComponentType());

                        assert targetPlayerRefComponent != null;

                        store.ensureAndGetComponent(ref, TeleportHistory.getComponentType()).append(world, oldPos, oldRot, "Teleport to " + targetPlayerRefComponent.getUsername());
                    });
                });
            }
        } else if (eventData.getSearchQuery() != null) {
            searchQuery = eventData.getSearchQuery().trim().toLowerCase();

            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            updatePlayerList(commandBuilder, eventBuilder);
            sendUpdate(commandBuilder, eventBuilder, false);
        }
    }

    public static class TeleportToPlayerPageEventData {
        static final String KEY_PLAYER = "Player";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        @Nonnull
        public static final BuilderCodec<TeleportToPlayerPageEventData> CODEC;
        private String player;
        private String searchQuery;

        public String getPlayer() { return player; }

        public String getSearchQuery() { return searchQuery; }

        static {
            CODEC = BuilderCodec.builder(TeleportToPlayerPageEventData.class, TeleportToPlayerPageEventData::new)
                    .append(new KeyedCodec<>("Player", Codec.STRING), (entry, s) -> entry.player = s, (entry) -> entry.player)
                    .add()
                    .append(new KeyedCodec<>("@SearchQuery", Codec.STRING), (entry, s) -> entry.searchQuery = s, (entry) -> entry.searchQuery)
                    .add()
                    .build();
        }
    }
}
