package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Persistent map of Bedrock Relic blocks placed in the world.
 * GILDED_BLACKSTONE has no TileState/PDC, so plugin-side storage is used instead,
 * keyed by world:x:y:z.
 */
public class BedrockRelicBlockManager {
    private final TotemAltars plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<String, RelicBlockEntry> placedBlocks = new HashMap<>();

    public BedrockRelicBlockManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "bedrock_relic_blocks.yml");
        this.load();
    }

    public void register(Location loc, boolean activated) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        placedBlocks.put(makeKey(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                new RelicBlockEntry(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), activated));
        save();
    }

    public boolean isRelicBlock(Location loc) {
        return placedBlocks.containsKey(key(loc));
    }

    public boolean isActivatedBlock(Location loc) {
        RelicBlockEntry e = placedBlocks.get(key(loc));
        return e != null && e.activated;
    }

    public void unregister(Location loc) {
        if (placedBlocks.remove(key(loc)) != null) {
            save();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void load() {
        data = dataFile.exists() ? YamlConfiguration.loadConfiguration(dataFile) : new YamlConfiguration();
        placedBlocks.clear();
        for (Object obj : data.getList("blocks", new ArrayList<>())) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;
            try {
                String world = (String) rawMap.get("world");
                int x = ((Number) rawMap.get("x")).intValue();
                int y = ((Number) rawMap.get("y")).intValue();
                int z = ((Number) rawMap.get("z")).intValue();
                boolean activated = Boolean.TRUE.equals(rawMap.get("activated"));
                placedBlocks.put(makeKey(world, x, y, z), new RelicBlockEntry(world, x, y, z, activated));
            } catch (Exception ex) {
                plugin.getLogger().warning("[relic-blocks] Skipping malformed entry: " + ex.getMessage());
            }
        }
        if (!placedBlocks.isEmpty()) {
            plugin.getLogger().info("[relic-blocks] Loaded " + placedBlocks.size() + " placed Bedrock Relic block(s).");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void save() {
        ArrayList list = new ArrayList();
        for (RelicBlockEntry e : placedBlocks.values()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("world", e.world);
            map.put("x", e.x);
            map.put("y", e.y);
            map.put("z", e.z);
            map.put("activated", e.activated);
            list.add(map);
        }
        data.set("blocks", list);
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("[relic-blocks] Failed to save bedrock_relic_blocks.yml: " + ex.getMessage());
        }
    }

    private String key(Location loc) {
        return makeKey(loc.getWorld() != null ? loc.getWorld().getName() : "world",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String makeKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    private record RelicBlockEntry(String world, int x, int y, int z, boolean activated) {}
}
