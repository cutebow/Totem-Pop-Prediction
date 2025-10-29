package me.cutebow.totem_pop_predictor.client.hud;

import me.cutebow.totem_pop_predictor.client.config.TppConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class TppHud {
    private static boolean active = false;

    public static void show() { active = true; }
    public static void hide() { active = false; }
    public static boolean isActive() { return active; }

    public static void render(DrawContext dc){
        if(!TppConfig.enabled || !active) return;
        MinecraftClient c = MinecraftClient.getInstance();
        TextRenderer tr = c.textRenderer;
        int h = c.getWindow().getScaledHeight();
        int y = Math.max(2, Math.min(h - 10, (int)((1f - TppConfig.heightNorm) * h)));
        int cx = c.getWindow().getScaledWidth()/2;
        float hu = TppConfig.hue/360f;
        int rgb = java.awt.Color.HSBtoRGB(hu,1f,1f)&0xFFFFFF;
        int a = (int)(Math.max(0f, Math.min(1f, TppConfig.alpha))*255) << 24;
        int color = a | rgb;
        String s = TppConfig.text == null || TppConfig.text.isBlank() ? "ReTotem" : TppConfig.text;
        int w = tr.getWidth(s);
        dc.drawTextWithShadow(tr, Text.literal(s), cx - w/2, y, color);
    }
}
