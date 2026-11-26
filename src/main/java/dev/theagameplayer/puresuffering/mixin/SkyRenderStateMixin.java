package dev.theagameplayer.puresuffering.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.theagameplayer.puresuffering.client.renderer.InvasionSkyRenderer;
import dev.theagameplayer.puresuffering.client.renderer.PSSkyTextureState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
@Mixin(SkyRenderState.class)
public final class SkyRenderStateMixin implements PSSkyTextureState {
    @Unique private InvasionSkyRenderer.Textures psTextures;
    public void psSetTextures(InvasionSkyRenderer.Textures textures){psTextures=textures;}
    public InvasionSkyRenderer.Textures psGetTextures(){return psTextures;}
}
