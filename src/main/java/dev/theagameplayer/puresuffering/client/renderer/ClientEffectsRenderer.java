package dev.theagameplayer.puresuffering.client.renderer;

import java.util.ArrayList;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.theagameplayer.puresuffering.client.ClientTransitionHandler;
import dev.theagameplayer.puresuffering.config.PSConfigValues;
import dev.theagameplayer.puresuffering.invasion.InvasionDifficulty;
import dev.theagameplayer.puresuffering.util.PSRGB;
import dev.theagameplayer.puresuffering.invasion.Invasion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.dimension.DimensionType;

public final class ClientEffectsRenderer {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("neoforge", "textures/white.png");
	private final ArrayList<SoundRing> soundRings = new ArrayList<>();
	private final ArrayList<VortexParticle> vortexParticles = new ArrayList<>();
	private final InvasionDifficulty difficulty;
	private final boolean canSeeSky;
	private final int[] delays = new int[2];

	public ClientEffectsRenderer(final InvasionDifficulty pDifficulty) {
		final Minecraft mc = Minecraft.getInstance();
		final DimensionType dimType = mc.level.dimensionType();
		this.difficulty = pDifficulty;
		this.canSeeSky = !dimType.hasCeiling();
	}

	public final void tick(final RandomSource pRandom, final long pDayTime, final int pStartTime) {
		if (pStartTime < 40 && PSConfigValues.client.enableInvasionStartEffects) {
			if (this.delays[0] < 1) {
				if (this.canSeeSky) this.soundRings.add(new SoundRing(this.difficulty, pStartTime));
				this.delays[0] = this.difficulty.getRingDelay();
			} else {
				--this.delays[0];
			}
		}
		if (this.difficulty.isHyper()) {
			if (this.delays[1] < 1) {
				this.vortexParticles.add(new VortexParticle(pRandom, this.difficulty));
				this.delays[1] = PSConfigValues.client.vortexParticleDelay;
			} else {
				--this.delays[1];
			}
		}
		this.soundRings.removeIf(ring -> ring.tick());
		this.vortexParticles.removeIf(particle -> particle.tick(pDayTime));
	}

    public record Quad(org.joml.Vector3f a,org.joml.Vector3f b,org.joml.Vector3f c,org.joml.Vector3f d,int color) {}
    public java.util.List<Quad> extract(float partialTicks) {
        final Minecraft mc = Minecraft.getInstance();
        final float weatherVisibility = mc.level.getRainLevel(partialTicks)*0.15F;
        final long dayTime = mc.level.getOverworldClockTime()%12000L;
        var result = new ArrayList<Quad>();
        var pose = new PoseStack();
        this.soundRings.forEach(ring -> ring.render(pose,partialTicks,result));
        if (this.difficulty.isHyper())
            this.vortexParticles.forEach(particle -> particle.render(pose,partialTicks,weatherVisibility,dayTime,result));
        return java.util.List.copyOf(result);
    }
    private static void quad(java.util.List<Quad> result,Matrix4f matrix,float sizeX,float sizeZ,float red,float green,float blue,float alpha) {
        int color=net.minecraft.util.ARGB.colorFromFloat(net.minecraft.util.Mth.clamp(alpha,0,1),net.minecraft.util.Mth.clamp(red,0,1),net.minecraft.util.Mth.clamp(green,0,1),net.minecraft.util.Mth.clamp(blue,0,1));
        result.add(new Quad(matrix.transformPosition(new org.joml.Vector3f(-sizeX,100,-sizeZ)),matrix.transformPosition(new org.joml.Vector3f(sizeX,100,-sizeZ)),matrix.transformPosition(new org.joml.Vector3f(sizeX,100,sizeZ)),matrix.transformPosition(new org.joml.Vector3f(-sizeX,100,sizeZ)),color));
    }

	private static final float getAlpha(final float pAlphaOffset, final long pDayTime) {
		float alpha = pAlphaOffset;
		if (pDayTime < Invasion.TRANSITION_TIME) {
			alpha = (pAlphaOffset/Invasion.TRANSITION_TIME) * (pDayTime + 1); //0-1
		} else if (pDayTime > 11999L - Invasion.TRANSITION_TIME) {
			alpha = (pAlphaOffset/Invasion.TRANSITION_TIME) * (12000L - pDayTime); //1-0
		}
		return alpha;
	}

	private static final class SoundRing {
		private final float[] rgb = new float[3];
		private final float[] dist = new float[2];
		private final float[] sizeAlpha = new float[2];
		private final float[] length = new float[4];
		private int ticksAlive;

		private SoundRing(final InvasionDifficulty pDifficulty, final int pStartTime) {
			final PSRGB color = pDifficulty.getInterColor(40, 40 - pStartTime);
			this.rgb[0] = color.red();
			this.rgb[1] = color.green();
			this.rgb[2] = color.blue();
			this.ticksAlive = Invasion.HALF_TRANSITION;
		}

		private final boolean tick() {
			this.dist[1] = this.dist[0];
			this.dist[0] = Invasion.HALF_TRANSITION - this.ticksAlive;
			this.sizeAlpha[1] = this.sizeAlpha[0];
			this.sizeAlpha[0] = (float)this.ticksAlive/Invasion.HALF_TRANSITION;
			this.length[1] = this.length[0];
			this.length[0] = this.dist[0]/this.sizeAlpha[0];
			--this.ticksAlive;
			return this.ticksAlive < 1;
		}

		private final void render(final PoseStack pPoseStack, final float pPartialTicks,final java.util.List<Quad> result) {
			final float dist1 = this.dist[1] + (this.dist[0] - this.dist[1]) * pPartialTicks;
			final float dist2 = -dist1/3;
			final float sizeAlpha = this.sizeAlpha[1] + (this.sizeAlpha[0] - this.sizeAlpha[1]) * pPartialTicks;
			final float length = this.length[1] + (this.length[0] - this.length[1]) * pPartialTicks;


			for (final Direction dir : Direction.Plane.HORIZONTAL) {
				pPoseStack.pushPose();
				pPoseStack.rotate(Axis.YP.rotationDegrees(dir.toYRot()));
				pPoseStack.translate(dist1, dist2, sizeAlpha);
				final Matrix4f matrix4f = pPoseStack.last().pose();
                quad(result,matrix4f,sizeAlpha,sizeAlpha*length,rgb[0],rgb[1],rgb[2],sizeAlpha);
				pPoseStack.popPose();
			}
		}
	}

	private static final class VortexParticle {
		private final float speed;
		private final float degreesStart, degreesFinish;
		private final float maxSize, maxAlpha;
		private final float[] rgb = new float[3];
		private final Axis[] axis = new Axis[3];
		private final float[][] rot;
		private final float scaleFactor;
		private float size, a, alpha;
		private float ticksAlive;

		private VortexParticle(final RandomSource pRandom, final InvasionDifficulty pDifficulty) { 
			this.speed = pRandom.nextInt(PSConfigValues.client.maxVortexParticleLifespan - PSConfigValues.client.minVortexParticleLifespan + 1) + PSConfigValues.client.minVortexParticleLifespan;
			this.degreesStart = pRandom.nextInt(360);
			this.degreesFinish = pRandom.nextInt(360) + 180;
			this.maxSize = pRandom.nextFloat() + 0.75F;
			this.maxAlpha = pRandom.nextFloat() * 0.5F + 0.5F;
			final PSRGB color = pDifficulty.getRandomColor(pRandom);
			this.rgb[0] = color.red();
			this.rgb[1] = color.green();
			this.rgb[2] = color.blue();
			this.axis[0] = pDifficulty.isNightmare() ? Axis.YN : Axis.YP;
			this.axis[1] = pRandom.nextBoolean() ? Axis.XP : Axis.XN;
			this.axis[2] = pRandom.nextBoolean() ? Axis.ZP : Axis.ZN;
			this.rot = new float[PSConfigValues.client.useFastEffects ? 2 : pRandom.nextInt(3) + 3][3];
			this.scaleFactor = 1.0F/(this.rot.length - 1);
			this.ticksAlive = this.speed;
		}

		private final boolean tick(final long pDayTime) {
			final float percentAlive = this.ticksAlive/this.speed;
			this.a = this.vortexValue(this.a, this.maxAlpha, pDayTime);
			this.alpha = getAlpha(this.a, pDayTime);
			for (int i = this.rot.length - 1; i > 0; --i) {
				this.rot[i][0] = this.rot[i - 1][0];
				this.rot[i][1] = this.rot[i - 1][1];
				this.rot[i][2] = this.rot[i - 1][2];
			}
			this.rot[0][0] = this.degreesStart + percentAlive * this.degreesFinish;
			this.rot[0][1] = percentAlive > 0.5F ? percentAlive * 180.0F : 180.0F - percentAlive * 180.0F;
			this.rot[0][2] = percentAlive > 0.5F ? 180.0F + percentAlive * 180.0F : 180.0F + percentAlive * 180.0F;
			this.size = this.vortexValue(this.size, this.maxSize, pDayTime);
			--this.ticksAlive;
			return this.ticksAlive < 1;
		}

		private final float vortexValue(final float pCurrent, final float pMax, final long pDayTime) {
			final float result = this.ticksAlive > this.speed/2 ? pMax/this.speed * (this.speed - this.ticksAlive) : pMax/this.speed * this.ticksAlive;
			if (pDayTime < Invasion.HALF_TRANSITION) {
				return result + pCurrent/Invasion.HALF_TRANSITION; //0-1
			} else if (pDayTime > 11999L - Invasion.HALF_TRANSITION) {
				return result - pCurrent/Invasion.HALF_TRANSITION; //1-0
			}
			return result;
		}

		private final void render(final PoseStack pPoseStack, final float pPartialTicks, final float pWeatherVisibility, final long pDayTime,final java.util.List<Quad> result) {
			final float alpha = this.alpha - ClientTransitionHandler.getWeatherVisibility(pWeatherVisibility, pDayTime);
			final int length = PSConfigValues.client.useFastEffects ? 2 : this.rot.length;

			for (int i = 1; i < length; ++i) {
				final float s = this.scaleFactor * (this.rot.length - i);
				final float size = this.size * s;

				pPoseStack.pushPose();
				pPoseStack.rotate(this.axis[0].rotationDegrees(this.rot[i][0] + (this.rot[i - 1][0] - this.rot[i][0]) * pPartialTicks));
				pPoseStack.rotate(this.axis[1].rotationDegrees(this.rot[i][1] + (this.rot[i - 1][1] - this.rot[i][1]) * pPartialTicks));
				pPoseStack.rotate(this.axis[2].rotationDegrees(this.rot[i][2] + (this.rot[i - 1][2] - this.rot[i][2]) * pPartialTicks));
				final Matrix4f matrix4f = pPoseStack.last().pose();
                quad(result,matrix4f,size,size,rgb[0]*s,rgb[1]*s,rgb[2]*s,alpha*s);
				pPoseStack.popPose();
			}
		}
	}
}
