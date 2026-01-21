package com.github.breakfastpasta.expandedalchemy;

import com.github.breakfastpasta.expandedalchemy.commands.AlchemyCommand;
import com.github.breakfastpasta.expandedalchemy.interactions.TeleportHomeInteraction;
import com.github.breakfastpasta.expandedalchemy.ui.TeleportToPlayerPageSupplier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class ExpandedAlchemy extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ExpandedAlchemy(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry()
                .registerCommand(new AlchemyCommand(this.getName(), this.getManifest().getVersion().toString()));

        this.getCodecRegistry(Interaction.CODEC)
                .register("EATeleportHome", TeleportHomeInteraction.class, TeleportHomeInteraction.CODEC);

        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC)
                .register("EATeleportToPlayerPage", TeleportToPlayerPageSupplier.class, TeleportToPlayerPageSupplier.CODEC);
    }
}
