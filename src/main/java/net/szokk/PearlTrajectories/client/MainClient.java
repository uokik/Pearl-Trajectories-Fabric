package net.szokk.PearlTrajectories.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.szokk.PearlTrajectories.client.config.TrajectoriesConfig;
import net.szokk.PearlTrajectories.client.render.BeamRenderer;
import net.szokk.PearlTrajectories.client.render.TrailRenderer;
import net.szokk.PearlTrajectories.client.gui.PearlTrajectoriesConfigScreen;
import org.lwjgl.glfw.GLFW;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class MainClient implements ClientModInitializer {
    private static final Map<EnderPearlEntity, List<Vec3d>> PEARL_TRAILS = new HashMap<>();
    private static final Map<EnderPearlEntity, Long> PEARL_REMOVAL_TIME = new HashMap<>();
    private static final Map<EnderPearlEntity, Vec3d> PEARL_LAST_POS = new HashMap<>();
    private static final Map<Vec3d, Long> LANDING_EFFECTS = new HashMap<>();

    private static final long LANDING_EFFECT_DURATION = 3000L;
    private static final long TRAIL_PERSIST_TIME = 2000L;
    private static final double MIN_DISTANCE_THRESHOLD = 0.02D;

    public static TrajectoriesConfig CONFIG;
    private TrailRenderer trailRenderer;
    private BeamRenderer beamRenderer;

    private static KeyBinding CONFIG_KEY;
    private static KeyBinding TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        System.out.println("[Pearl Trajectories] Initializing :v");

        CONFIG = TrajectoriesConfig.load();
        trailRenderer = new TrailRenderer();
        beamRenderer = new BeamRenderer();

        KeyBinding.Category category = KeyBinding.Category.create(
                Identifier.of("pearltrajectory", "keybinds")
        );


        CONFIG_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pearltrajectories.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                category
        ));


        TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pearltrajectories.toggle",
                GLFW.GLFW_KEY_O,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        WorldRenderEvents.AFTER_ENTITIES.register(this::onRenderWorld);

        System.out.println("Mod initialized :v");
    }

    private void onClientTick(MinecraftClient client) {
        while (CONFIG_KEY.wasPressed()) {
            client.setScreen(new PearlTrajectoriesConfigScreen(client.currentScreen));
        }

        while (TOGGLE_KEY.wasPressed()) {
            CONFIG.enabled = !CONFIG.enabled;
            CONFIG.save();

            Text status = CONFIG.enabled
                    ? Text.literal("Enabled").formatted(Formatting.GREEN)
                    : Text.literal("Disabled").formatted(Formatting.RED);

            client.inGameHud.setOverlayMessage(
                    Text.literal("Pearl Trajectories: ").append(status),
                    false
            );
        }

        if (client.world == null || client.player == null) return;

        if (!CONFIG.enabled) {
            PEARL_TRAILS.clear();
            PEARL_REMOVAL_TIME.clear();
            PEARL_LAST_POS.clear();
            LANDING_EFFECTS.clear();
            return;
        }

        Vec3d playerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        Box searchBox = new Box(
                playerPos.subtract(200.0, 200.0, 200.0),
                playerPos.add(200.0, 200.0, 200.0)
        );
        List<EnderPearlEntity> pearls = client.world.getEntitiesByClass(
                EnderPearlEntity.class, searchBox, entity -> true
        );
        long currentTime = System.currentTimeMillis();

        for (EnderPearlEntity pearl : PEARL_TRAILS.keySet()) {
            if ((pearl.isRemoved() || !pearls.contains(pearl))
                    && !PEARL_REMOVAL_TIME.containsKey(pearl)) {
                PEARL_REMOVAL_TIME.put(pearl, currentTime);

                if (!isOwnedByPlayer(pearl, client)) {
                    Vec3d landingPos = PEARL_LAST_POS.get(pearl);
                    if (landingPos != null) {
                        LANDING_EFFECTS.put(landingPos, currentTime);

                        for (int i = 0; i < 30; i++) {
                            client.particleManager.addParticle(
                                    ParticleTypes.PORTAL,
                                    landingPos.x + (Math.random() - 0.5) * 2,
                                    landingPos.y + Math.random() * 2,
                                    landingPos.z + (Math.random() - 0.5) * 2,
                                    (Math.random() - 0.5) * 0.5,
                                    Math.random() * 0.5,
                                    (Math.random() - 0.5) * 0.5
                            );
                        }
                    }
                }
            }
        }

        PEARL_TRAILS.keySet().removeIf(pearl -> {
            boolean shouldRemove = PEARL_REMOVAL_TIME.containsKey(pearl)
                    && currentTime - PEARL_REMOVAL_TIME.get(pearl) > TRAIL_PERSIST_TIME;
            if (shouldRemove) {
                PEARL_LAST_POS.remove(pearl);
            }
            return shouldRemove;
        });

        LANDING_EFFECTS.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > LANDING_EFFECT_DURATION
        );

        for (EnderPearlEntity pearl : pearls) {
            if (!pearl.isRemoved() && !isOwnedByPlayer(pearl, client)) {
                Vec3d currentPos = new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ());
                Vec3d lastPos = PEARL_LAST_POS.get(pearl);

                if (lastPos != null) {
                    double distance = lastPos.distanceTo(currentPos);

                    if (distance > MIN_DISTANCE_THRESHOLD) {
                        List<Vec3d> trail = PEARL_TRAILS.computeIfAbsent(pearl, k -> new ArrayList<>());
                        trail.add(currentPos);

                        if (trail.size() > 150) {
                            trail.remove(0);
                        }

                        PEARL_LAST_POS.put(pearl, currentPos);
                    }
                } else {
                    PEARL_LAST_POS.put(pearl, currentPos);
                }
            }
        }
    }

    private void onRenderWorld(WorldRenderContext context) {
        if (!CONFIG.enabled) return;

        trailRenderer.render(context, PEARL_TRAILS);
        beamRenderer.render(context, LANDING_EFFECTS);
    }

    private boolean isOwnedByPlayer(EnderPearlEntity pearl, MinecraftClient client) {
        if (client.player == null) return false;
        var owner = pearl.getOwner();
        return owner != null && owner.getUuid().equals(client.player.getUuid());
    }
}
