package dev.theagameplayer.puresuffering.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.Minecraft;
import dev.theagameplayer.puresuffering.config.PSConfigValues;
import dev.theagameplayer.puresuffering.util.invasion.ClientInvasionHandler;
@Mixin(LightmapRenderStateExtractor.class)
public final class DimensionSpecialEffectsMixin {
    @Inject(method="extract",at=@At("RETURN"))
    private void psAdjustAmbient(LightmapRenderState state,float partialTicks,CallbackInfo ci) {
        var level=Minecraft.getInstance().level;
        if (!state.needsUpdate || level==null || PSConfigValues.client==null || !PSConfigValues.client.canInvasionsChangeBrightness) return;
        state.skyFactor=ClientInvasionHandler.handleSkyDarken(state.skyFactor,level);
        if (!ClientInvasionHandler.handleBrightLightmap(true,level)) state.ambientColor=new org.joml.Vector3f(state.ambientColor).mul(state.skyFactor);
    }
}
