package com.gmail.necnionch.myplugin.cerespawnscreen;

import dev.jorel.commandapi.wrappers.FunctionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RespawnPlayer {

    private final Player player;
    private final int delay;
    private final @Nullable FunctionWrapper[] functions;
    private GameMode gameMode;
    private boolean flying;
    private boolean allowFlight;
    private boolean canPickupItems;
    private boolean collidable;

    public RespawnPlayer(Player player, int delay, @Nullable FunctionWrapper[] functions) {
        this.player = player;
        this.delay = delay;
        this.functions = functions;
    }

    public RespawnPlayer savePlayerState(boolean specSet) {
        gameMode = player.getGameMode();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        canPickupItems = player.getCanPickupItems();
        collidable = player.isCollidable();

        if (specSet) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setCanPickupItems(false);
            player.setCollidable(false);
        }
        return this;
    }

    public void restorePlayerState() {
        Optional.ofNullable(gameMode)
                .ifPresent(player::setGameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
        player.setCanPickupItems(canPickupItems);
        player.setCollidable(collidable);
    }


    public Player getPlayer() {
        return player;
    }

    public int getDelay() {
        return delay;
    }

    public FunctionWrapper[] getFunctions() {
        return functions != null ? functions : new FunctionWrapper[0];
    }

    public void executeFunctions() {
        if (functions == null)
            return;
//        SilentCommandSender sender = new SilentCommandSender();

        Location location = player.getLocation();
        location.setY(-256);
        CommandMinecart sender = (CommandMinecart) player.getWorld().spawnEntity(location, EntityType.MINECART_COMMAND);
        try {
            for (FunctionWrapper function : functions) {
                String funcKey = function.getKey().toString();
                Bukkit.dispatchCommand(sender, String.format("execute as %s at @s run function %s", player.getUniqueId(), funcKey));
            }
        } finally {
            sender.remove();
        }
    }

}
