package com.gmail.necnionch.myplugin.cerespawnscreen;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class SilentCommandSender implements CommandSender {
    @Override
    public void sendMessage(@NotNull String message) {

    }

    @Override
    public void sendMessage(@NotNull String[] messages) {

    }

    @Override
    public @NotNull Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public @NotNull String getName() {
        return "SilentCommandSender";
    }

    @Override
    public @NotNull Spigot spigot() {
        return Bukkit.getConsoleSender().spigot();
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return Bukkit.getConsoleSender().isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return Bukkit.getConsoleSender().isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return Bukkit.getConsoleSender().hasPermission(name);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return Bukkit.getConsoleSender().hasPermission(perm);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Bukkit.getConsoleSender().getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
    }

}
