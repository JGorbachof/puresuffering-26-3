package dev.theagameplayer.puresuffering.event;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import dev.theagameplayer.puresuffering.PureSufferingMod;
import dev.theagameplayer.puresuffering.client.invasion.ClientInvasionSession;
import dev.theagameplayer.puresuffering.client.renderer.entity.layers.HyperChargeLayer;
import dev.theagameplayer.puresuffering.client.sounds.InvasionMusicManager;
import dev.theagameplayer.puresuffering.config.PSConfigValues;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class PSClientEvents {
	private static final Logger LOGGER = PureSufferingMod.LOGGER;

	@SuppressWarnings("unchecked")
	public static final void addLayers(final EntityRenderersEvent.AddLayers pEvent) { //TODO: Redo for mobs not using a mob renderer
		final List<EntityType<? extends Mob>> entityTypes = List.copyOf(
				BuiltInRegistries.ENTITY_TYPE.listElements()
				.filter(et -> DefaultAttributes.hasSupplier(et.value()) && et.value().getCategory() == MobCategory.MONSTER)
				.map(et -> (EntityType<? extends Mob>)et.value())
				.collect(Collectors.toList()));
		entityTypes.forEach(et -> {
			final EntityRenderer<?,?> renderer = pEvent.getRenderer(et);
			if (renderer instanceof MobRenderer mobRenderer) {
				mobRenderer.addLayer(new HyperChargeLayer(mobRenderer));
				return;
			}
			LOGGER.warn("HyperChargeLayer failed to apply to " + BuiltInRegistries.ENTITY_TYPE.getKey(et) + ", perhaps renderer is not instance of MobRenderer?");
		});
	}

	public static final void loggedIn(final ClientPlayerNetworkEvent.LoggingIn pEvent) {
		InvasionMusicManager.reloadMusic();
	}

	public static final void loggedOut(final ClientPlayerNetworkEvent.LoggingOut pEvent) {
		ClientInvasionSession.clear();
        dev.theagameplayer.puresuffering.registries.other.PSGameRules.clearClient();
	}

    public static void debugText(RegisterDebugEntriesEvent event) {
        var id=PureSufferingMod.namespace("invasions");
        event.includeInProfile(id,net.minecraft.client.gui.components.debug.DebugScreenProfile.DEFAULT,net.minecraft.client.gui.components.debug.DebugScreenEntryStatus.IN_OVERLAY);
        event.register(id,(display,level,clientChunk,serverChunk)->{
            var session=ClientInvasionSession.get(Minecraft.getInstance().level);
            display.addLine("[PureSuffering] Current Invasions: "+(session==null?0:session.size()));
            display.addLine("[PureSuffering] Invasion XP Multiplier: "+(session==null?0:session.getXPMultiplier())+"x");
        });
    }
    public static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new com.google.common.reflect.TypeToken<MobRenderer<Mob,net.minecraft.client.renderer.entity.state.LivingEntityRenderState,?>>() {}, (entity,state) -> {
            state.setRenderData(HyperChargeLayer.CHARGE,entity instanceof dev.theagameplayer.puresuffering.world.entity.PSInvasionMob mob ? mob.psGetHyperCharge() : 0);
        });
    }
    private static final net.minecraft.util.context.ContextKey<java.util.List<dev.theagameplayer.puresuffering.client.renderer.ClientEffectsRenderer.Quad>> EFFECTS = new net.minecraft.util.context.ContextKey<>(PureSufferingMod.namespace("effects"));
    public static void extract(ExtractLevelRenderStateEvent event) {
        if (PSConfigValues.client==null || !PSConfigValues.client.enableSkyEffects) return;
        var session=ClientInvasionSession.get(event.getLevel());
        if (session==null) return;
        event.getRenderState().setRenderData(EFFECTS,session.getClientEffectsRenderer().extract(event.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
    }
    public static void submit(SubmitCustomGeometryEvent event) {
        var quads=event.getLevelRenderState().getRenderData(EFFECTS);
        if (quads==null || quads.isEmpty()) return;
        event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(),net.minecraft.client.renderer.rendertype.RenderTypes.debugQuads(),(pose,buffer)->{
            for (var quad:quads) {
                buffer.addVertex(pose,quad.a()).setColor(quad.color());
                buffer.addVertex(pose,quad.b()).setColor(quad.color());
                buffer.addVertex(pose,quad.c()).setColor(quad.color());
                buffer.addVertex(pose,quad.d()).setColor(quad.color());
            }
        });
    }

	public static final void screenInitPre(final ScreenEvent.Init.Pre pEvent) {
		if (pEvent.getScreen() instanceof CreateWorldScreen createWorldScreen)
			createWorldScreen.getUiState().setDifficulty(Difficulty.HARD);
	}

	public static final void fogColors(final ViewportEvent.ComputeFogColor pEvent) { //Render Tick
		final FogType fogType = pEvent.getCamera().getFluidInCamera();
		if (fogType != FogType.NONE && fogType != FogType.WATER) return;
		final Entity entity = pEvent.getCamera().entity();
		if (entity instanceof net.minecraft.world.entity.LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
		final Minecraft mc = Minecraft.getInstance();
		final ClientInvasionSession session = ClientInvasionSession.get(mc.level);
		if (session == null) return;
		final float[] rgb = session.getFogRGB();
		pEvent.setRed(pEvent.getRed() + rgb[0]);
		pEvent.setGreen(pEvent.getGreen() + rgb[1]);
		pEvent.setBlue(pEvent.getBlue() + rgb[2]);
	}
}
