package me.cutebow.totem_pop_predictor.client;

import me.cutebow.totem_pop_predictor.client.config.TppConfig;
import me.cutebow.totem_pop_predictor.client.hud.TppHud;
import me.cutebow.totem_pop_predictor.client.logic.TppLogic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class Totem_pop_predictorClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TppConfig.load();
        HudRenderCallback.EVENT.register((dc, counter) -> TppHud.render(dc));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) TppLogic.tick(client);
        });
    }
}
