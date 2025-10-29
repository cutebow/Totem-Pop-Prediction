package me.cutebow.totem_pop_predictor.client;

import me.cutebow.totem_pop_predictor.client.config.TppConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<?> getModConfigScreenFactory() { return parent -> new TppConfigScreen(parent); }

    public static class TppConfigScreen extends Screen {
        private final Screen parent;
        private static final String DISCORD  = "https://discord.gg/A2Nx9kxDeB";
        private float hueNorm;
        private float alphaNorm;
        private float heightNorm;
        private int sliderX, hueY, alphaY, heightY;
        private TextFieldWidget messageBox;

        public TppConfigScreen(Screen parent){ super(Text.literal("Totem Pop Predictor")); this.parent = parent; }

        @Override
        protected void init() {
            hueNorm = TppConfig.hue/360f;
            alphaNorm = TppConfig.alpha;
            heightNorm = TppConfig.heightNorm;

            int y = 26;
            sliderX = this.width/2 - 100;

            this.addDrawableChild(ButtonWidget.builder(Text.literal(TppConfig.enabled ? "Enabled" : "Disabled"), b -> {
                TppConfig.enabled = !TppConfig.enabled;
                b.setMessage(Text.literal(TppConfig.enabled ? "Enabled" : "Disabled"));
                TppConfig.save();
            }).dimensions(sliderX, y, 200, 20).build());

            y += 28;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(modeLabel()), b -> {
                if (TppConfig.mode == TppConfig.Mode.PREDICTION) TppConfig.mode = TppConfig.Mode.TIP;
                else if (TppConfig.mode == TppConfig.Mode.TIP) TppConfig.mode = TppConfig.Mode.BOTH;
                else TppConfig.mode = TppConfig.Mode.PREDICTION;
                b.setMessage(Text.literal(modeLabel()));
                TppConfig.save();
            }).dimensions(sliderX, y, 200, 20).build());

            y += 28;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(predLabel()), b -> {
                TppConfig.predMode = TppConfig.predMode == TppConfig.PredMode.SIMPLE ? TppConfig.PredMode.ADVANCED : TppConfig.PredMode.SIMPLE;
                b.setMessage(Text.literal(predLabel()));
                TppConfig.save();
            }).dimensions(sliderX, y, 200, 20).build());

            y += 56;
            messageBox = new TextFieldWidget(this.textRenderer, sliderX, y, 200, 20, Text.literal("Message"));
            messageBox.setMaxLength(32);
            messageBox.setText(TppConfig.text == null ? "ReTotem" : TppConfig.text);
            messageBox.setEditable(true);
            this.setInitialFocus(messageBox);
            messageBox.setChangedListener(s -> { TppConfig.text = s; TppConfig.save(); });
            this.addDrawableChild(messageBox);

            y += 44; hueY = y;
            this.addDrawableChild(new SliderWidget(sliderX, y, 200, 20, Text.literal("Hue"), hueNorm) {
                @Override protected void updateMessage() { setMessage(Text.literal("Hue")); }
                @Override protected void applyValue() { hueNorm = (float)this.value; TppConfig.hue = hueNorm*360f; TppConfig.save(); }
            });

            y += 38; alphaY = y;
            this.addDrawableChild(new SliderWidget(sliderX, y, 200, 20, Text.literal("Transparency"), alphaNorm) {
                @Override protected void updateMessage() { setMessage(Text.literal("Transparency")); }
                @Override protected void applyValue() { alphaNorm = (float)this.value; TppConfig.alpha = alphaNorm; TppConfig.save(); }
            });

            y += 38; heightY = y;
            this.addDrawableChild(new SliderWidget(sliderX, y, 200, 20, Text.literal("HUD Height: " + (int)(heightNorm*100) + "%"), heightNorm) {
                @Override protected void updateMessage() { setMessage(Text.literal("HUD Height: " + (int)(this.value*100) + "%")); }
                @Override protected void applyValue() { heightNorm = (float)this.value; TppConfig.heightNorm = heightNorm; TppConfig.save(); }
            });

            y += 40;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Preview"), b -> MinecraftClient.getInstance().setScreen(new PreviewScreen(this))).dimensions(sliderX, y, 200, 20).build());

            int pad = 12;
            String label = "Discord";
            int w = this.textRenderer.getWidth(label) + pad*2;
            int x = this.width - w - 10;
            int by = this.height - 28;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> { if(!DISCORD.isBlank()) Util.getOperatingSystem().open(DISCORD); }).dimensions(x, by, w, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> MinecraftClient.getInstance().setScreen(parent)).dimensions(this.width/2 - 100, this.height - 28, 200, 20).build());
        }

        private String modeLabel() {
            if (TppConfig.mode == TppConfig.Mode.PREDICTION) return "Mode: Totem Prediction";
            if (TppConfig.mode == TppConfig.Mode.TIP) return "Mode: Totem Tip";
            return "Mode: Both";
        }

        private String predLabel() {
            return TppConfig.predMode == TppConfig.PredMode.SIMPLE ? "Prediction: Simple" : "Prediction: Advanced";
        }

        @Override
        public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
            this.renderBackground(dc, mouseX, mouseY, delta);
            super.render(dc, mouseX, mouseY, delta);

            dc.drawCenteredTextWithShadow(this.textRenderer, Text.literal(TppConfig.explainMode), this.width/2, 12, 0xAAAAAA);
            dc.drawTextWithShadow(this.textRenderer, Text.literal("Message shown on screen"), sliderX, messageBox.getY() - 14, 0xFFFFFFFF);

            int hueBoxX = sliderX + 206;
            int hueBoxY = hueY;
            int rgb = java.awt.Color.HSBtoRGB(hueNorm,1f,1f) & 0xFFFFFF;
            int colHue = (0xFF << 24) | rgb;
            dc.fill(hueBoxX, hueBoxY, hueBoxX+40, hueBoxY+20, colHue);

            int alphaBoxX = sliderX + 206;
            int alphaBoxY = alphaY;
            int a = (int)(alphaNorm*255) & 0xFF;
            int colAlpha = (a << 24) | 0xFFFFFF;
            dc.fill(alphaBoxX, alphaBoxY, alphaBoxX+40, alphaBoxY+20, colAlpha);

            dc.drawTextWithShadow(this.textRenderer, Text.literal("Made by cutebow"), 10, this.height - 24, 0xFFFFFFFF);
        }
    }

    public static class PreviewScreen extends Screen {
        private final Screen parent;

        public PreviewScreen(Screen parent) { super(Text.literal("Preview")); this.parent = parent; }

        @Override
        protected void init() {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(parent)).dimensions(10, this.height - 28, 80, 20).build());
        }

        @Override
        public boolean shouldPause() { return false; }

        @Override
        public void renderBackground(DrawContext dc, int mouseX, int mouseY, float delta) { }

        @Override
        public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
            super.render(dc, mouseX, mouseY, delta);
            String s = TppConfig.text == null || TppConfig.text.isBlank() ? "ReTotem" : TppConfig.text;
            float hu = TppConfig.hue/360f;
            int rgb = java.awt.Color.HSBtoRGB(hu,1f,1f)&0xFFFFFF;
            int a = (int)(Math.max(0f, Math.min(1f, TppConfig.alpha))*255) << 24;
            int color = a | rgb;
            int h = this.client.getWindow().getScaledHeight();
            int y = Math.max(2, Math.min(h - 10, (int)((1f - TppConfig.heightNorm) * h)));
            int cx = this.client.getWindow().getScaledWidth()/2;
            int w = this.textRenderer.getWidth(s);
            dc.drawTextWithShadow(this.textRenderer, Text.literal(s), cx - w/2, y, color);
        }
    }
}
