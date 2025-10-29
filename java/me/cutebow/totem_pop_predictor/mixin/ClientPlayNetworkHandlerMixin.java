package me.cutebow.totem_pop_predictor.mixin;

import me.cutebow.totem_pop_predictor.client.logic.TppLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void tpp$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        TppLogic.onEntityStatus(packet, MinecraftClient.getInstance());
    }
}
