package com.jjkcraft.combat;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.combat.CombatMath;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.CombatPayloads;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.technique.*;

/** Shared move engine for players and curses. All hits and frame windows run on the server. */
public final class CursedCombat {
    public static final ResourceKey<Registry<CombatMove>> MOVES = ResourceKey.createRegistryKey(JjkCraft.id("combat_move"));
    private static final Map<UUID, Actor> ACTORS = new HashMap<>();
    private static final Map<UUID, CombatMove> INCOMING = new HashMap<>();
    private static final RateLimiter INPUT = new RateLimiter(24, 30);
    public static final class Actor {
        final LivingEntity entity;
        boolean enabled = true;
        long pressed = -1, started, ready, stunned, dodge, dashReady, guardRaised, guardSeen, lastHit,escapeReady,knockedUntil,lastChain=-100,bufferUntil,combatUntil;
        int chain=-1;
        boolean buffered;
        boolean assisted,feint,autoContact;
        int autoAttempts,autoHits,autoFeints,autoDefences,autoGuardChoice=-1;
        long autoReady;
        long lightTap=-100,heavyTap=-100,specialAt,specialUntil;
        String special="";
        int guardHeight = -1, combo;
        double guard = 100;
        String moveId = "";
        CombatMove move;
        Vec3 aim = Vec3.ZERO;
        final Set<UUID> hit = new HashSet<>();
        final ArrayDeque<Vec3> history = new ArrayDeque<>();
        Actor(LivingEntity e) { entity = e; }
    }
    private CursedCombat() { }
    public static void register() { DynamicRegistries.registerSynced(MOVES, CombatMove.CODEC); }
    public static Actor actor(LivingEntity entity) { return ACTORS.computeIfAbsent(entity.getUUID(), id -> new Actor(entity)); }
    public static boolean enabled(Player p) { return JjkConfig.server().systems.cursedCombat && Progression.get(p).awakened() && actor(p).enabled && !p.isSpectator(); }
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(CombatPayloads.Input.TYPE, (input, context) -> input(context.player(), input));
        ServerTickEvents.END_SERVER_TICK.register(CursedCombat::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ACTORS.remove(handler.getPlayer().getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ACTORS.clear(); INCOMING.clear(); });
        SorcererLifecycle.addFullSync(CursedCombat::sync);
        SorcererLifecycle.addDeathCopier((oldP, newP, policy, cfg) -> ACTORS.remove(oldP.getUUID()));
        AttackEntityCallback.EVENT.register((p, level, hand, target, hit) -> {
            if (!level.isClientSide() && (com.jjkcraft.awakening.CharacterAwakening.pending(p) || PersonalPacts.forbids(p,TechniquePact.COMBAT) || enabled(p))) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        TechniqueService.addCastBlocker((caster, id, def) -> interrupted(caster) ? CastFailure.STUNNED : null);
        DamageModifiers.add((level, target, source, amount) -> {
            if(source.getEntity() instanceof Player p&&PersonalPacts.forbids(p,TechniquePact.COMBAT)&&source.typeHolder().unwrapKey().map(k->k.identifier().getNamespace().equals("minecraft")).orElse(false))return 0;
            if(source.is(com.jjkcraft.damage.JjkDamageTypes.SURE_HIT))return amount;
            if (target instanceof ServerPlayer p && com.jjkcraft.awakening.CharacterAwakening.pending(p)) return 0;
            Actor state = ACTORS.get(target.getUUID());
            if (state == null || !(source.getEntity() instanceof LivingEntity attacker)) return amount;
            long now = level.getGameTime();
            state.combatUntil=now+80;actor(attacker).combatUntil=now+80;
            if (state.dodge > now) return 0;
            if (state.guardHeight < 0 || state.stunned > now || state.guardSeen + 10 < now) return amount;
            Vec3 toward = attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
            if (target.getLookAngle().multiply(1, 0, 1).normalize().dot(toward) < 0.25) return amount;
            CombatMove move = INCOMING.get(target.getUUID()); int height = move == null ? 1 : move.height();
            if (height == 0 ? state.guardHeight != 0 : state.guardHeight < 1) return amount;
            if (CombatMath.parry(state.guardRaised, now, height, height)) {
                Actor attacking = actor(attacker); attacking.stunned = now + 16; attacking.move = null;if(attacker instanceof CurseEntity c)c.triggerAnim("action","stagger");
                state.guard = Math.min(100, state.guard + 12); state.combo = 0;
                if (target instanceof ServerPlayer p) Progression.addCounter(p, "combat_parries", 1);
                sparks(level, target, 0xD8B96A, 14);impact(level,attacker,target,2);return 0;
            }
            double loss = move == null ? amount * 3 : move.guardDamage();
            if (target.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD)) loss *= 0.7;
            state.guard = Math.max(0, state.guard - loss);
            CursedEnergy.trySpend(target, Math.min(3, loss * 0.1));
            if (state.guard <= 0) { state.guardHeight = -1; state.stunned = now + 24; sparks(level, target, 0xE0303F, 18);impact(level,attacker,target,3);return amount; }
            attacker.knockback(0.2, target.getX() - attacker.getX(), target.getZ() - attacker.getZ(), source, 0f);
            impact(level,attacker,target,4);return amount * 0.18f;
        });
    }
    public static void input(ServerPlayer p, CombatPayloads.Input input) {
        long now = p.level().getGameTime();
        if (!p.isAlive() || p.isSpectator() || !INPUT.tryAcquire(p.getUUID(), now)) return;
        Actor state = actor(p);
        if (input.action() == 4) { state.enabled = !state.enabled; state.guardHeight = -1; state.pressed = -1; sync(p); return; }
        if (!enabled(p)) return;
        if(input.action()==6){autoAttack(p,input.data());return;}
        if(input.action()==7){
            int height=Math.clamp(input.data(),-1,1);
            if(height<0){state.autoGuardChoice=-1;state.guardHeight=-1;return;}
            if(state.autoGuardChoice<0)state.autoGuardChoice=com.jjkcraft.core.combat.AutoCombatMath.commit(state.autoDefences++)?1:0;
            if(state.autoGuardChoice==1&&!PersonalPacts.forbids(p,TechniquePact.GUARD)&&state.stunned<=now&&state.move==null&&state.guard>0){state.guardHeight=height;state.guardSeen=now;state.guardRaised=now-8;}else state.guardHeight=-1;
            return;
        }
        boolean escaped=false;
        if((input.action()==0||input.action()==5)&&PersonalPacts.forbids(p,TechniquePact.COMBAT))return;
        if(input.action()==0&&state.stunned<=now&&state.knockedUntil<=now){
            if(now-state.lightTap<=6){state.special="uppercut";state.specialAt=now;state.specialUntil=Math.max(now,state.ready)+8;state.buffered=false;state.lightTap=-100;}
            else{state.buffered=true;state.bufferUntil=Math.max(now,state.ready)+5;state.lightTap=now;basic(p,state,now);}
        }
        if(input.action()==5&&state.stunned<=now&&state.knockedUntil<=now){
            boolean twice=now-state.heavyTap<=6;state.special=twice?"sweep":"heavy";state.specialAt=twice?now:now+6;state.specialUntil=Math.max(now,state.ready)+14;state.heavyTap=twice?-100:now;state.buffered=false;
        }
        if(input.action()==1)state.pressed=-1;
        if (input.action() == 2) {
            state.autoGuardChoice=-1;
            int height = PersonalPacts.forbids(p,TechniquePact.GUARD)?-1:Math.clamp(input.data(), -1, 2);
            if (height >= 0 && state.stunned <= now && state.move == null && state.guard > 0) {
                if (state.guardHeight < 0) state.guardRaised = now;
                state.guardHeight = height; state.guardSeen = now;
            } else state.guardHeight = -1;
        }
        if(input.action()==3&&PersonalPacts.forbids(p,TechniquePact.DASH))return;
        if(input.action()==3&&(state.stunned>now||state.knockedUntil>now)&&state.escapeReady<=now){
            if(!Progression.get(p).heavenlyRestricted()&&!CursedEnergy.trySpend(p,33))return;
            state.stunned=0;state.knockedUntil=0;state.move=null;state.ready=now;state.dodge=now+6;state.escapeReady=now+200;state.dashReady=now;
            p.removeEffect(com.jjkcraft.effect.JjkEffects.STUNNED);state.buffered=false;
            escaped=true;
        }
        if (input.action() == 3 && state.dashReady <= now && state.stunned <= now && state.move == null) {
            boolean heavenly = Progression.get(p).heavenlyRestricted();
            if (!heavenly && !escaped && !CursedEnergy.trySpend(p, state.guardHeight >= 0 ? 4 : 8)) return;
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 direction = switch (input.data()) { case -1 -> look.scale(-1); case 1 -> new Vec3(-look.z, 0, look.x); case 2 -> new Vec3(look.z, 0, -look.x); default -> look; };
            p.setDeltaMovement(direction.scale(state.guardHeight >= 0 ? 0.65 : 1.1).add(0, 0.08, 0)); p.needsSync = true;
            state.dodge = Math.max(state.dodge,now + 4); state.dashReady = now + 20;
            sync(p);
            sparks(p.level(), p, 0x8F5CFF, 8);
        }
    }
    private static void basic(ServerPlayer p,Actor state,long now){
        if(state.move!=null||state.ready>now||state.guardHeight>=0||!state.special.isEmpty()||PersonalPacts.forbids(p,TechniquePact.COMBAT))return;
        int step=CombatMath.chainStep(state.chain,state.lastChain,now);
        if(start(p,JjkCraft.id(CombatMath.chainMove(step,!p.onGround(),p.isShiftKeyDown())),p.getLookAngle())){
            state.chain=step;state.lastChain=now;state.buffered=false;
        }
    }
    public static boolean autoAttack(ServerPlayer p,int choice){
        var s=actor(p);long now=p.level().getGameTime();
        if(!enabled(p)||now<s.autoReady||s.move!=null||PersonalPacts.forbids(p,TechniquePact.COMBAT))return false;
        boolean committed=com.jjkcraft.core.combat.AutoCombatMath.commit(s.autoAttempts);
        String key=choice==5?"heavy":CombatMath.chainMove(CombatMath.chainStep(s.chain,s.lastChain,now),!p.onGround(),p.isShiftKeyDown());
        if(!start(p,JjkCraft.id(key),p.getLookAngle(),true,!committed))return false;
        s.autoAttempts++;if(s.feint)s.autoFeints++;
        s.autoReady=s.ready+com.jjkcraft.core.combat.AutoCombatMath.recovery(s.autoAttempts);
        s.chain=CombatMath.chainStep(s.chain,s.lastChain,now);s.lastChain=now;s.buffered=false;s.special="";sync(p);return true;
    }
    public static int[] autoStats(LivingEntity e){var s=actor(e);return new int[]{s.autoAttempts,s.autoHits,s.autoFeints};}
    public static long escapeReady(LivingEntity e){return actor(e).escapeReady;}
    public static boolean knockedDown(LivingEntity e){return actor(e).knockedUntil>e.level().getGameTime();}
    public static boolean start(LivingEntity entity, Identifier id, Vec3 aim) {
        return start(entity,id,aim,false,false);
    }
    private static boolean start(LivingEntity entity, Identifier id, Vec3 aim,boolean assisted,boolean feint) {
        Actor state = actor(entity); long now = entity.level().getGameTime();
        if (state.move != null || state.ready > now || state.stunned > now || state.knockedUntil>now || !entity.isAlive()
                || entity.hasEffect(com.jjkcraft.effect.JjkEffects.STUNNED) || entity.hasEffect(com.jjkcraft.effect.JjkEffects.FROZEN_FRAME)) return false;
        if(PersonalPacts.forbids(entity,TechniquePact.COMBAT))return false;
        CombatMove move = entity.registryAccess().lookupOrThrow(MOVES).getValue(id); if (move == null) return false;
        boolean heavenly = entity instanceof Player p && Progression.get(p).heavenlyRestricted();
        if (!heavenly && (entity instanceof Player||CursedEnergy.max(entity)>0) && !CursedEnergy.trySpend(entity, move.ce())) return false;
        state.move = move; state.moveId = id.getPath(); state.started = now; state.ready = now + move.duration();
        state.assisted=assisted;state.feint=feint;state.autoContact=false;state.combatUntil=now+80;if(feint)state.moveId="feint";
        state.aim = aim.normalize(); state.hit.clear(); state.guardHeight = -1;
        if (entity instanceof CurseEntity curse) curse.triggerAnim("action",id.getPath().equals("jab")?"jab":id.getPath().equals("sweep")?"sweep":id.getPath().equals("uppercut")?"uppercut":id.getPath().equals("heavy")?"heavy":"attack_windup");
        if (entity instanceof ServerPlayer p) {
            sync(p);
        }
        return true;
    }
    public static boolean startMob(net.minecraft.world.entity.Mob mob,LivingEntity target){
        var a=actor(mob);long now=mob.level().getGameTime();int step=CombatMath.chainStep(a.chain,a.lastChain,now);
        String key=CombatMath.chainMove(step,false,false);boolean result=start(mob,JjkCraft.id(key),target.getEyePosition().subtract(mob.getEyePosition()));
        if(result){a.chain=step;a.lastChain=now;broadcastPose(mob);}return result;
    }
    private static void broadcastPose(LivingEntity e){
        var s=actor(e);long now=e.level().getGameTime();boolean down=s.knockedUntil>now;
        var pose=new com.jjkcraft.network.payload.CombatPosePayload(e.getId(),down?"knockdown":s.move==null?"":s.moveId,down?s.knockedUntil-24:s.started,
            down?1:s.move==null?0:s.move.startup(),down?24:s.move==null?0:s.move.active(),down?4:s.move==null?0:s.move.recovery(),s.guardHeight,(int)Math.max(0,s.stunned-now));
        for(var p:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(e))JjkNetworking.send(p,pose);
    }
    public static boolean startCurse(CurseEntity curse, LivingEntity target) {
        List<Identifier> moves = curse.definition().map(d -> d.combatMoves()).orElse(List.of());
        if (moves.isEmpty()) return false;
        var a=actor(curse);long now=curse.level().getGameTime();
        int step=CombatMath.chainStep(a.chain,a.lastChain,now);
        Identifier move=step==3?JjkCraft.id(curse.getRandom().nextBoolean()?"uppercut":"heavy"):moves.get(Math.floorMod(step,moves.size()));
        boolean started=start(curse,move,target.getEyePosition().subtract(curse.getEyePosition()));
        if(started){a.chain=step;a.lastChain=now;}return started;
    }
    public static boolean busy(LivingEntity e){var a=actor(e);long now=e.level().getGameTime();return a.move!=null||a.stunned>now||a.knockedUntil>now;}
    public static String currentMove(LivingEntity e){var a=actor(e);return a.move==null?"":a.moveId;}
    public static boolean attacking(LivingEntity e){return actor(e).move!=null;}
    public static boolean interrupted(LivingEntity e){var a=actor(e);long now=e.level().getGameTime();return a.stunned>now||a.knockedUntil>now;}
    public static boolean guardMob(CurseEntity e,int height){
        var a=actor(e);long now=e.level().getGameTime();if(height<0){a.guardHeight=-1;return false;}
        if(busy(e)||a.guard<15)return false;
        if(a.guardHeight<0){a.guardRaised=now;e.triggerAnim("action","guard");}
        a.guardHeight=height;a.guardSeen=now;return true;
    }
    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) actor(p);
		for (Actor state : List.copyOf(ACTORS.values())) {
			LivingEntity e = state.entity;
			if (!e.isAlive() || e.isRemoved()) { ACTORS.remove(e.getUUID()); continue; }
            long now = e.level().getGameTime(); state.history.addLast(e.position()); while (state.history.size() > 4) state.history.removeFirst();
            if (state.guardSeen + 10 < now) state.guardHeight = -1;
            if(e instanceof net.minecraft.world.entity.Mob mob && !(e instanceof CurseEntity)&&mob.getTarget()!=null&&server.getTickCount()%12==0&&state.move==null&&state.stunned<=now&&mob.distanceToSqr(mob.getTarget())<16&&attacking(mob.getTarget())&&mob.getRandom().nextFloat()<0.2f){
                state.guardHeight=1;state.guardSeen=now;state.guardRaised=now;}

            if (state.guardHeight < 0 && state.stunned <= now) state.guard = Math.min(100, state.guard + 0.65);
            if (state.lastHit + 50 < now) state.combo = 0;
            if(state.knockedUntil>now){e.setDeltaMovement(e.getDeltaMovement().multiply(0.6,1,0.6));e.setSprinting(false);}
            if (state.stunned > now || state.guardHeight >= 0) e.setDeltaMovement(e.getDeltaMovement().multiply(0.45, 1, 0.45));
            if (state.move != null) {
                int age = (int) (now - state.started);
                if (age >= state.move.duration() || state.stunned > now) state.move = null;
                else if (CombatMath.active(age, state.move.startup(), state.move.active()) && e.level() instanceof ServerLevel level) strike(level, state);
            }
            if(e instanceof ServerPlayer p&&!state.special.isEmpty()){
                if(now>state.specialUntil||PersonalPacts.forbids(p,TechniquePact.COMBAT))state.special="";
                else if(now>=state.specialAt&&state.move==null&&state.ready<=now&&state.guardHeight<0&&start(p,JjkCraft.id(state.special),p.getLookAngle()))state.special="";
            }
            if(e instanceof ServerPlayer p&&state.buffered){if(now>state.bufferUntil)state.buffered=false;else basic(p,state,now);}
            if(server.getTickCount()%4==0){if(e instanceof ServerPlayer p)sync(p);else broadcastPose(e);}
        }
    }
    private static void strike(ServerLevel level, Actor state) {
        if(state.feint)return;
        LivingEntity source = state.entity; CombatMove move = state.move;
        Vec3 from = source.getEyePosition(), end = from.add(state.aim.scale(move.reach()));
        var block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        final Vec3 limit = end;
        var candidates=level.getEntitiesOfClass(LivingEntity.class,source.getBoundingBox().inflate(move.reach()+0.5),t->t!=source&&t.isAlive()&&!t.isSpectator());
        candidates.sort(java.util.Comparator.comparingDouble(source::distanceToSqr));
        for (LivingEntity target : candidates) {
            if(source.isAlliedTo(target)||target instanceof net.minecraft.world.entity.OwnableEntity own&&own.getOwner()==source||target instanceof CurseEntity c&&c.getAttachedOrCreate(com.jjkcraft.awakening.ControlledCurses.OWNER).equals(source.getUUID().toString()))continue;
            if(knockedDown(target))continue;
            if (state.hit.contains(target.getUUID()) || !source.hasLineOfSight(target) || target instanceof Player p && (p.isCreative() || (source instanceof Player attacker && !attacker.canHarmPlayer(p)))) continue;
            var box = target.getBoundingBox().inflate(0.25);
            // History is bounded to three ticks (150 ms); never extend the move's reach or see through a wall.
            Actor other = ACTORS.get(target.getUUID());
            Vec3 toward=target.getBoundingBox().getCenter().subtract(source.getBoundingBox().getCenter());
            boolean intersects = box.clip(from, limit).isPresent() || box.contains(from) || (toward.length()<move.reach()&&toward.normalize().dot(state.aim)>0.72&&Math.abs(toward.y)<2.5);
            if (!intersects && other != null && source.distanceToSqr(target) <= move.reach() * move.reach())
                for (Vec3 prior : other.history) if (box.move(prior.subtract(target.position())).clip(from, limit).isPresent()) { intersects = true; break; }
            if (!intersects) continue;
            state.hit.add(target.getUUID());
            double attribute = source.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double damage = move.damage() * Math.max(1, attribute * (source instanceof Player ? 0.7 : 0.4));
            var damageSource = source instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(source);
            damage = net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(level, source.getMainHandItem(), target, damageSource, (float) damage);
            if (source instanceof Player p) damage *= Progression.get(p).heavenlyRestricted() ? 1.35 : 1 + Progression.get(p).growth().output() * 0.025;
            if(source instanceof CurseEntity c)damage*=c.mechanic().attackMultiplier(c,target);
            damage *= CombatMath.combo(state.combo+1);
            double fraction = (from.y + state.aim.y * from.distanceTo(target.getEyePosition()) - target.getY()) / target.getBbHeight();
            if (source instanceof Player p && TechniqueService.knows(p, JjkCraft.id("ratio")) && CombatMath.ratio(fraction)) damage *= 1.5;
            boolean actualContact=false;
            INCOMING.put(target.getUUID(), move);
            try {
                var definition = Techniques.get(level.registryAccess(), JjkCraft.id("ink_iron_palm"));
                boolean flash = false;
                if (definition.isPresent() && !(source instanceof Player p && Progression.get(p).heavenlyRestricted())) {
                    var ctx = new CastContext(source, level, JjkCraft.id("ink_iron_palm"), definition.get(), 1, 1);
                    var resolved=StrikeHooks.resolve(ctx, target, damage, true);flash=resolved.handled();
                    if(flash&&resolved.damage()>0){actualContact=true;
                        state.combo++;state.lastHit=level.getGameTime();var defender=actor(target);
                        if(defender.guardHeight<0){defender.stunned=Math.max(defender.stunned,level.getGameTime()+move.stun());
                            if(state.moveId.equals("heavy")||state.moveId.equals("sweep")||state.moveId.equals("uppercut")){defender.knockedUntil=level.getGameTime()+24;defender.stunned=Math.max(defender.stunned,defender.knockedUntil);defender.move=null;defender.buffered=false;target.needsSync=true;}}
                    }
                }
                if (!flash) {
                    target.damageCooldownTime = 0;
                    boolean landed = target.hurtServer(level, source instanceof Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(source), (float) damage);
                    if (landed) {actualContact=true;
                        state.combo++;state.lastHit=level.getGameTime();
                        Actor defender = actor(target);
                        if (defender.guardHeight < 0) defender.stunned = Math.max(defender.stunned, level.getGameTime() + move.stun());
                        target.knockback(move.knockback(), source.getX() - target.getX(), source.getZ() - target.getZ(), level.damageSources().mobAttack(source), 0f);
                        if (move.lift() > 0) target.setDeltaMovement(target.getDeltaMovement().add(0, move.lift(), 0));
                        if(defender.guardHeight<0&&(state.moveId.equals("heavy")||state.moveId.equals("sweep")||state.moveId.equals("uppercut"))){
                            defender.knockedUntil=level.getGameTime()+24;defender.stunned=Math.max(defender.stunned,defender.knockedUntil);defender.move=null;defender.buffered=false;
                            target.needsSync=true;
                        }
                        impact(level,source,target,knockedDown(target)?1:0);
                    }
                }
                if(actualContact){
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(level,target,damageSource,source.getMainHandItem());
                    if(source.getMainHandItem().hurtEnemy(target,source))source.getMainHandItem().postHurtEnemy(target,source);
                }
            } finally { INCOMING.remove(target.getUUID()); }
            if(actualContact&&state.assisted&&!state.autoContact){state.autoHits++;state.autoContact=true;}
            if(actualContact&&source instanceof ServerPlayer p)Progression.addCounter(p,"combat_hits",1);
            if(actualContact&&source instanceof CurseEntity c)c.combatLanded(level,target);
            if (source instanceof CurseEntity curse && !java.util.Set.of("jab","heavy","uppercut","sweep").contains(state.moveId)) curse.triggerAnim("action", "attack");
            sparks(level, target, state.moveId.equals("heavy") ? 0xE0303F : 0xB1A0E8, 8);
            break;
        }
    }
    private static void sparks(ServerLevel level, LivingEntity target, int color, int count) {
        level.sendParticles(JjkParticles.of(JjkParticles.CURSED_SPARK, color, 0.8f), target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), count, 0.2, 0.3, 0.2, 0.08);
    }
    private static void impact(ServerLevel level,LivingEntity attacker,LivingEntity target,int kind){
        var at=target.getBoundingBox().getCenter();var payload=new CombatPayloads.Impact(attacker.getId(),target.getId(),kind,at.x,at.y,at.z);
        var recipients=new java.util.HashSet<ServerPlayer>(net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(target));
        if(attacker instanceof ServerPlayer p)recipients.add(p);if(target instanceof ServerPlayer p)recipients.add(p);
        for(var p:recipients)JjkNetworking.send(p,payload);
        net.minecraft.sounds.SoundEvent sound=kind==2?net.minecraft.sounds.SoundEvents.ANVIL_LAND:kind==4?net.minecraft.sounds.SoundEvents.SHIELD_BLOCK.value():kind==1||kind==3?net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG:net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_KNOCKBACK;
        level.playSound(null,at.x,at.y,at.z,sound,net.minecraft.sounds.SoundSource.PLAYERS,kind==2?0.2f:0.55f,kind==1?0.7f:1.15f);
    }
    public static void sync(ServerPlayer p) {
        Actor s = actor(p); long now = p.level().getGameTime();
        JjkNetworking.send(p,new CombatPayloads.AutoStats(s.autoAttempts,s.autoHits,s.autoFeints));
        JjkNetworking.send(p, new CombatPayloads.View(enabled(p), (float) s.guard, s.combo, (int) Math.max(0, s.stunned - now), s.move == null ? "" : s.moveId, s.move == null ? 0 : (int) (now - s.started)));
        boolean down=s.knockedUntil>now;
        var pose=new com.jjkcraft.network.payload.CombatPosePayload(p.getId(),down?"knockdown":s.move==null?(enabled(p)&&s.combatUntil>now?"stance":""):s.moveId,down?s.knockedUntil-24:s.started,
            down?1:s.move==null?0:s.move.startup(),down?24:s.move==null?0:s.move.active(),down?4:s.move==null?0:s.move.recovery(),
            enabled(p)?s.guardHeight:-1,(int)Math.max(0,s.stunned-now));
        JjkNetworking.send(p,pose);
        for(var observer:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(p))JjkNetworking.send(observer,pose);
        JjkNetworking.send(p,new CombatPayloads.Resources((int)Math.max(0,s.escapeReady-now),(int)Math.max(0,s.knockedUntil-now),Math.max(0,s.chain+1)));
    }
}
