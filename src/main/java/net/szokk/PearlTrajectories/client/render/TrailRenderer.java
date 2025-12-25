package net.szokk.PearlTrajectories.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;
import net.szokk.PearlTrajectories.client.MainClient;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class TrailRenderer {
    private static final int CYLINDER_SEGMENTS = 16;
    private static final float CYLINDER_RADIUS = 0.05f;

    private static final RenderLayer TRAIL_LAYER = RenderLayer.getDebugQuads();

    public void render(WorldRenderContext context, Map<EnderPearlEntity, List<Vec3d>> trails) {
        if (trails == null || trails.isEmpty()) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = context.matrices();

        VertexConsumer vc = consumers.getBuffer(TRAIL_LAYER);

        matrices.push();

        for (Map.Entry<EnderPearlEntity, List<Vec3d>> entry : trails.entrySet()) {
            List<Vec3d> trail = entry.getValue();
            if (trail != null && trail.size() >= 2) {
                renderCylindricalTrail(trail, cameraPos, matrices, vc);
            }
        }

        matrices.pop();

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private void renderCylindricalTrail(List<Vec3d> trail, Vec3d cameraPos,
                                        MatrixStack matrices, VertexConsumer vc) {

        for (int i = 0; i < trail.size() - 1; i++) {
            Vec3d start = trail.get(i).subtract(cameraPos);
            Vec3d end = trail.get(i + 1).subtract(cameraPos);

            float progressStart = (float) i / (float) (trail.size() - 1);
            float progressEnd = (float) (i + 1) / (float) (trail.size() - 1);

            float r1f = smoothLerp(MainClient.CONFIG.trailStartR, MainClient.CONFIG.trailEndR, progressStart);
            float g1f = smoothLerp(MainClient.CONFIG.trailStartG, MainClient.CONFIG.trailEndG, progressStart);
            float b1f = smoothLerp(MainClient.CONFIG.trailStartB, MainClient.CONFIG.trailEndB, progressStart);
            int a1 = (int) (MainClient.CONFIG.trailAlpha * 255 * (1.0f - progressStart * 0.3f));

            float r2f = smoothLerp(MainClient.CONFIG.trailStartR, MainClient.CONFIG.trailEndR, progressEnd);
            float g2f = smoothLerp(MainClient.CONFIG.trailStartG, MainClient.CONFIG.trailEndG, progressEnd);
            float b2f = smoothLerp(MainClient.CONFIG.trailStartB, MainClient.CONFIG.trailEndB, progressEnd);
            int a2 = (int) (MainClient.CONFIG.trailAlpha * 255 * (1.0f - progressEnd * 0.3f));

            int r1 = (int) (r1f * 255), g1 = (int) (g1f * 255), b1 = (int) (b1f * 255);
            int r2 = (int) (r2f * 255), g2 = (int) (g2f * 255), b2 = (int) (b2f * 255);

            renderCylinderSegment(start, end, matrices, vc,
                    r1 / 3, g1 / 3, b1 / 2, a1 / 4,
                    r2 / 3, g2 / 3, b2 / 2, a2 / 4,
                    CYLINDER_RADIUS * 2.0f);

            renderCylinderSegment(start, end, matrices, vc,
                    r1, g1, b1, a1,
                    r2, g2, b2, a2,
                    CYLINDER_RADIUS);

            renderCylinderSegment(start, end, matrices, vc,
                    255, 255, 255, a1 / 2,
                    255, 255, 255, a2 / 2,
                    CYLINDER_RADIUS * 0.3f);
        }
    }

    private void renderCylinderSegment(Vec3d start, Vec3d end, MatrixStack matrices, VertexConsumer vc,
                                       int r1, int g1, int b1, int a1,
                                       int r2, int g2, int b2, int a2,
                                       float radius) {

        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 1e-8) return;

        Vec3d dir = direction.normalize();
        Vec3d p1 = findPerpendicularVector(dir);
        Vec3d p2 = dir.crossProduct(p1).normalize();

        Vec3d[] startCircle = new Vec3d[CYLINDER_SEGMENTS];
        Vec3d[] endCircle = new Vec3d[CYLINDER_SEGMENTS];
        Vector3f[] normals = new Vector3f[CYLINDER_SEGMENTS];

        for (int i = 0; i < CYLINDER_SEGMENTS; i++) {
            double ang = 2.0 * Math.PI * i / (double) CYLINDER_SEGMENTS;
            double cos = Math.cos(ang);
            double sin = Math.sin(ang);

            Vec3d offset = p1.multiply(cos * radius).add(p2.multiply(sin * radius));

            startCircle[i] = start.add(offset);
            endCircle[i] = end.add(offset);

            normals[i] = new Vector3f(
                    (float) (cos * p1.x + sin * p2.x),
                    (float) (cos * p1.y + sin * p2.y),
                    (float) (cos * p1.z + sin * p2.z)
            ).normalize();
        }

        for (int i = 0; i < CYLINDER_SEGMENTS; i++) {
            int nextI = (i + 1) % CYLINDER_SEGMENTS;

            addVertex(vc, matrices, startCircle[i], normals[i], r1, g1, b1, a1);
            addVertex(vc, matrices, startCircle[nextI], normals[nextI], r1, g1, b1, a1);
            addVertex(vc, matrices, endCircle[nextI], normals[nextI], r2, g2, b2, a2);
            addVertex(vc, matrices, endCircle[i], normals[i], r2, g2, b2, a2);
        }
    }

    private void addVertex(VertexConsumer vc, MatrixStack matrices,
                           Vec3d pos, Vector3f normal, int r, int g, int b, int a) {
        vc.vertex(matrices.peek().getPositionMatrix(), (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, a)
                .normal(normal.x(), normal.y(), normal.z());
    }

    private float smoothLerp(float start, float end, float t) {
        float smoothT = t * t * (3.0f - 2.0f * t);
        return start + (end - start) * smoothT;
    }

    private Vec3d findPerpendicularVector(Vec3d vector) {
        if (Math.abs(vector.x) < 0.9) return new Vec3d(1.0, 0.0, 0.0).crossProduct(vector).normalize();
        return new Vec3d(0.0, 1.0, 0.0).crossProduct(vector).normalize();
    }
}
