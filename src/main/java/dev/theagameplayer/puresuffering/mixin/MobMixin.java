package dev.theagameplayer.puresuffering.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.theagameplayer.puresuffering.world.entity.PSInvasionMob;
import net.minecraft.world.entity.Mob;

@Mixin(Mob.class)
public final class MobMixin implements PSInvasionMob {
	@Inject(at = @At("RETURN"), method = "Lnet/minecraft/world/entity/Mob;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V")
	private final void addAdditionalSaveData(final net.minecraft.world.level.storage.ValueOutput pCompound, final CallbackInfo pCallback) {
		pCompound.putInt(HYPER_CHARGE, this.psGetHyperCharge());
	}

	@Inject(at = @At("RETURN"), method = "Lnet/minecraft/world/entity/Mob;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V")
	private final void readAdditionalSaveData(final net.minecraft.world.level.storage.ValueInput pCompound, final CallbackInfo pCallback) {
		this.psSetHyperCharge(pCompound.getIntOr(HYPER_CHARGE,0));
	}

	@Override
	public final int psGetHyperCharge() {
		return ((Mob)(Object)this).getData(dev.theagameplayer.puresuffering.registries.PSAttachments.HYPER_CHARGE);
	}

	@Override
	public final void psSetHyperCharge(final int pHyperCharge) {
		((Mob)(Object)this).setData(dev.theagameplayer.puresuffering.registries.PSAttachments.HYPER_CHARGE, pHyperCharge);
	}
}
