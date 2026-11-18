package com.example.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatLogPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Long> combatTagTime = new HashMap<>();
    private final int COMBAT_DURATION_SECONDS = 20;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("combat").setExecutor(this);
        getLogger().info("CombatLogPlugin byl úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
        combatTagTime.clear();
    }

    // Když hráč trefí jiného hráče
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();

            tagPlayer(victim);
            tagPlayer(damager);
        }
    }

    private void tagPlayer(Player player) {
        long expireTime = System.currentTimeMillis() + (COMBAT_DURATION_SECONDS * 1000L);
        boolean wasInCombat = isInCombat(player);

        combatTagTime.put(player.getUniqueId(), expireTime);

        // Přidáme červenou záři (Glowing efekt)
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, COMBAT_DURATION_SECONDS * 20, 0, false, false));

        if (!wasInCombat) {
            player.sendMessage(ChatColor.RED + "⚠ Jsi v bojovém stavu (Combat)! Teleportace je zakázána na 20 sekund.");
        }
    }

    private boolean isInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTagTime.containsKey(uuid)) return false;

        if (System.currentTimeMillis() < combatTagTime.get(uuid)) {
            return true;
        } else {
            combatTagTime.remove(uuid);
            return false;
        }
    }

    // Blokování teleportů v combatu
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (isInCombat(player)) {
            String message = event.getMessage().toLowerCase();

            if (message.startsWith("/home") || message.startsWith("/spawn") || 
                message.startsWith("/tp") || message.startsWith("/warp") || 
                message.startsWith("/back") || message.startsWith("/rtp") || 
                message.startsWith("/cave") || message.startsWith("/aurora")) {
                
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "⚠ Nemůžeš použít teleport, dokud jsi v boji!");
            }
        }
    }

    // Trest za odpojení v combatu: zabije ho a věci přirozeně vypadnou
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isInCombat(player)) {
            player.setHealth(0.0); // Zabije ho to na místě, věci vypadnou na zem a hra napíše úmrtní hlášku
            Bukkit.broadcastMessage(ChatColor.RED + "⚡ Hráč " + player.getName() + " se odpojil v boji a zemřel!");
        }
        combatTagTime.remove(player.getUniqueId());
    }

    // Příkaz /combat pro zjištění stavu
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (isInCombat(player)) {
            long remaining = (combatTagTime.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            player.sendMessage(ChatColor.YELLOW + "⚠ Jsi v combatu ještě " + remaining + " sekund!");
        } else {
            player.sendMessage(ChatColor.GREEN + "✔ Nejsi v combatu, můžeš bezpečně teleportovat.");
        }
        return true;
    }
}
