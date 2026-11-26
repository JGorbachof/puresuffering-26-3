package dev.theagameplayer.puresuffering.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import dev.theagameplayer.puresuffering.client.invasion.ClientInvasionSession;
@Mixin(ClientLevel.class)
public final class ClientLevelMixin {
    @Inject(method="disconnect",at=@At("HEAD"))
    private void psClearInvasions(net.minecraft.network.chat.Component message,CallbackInfo ci){ClientInvasionSession.clear();}
}
