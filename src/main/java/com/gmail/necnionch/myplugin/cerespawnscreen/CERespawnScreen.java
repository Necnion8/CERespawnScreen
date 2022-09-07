package com.gmail.necnionch.myplugin.cerespawnscreen;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CERespawnScreen extends JavaPlugin implements Listener {
    public static final String SCORE_NAME = "CERespawnTimer";
    private final Set<Player> spectators = Sets.newHashSet();
    private final Map<Player, RespawnPlayer> settings = Maps.newHashMap();
    private final Map<Player, BukkitTask> spectatorsTimer = Maps.newHashMap();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("cerespawn")
                .withPermission("cerespawnscreen.command.cerespawn")
                .withSubcommand(new CommandAPICommand("reset")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .executesNative(this::execReset))
                .withSubcommand(new CommandAPICommand("set")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("respawnSeconds"))
                        .withArguments(new FunctionArgument("timerFunction"))
                        .executesNative(this::execSet))
                .withSubcommand(new CommandAPICommand("set")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("respawnSeconds"))
                        .executesNative(this::execSet))
                .withSubcommand(new CommandAPICommand("start")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("respawnSeconds"))
                        .withArguments(new FunctionArgument("timerFunction"))
                        .executesNative(this::execStart))
                .withSubcommand(new CommandAPICommand("start")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new IntegerArgument("respawnSeconds"))
                        .executesNative(this::execStart))
                .withSubcommand(new CommandAPICommand("start")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .executesNative(this::execStart))
                .withSubcommand(new CommandAPICommand("stop")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .withArguments(new FunctionArgument("timerFunction"))
                        .executesNative(this::execStop))
                .withSubcommand(new CommandAPICommand("stop")
                        .withArguments(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
                        .executesNative(this::execStop))
                .register();
    }

    @Override
    public void onDisable() {
        CommandAPI.unregister("cerespawn", true);
    }


    public void startRespawnScreen(RespawnPlayer respawn) {
        Player player = respawn.getPlayer();

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.hidePlayer(this, player);
        });

        player.getWorld().getEntitiesByClass(Mob.class).forEach(e -> {
            if (player.equals(e.getTarget()))
                e.setTarget(null);
        });

        spectators.add(player);
        respawn.savePlayerState(true);

        Score score = updateTimerScore(player, Math.max(1, respawn.getDelay()));
        respawn.executeFunctions();

        spectatorsTimer.put(player, getServer().getScheduler().runTaskTimer(this, () -> {
            int rem = score.getScore() - 1;
            score.setScore(rem);

            if (rem <= 0) {
                stopRespawnScreen(respawn);
                return;
            }

            respawn.executeFunctions();

        }, 20, 20));
    }

    public void stopRespawnScreen(RespawnPlayer respawn) {
        Player player = respawn.getPlayer();
        spectators.remove(player);
        respawn.restorePlayerState();
        player.setFallDistance(0);

        Optional.ofNullable(spectatorsTimer.remove(player))
                .ifPresent(BukkitTask::cancel);

        updateTimerScore(player, 0);
        respawn.executeFunctions();

        resetTimerScore(player);

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.showPlayer(this, player);
        });
    }

    public Score updateTimerScore(Player player, int score) {
        Scoreboard sb = getServer().getScoreboardManager().getMainScoreboard();
        Objective objective = sb.getObjective(SCORE_NAME);
        if (objective == null)
            objective = sb.registerNewObjective(SCORE_NAME, "dummy", SCORE_NAME);

        Score eScore = objective.getScore(player.getName());
        eScore.setScore(score);
        return eScore;
    }

    public void resetTimerScore(Player player) {
        Scoreboard sb = getServer().getScoreboardManager().getMainScoreboard();
        Objective objective = sb.getObjective(SCORE_NAME);
        if (objective == null)
            return;

        // get setting
        DisplaySlot displaySlot = objective.getDisplaySlot();
        RenderType renderType = objective.getRenderType();
        String displayName = objective.getDisplayName();
        String criteria = objective.getCriteria();

        // get scores
        Map<String, Integer> scoreValues = sb.getEntries().stream()
                .map(objective::getScore)
                .filter(Score::isScoreSet)
                .filter(s -> !s.getEntry().equals(player.getName()))
                .collect(Collectors.toMap(Score::getEntry, Score::getScore));

        // recreate
        objective.unregister();
        Objective newObjective = sb.registerNewObjective(SCORE_NAME, criteria, displayName, renderType);
        newObjective.setDisplaySlot(displaySlot);

        // rollback scores
        scoreValues.forEach((e, v) -> {
            newObjective.getScore(e).setScore(v);
        });
    }


    private int execReset(NativeProxyCommandSender sender, Object[] args) {
        @SuppressWarnings("unchecked")
        List<Player> players = (List<Player>) args[0];
        settings.keySet().removeIf(players::contains);
        if (!players.isEmpty())
            sender.sendMessage(ChatColor.GOLD + "対象のプレイヤーのリスポーンスクリーン設定を解除しました");
        return players.size();
    }

    private int execSet(NativeProxyCommandSender sender, Object[] args) {
        @SuppressWarnings("unchecked")
        List<Player> players = (List<Player>) args[0];
        int delay = (int) args[1];
        FunctionWrapper[] functions = (args.length >= 3) ? (FunctionWrapper[]) args[2] : null;

        players.forEach(p -> {
            settings.put(p, new RespawnPlayer(p, delay, functions));
        });
        if (!players.isEmpty())
            sender.sendMessage(ChatColor.GOLD + "対象のプレイヤーのリスポーンスクリーンを設定しました");
        return players.size();
    }

    private int execStart(NativeProxyCommandSender sender, Object[] args) {
        @SuppressWarnings("unchecked")
        List<Player> players = Lists.newArrayList((List<Player>) args[0]);
        int delay = (args.length >= 2 ) ? (int) args[1] : -1;
        FunctionWrapper[] functions = (args.length >= 3) ? (FunctionWrapper[]) args[2] : null;

        players.removeIf(spectators::contains);
        players.forEach(p -> {
            RespawnPlayer setting = settings.get(p);
            setting = new RespawnPlayer(p,
                    (delay != -1) ? delay : ((setting != null) ? setting.getDelay() : 10),
                    (functions == null && setting != null) ? setting.getFunctions() : functions
            );
            settings.put(p, setting);
            startRespawnScreen(setting);
        });
        if (!players.isEmpty())
            sender.sendMessage(ChatColor.GOLD + "対象のプレイヤーのリスポーンスクリーンを開始しました");
        return players.size();
    }

    private int execStop(NativeProxyCommandSender sender, Object[] args) {
        @SuppressWarnings("unchecked")
        List<Player> players = Lists.newArrayList((List<Player>) args[0]);
        FunctionWrapper[] functions = (args.length >= 2) ? (FunctionWrapper[]) args[1] : null;

        players.removeIf(p -> !spectators.contains(p));
        players.forEach(p -> {
            RespawnPlayer setting = settings.get(p);
            if (setting == null) {
                setting = new RespawnPlayer(p, 0, functions);
            } else if (functions != null) {
                setting.setFunctions(functions);
            }
            stopRespawnScreen(setting);
        });
        if (!players.isEmpty())
            sender.sendMessage(ChatColor.GOLD + "対象のプレイヤーのリスポーンスクリーンを停止しました");
        return players.size();
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        spectators.forEach(p -> event.getPlayer().hidePlayer(this, p));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        spectators.remove(event.getPlayer());
        RespawnPlayer respawn = settings.remove(event.getPlayer());
        if (respawn != null)
            stopRespawnScreen(respawn);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // スペクテイタで死んだらそれはおかしい。
        if (!spectators.remove(event.getEntity())) {
            RespawnPlayer respawn = settings.get(event.getEntity());
            if (respawn != null)
                startRespawnScreen(respawn);
        }
    }

    @EventHandler  // (ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (spectators.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (spectators.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (spectators.contains(event.getPlayer())) {
            switch (event.getCause()) {
                case COMMAND:
                case PLUGIN:
                case SPECTATE:
                    break;
                default:
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (spectators.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player && spectators.contains(((Player) entity))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        spectators.remove(event.getPlayer());
    }


    @EventHandler(ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (spectators.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (spectators.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSwap(PlayerSwapHandItemsEvent event) {
        if (spectators.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (spectators.contains(player)) {
                event.setCancelled(true);
            } else if (player.getHealth() - event.getFinalDamage() <= 0 && settings.containsKey(player)) {
                event.setCancelled(true);
                player.setVelocity(new Vector());
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                player.setFoodLevel(20);
                player.getActivePotionEffects().forEach(eff -> player.removePotionEffect(eff.getType()));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1, 1);
                startRespawnScreen(settings.get(entity));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityAttack(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity instanceof Player && spectators.contains(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        Entity entity = event.getTarget();
        if (entity instanceof Player && spectators.contains(entity)) {
            event.setCancelled(true);
        }
    }

    // TODO: フルブロックは問題なく置ける。ハーフや雪が2段目以降置けなくなる問題あり。(感圧版も怪しい)
    @EventHandler
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        if (event.isBuildable()) return;

        Block block = event.getBlock();
        if (!event.getBlock().getType().equals(Material.AIR)) return;

        World world = block.getWorld();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        boolean denied = false;

        for (Entity e : world.getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
            if (e instanceof Player) {
                if (!spectators.contains(e)) {
                    denied = true;
                    break;
                }
            } else if (e instanceof LivingEntity) {
                denied = true;
                break;
            }
        }

        if (!denied) {
            event.setBuildable(true);
        }
    }

}

