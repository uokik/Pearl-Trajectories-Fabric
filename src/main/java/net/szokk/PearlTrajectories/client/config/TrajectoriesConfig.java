package net.szokk.PearlTrajectories.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class TrajectoriesConfig {
    public boolean enabled = true;
    public float trailStartR = 0.5f;
    public float trailStartG = 0.2f;
    public float trailStartB = 0.9f;

    public float trailEndR = 0.0f;
    public float trailEndG = 0.9f;
    public float trailEndB = 1.0f;

    public float beamR = 0.4f;
    public float beamG = 0.2f;
    public float beamB = 1.0f;

    public float beamGlowR = 1.0f;
    public float beamGlowG = 1.0f;
    public float beamGlowB = 1.0f;

    public float trailAlpha = 0.85f;
    public float beamAlpha = 1.0f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "pearl-trajectories.json"
    );

    public static TrajectoriesConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, TrajectoriesConfig.class);
            } catch (Exception e) {
                System.err.println("[Pearl Trajectories] Failed to load cfg: " + e.getMessage());
            }
        }
        TrajectoriesConfig config = new TrajectoriesConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("[Pearl Trajectories] Failed to save cfg: " + e.getMessage());
        }
    }
}