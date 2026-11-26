package dev.theagameplayer.puresuffering.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.level.MoonPhase;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import dev.theagameplayer.puresuffering.client.invasion.ClientInvasionSession;
import dev.theagameplayer.puresuffering.client.renderer.InvasionSkyRenderer;
import dev.theagameplayer.puresuffering.client.renderer.PSSkyTextureState;
import dev.theagameplayer.puresuffering.config.PSConfigValues;
@Mixin(SkyRenderer.class)
public abstract class LevelRendererMixin {
    @Unique private InvasionSkyRenderer.Textures psActiveTextures;
    @Unique private GpuBuffer psSunQuad,psMoonQuads;
    @Shadow private Matrix4f applyCelestialBodyTransform(PoseStack pose,float height,float size){throw new AssertionError();}
    @Inject(method="extractRenderState",at=@At("RETURN"))
    private void psExtract(ClientLevel level,float partialTicks,Camera camera,SkyRenderState state,CallbackInfo ci){
        PSSkyTextureState target=(PSSkyTextureState)(Object)state;
        target.psSetTextures(null);
        if (PSConfigValues.client==null || !PSConfigValues.client.useSkyBoxRenderer) return;
        var session=ClientInvasionSession.get(level);
        if(session!=null) target.psSetTextures(session.getInvasionSkyRenderer().extract(state,level.getOverworldClockTime()%12000L));
    }
    @Inject(method="render",at=@At("HEAD"))
    private void psRenderStart(GpuBufferSlice fog,SkyRenderState state,CallbackInfo ci){
        psActiveTextures=((PSSkyTextureState)(Object)state).psGetTextures();
        if (psActiveTextures != null) {
            var textures=Minecraft.getInstance().getTextureManager();
            if(psActiveTextures.sun()!=null){if(psSunQuad==null)psSunQuad=psBuildQuad(false);textures.getTexture(psActiveTextures.sun());}
            if(psActiveTextures.moon()!=null){if(psMoonQuads==null)psMoonQuads=psBuildQuad(true);textures.getTexture(psActiveTextures.moon());}
            if(psActiveTextures.sky()!=null)textures.getTexture(psActiveTextures.sky());
        }
    }
    @Inject(method="render",at=@At("RETURN"))
    private void psRenderEnd(GpuBufferSlice fog,SkyRenderState state,CallbackInfo ci){psActiveTextures=null;}
    @Inject(method="renderSun",at=@At("HEAD"),cancellable=true)
    private void psSun(RenderPass pass,float alpha,PoseStack pose,CallbackInfo ci){
        if(psActiveTextures==null || psActiveTextures.sun()==null)return;
        if(psSunQuad==null)psSunQuad=psBuildQuad(false);
        psDraw(pass,pose,alpha,30,psActiveTextures.sun(),psSunQuad,0);
        ci.cancel();
    }
    @Inject(method="renderMoon",at=@At("HEAD"),cancellable=true)
    private void psMoon(RenderPass pass,MoonPhase phase,float alpha,PoseStack pose,CallbackInfo ci){
        if(psActiveTextures==null || psActiveTextures.moon()==null)return;
        if(psMoonQuads==null)psMoonQuads=psBuildQuad(true);
        psDraw(pass,pose,alpha,20,psActiveTextures.moon(),psMoonQuads,phase.index()*4);
        ci.cancel();
    }
    @Unique private void psDraw(RenderPass pass,PoseStack pose,float alpha,float size,net.minecraft.resources.Identifier id,GpuBuffer quad,int base){
        var texture=Minecraft.getInstance().getTextureManager().getTexture(id);
        var indices=RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        var indexBuffer=indices.getBuffer(6);
        var transforms=RenderSystem.getDynamicUniforms().writeTransform(applyCelestialBodyTransform(pose,100,size),new Vector4f(1,1,1,alpha));
        pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.CELESTIAL));
        pass.setUniform("Sampler0",texture.getTextureView(),texture.getSampler());
        pass.setUniform("DynamicTransforms",transforms);
        pass.setVertexBuffer(0,quad.slice());
        pass.setIndexBuffer(indexBuffer,indices.type());
        pass.drawIndexed(6,1,0,base,0);
    }
    @Unique private static GpuBuffer psBuildQuad(boolean moon){
        int count=moon?8:1;
        try(var builder=ByteBufferBuilder.exactlySized(count*4*DefaultVertexFormat.POSITION_TEX.getVertexSize())){
            var buffer=new BufferBuilder(builder,PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_TEX);
            for(int i=0;i<count;i++){
                float u0=moon?(i%4)/4f:0,u1=moon?((i%4)+1)/4f:1;
                float v0=moon?(i/4)/2f:0,v1=moon?((i/4)+1)/2f:1;
                if(moon){
                    buffer.addVertex(-1,0,-1).setUv(u1,v1);buffer.addVertex(1,0,-1).setUv(u0,v1);
                    buffer.addVertex(1,0,1).setUv(u0,v0);buffer.addVertex(-1,0,1).setUv(u1,v0);
                }else{
                    buffer.addVertex(-1,0,-1).setUv(u0,v0);buffer.addVertex(1,0,-1).setUv(u1,v0);
                    buffer.addVertex(1,0,1).setUv(u1,v1);buffer.addVertex(-1,0,1).setUv(u0,v1);
                }
            }
            try(var mesh=buffer.buildOrThrow()){return RenderSystem.getDevice().createBuffer(()->"Pure Suffering celestial quad",32,mesh.vertexBuffer());}
        }
    }
    @Redirect(method="renderEndSky",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/SkyRenderer;endSkyTexture:Lnet/minecraft/client/renderer/texture/AbstractTexture;"))
    private AbstractTexture psEndTexture(SkyRenderer self){
        if(psActiveTextures!=null && psActiveTextures.sky()!=null)return Minecraft.getInstance().getTextureManager().getTexture(psActiveTextures.sky());
        return endSkyTexture;
    }
    @Shadow @org.spongepowered.asm.mixin.Final private AbstractTexture endSkyTexture;
    @Inject(method="renderEndSky",at=@At("HEAD"))
    private void psEndRotation(RenderPass pass,CallbackInfo ci){
        if(psActiveTextures!=null && psActiveTextures.endRotation()!=0){
            float angle=(float)Math.toRadians(psActiveTextures.endRotation());
            RenderSystem.getModelViewStack().pushMatrix().rotateXYZ(angle,angle,angle);
        }
    }
    @Inject(method="renderEndSky",at=@At("RETURN"))
    private void psEndRotationDone(RenderPass pass,CallbackInfo ci){
        if(psActiveTextures!=null && psActiveTextures.endRotation()!=0)RenderSystem.getModelViewStack().popMatrix();
    }
    @Inject(method="close",at=@At("HEAD"))
    private void psClose(CallbackInfo ci){if(psSunQuad!=null){psSunQuad.close();psSunQuad=null;}if(psMoonQuads!=null){psMoonQuads.close();psMoonQuads=null;}}
}
