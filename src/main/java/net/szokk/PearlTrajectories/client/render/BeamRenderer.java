package net.szokk.PearlTrajectories.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.szokk.PearlTrajectories.client.MainClient;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Random;

public class BeamRenderer {

    private static final RenderLayer BEAM_LAYER = RenderLayer.getDebugQuads();

    public void render(WorldRenderContext context, Map<Vec3d, Long> landingEffects) {
        if (landingEffects == null || landingEffects.isEmpty()) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null) return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        MatrixStack matrices = context.matrices();

        VertexConsumer vc = consumers.getBuffer(BEAM_LAYER);

        matrices.push();

        long now = System.currentTimeMillis();
        for (var entry : landingEffects.entrySet()) {
            Vec3d landingPos = entry.getKey();
            long spawnTime = entry.getValue();

            float age = (now - spawnTime) / 1000.0f;
            if (age > 3.0f) continue;

            renderLandingEffect(landingPos, spawnTime, age, cameraPos, matrices, vc);
        }

        matrices.pop();

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private void renderLandingEffect(Vec3d landingPos, long spawnTime, float age, Vec3d cameraPos,
                                     MatrixStack matrices, VertexConsumer vc) {

        Vec3d relativePos = landingPos.subtract(cameraPos);

        float fade = age / 3.0f;
        float alphaF = 1.0f - fade;
        if (alphaF <= 0) return;

        int baseAlpha = (int) (alphaF * MainClient.CONFIG.beamAlpha * 255);
        if (baseAlpha <= 0) return;

        renderVerticalBeam(relativePos, cameraPos, matrices, vc, baseAlpha);
        renderLightningBolt(relativePos, spawnTime, cameraPos, matrices, vc, baseAlpha, age);
    }

    private void renderVerticalBeam(Vec3d relativePos, Vec3d cameraPos,
                                    MatrixStack matrices, VertexConsumer vc, int baseAlpha) {

        Vec3d start = relativePos;
        Vec3d end = new Vec3d(relativePos.x, 255 - cameraPos.y, relativePos.z);

        float baseRadius = 0.10f;
        int segments = 8;

        for (int layer = 0; layer < 3; layer++) {
            float radius = baseRadius * (1.0f + layer * 0.8f);
            int a = baseAlpha / (layer + 1);

            int r, g, b;
            if (layer == 2) {
                r = (int) (MainClient.CONFIG.beamGlowR * 255);
                g = (int) (MainClient.CONFIG.beamGlowG * 255);
                b = (int) (MainClient.CONFIG.beamGlowB * 255);
            } else {
                r = (int) (MainClient.CONFIG.beamR * 255);
                g = (int) (MainClient.CONFIG.beamG * 255);
                b = (int) (MainClient.CONFIG.beamB * 255);
            }

            renderVerticalCylinder(start, end, matrices, vc, r, g, b, a, radius, segments);
        }
    }

    private void renderVerticalCylinder(Vec3d start, Vec3d end, MatrixStack matrices,
                                        VertexConsumer vc, int r, int g, int b, int a,
                                        float radius, int segments) {

        Vec3d[] bottom = new Vec3d[segments];
        Vec3d[] top = new Vec3d[segments];
        Vector3f[] normals = new Vector3f[segments];

        for (int i = 0; i < segments; i++) {
            double ang = 2.0 * Math.PI * i / (double) segments;
            double ox = Math.cos(ang) * radius;
            double oz = Math.sin(ang) * radius;

            bottom[i] = start.add(ox, 0, oz);
            top[i] = end.add(ox, 0, oz);

            normals[i] = new Vector3f((float) ox, 0, (float) oz).normalize();
        }

        for (int i = 0; i < segments; i++) {
            int ni = (i + 1) % segments;

            addVertex(vc, matrices, bottom[i], normals[i], r, g, b, a);
            addVertex(vc, matrices, bottom[ni], normals[ni], r, g, b, a);
            addVertex(vc, matrices, top[ni], normals[ni], r, g, b, a);
            addVertex(vc, matrices, top[i], normals[i], r, g, b, a);
        }
    }

    private void renderLightningBolt(Vec3d relativePos, long spawnTime, Vec3d cameraPos, MatrixStack matrices,
                                     VertexConsumer vc, int baseAlpha, float age) {

        Vec3d target = relativePos;

        Random baseRand = new Random(spawnTime);
        Vec3d skyStart = new Vec3d(
                relativePos.x + (baseRand.nextDouble() - 0.5) * 2,
                255 - cameraPos.y,
                relativePos.z + (baseRand.nextDouble() - 0.5) * 2
        );

        int segments = 20;

        Vec3d current = skyStart;
        for (int i = 0; i < segments; i++) {
            float t = (float) (i + 1) / segments;
            Vec3d nextTarget = skyStart.lerp(target, t);

            Random anim = new Random(spawnTime ^ (long)(age * 1000) ^ (i * 31L));
            Vec3d next = nextTarget.add(
                    (anim.nextDouble() - 0.5) * 0.5,
                    0,
                    (anim.nextDouble() - 0.5) * 0.5
            );

            int r = (int) (MainClient.CONFIG.beamR * 200);
            int g = (int) (MainClient.CONFIG.beamG * 200);
            int b = (int) (MainClient.CONFIG.beamB * 255);

            renderLightningSegment(current, next, matrices, vc, r, g, b, baseAlpha, 0.04f);
            current = next;
        }

        current = skyStart;
        for (int i = 0; i < segments; i++) {
            float t = (float) (i + 1) / segments;
            Vec3d nextTarget = skyStart.lerp(target, t);

            Random anim = new Random(~spawnTime ^ (long)(age * 1200) ^ (i * 131L));
            Vec3d next = nextTarget.add(
                    (anim.nextDouble() - 0.5) * 0.3,
                    0,
                    (anim.nextDouble() - 0.5) * 0.3
            );

            int gr = (int) (MainClient.CONFIG.beamGlowR * 255);
            int gg = (int) (MainClient.CONFIG.beamGlowG * 255);
            int gb = (int) (MainClient.CONFIG.beamGlowB * 255);

            float pulse = 0.75f + 0.25f * (float)Math.sin(age * 18.0f);
            int glowAlpha = Math.min(255, (int)(baseAlpha * 0.35f * pulse));

            renderLightningSegment(current, next, matrices, vc, gr, gg, gb, glowAlpha, 0.06f);
            current = next;
        }
    }

    private void renderLightningSegment(Vec3d start, Vec3d end, MatrixStack matrices,
                                        VertexConsumer vc, int r, int g, int b, int a, float width) {

        Vec3d dir = end.subtract(start);
        if (dir.lengthSquared() < 1e-6) return;

        Vec3d perp = new Vec3d(-dir.z, 0, dir.x).normalize();
        Vec3d off = perp.multiply(width);

        Vector3f normal = new Vector3f(0, 1, 0);

        addVertex(vc, matrices, start.add(off), normal, r, g, b, a);
        addVertex(vc, matrices, start.subtract(off), normal, r, g, b, a);
        addVertex(vc, matrices, end.subtract(off), normal, r, g, b, a);
        addVertex(vc, matrices, end.add(off), normal, r, g, b, a);
    }

    private void addVertex(VertexConsumer vc, MatrixStack matrices, Vec3d pos, Vector3f normal,
                           int r, int g, int b, int a) {
        vc.vertex(matrices.peek().getPositionMatrix(), (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, a)
                .normal(normal.x(), normal.y(), normal.z());
    }
}
