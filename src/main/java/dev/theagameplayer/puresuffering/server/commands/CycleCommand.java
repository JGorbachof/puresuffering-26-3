package dev.theagameplayer.puresuffering.server.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class CycleCommand {
	public static final ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("cycle").requires(css -> {
					return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(css);
				}).executes(ctx -> {
					final long dayTime = ctx.getSource().getLevel().getOverworldClockTime();
					final long addTime = 12000L - dayTime % 12000L;
					final ServerLevel level = ctx.getSource().getLevel();
                    level.clockManager().addTicks(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD), (int)addTime);
					ctx.getSource().sendSuccess(() -> Component.translatable("commands.puresuffering.cycle", dayTime + addTime), true);
					return (int)addTime;
				});
	}
}
