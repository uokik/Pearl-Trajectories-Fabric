package net.szokk.PearlTrajectories.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.szokk.PearlTrajectories.client.MainClient;
import net.szokk.PearlTrajectories.client.config.TrajectoriesConfig;
import org.joml.Math;

import java.util.function.Consumer;

public class PearlTrajectoriesConfigScreen extends Screen {
    private final Screen parent;
    private TrajectoriesConfig workingCopy;

    public PearlTrajectoriesConfigScreen(Screen parent) {
        super(Text.literal("Pearl Trajectories Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        workingCopy = new TrajectoriesConfig();
        copyConfig(MainClient.CONFIG, workingCopy);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int sliderW = 120;
        int sliderH = 20;
        int gap = 24;

        int leftX = centerX - 140;
        int topY = 40;

        addSlider(leftX, topY + gap*0, sliderW, sliderH, "Red", workingCopy.trailStartR, v -> workingCopy.trailStartR = v);
        addSlider(leftX, topY + gap*1, sliderW, sliderH, "Green", workingCopy.trailStartG, v -> workingCopy.trailStartG = v);
        addSlider(leftX, topY + gap*2, sliderW, sliderH, "Blue", workingCopy.trailStartB, v -> workingCopy.trailStartB = v);

        int rightX = centerX + 20;
        addSlider(rightX, topY + gap*0, sliderW, sliderH, "Red", workingCopy.trailEndR, v -> workingCopy.trailEndR = v);
        addSlider(rightX, topY + gap*1, sliderW, sliderH, "Green", workingCopy.trailEndG, v -> workingCopy.trailEndG = v);
        addSlider(rightX, topY + gap*2, sliderW, sliderH, "Blue", workingCopy.trailEndB, v -> workingCopy.trailEndB = v);

        addSlider(centerX - 100, topY + gap*3 + 5, 200, sliderH, "Trail Opacity", workingCopy.trailAlpha, v -> workingCopy.trailAlpha = v);

        int beamY = topY + gap*5;
        addSlider(centerX - 100, beamY + gap*0, 200, sliderH, "Beam Red", workingCopy.beamR, v -> updateBeam(v, workingCopy.beamG, workingCopy.beamB));
        addSlider(centerX - 100, beamY + gap*1, 200, sliderH, "Beam Green", workingCopy.beamG, v -> updateBeam(workingCopy.beamR, v, workingCopy.beamB));
        addSlider(centerX - 100, beamY + gap*2, 200, sliderH, "Beam Blue", workingCopy.beamB, v -> updateBeam(workingCopy.beamR, workingCopy.beamG, v));
        addSlider(centerX - 100, beamY + gap*3, 200, sliderH, "Beam Opacity", workingCopy.beamAlpha, v -> workingCopy.beamAlpha = v);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> {
            copyConfig(workingCopy, MainClient.CONFIG);
            MainClient.CONFIG.save();
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(centerX - 105, this.height - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(centerX + 5, this.height - 30, 100, 20).build());
    }

    private void updateBeam(float r, float g, float b) {
        workingCopy.beamR = r; workingCopy.beamG = g; workingCopy.beamB = b;
        workingCopy.beamGlowR = Math.min(1.0f, r + 0.2f);
        workingCopy.beamGlowG = Math.min(1.0f, g + 0.2f);
        workingCopy.beamGlowB = Math.min(1.0f, b + 0.2f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int topY = 40;
        int gap = 24;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 10, 0xFFFFFF);

        int leftX = centerX - 140;
        int colorStart = ((int)(workingCopy.trailStartR * 255) << 16) |
                ((int)(workingCopy.trailStartG * 255) << 8) |
                ((int)(workingCopy.trailStartB * 255)) | 0xFF000000;
        context.fill(leftX, topY - 14, leftX + 10, topY - 4, colorStart);
        context.drawTextWithShadow(this.textRenderer, "Trail Start", leftX + 14, topY - 14, 0xFFAAAAAA);

        int rightEdgeX = centerX + 140;
        int colorEnd = ((int)(workingCopy.trailEndR * 255) << 16) |
                ((int)(workingCopy.trailEndG * 255) << 8) |
                ((int)(workingCopy.trailEndB * 255)) | 0xFF000000;
        String endText = "Trail End";
        int endTextWidth = this.textRenderer.getWidth(endText);
        context.drawTextWithShadow(this.textRenderer, endText, rightEdgeX - endTextWidth - 14, topY - 14, 0xFFAAAAAA);
        context.fill(rightEdgeX - 10, topY - 14, rightEdgeX, topY - 4, colorEnd);

        int beamLabelY = topY + (gap * 4) + 12;
        int colorBeam = ((int)(workingCopy.beamR * 255) << 16) |
                ((int)(workingCopy.beamG * 255) << 8) |
                ((int)(workingCopy.beamB * 255)) | 0xFF000000;
        String beamTitle = "Beam Settings";
        int beamTitleWidth = this.textRenderer.getWidth(beamTitle);
        int beamStartX = centerX - ((beamTitleWidth + 14) / 2);
        context.drawTextWithShadow(this.textRenderer, beamTitle, beamStartX, beamLabelY, 0xFFAAAAAA);
        context.fill(beamStartX + beamTitleWidth + 4, beamLabelY, beamStartX + beamTitleWidth + 14, beamLabelY + 10, colorBeam);
    }

    private void addSlider(int x, int y, int w, int h, String label, float value, Consumer<Float> onChange) {
        this.addDrawableChild(new ConfigSlider(x, y, w, h, label, value, onChange));
    }

    private void copyConfig(TrajectoriesConfig src, TrajectoriesConfig dst) {
        dst.trailStartR = src.trailStartR; dst.trailStartG = src.trailStartG; dst.trailStartB = src.trailStartB;
        dst.trailEndR = src.trailEndR;     dst.trailEndG = src.trailEndG;     dst.trailEndB = src.trailEndB;
        dst.trailAlpha = src.trailAlpha;   dst.beamR = src.beamR;             dst.beamG = src.beamG;
        dst.beamB = src.beamB;             dst.beamGlowR = src.beamGlowR;     dst.beamGlowG = src.beamGlowG;
        dst.beamGlowB = src.beamGlowB;     dst.beamAlpha = src.beamAlpha;
    }

    private static class ConfigSlider extends SliderWidget {
        private final Consumer<Float> onChange;
        private final String label;

        public ConfigSlider(int x, int y, int w, int h, String label, float value, Consumer<Float> onChange) {
            super(x, y, w, h, Text.literal(""), value);
            this.label = label;
            this.onChange = onChange;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int displayValue = (int)(this.value * 255);
            if (label.contains("Opacity")) {
                displayValue = (int)(this.value * 100);
                this.setMessage(Text.literal(label + ": " + displayValue + "%"));
            } else {
                this.setMessage(Text.literal(label + ": " + displayValue));
            }
        }

        @Override
        protected void applyValue() {
            this.onChange.accept((float)this.value);
        }
    }
}