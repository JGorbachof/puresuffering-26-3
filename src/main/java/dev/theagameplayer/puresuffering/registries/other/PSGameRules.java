package dev.theagameplayer.puresuffering.registries.other;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Supplier;
import dev.theagameplayer.puresuffering.PureSufferingMod;
import dev.theagameplayer.puresuffering.config.PSConfigValues;
import dev.theagameplayer.puresuffering.network.UpdateGameRulePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.level.GameRuleChangedEvent;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;

public final class PSGameRules {
	public static final ArrayList<PSGameRule<?>> GAME_RULES = new ArrayList<>();

	public static final BooleanRule ENABLE_INVASIONS = new BooleanRule("enableInvasions", true, () -> true);
	public static final BooleanRule ENABLE_HYPER_INVASIONS = new BooleanRule("enableHyperInvasions", true, () -> PSConfigValues.common.enableHyperInvasions);
	public static final BooleanRule ENABLE_NIGHTMARE_INVASIONS = new BooleanRule("enableNightmareInvasions", true, () -> PSConfigValues.common.enableNightmareInvasions);
	public static final BooleanRule INVASION_ANTI_GRIEF = new BooleanRule("invasionAntiGrief", false, () -> PSConfigValues.common.invasionAntiGrief);
	public static final BooleanRule CONSISTENT_INVASIONS = new BooleanRule("consistentInvasions", false, () -> PSConfigValues.common.consistentInvasions);
	public static final BooleanRule TIERED_INVASIONS = new BooleanRule("tieredInvasions", false, () -> PSConfigValues.common.tieredInvasions);
	public static final BooleanRule CANCELABLE_INVASIONS = new BooleanRule("cancelableInvasions", false, () -> true);
	public static final BooleanRule HYPER_AGGRESSION = new BooleanRule("hyperAggression", false, () -> PSConfigValues.common.hyperAggression);
	public static final BooleanRule HYPER_CHARGE = new BooleanRule("hyperCharge", true, () -> PSConfigValues.common.hyperCharge);
	public static final BooleanRule FORCE_INVASION_SLEEPLESSNESS = new BooleanRule("forceInvasionSleeplessness", false, () -> PSConfigValues.common.forceInvasionSleeplessness);
	public static final BooleanRule USE_XP_MULTIPLIER = new BooleanRule("useXPMultiplier", false, () -> PSConfigValues.common.useXPMultiplier);
	public static final BooleanRule MOBS_DIE_AT_END_OF_INVASIONS = new BooleanRule("mobsDieAtEndOfInvasions", false, () -> PSConfigValues.common.mobsDieAtEndOfInvasions);
	public static final BooleanRule WEAKENED_INVASION_VEXES = new BooleanRule("weakenedInvasionVexes", false, () -> PSConfigValues.common.weakenedInvasionVexes);
	public static final BooleanRule ENABLE_INVASION_AMBIENCE = new BooleanRule("enableInvasionAmbience", false, () -> PSConfigValues.common.enableInvasionAmbience);
	public static final BooleanRule NOTIFY_PLAYERS_ABOUT_INVASIONS = new BooleanRule("notifyPlayersAboutInvasions", false, () -> PSConfigValues.common.notifyPlayersAboutInvasions);
	public static final BooleanRule ZERO_TICK_DELAY = new BooleanRule("zeroTickDelay", false, () -> PSConfigValues.common.zeroTickDelay);
	public static final BooleanRule NO_INVASIONS_DURING_FIRST_ENDER_DRAGON_FIGHT = new BooleanRule("noInvasionsDuringFirstEnderDragonFight", false, () -> PSConfigValues.common.noInvasionsDuringFirstEnderDragonFight);

	public static final IntegerRule INVASION_START_DELAY = new IntegerRule("invasionStartDelay", false, () -> PSConfigValues.common.invasionStartDelay);
	public static final IntegerRule PRIMARY_INVASION_MOB_CAP = new IntegerRule("primaryInvasionMobCap", false, () -> PSConfigValues.common.primaryInvasionMobCap);
	public static final IntegerRule SECONDARY_INVASION_MOB_CAP = new IntegerRule("secondaryInvasionMobCap", false, () -> PSConfigValues.common.secondaryInvasionMobCap);
	public static final IntegerRule MOB_KILL_LIMIT = new IntegerRule("mobKillLimit", false, () -> PSConfigValues.common.mobKillLimit);
	public static final IntegerRule MOB_SPAWN_CHUNK_RADIUS = new IntegerRule("mobSpawnChunkRadius", false, () -> PSConfigValues.common.mobSpawnChunkRadius);
	public static final IntegerRule NO_SPAWN_MOBS_BLOCK_RADIUS = new IntegerRule("noSpawnMobsBlockRadius", false, () -> PSConfigValues.common.noSpawnMobsBlockRadius);


    public static void registerGameRules(RegisterEvent event) {
        event.register(Registries.GAME_RULE, helper -> {
            for (PSGameRule<?> rule : GAME_RULES)
                helper.register(PureSufferingMod.namespace(rule.name.replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT)),rule.key);
        });
    }
    public static PSGameRule<?> fromString(String name) {
        for (PSGameRule<?> rule : GAME_RULES) if (rule.name.equals(name)) return rule;
        return null;
    }
    public static void syncToConfig(MinecraftServer server) {
        for (PSGameRule<?> rule : GAME_RULES) rule.syncToConfig(server);
    }
    public static void syncToServer(ServerPlayer player) {
        for (PSGameRule<?> rule : GAME_RULES) if (rule.updatesClient) rule.send(player);
    }
    public static void changed(GameRuleChangedEvent event) {
        for (PSGameRule<?> rule : GAME_RULES) if (rule.key == event.getGameRule() && rule.updatesClient)
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) rule.send(player);
    }
    public static void clearClient() { for (PSGameRule<?> rule : GAME_RULES) rule.resetClient(); }
    public abstract static class PSGameRule<T> {
        private final String name;
        private final boolean updatesClient;
        protected final GameRule<T> key;
        private final Supplier<T> configDefault;
        private T clientValue;
        private PSGameRule(String name, boolean updates, Supplier<T> config, GameRule<T> key) {
            this.name=name;this.updatesClient=updates;this.configDefault=config;this.key=key;this.clientValue=key.defaultValue();
            GAME_RULES.add(this);
        }
        public T getRule(GameRules rules) {return rules.get(key);}
        public void syncToConfig(MinecraftServer server) {server.getGameRules().set(key,configDefault.get(),server);}
        public void setClient(T value) {clientValue=value;}
        public void resetClient(){clientValue=key.defaultValue();}
        protected T value(Level level) {
            if (level instanceof ServerLevel serverLevel) {
                if (PSConfigValues.common != null && PSConfigValues.common.overrideGameRules && this != ENABLE_INVASIONS) return configDefault.get();
                return serverLevel.getGameRules().get(key);
            }
            return clientValue;
        }
        protected abstract void send(ServerPlayer player);
        @Override public String toString(){return name;}
    }
    public static final class BooleanRule extends PSGameRule<Boolean> {
        private BooleanRule(String name,boolean updates,Supplier<Boolean> config) {
            super(name,updates,config,new GameRule<>(GameRuleCategory.MISC,GameRuleType.BOOL,BoolArgumentType.bool(),GameRuleTypeVisitor::visitBoolean,Codec.BOOL,b -> b ? 1 : 0,defaultBool(name),FeatureFlagSet.of()));
        }
        public boolean get(Level level){return value(level);}
        protected void send(ServerPlayer player){PSPackets.sendToClient(new UpdateGameRulePacket(this,get(player.level())),player);}
    }
    public static final class IntegerRule extends PSGameRule<Integer> {
        private IntegerRule(String name,boolean updates,Supplier<Integer> config) {
            super(name,updates,config,new GameRule<>(GameRuleCategory.MISC,GameRuleType.INT,IntegerArgumentType.integer(),GameRuleTypeVisitor::visitInteger,Codec.INT,i -> i,defaultInt(name),FeatureFlagSet.of()));
        }
        public int get(Level level){return value(level);}
        protected void send(ServerPlayer player){PSPackets.sendToClient(new UpdateGameRulePacket(this,get(player.level())),player);}
    }
    private static boolean defaultBool(String name) {
        return switch(name) {
            case "enableInvasions","cancelableInvasions" -> true;
            case "enableHyperInvasions","enableNightmareInvasions","tieredInvasions","hyperAggression","hyperCharge","useXPMultiplier","weakenedInvasionVexes","enableInvasionAmbience","notifyPlayersAboutInvasions" -> true;
            default -> false;
        };
    }
    private static int defaultInt(String name) {
        return switch(name) {
            case "invasionStartDelay" -> 1;
            case "primaryInvasionMobCap" -> 100;
            case "secondaryInvasionMobCap" -> 25;
            case "mobKillLimit" -> 0;
            case "mobSpawnChunkRadius" -> 8;
            case "noSpawnMobsBlockRadius" -> 16;
            default -> 0;
        };
    }
}
