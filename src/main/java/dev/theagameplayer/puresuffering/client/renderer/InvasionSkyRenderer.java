package dev.theagameplayer.puresuffering.client.renderer;

import java.util.ArrayList;
import net.minecraft.world.level.dimension.DimensionType.Skybox;


import dev.theagameplayer.puresuffering.client.ClientTransitionHandler;
import dev.theagameplayer.puresuffering.client.invasion.ClientInvasion;
import dev.theagameplayer.puresuffering.client.invasion.InvasionSkyRenderInfo;
import dev.theagameplayer.puresuffering.invasion.InvasionDifficulty;
import dev.theagameplayer.puresuffering.invasion.Invasion;
import net.minecraft.client.Minecraft;



import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class InvasionSkyRenderer {
	private static final Identifier DEFAULT_SUN = Identifier.withDefaultNamespace("textures/environment/sun.png");
	private static final Identifier DEFAULT_MOON = Identifier.withDefaultNamespace("textures/environment/moon_phases.png");
	private static final Identifier DEFAULT_END_SKY = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
	private final Skybox skyType;
	private final InvasionDifficulty difficulty;
	private final boolean noTick;
	private ArrayList<ClientInvasion> invasions;
	private Identifier sun, moon, sky;
	private boolean noSunOrMoon;
	private float[][] rgb;
	private float alphaOffset, weatherVisibilityOffset;
	private float alpha, weatherVisibility;

	public InvasionSkyRenderer(final InvasionDifficulty pDifficulty) {
		final Minecraft mc = Minecraft.getInstance();
		final ClientLevel level = mc.level;
		this.skyType = level.dimensionType().skybox();
		this.difficulty = pDifficulty;
		this.noTick = this.skyType != Skybox.OVERWORLD;
	}
	
	public final void update(final InvasionSkyRenderInfo pPrimaryInfo, final ArrayList<ClientInvasion> pInvasions) {
		switch (this.skyType) {
		case NONE: return;
		case OVERWORLD: {
			final ArrayList<InvasionSkyRenderInfo> skyColorList = new ArrayList<>(pInvasions.size()); //Invasions that change the sky colors
			final ArrayList<InvasionSkyRenderInfo> sunMoonAlphaList = new ArrayList<>(pInvasions.size()); //Invasions that changes the sun & moon alpha.
			final ArrayList<InvasionSkyRenderInfo> weatherVisibilityList = new ArrayList<>(pInvasions.size()); //Invasions that are visible in weather
			this.sun = pPrimaryInfo.getSunTexture();
			this.moon = pPrimaryInfo.getMoonTexture();
			this.noSunOrMoon = false;
			for (final ClientInvasion invasion : pInvasions) {
				final InvasionSkyRenderInfo renderInfo = invasion.getSkyRenderInfo();
				if (!this.noSunOrMoon)
					this.noSunOrMoon = renderInfo.getSunMoonAlpha() == 0;
				if (renderInfo.getSunMoonAlpha() < 1 && renderInfo.isSunMoonAlphaChanged())
					sunMoonAlphaList.add(renderInfo);
				if (!this.noSunOrMoon && renderInfo.isWeatherVisibilityChanged())
					weatherVisibilityList.add(renderInfo);
				if (!this.difficulty.isNightmare() && renderInfo.isSkyColorChanged())
					skyColorList.add(renderInfo);
			}
			this.invasions = pInvasions;
			this.rgb = new float[2][3];
			if (this.difficulty.isNightmare()) {
				this.rgb[0][0] = -1.0F;
				this.rgb[0][1] = -1.0F;
				this.rgb[0][2] = -1.0F;
			} else {
				for (final InvasionSkyRenderInfo renderInfo : skyColorList) {
					this.rgb[0][0] += renderInfo.getRGBOffset(0) / skyColorList.size();
					this.rgb[0][1] += renderInfo.getRGBOffset(1) / skyColorList.size();
					this.rgb[0][2] += renderInfo.getRGBOffset(2) / skyColorList.size();
				}
			}
			this.alphaOffset = sunMoonAlphaList.isEmpty() ? 1.0F : 0.0F;
			if (!this.noSunOrMoon) {
				for (final InvasionSkyRenderInfo renderInfo : sunMoonAlphaList)
					this.alphaOffset += renderInfo.getSunMoonAlpha() / sunMoonAlphaList.size();
			}
			this.weatherVisibilityOffset = 0.0F;
			if (!this.noSunOrMoon) {
				for (final InvasionSkyRenderInfo renderInfo : weatherVisibilityList)
					this.weatherVisibilityOffset += renderInfo.getWeatherVisibility() / weatherVisibilityList.size();
			}
            break;
		}
		case END: {
			this.sky = pPrimaryInfo.getFixedSkyTexture();
			this.invasions = pInvasions;
		}
		}
	}
	
	public final void tick(final long pDayTime) {
		if (this.noTick || this.rgb == null || this.invasions == null) return;
		this.rgb[1][0] = this.rgb[0][0];
		this.rgb[1][1] = this.rgb[0][1];
		this.rgb[1][2] = this.rgb[0][2];
		for (final ClientInvasion invasion : this.invasions)
			invasion.flickerSkyRGB(this.rgb[1]);
		ClientTransitionHandler.getSkyColor(this.rgb[1], pDayTime);
		for (final ClientInvasion invasion : this.invasions)
			this.alphaOffset = invasion.flickerAlpha(this.alphaOffset);
		this.alpha = ClientTransitionHandler.getSunMoonAlpha((this.sun != null || this.moon != null) && !this.noSunOrMoon, this.alphaOffset, pDayTime);
		this.weatherVisibility = Mth.clamp(ClientTransitionHandler.getWeatherVisibility(this.weatherVisibilityOffset, pDayTime), 0.0F, 1.0F);
	}
	
    public record Textures(Identifier sun, Identifier moon, Identifier sky, float endRotation) {}

    public Textures extract(net.minecraft.client.renderer.state.level.SkyRenderState state,long dayTime) {
        if (state.skybox == Skybox.OVERWORLD && rgb != null) {
            if (state.skyColor != null) state.skyColor = new org.joml.Vector3f(
                Mth.clamp(state.skyColor.x()+rgb[1][0],0,1),
                Mth.clamp(state.skyColor.y()+rgb[1][1],0,1),
                Mth.clamp(state.skyColor.z()+rgb[1][2],0,1));
            state.rainBrightness = Mth.clamp(alpha - (1-state.rainBrightness) + weatherVisibility,0,1);
            state.starBrightness *= Mth.clamp(alpha,0,1);
        }
        boolean transition = dayTime < Invasion.HALF_TRANSITION || dayTime > 11999L-Invasion.HALF_TRANSITION;
        return new Textures(transition ? null : sun,transition ? null : moon,sky,difficulty.isNightmare() ? dayTime : 0);
    }
}
