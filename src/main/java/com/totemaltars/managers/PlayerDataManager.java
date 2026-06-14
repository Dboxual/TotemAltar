package com.totemaltars.managers;

import com.totemaltars.TotemAltars;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerDataManager {
    private final TotemAltars plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Set<UUID> guidedPlayers = new HashSet<>();

    public PlayerDataManager(TotemAltars plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player_data.yml");
        this.load();
    }

    public boolean hasReceivedGuide(UUID uuid) {
        return this.guidedPlayers.contains(uuid);
    }

    public void markGuideReceived(UUID uuid) {
        if (this.guidedPlayers.add(uuid)) {
            this.save();
        }
    }

    private void load() {
        this.data = this.dataFile.exists() ? YamlConfiguration.loadConfiguration(this.dataFile) : new YamlConfiguration();
        this.guidedPlayers.clear();
        for (String s : this.data.getStringList("guided-players")) {
            try {
                this.guidedPlayers.add(UUID.fromString(s));
            } catch (IllegalArgumentException ex) {
                this.plugin.getLogger().warning("Skipping malformed UUID in player_data.yml: " + s);
            }
        }
        this.plugin.getLogger().info("Loaded " + this.guidedPlayers.size() + " guided player(s) from player_data.yml.");
    }

    private void save() {
        List<String> list = this.guidedPlayers.stream().map(UUID::toString).toList();
        this.data.set("guided-players", list);
        try {
            this.data.save(this.dataFile);
        } catch (IOException ex) {
            this.plugin.getLogger().severe("Failed to save player_data.yml: " + ex.getMessage());
        }
    }
}
