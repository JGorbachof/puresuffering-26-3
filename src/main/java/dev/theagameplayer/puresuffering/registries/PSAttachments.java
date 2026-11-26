package dev.theagameplayer.puresuffering.registries;

import dev.theagameplayer.puresuffering.PureSufferingMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class PSAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PureSufferingMod.MODID);
    public static final Supplier<AttachmentType<Integer>> HYPER_CHARGE =
        TYPES.register("hyper_charge", () -> AttachmentType.builder(() -> 0)
            .sync(ByteBufCodecs.VAR_INT).build());
}
