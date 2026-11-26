package qc;
import dev.theagameplayer.puresuffering.config.PSConfigValues;
import dev.theagameplayer.puresuffering.registries.other.PSGameRules;
import dev.theagameplayer.puresuffering.registries.other.PSReloadListeners;
import dev.theagameplayer.puresuffering.world.level.saveddata.InvasionLevelData;
import dev.theagameplayer.puresuffering.world.entity.PSInvasionMob;
import dev.theagameplayer.puresuffering.invasion.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;
@Mod("psqc")
public class PSQC {
 public PSQC(IEventBus bus){bus.addListener(this::register);}
 private void register(RegisterEvent e){e.register(Registries.TEST_FUNCTION,r->r.register(Identifier.parse("psqc:invasions"),PSQC::test));}
 private static void test(GameTestHelper h){
   var level=h.getLevel();
   h.assertTrue(PSGameRules.GAME_RULES.size()==23,"rule count");
   h.assertTrue(PSConfigValues.common!=null,"common config missing");
   h.assertTrue(PSConfigValues.LEVELS.containsKey(level),"level config missing");
   var types=PSReloadListeners.getInvasionTypeManager();
   h.assertTrue(types!=null,"reload manager missing");
   long count=types.getAllInvasionTypes().count();
   h.assertTrue(count>=18,"invasion types "+count);
   var zombie=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
   h.assertTrue(zombie instanceof PSInvasionMob,"mob mixin missing");
   ((PSInvasionMob)zombie).psSetHyperCharge(3);
   h.assertTrue(((PSInvasionMob)zombie).psGetHyperCharge()==3,"hypercharge state");
   var data=InvasionLevelData.get(level);
   data.setInvasionTime(12000);data.setXPMultiplier(9);
   var codec=InvasionLevelData.type(level).codec();
   var restored=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
   h.assertTrue(restored.getInvasionTime()==12000&&restored.getXPMultiplier()==9,"saved data codec");
   data.setInvasionTime(level.getOverworldClockTime());data.setXPMultiplier(0);
   System.out.println("PS_QC PASS: "+count+" invasion types, 23 rules, level config, hypercharge and saved data codec");
   var manager=data.getInvasionManager();
   var type=types.getInvasionType(Identifier.parse("puresuffering:zombie"));
   h.assertTrue(type!=null,"zombie type");
   var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ps-qc"),false);
   var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation()) {
     @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
   };
   var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
   new io.netty.channel.embedded.EmbeddedChannel(connection);
   net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
   level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
   player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
   var pos=h.absolutePos(new net.minecraft.core.BlockPos(0,5,0));
   int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ());
   player.snapTo(pos.getX()+0.5,y,pos.getZ()+0.5,0,0);
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamerule puresuffering:mob_spawn_chunk_radius 1");
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamerule puresuffering:no_spawn_mobs_block_radius 2");
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamerule puresuffering:primary_invasion_mob_cap 5");
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamerule puresuffering:invasion_anti_grief true");
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"gamerule puresuffering:mob_kill_limit 1");
   h.assertTrue(PSGameRules.PRIMARY_INVASION_MOB_CAP.get(level)==5&&PSGameRules.MOB_KILL_LIMIT.get(level)==1,"gamerule command migration");
   h.setTime(18000);
   level.updateSkyBrightness();
   var chunk=level.getChunk(player.blockPosition()).getPos();
   for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk(chunk.x()+dx,chunk.z()+dz);
   for(var difficulty:InvasionDifficulty.values()){
     var sessionType=InvasionSessionType.getActive(level);
     manager.setQueued(sessionType,difficulty).add(new Invasion.BuildInfo(type,0,true));
     manager.setInvasions(level);
     var session=manager.getActiveSession(level);
     h.assertTrue(session!=null&&session.getDifficulty()==difficulty,"session difficulty "+difficulty);
     var inv=session.getPrimary();
     h.assertTrue(inv.getMobCap()==3,"cap");
     inv.mobsKilledByPlayer=2;session.mobsKilledByPlayer=0;
     var encoded=codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow();
     var decoded=codec.parse(NbtOps.INSTANCE,encoded).getOrThrow();
     h.assertTrue(decoded.getInvasionManager().getActiveSession(level).getPrimary().mobsKilledByPlayer==2,"active save counter");
     inv.mobsKilledByPlayer=0;
     var packet=new dev.theagameplayer.puresuffering.network.AddInvasionPacket(sessionType,difficulty,inv);
     var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
     try{
       dev.theagameplayer.puresuffering.network.AddInvasionPacket.STREAM_CODEC.encode(buf,packet);
       byte[] original=new byte[buf.readableBytes()];buf.getBytes(buf.readerIndex(),original);
       var received=dev.theagameplayer.puresuffering.network.AddInvasionPacket.STREAM_CODEC.decode(buf);
       h.assertTrue(!buf.isReadable(),"packet trailing bytes");
       var second=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
       try{dev.theagameplayer.puresuffering.network.AddInvasionPacket.STREAM_CODEC.encode(second,received);byte[] actual=new byte[second.readableBytes()];second.getBytes(second.readerIndex(),actual);h.assertTrue(java.util.Arrays.equals(original,actual),"packet changed bytes");}finally{second.release();}
     }finally{buf.release();}
     net.minecraft.world.entity.Mob spawned=null;
     for(int t=0;t<100&&spawned==null;t++){
       manager.tick(level);
       for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.Mob mob&&inv.hasMob(mob)>=0){spawned=mob;break;}
     }
     h.assertTrue(spawned!=null,"actual spawn "+difficulty+" player="+player.blockPosition()+" sky="+level.getSkyDarken()+" time="+level.getOverworldClockTime());
     int charge=((PSInvasionMob)spawned).psGetHyperCharge();
     h.assertTrue(!difficulty.isHyper()||charge>=1,"hypercharge not applied");
     h.assertTrue(!difficulty.isNightmare()||charge==4,"nightmare charge");
     var grief=new net.neoforged.neoforge.event.entity.EntityMobGriefingEvent(level,spawned);
     dev.theagameplayer.puresuffering.event.PSEntityEvents.mobGriefing(grief);
     h.assertTrue(!grief.canGrief(),"anti-grief");
     dev.theagameplayer.puresuffering.event.PSTickEvents.entityTickPost(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(spawned));
     h.assertTrue(spawned.getTarget()==player,"aggression");
     var xp=new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(spawned,player,9);
     dev.theagameplayer.puresuffering.event.PSLivingEvents.experienceDrop(xp);
     h.assertTrue(data.getXPMultiplier()>0,"xp counter");
     dev.theagameplayer.puresuffering.event.PSLivingEvents.death(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(spawned,level.damageSources().playerAttack(player)));
     manager.tick(level);
     h.assertTrue(manager.getActiveSession(level)==null,"kill limit termination");
     for(var e:java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).toList()) if(e instanceof net.minecraft.world.entity.Mob mob&&mob.getPersistentData().contains(Invasion.INVASION_MOB))mob.discard();
     System.out.println("PS_CYCLE PASS "+difficulty+" spawn charge="+charge+" save, packet, aggression, anti-grief, XP, kill limit");
   }
   // Exercise the actual on-disk SavedDataStorage write/close/read API, not just the codec.
   h.setTime(18000);
   var night=InvasionSessionType.getActive(level);
   manager.setQueued(night,InvasionDifficulty.NIGHTMARE).add(new Invasion.BuildInfo(type,0,true));
   manager.setInvasions(level);
   var before=manager.getActiveSession(level);
   before.mobsKilledByPlayer=7;before.getPrimary().mobsKilledByPlayer=3;
   data.setXPMultiplier(11);
   data.setInvasionTime(12000L);
   java.nio.file.Path temp;
   try {temp=java.nio.file.Files.createTempDirectory("psqc-active-save-");}catch(java.io.IOException e){throw new RuntimeException(e);}
   var dataType=InvasionLevelData.type(level);
   try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(level,temp,net.minecraft.util.datafix.DataFixers.getDataFixer(),level.registryAccess())){
     disk.set(dataType,data);
     data.setDirty();
     disk.saveAndJoin();
   }
   // Change the clock before reading: restores the saved NIGHT slot instead of today's DAY slot.
   h.setTime(6000);
   try(var disk=new net.minecraft.world.level.storage.SavedDataStorage(level,temp,net.minecraft.util.datafix.DataFixers.getDataFixer(),level.registryAccess())){
     var loaded=disk.get(dataType);
     h.assertTrue(loaded!=null&&loaded.getInvasionTime()==12000L&&loaded.getXPMultiplier()==11,"disk header");
     var restoredNight=loaded.getInvasionManager().getSession(InvasionSessionType.NIGHT);
     h.assertTrue(restoredNight!=null&&restoredNight.getSessionType()==InvasionSessionType.NIGHT&&restoredNight.getDifficulty()==InvasionDifficulty.NIGHTMARE&&restoredNight.mobsKilledByPlayer==7&&restoredNight.getPrimary().mobsKilledByPlayer==3,"saved inactive session");
     h.assertTrue(restoredNight.getPrimary().getType().getId().equals(type.getId()),"disk type identity");
   }
   System.out.println("PS_DISK PASS: active nightmare saved through native storage, reopened during day, exact counters and session slot retained");
   // Exercise mixin creation for villagers and withers, beyond zombie-only tests.
   var villager=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);
   var wither=EntityTypes.WITHER.create(level,EntitySpawnReason.COMMAND);
   h.assertTrue(villager instanceof PSInvasionMob&&wither instanceof PSInvasionMob,"AI mob mixins");
   System.out.println("PS_AI PASS: villager and wither constructed with mixins applied");
   // Queue a fresh day invasion; let actual server tick events start, spawn, and transition it.
   before.clear(level);
   manager.tick(level);
   manager.setQueued(InvasionSessionType.NIGHT,null);
   manager.setQueued(InvasionSessionType.DAY,InvasionDifficulty.HYPER).add(new Invasion.BuildInfo(type,0,true));
   h.setTime(0);
   data.setInvasionTime(12000);
   h.runAfterDelay(4,()->{
     var live=manager.getActiveSession(level);
     h.assertTrue(live!=null&&live.getDifficulty()==InvasionDifficulty.HYPER,"native tick queued session");
     h.setTime(12000);
     manager.setQueued(InvasionSessionType.NIGHT,InvasionDifficulty.DEFAULT).add(new Invasion.BuildInfo(type,0,true));
     h.runAfterDelay(4,()->{
       h.assertTrue(manager.getActiveSession(level)!=null&&manager.getActiveSession(level).getDifficulty()==InvasionDifficulty.DEFAULT,"native tick transition");
       System.out.println("PS_TICKS PASS: native day-to-night queue and transition");
       manager.getActiveSession(level).clear(level);
       h.succeed();
     });
   });
 }
}