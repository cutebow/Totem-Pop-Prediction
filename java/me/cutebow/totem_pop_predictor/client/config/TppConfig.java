package me.cutebow.totem_pop_predictor.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class TppConfig {
    public enum Mode { PREDICTION, TIP, BOTH }
    public enum PredMode { SIMPLE, ADVANCED }

    public static boolean enabled = true;
    public static Mode mode = Mode.PREDICTION;
    public static PredMode predMode = PredMode.SIMPLE;
    public static float hue = 48f;
    public static float alpha = 1.0f;
    public static float heightNorm = 0.27f;
    public static String text = "ReTotem";
    public static String explainMode = "Prediction warns before a pop. Tip reminds after a pop.";

    private static final File FILE = new File("config/totem_pop_predictor.json");
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        boolean enabled;
        Mode mode;
        PredMode predMode;
        float hue;
        float alpha;
        float heightNorm;
        String text;
        String explainMode;
    }

    public static void load() {
        try {
            if (!FILE.exists()) save();
            try (FileReader r = new FileReader(FILE)) {
                Data d = G.fromJson(r, Data.class);
                if (d != null) {
                    enabled = d.enabled;
                    mode = d.mode == null ? Mode.PREDICTION : d.mode;
                    predMode = d.predMode == null ? PredMode.SIMPLE : d.predMode;
                    hue = d.hue;
                    alpha = d.alpha;
                    heightNorm = d.heightNorm;
                    text = (d.text == null || d.text.isBlank()) ? "ReTotem" : d.text;
                    explainMode = d.explainMode == null ? explainMode : d.explainMode;
                }
            }
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            Data d = new Data();
            d.enabled = enabled;
            d.mode = mode;
            d.predMode = predMode;
            d.hue = hue;
            d.alpha = alpha;
            d.heightNorm = heightNorm;
            d.text = text;
            d.explainMode = explainMode;
            try (FileWriter w = new FileWriter(FILE)) {
                G.toJson(d, w);
            }
        } catch (Exception ignored) { }
    }
}
