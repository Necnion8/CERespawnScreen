package com.gmail.necnionch.myplugin.cerespawnscreen;

import dev.jorel.commandapi.wrappers.FunctionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public class RespawnPlayer {

    private final Player player;
    private final int delay;
    private @Nullable FunctionWrapper[] functions;
    private GameMode gameMode;
    private boolean flying;
    private boolean allowFlight;
    private boolean canPickupItems;
    private boolean collidable;
    private @Nullable Collection<PotionEffect> activePotionEffects;

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
        activePotionEffects = player.getActivePotionEffects();

        if (specSet) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setCanPickupItems(false);
            player.setCollidable(false);
            player.setFireTicks(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
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
        player.setFireTicks(0);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Optional.ofNullable(activePotionEffects).ifPresent(player::addPotionEffects);
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

    public void setFunctions(FunctionWrapper[] functions) {
        this.functions = functions;
    }

    public void executeFunctions() {
        if (functions == null)
            return;

        Location location = player.getLocation();
        location.setY(-256);
        CommandMinecart sender = (CommandMinecart) player.getWorld().spawnEntity(location, EntityType.MINECART_COMMAND);
        try {
            for (FunctionWrapper function : functions) {
                String funcKey = function.getKey().toString();
                Bukkit.dispatchCommand(sender, String.format("execute as \"%s\" at @s run function %s", player.getUniqueId(), funcKey));
            }
        } finally {
            sender.remove();
        }
    }

}
