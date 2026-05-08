package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarManager implements Listener {

    private final TotemAltars plugin;
    private final File dataFile;
    private YamlConfiguration data;

    private final List<Location> altars = new ArrayList<>();
    private final Map<String, TextDisplay> holograms = new HashMap<>();

    public AltarManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "altars.yml");
        load();
    }

    // ── Persistence ───────────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create altars.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : data.getStringList("altars")) {
            Location loc = fromKey(key);
            if (loc != null) {
                altars.add(loc);
                spawnHologram(loc);
            } else {
                plugin.getLogger().warning("Could not load altar '" + key + "' — world not loaded yet.");
            }
        }
        plugin.getLogger().info("Loaded " + altars.size() + " altar(s).");
    }

    private void save() {
        List<String> keys = new ArrayList<>();
        for (Location loc : altars) keys.add(toKey(loc));
        data.set("altars", keys);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save altars.yml: " + e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    public boolean createAltar(Location loc) {
        Location block = loc.getBlock().getLocation();
        if (isAltar(block)) return false;
        altars.add(block);
        spawnHologram(block);
        save();
        return true;
    }

    public boolean removeAltar(Location loc) {
        String key = toKey(loc.getBlock().getLocation());
        boolean removed = altars.removeIf(l -> toKey(l).equals(key));
        if (removed) {
            removeHologram(key);
            save();
        }
        return removed;
    }

    public boolean isAltar(Location loc) {
        String key = toKey(loc.getBlock().getLocation());
        return altars.stream().anyMatch(l -> toKey(l).equals(key));
    }

    /** Returns the nearest altar in the same world, or the first altar in any world if none match. */
    public Location getNearestAltar(Location from) {
        Location nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Location altar : altars) {
            if (!altar.getWorld().equals(from.getWorld())) continue;
            double dist = from.distanceSquared(altar);
            if (dist < minDist) {
                minDist = dist;
                nearest = altar;
            }
        }
        if (nearest != null) return nearest;
        return altars.isEmpty() ? null : altars.get(0);
    }

    public int getAltarCount() {
        return altars.size();
    }

    // ── Holograms ─────────────────────────────────────────────────────────────────

    private void spawnHologram(Location altarLoc) {
        World world = altarLoc.getWorld();
        if (world == null) return;

        String key = toKey(altarLoc);
        double cx = altarLoc.getBlockX() + 0.5;
        double cz = altarLoc.getBlockZ() + 0.5;

        spawnLine(key + "_top", world, cx, altarLoc.getBlockY() + 2.0, cz,
                Component.text("Totem Altar")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));

        spawnLine(key + "_bot", world, cx, altarLoc.getBlockY() + 1.5, cz,
                Component.text("Right-click to enchant")
                        .color(NamedTextColor.YELLOW));
    }

    private void spawnLine(String holoKey, World world, double x, double y, double z, Component text) {
        TextDisplay entity = world.spawn(new Location(world, x, y, z), TextDisplay.class, td -> {
            td.text(text);
            td.setPersistent(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setDefaultBackground(false);
            td.setAlignment(TextDisplay.TextAlignment.CENTER);
            td.setShadowed(true);
        });
        holograms.put(holoKey, entity);
    }

    private void removeHologram(String key) {
        TextDisplay top = holograms.remove(key + "_top");
        TextDisplay bot = holograms.remove(key + "_bot");
        if (top != null && top.isValid()) top.remove();
        if (bot != null && bot.isValid()) bot.remove();
    }

    public void disable() {
        for (TextDisplay td : holograms.values()) {
            if (td != null && td.isValid()) td.remove();
        }
        holograms.clear();
    }

    // ── Respawn holograms when their chunk reloads ────────────────────────────────

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk loaded = event.getChunk();
        for (Location altarLoc : altars) {
            World altarWorld = altarLoc.getWorld();
            if (altarWorld == null || !altarWorld.equals(event.getWorld())) continue;
            if (altarLoc.getChunk().getX() != loaded.getX() ||
                altarLoc.getChunk().getZ() != loaded.getZ()) continue;

            String key = toKey(altarLoc);
            TextDisplay top = holograms.get(key + "_top");
            TextDisplay bot = holograms.get(key + "_bot");
            if (top == null || !top.isValid() || bot == null || !bot.isValid()) {
                removeHologram(key);
                plugin.getServer().getScheduler().runTask(plugin, () -> spawnHologram(altarLoc));
            }
        }
    }

    // ── Key helpers ───────────────────────────────────────────────────────────────

    private String toKey(Location loc) {
        return loc.getWorld().getName() + ":"
                + loc.getBlockX() + ":"
                + loc.getBlockY() + ":"
                + loc.getBlockZ();
    }

    private Location fromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
