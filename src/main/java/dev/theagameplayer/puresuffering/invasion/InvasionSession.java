package dev.theagameplayer.puresuffering.invasion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.theagameplayer.puresuffering.config.PSConfigValues;
import dev.theagameplayer.puresuffering.invasion.InvasionType.WeatherType;
import dev.theagameplayer.puresuffering.network.AddInvasionPacket;
import dev.theagameplayer.puresuffering.network.ClearInvasionsPacket;
import dev.theagameplayer.puresuffering.network.RemoveInvasionPacket;
import dev.theagameplayer.puresuffering.registries.other.PSPackets;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class InvasionSession implements Iterable<Invasion>, CommandSource {
	private final ArrayList<Invasion> invasions = new ArrayList<>();
	private final CommandSourceStack commandSource;
	private final MinecraftServer server;
	private final InvasionSessionType sessionType;
	private final InvasionDifficulty difficulty;
	private final int mobKillLimit;
	private final Style style;
	private WeatherType weatherType = WeatherType.DEFAULT;
	private int weatherChangeDelay, lightLevel;
	private boolean stopsConversions, forceNoSleep;
	public int mobsKilledByPlayer;

	public InvasionSession(final ServerLevel pLevel, final InvasionSessionType pSessionType, final InvasionDifficulty pDifficulty, final int pMobKillLimit) {
		this.commandSource = new CommandSourceStack(this, Vec3.atLowerCornerOf(pLevel.getRespawnData().pos()), Vec2.ZERO, pLevel, net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS, Component.literal("InvasionSession"), pLevel.getServer());
		this.server = pLevel.getServer();
		this.sessionType = pSessionType;
		this.difficulty = pDifficulty;
		this.mobKillLimit = pMobKillLimit;
		this.style = Style.EMPTY.withBold(pDifficulty.isHyper()).withItalic(pDifficulty.isNightmare());
	}

	public final Invasion getPrimary() {
		return this.invasions.get(0);
	}

	public final InvasionSessionType getSessionType() {
		return this.sessionType;
	}

	public final InvasionDifficulty getDifficulty() {
		return this.difficulty;
	}

	public final int getMobKillLimit() {
		return this.mobKillLimit;
	}

	public final Style getStyle() {
		return this.style;
	}

	public final boolean replaceMob(final Mob pOldMob, final Mob pMob) {
		for (final Invasion invasion : this.invasions) {
			final int index = invasion.hasMob(pOldMob);
			if (index >= 0) {
				invasion.replaceMob(pMob, index);
				return true;
			}
		}
		return false;
	}

	public final boolean splitMob(final Mob pParent, final List<Mob> pChildren) {
		for (final Invasion invasion : this.invasions) {
			if (invasion.hasSameInvasion(pParent)) {
				invasion.splitMob(pChildren);
				return true;
			}
		}
		return false;
	}

	public final void relocateMob(final Mob pMob) {
		for (final Invasion invasion : this.invasions) {
			final int index = invasion.hasMob(pMob);
			if (index > -1) invasion.relocateMob(index);
		}
	}

	public final boolean hasMob(final Mob pMob) {
		for (final Invasion invasion : this.invasions) {
			if (invasion.hasMob(pMob) > -1) return true;
		}
		return false;
	}

	public final Invasion getInvasion(final Mob pMob) {
		for (final Invasion invasion : this.invasions) {
			if (invasion.hasMob(pMob) > -1) return invasion;
		}
		return null;
	}

	public final void loadMob(final Mob pMob) {
		for (final Invasion invasion : this.invasions) {
			if (invasion.loadMob(pMob)) return;
		}
	}

	private final void update(final ServerLevel pLevel) {
		this.weatherType = WeatherType.DEFAULT;
		this.stopsConversions = false;
		this.forceNoSleep = this.difficulty.isNightmare();
		int lightLevel = 0, lc = 0;
		for (final Invasion invasion : this.invasions) {
			if (this.weatherType == WeatherType.DEFAULT)
				this.weatherType = invasion.getType().getWeatherType();
			this.stopsConversions |= invasion.getType().stopsConversions();
			this.forceNoSleep |= invasion.getSeverityInfo().forcesNoSleep();
			if (this.difficulty.isNightmare()) continue;
			final int ll = PSConfigValues.LEVELS.get(pLevel).zeroLightLevelDuringInvasions ? 15 : invasion.getSeverityInfo().getSkyRenderInfo().getLightLevel();
			if (ll < 0) continue;
			lightLevel += ll;
			lc++;
		}
		this.lightLevel = this.difficulty.isNightmare() ? 15 : (lc > 0 ? lightLevel/lc : -1);
	}

	public final boolean stopOrTick(final ServerLevel pLevel) {
		if (this.invasions.isEmpty()) return true;
		switch (this.weatherType) {
		case DEFAULT: break;
		case CLEAR: {
			if (pLevel.isRaining() || pLevel.isThundering())
				setWeather(pLevel,12000 - (int)(pLevel.getOverworldClockTime() % 12000L), 0, false, false);
			break;
		}
		case RAIN: {
			if (!pLevel.isRaining())
				setWeather(pLevel,0, 12000 - (int)(pLevel.getOverworldClockTime() % 12000L), true, false);
			break;
		}
		case THUNDER: {
			if (!pLevel.isThundering())
				setWeather(pLevel,0, 12000 - (int)(pLevel.getOverworldClockTime() % 12000L), true, true);
			break;
		}
		case UNSTABLE: {
			if (this.weatherChangeDelay < 0) {
				final int type = pLevel.getRandom().nextInt(5);
				final int time = Math.min(150 + pLevel.getRandom().nextInt(Invasion.HALF_TRANSITION + 1), 12000 - (int)(pLevel.getOverworldClockTime() % 12000L));
				setWeather(pLevel,type < 2 ? time : 0, type < 2 ? 0 : time, type == 2, type > 2);
				this.weatherChangeDelay = time;
			} else {
				--this.weatherChangeDelay;
			}
			break;
		}
		}
		this.invasions.get((int)(pLevel.getOverworldClockTime() % this.invasions.size())).tick(pLevel, this.invasions.size());
		return false;
	}

	public final boolean stopsConversions() {
		return this.stopsConversions;
	}

	public final boolean forcesNoSleep() {
		return this.forceNoSleep;
	}

	public final int getLightLevelOrDefault(final int pLightLevel) {
		return this.lightLevel > 0 ? this.lightLevel : pLightLevel;
	}

	public static final InvasionSession load(final ServerLevel pLevel, final InvasionSessionType type, final CompoundTag pNbt) {
		final InvasionDifficulty difficulty = InvasionDifficulty.values()[pNbt.getIntOr("Difficulty",0)];
		final InvasionSession session = new InvasionSession(pLevel, type, difficulty, pNbt.getIntOr("MobKillLimit",0));
		session.mobsKilledByPlayer = pNbt.getIntOr("MobsKilledByPlayer",0);
		final ListTag invasionsNBT = pNbt.getListOrEmpty(session.sessionType.getDefaultName() + "Invasions");
		for (final Tag inbt : invasionsNBT) {
			if (inbt instanceof CompoundTag nbt) {
                Invasion invasion = Invasion.load(pLevel,difficulty,nbt);
                if (invasion != null) session.add(pLevel,invasion,true);
            }
		}
		return session;
	}

	public final CompoundTag save() {
		final CompoundTag nbt = new CompoundTag();
		final ListTag invasionsNBT = new ListTag();
		nbt.putInt("Difficulty", this.difficulty.ordinal());
		nbt.putInt("MobKillLimit", this.mobKillLimit);
		nbt.putInt("MobsKilledByPlayer", this.mobsKilledByPlayer);
		for (final Invasion invasion : this.invasions)
			invasionsNBT.add(invasion.save());
		nbt.put(this.sessionType.getDefaultName() + "Invasions", invasionsNBT);
		return nbt;
	}

	//Sync to Client
	public final void add(final ServerLevel pLevel, final Invasion pInvasion, final boolean pIsLoaded) {
		this.invasions.add(pInvasion);
		if (!pIsLoaded) {
			for (final String cmd : pInvasion.getSeverityInfo().getStartCommands())
				this.server.getCommands().performPrefixedCommand(this.commandSource, cmd);
		}
		this.update(pLevel);
		if (!pIsLoaded) PSPackets.sendToClientsIn(new AddInvasionPacket(this.sessionType, this.difficulty, pInvasion), pLevel);
	}

	public final void remove(final ServerLevel pLevel, final Invasion pInvasion) {
		if (pInvasion.isPrimary()) {
			this.clear(pLevel);
			return;
		}
		for (final String cmd : pInvasion.getSeverityInfo().getEndCommands())
			this.server.getCommands().performPrefixedCommand(this.commandSource, cmd);
		this.invasions.remove(pInvasion);
		this.update(pLevel);
		PSPackets.sendToClientsIn(new RemoveInvasionPacket(pInvasion.getSeverityInfo().getSkyRenderInfo()), pLevel);
	}

	public final void clear(final ServerLevel pLevel) {
		for (final Invasion invasion : this.invasions) {
			for (final String cmd : invasion.getSeverityInfo().getEndCommands())
				this.server.getCommands().performPrefixedCommand(this.commandSource, cmd);
		}
		this.invasions.clear();
        this.update(pLevel);
		PSPackets.sendToClientsIn(new ClearInvasionsPacket(), pLevel);
	}

	public final void updateClient(final ServerPlayer playerIn) {
		PSPackets.sendToClient(new ClearInvasionsPacket(), playerIn);
		for (int index = 0; index < this.invasions.size(); ++index) {
			final Invasion invasion = this.invasions.get(index);
			PSPackets.sendToClient(new AddInvasionPacket(this.sessionType, this.difficulty, invasion), playerIn);
		}
	}

	@Override
	public final String toString() {
		return this.invasions.toString();
	}

	@Override
	public final Iterator<Invasion> iterator() {
		return this.invasions.iterator();
	}

	@Override
	public void sendSystemMessage(final Component pComponent) {
		this.server.sendSystemMessage(pComponent);
	}

	@Override
	public boolean acceptsSuccess() {
		return this.server.acceptsSuccess();
	}

	@Override
	public boolean acceptsFailure() {
		return this.server.acceptsFailure();
	}

	@Override
	public boolean shouldInformAdmins() {
		return this.server.shouldInformAdmins();
	}
    private static void setWeather(ServerLevel level,int clear,int rain,boolean raining,boolean thunder) {
        var data=level.getWeatherData();
        data.setClearWeatherTime(clear); data.setRainTime(rain); data.setThunderTime(rain);
        data.setRaining(raining);data.setThundering(thunder);
    }
}
