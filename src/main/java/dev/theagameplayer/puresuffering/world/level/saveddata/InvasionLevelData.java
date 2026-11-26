package dev.theagameplayer.puresuffering.world.level.saveddata;


import dev.theagameplayer.puresuffering.world.level.InvasionManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;

public final class InvasionLevelData extends SavedData {

	private final InvasionManager invasionManager;
	private long invasionTime;
	private int xpMult;

	private InvasionLevelData(final ServerLevel pLevel) {
		this.invasionManager = new InvasionManager(pLevel.dimensionType().hasFixedTime());
		this.setDirty();
	}

    public static SavedDataType<InvasionLevelData> type(final ServerLevel level) {
        return new SavedDataType<>(Identifier.fromNamespaceAndPath("puresuffering",getFileId(level.dimensionTypeRegistration())),
            () -> new InvasionLevelData(level),
            CompoundTag.CODEC.xmap(tag -> load(level,tag), data -> data.save(new CompoundTag())));
    }

    public static String getFileId(final Holder<DimensionType> type) {
        return type.is(BuiltinDimensionTypes.END) ? "invasions_end" : "invasions";
    }

    public static InvasionLevelData get(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(type(level));
    }

	public final InvasionManager getInvasionManager() {
		return this.invasionManager;
	}

	public final long getInvasionTime() {
		return this.invasionTime;
	}

	public final void setInvasionTime(final long pInvasionTime) {
		this.invasionTime = pInvasionTime;
		this.setDirty();
	}

	public final int getXPMultiplier() {
		return this.xpMult;
	}

	public final void setXPMultiplier(final int pXPMult) {
		this.xpMult = pXPMult;
		this.setDirty();
	}

	public static final InvasionLevelData load(final ServerLevel pLevel, final CompoundTag pNbt) {
		final InvasionLevelData ilData = new InvasionLevelData(pLevel);
		ilData.getInvasionManager().load(pLevel, pNbt.getCompoundOrEmpty("InvasionManager"));
		ilData.invasionTime = pNbt.getLongOr("InvasionTime",0L);
		ilData.xpMult = pNbt.getIntOr("XPMult",0);
		return ilData;
	}

	public CompoundTag save(final CompoundTag pNbt) {
		pNbt.put("InvasionManager", this.invasionManager.save());
		pNbt.putLong("InvasionTime", this.invasionTime);
		pNbt.putInt("XPMult", this.xpMult);
		return pNbt;
	}
}
