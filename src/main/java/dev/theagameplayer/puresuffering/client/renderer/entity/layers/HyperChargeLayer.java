package dev.theagameplayer.puresuffering.client.renderer.entity.layers;
import dev.theagameplayer.puresuffering.PureSufferingMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;

public final class HyperChargeLayer<S extends LivingEntityRenderState,M extends EntityModel<? super S>> extends RenderLayer<S,M> {
    public static final ContextKey<Integer> CHARGE = new ContextKey<>(PureSufferingMod.namespace("hyper_charge"));
    private static final Identifier HYPER_CHARGE_LOCATION1 = PureSufferingMod.namespace("textures/entity/charged_armor1.png");
    private static final Identifier HYPER_CHARGE_LOCATION2 = PureSufferingMod.namespace("textures/entity/charged_armor2.png");
    public HyperChargeLayer(RenderLayerParent<S,M> renderer){super(renderer);}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,S state,float yaw,float pitch) {
        Integer charge=state.getRenderData(CHARGE);
        if (charge==null || charge<=0) return;
        float tick=state.ageInTicks;
        float color=Mth.clamp(0.2F*charge,0,1);
        collector.order(1).submitModel(getParentModel(),state,pose,RenderTypes.energySwirl(charge<4?HYPER_CHARGE_LOCATION1:HYPER_CHARGE_LOCATION2,Mth.sin(tick*0.007854F*charge),tick*(0.01F*charge)%1),light,OverlayTexture.NO_OVERLAY,ARGB.colorFromFloat(color,color,color,color),null,state.outlineColor);
    }
}
