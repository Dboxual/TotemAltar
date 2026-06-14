package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public class AltarManager {
    private final TotemAltars plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<String, AltarEntry> altars = new HashMap<String, AltarEntry>();

    public AltarManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "altars.yml");
        this.load();
    }

    public void registerAltar(Location loc) {
        String key = this.key(loc);
        if (this.altars.containsKey(key)) {
            return;
        }
        this.altars.put(key, new AltarEntry(this.worldName(loc), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), null, null, 0));
        this.save();
    }

    public boolean isAltar(Location loc) {
        return this.altars.containsKey(this.key(loc));
    }

    public boolean isBound(Location loc) {
        AltarEntry entry = this.altars.get(this.key(loc));
        return entry != null && entry.type != null;
    }

    public String getType(Location loc) {
        AltarEntry entry = this.altars.get(this.key(loc));
        return entry != null ? entry.type : null;
    }

    public void bindAltar(Location loc, String type, UUID owner) {
        String key = this.key(loc);
        AltarEntry existing = this.altars.get(key);
        if (existing == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(existing.world, existing.x, existing.y, existing.z, type, owner, existing.stage));
        this.save();
    }

    public int getStage(Location loc) {
        AltarEntry entry = this.altars.get(this.key(loc));
        return entry != null ? entry.stage : 0;
    }

    public void incrementStage(Location loc) {
        String key = this.key(loc);
        AltarEntry e = this.altars.get(key);
        if (e == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(e.world, e.x, e.y, e.z, e.type, e.owner, e.stage + 1));
        this.save();
    }

    public boolean isActive(Location loc) {
        return this.getStage(loc) >= 1;
    }

    public void setActive(Location loc) {
        String key = this.key(loc);
        AltarEntry e = this.altars.get(key);
        if (e == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(e.world, e.x, e.y, e.z, e.type, e.owner, 1));
        this.save();
    }

    public void setStage(Location loc, int stage) {
        String key = this.key(loc);
        AltarEntry e = this.altars.get(key);
        if (e == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(e.world, e.x, e.y, e.z, e.type, e.owner, stage));
        this.save();
    }

    public boolean isAwaitingTotem(Location loc) {
        return this.getStage(loc) == 2;
    }

    public boolean isTotemReady(Location loc) {
        return this.getStage(loc) == 3;
    }

    public UUID getOwner(Location loc) {
        AltarEntry entry = this.altars.get(this.key(loc));
        return entry != null ? entry.owner : null;
    }

    public void transitionToTotemReady(Location loc) {
        String key = this.key(loc);
        AltarEntry e = this.altars.get(key);
        if (e == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(e.world, e.x, e.y, e.z, e.type, e.owner, 3));
        this.save();
    }

    public void acceptShard(Location loc, String shardType, UUID owner) {
        String key = this.key(loc);
        AltarEntry e = this.altars.get(key);
        if (e == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(e.world, e.x, e.y, e.z, shardType, owner, 2));
        this.save();
    }

    public void completeForging(Location loc) {
        String key = this.key(loc);
        AltarEntry e = this.altars.get(key);
        if (e == null) {
            return;
        }
        this.altars.put(key, new AltarEntry(e.world, e.x, e.y, e.z, null, null, 1));
        this.save();
    }

    public void removeAltar(Location loc) {
        if (this.altars.remove(this.key(loc)) != null) {
            this.save();
        }
    }

    public boolean hasAltarByKey(String key) {
        return this.altars.containsKey(key);
    }

    public Collection<Location> getAllAltarLocations() {
        ArrayList<Location> result = new ArrayList<Location>(this.altars.size());
        for (AltarEntry e : this.altars.values()) {
            World w = Bukkit.getWorld((String)e.world);
            if (w == null) continue;
            result.add(new Location(w, (double)e.x, (double)e.y, (double)e.z));
        }
        return Collections.unmodifiableList(result);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void load() {
        this.data = this.dataFile.exists() ? YamlConfiguration.loadConfiguration((File)this.dataFile) : new YamlConfiguration();
        this.altars.clear();
        for (Map map : this.data.getMapList("altars")) {
            try {
                String world = (String)map.get("world");
                int x = ((Number)map.get("x")).intValue();
                int y = ((Number)map.get("y")).intValue();
                int z = ((Number)map.get("z")).intValue();
                String type = (String)map.get("type");
                String ownerStr = (String)map.get("owner");
                UUID owner = ownerStr != null ? UUID.fromString(ownerStr) : null;
                int stage = map.get("stage") != null ? ((Number)map.get("stage")).intValue() : 0;
                this.altars.put(this.makeKey(world, x, y, z), new AltarEntry(world, x, y, z, type, owner, stage));
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("Skipping malformed altar entry in altars.yml: " + ex.getMessage());
            }
        }
        this.plugin.getLogger().info("Loaded " + this.altars.size() + " altar(s) from altars.yml.");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void save() {
        ArrayList list = new ArrayList();
        for (AltarEntry e : this.altars.values()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("world", e.world);
            map.put("x", e.x);
            map.put("y", e.y);
            map.put("z", e.z);
            if (e.type != null) {
                map.put("type", e.type);
            }
            if (e.owner != null) {
                map.put("owner", e.owner.toString());
            }
            if (e.stage > 0) {
                map.put("stage", e.stage);
            }
            list.add(map);
        }
        this.data.set("altars", list);
        try {
            this.data.save(this.dataFile);
        }
        catch (IOException ex) {
            this.plugin.getLogger().severe("Failed to save altars.yml: " + ex.getMessage());
        }
    }

    private String key(Location loc) {
        return this.makeKey(this.worldName(loc), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private String makeKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    private String worldName(Location loc) {
        return loc.getWorld() != null ? loc.getWorld().getName() : "world";
    }

    private record AltarEntry(String world, int x, int y, int z, String type, UUID owner, int stage) {
    }
}
