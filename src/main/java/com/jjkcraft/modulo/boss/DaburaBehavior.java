package com.jjkcraft.modulo.boss;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.block.TemporaryBlocks;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.ModuloBlocks;
import com.jjkcraft.modulo.entity.ModuloBossEntity;

/**
 * Dabura Karaba (mission 10, chapters 15 to 23), for one to four fighters.
 * <ul>
 * <li>Light: a beam at a fighter, after a {@code beam_windup} tell of a thin line of light.</li>
 * <li>Below {@code pillar_below}: solid light. Pillars rise around the arena (cover), and fighters he
 * aims at are caged in breakable light for {@code pin_ticks}.</li>
 * <li>Below {@code domain_below}, once: his Domain Expansion, Otherworld Between Darkness and Light:
 * Reverse Transcendence (a Simple Domain weakens it, as any domain).</li>
 * <li>Below {@code darkness_below}: darkness, Light's Cursed Technique Reversal. A black mass grows
 * between his hands for {@code darkness_charge} ticks, then bursts: everyone within
 * {@code darkness_radius} who can see him takes {@code darkness_damage} (half inside a Simple
 * Domain); a pillar or a wall between saves you.</li>
 * <li>At {@code yield_below}, Maru's Harmony takes him away (as in chapter 23).</li>
 * </ul>
 * Params: {@code beam_interval} (70), {@code beam_windup} (20), {@code beam_damage} (9), {@code beam_range} (32),
 * {@code pillar_below} (0.75), {@code pillar_count} (5), {@code pin_ticks} (70), {@code domain_below} (0.5),
 * {@code darkness_below} (0.3), {@code darkness_charge} (100), {@code darkness_damage} (22), {@code darkness_radius} (18),
 * {@code darkness_interval} (300), {@code yield_below} (0.05), {@code arena_radius} (28).
 */
final class DaburaBehavior implements BossBehavior {
	static final Identifier DOMAIN = Identifier.fromNamespaceAndPath(Modulo.NAMESPACE, "reverse_transcendence");

	private int beamCooldown = 60;
	private int beamWindup = -1;
	private @Nullable Vec3 beamTo;
	private boolean pillars;
	private boolean domainUsed;
	private int darknessCooldown = 40;
	private int darknessCharge = -1;

	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
		float share = boss.getHealth() / boss.getMaxHealth();
		if (share <= boss.param("yield_below", 0.05)) {
			DomainService.close(boss);
			boss.yieldFight(Component.translatable("message.jjkcraft.modulo.dabura.harmony"));
			return;
		}

		if (!pillars && share <= boss.param("pillar_below", 0.75)) {
			pillars = true;
			raisePillars(boss, level);
		}

		if (!domainUsed && share <= boss.param("domain_below", 0.5)) {
			domainUsed = true;
			DomainService.expand(boss, DOMAIN, DomainMath.Variant.FULL, true);
		}

		if (darknessCharge >= 0) {
			darkness(boss, level);
			return;
		}

		if (share <= boss.param("darkness_below", 0.3) && --darknessCooldown <= 0) {
			darknessCharge = 0;
			for (ServerPlayer player : BossBehavior.playersNear(boss, level, 48)) {
				player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.dabura.darkness"), true);
			}

			return;
		}

		beam(boss, level);
	}

	private void beam(ModuloBossEntity boss, ServerLevel level) {
		LivingEntity target = boss.getTarget();
		if (beamWindup > 0 && beamTo != null) {
			boss.getNavigation().stop();
			if (beamWindup % 4 == 0) {
				BossBehavior.line(level, ParticleTypes.END_ROD, boss.getEyePosition(), beamTo, 1.5);
			}

			if (--beamWindup == 0) {
				fire(boss, level, beamTo);
				beamTo = null;
				beamCooldown = (int) boss.param("beam_interval", 70);
			}

			return;
		}

		if (target != null && target.isAlive() && --beamCooldown <= 0 && boss.distanceTo(target) < boss.param("beam_range", 32)) {
			Vec3 from = boss.getEyePosition();
			Vec3 dir = target.getEyePosition().subtract(from).normalize();
			beamTo = from.add(dir.scale(boss.param("beam_range", 32)));
			beamWindup = (int) boss.param("beam_windup", 20);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.4f, 1.8f);
			if (pillars && target instanceof ServerPlayer player) {
				cage(boss, level, player);
			}
		}
	}

	private void fire(ModuloBossEntity boss, ServerLevel level, Vec3 to) {
		Vec3 from = boss.getEyePosition();
		var hit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE, boss));
		Vec3 end = hit.getLocation();
		BossBehavior.line(level, ParticleTypes.ELECTRIC_SPARK, from, end, 1);
		BossBehavior.line(level, ParticleTypes.END_ROD, from, end, 0.4);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.6f, 2f);
		for (LivingEntity victim : BossBehavior.along(level, boss, from, end, 0.6)) {
			victim.hurtServer(level, level.damageSources().source(JjkDamageTypes.LIGHT, boss), (float) boss.param("beam_damage", 9));
		}
	}

	private void raisePillars(ModuloBossEntity boss, ServerLevel level) {
		int count = (int) boss.param("pillar_count", 5);
		double radius = boss.param("arena_radius", 28) * 0.55;
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * i / count + boss.getRandom().nextDouble() * 0.4;
			BlockPos base = BlockPos.containing(boss.getX() + Math.cos(angle) * radius, boss.getY(), boss.getZ() + Math.sin(angle) * radius);
			for (int y = 0; y < 4; y++) {
				for (int dx = 0; dx < 2; dx++) {
					TemporaryBlocks.place(level, base.offset(dx, y, 0), ModuloBlocks.HARD_LIGHT.defaultBlockState(), 900);
				}
			}
		}

		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.6f);
	}

	private void cage(ModuloBossEntity boss, ServerLevel level, ServerPlayer player) {
		int ticks = (int) boss.param("pin_ticks", 70);
		BlockPos feet = player.blockPosition();
		for (BlockPos side : new BlockPos[] {feet.north(), feet.south(), feet.east(), feet.west()}) {
			TemporaryBlocks.place(level, side, ModuloBlocks.HARD_LIGHT.defaultBlockState(), ticks);
			TemporaryBlocks.place(level, side.above(), ModuloBlocks.HARD_LIGHT.defaultBlockState(), ticks);
		}
	}

	private void darkness(ModuloBossEntity boss, ServerLevel level) {
		int total = (int) boss.param("darkness_charge", 100);
		boss.getNavigation().stop();
		darknessCharge++;
		boss.setFlag(ModuloBossEntity.FLAG_CHARGING, true);
		boss.setCharge(darknessCharge * 100 / Math.max(1, total));
		Vec3 hands = boss.getEyePosition().add(boss.getLookAngle().scale(0.8)).subtract(0, 0.5, 0);
		level.sendParticles(ParticleTypes.SQUID_INK, hands.x, hands.y, hands.z, 3, 0.2, 0.2, 0.2, 0.01);
		if (darknessCharge % 20 == 0) {
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.6f);
		}

		if (darknessCharge < total) {
			return;
		}

		double radius = boss.param("darkness_radius", 18);
		float damage = (float) boss.param("darkness_damage", 22);
		level.sendParticles(ParticleTypes.SQUID_INK, hands.x, hands.y, hands.z, 300, radius / 3, 1.5, radius / 3, 0.2);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.4f);
		List<ServerPlayer> near = BossBehavior.playersNear(boss, level, radius);
		for (ServerPlayer player : near) {
			if (!BossBehavior.clear(level, hands, player.getEyePosition(), boss)) {
				continue;
			}

			float dealt = DomainService.hasSimpleDomain(player) ? damage / 2 : damage;
			player.hurtServer(level, level.damageSources().source(JjkDamageTypes.DARKNESS, boss), dealt);
			Vec3 away = player.position().subtract(boss.position()).normalize().scale(1.2);
			player.push(away.x, 0.5, away.z);
			player.needsSync = true;
		}

		darknessCharge = -1;
		darknessCooldown = (int) boss.param("darkness_interval", 300);
		boss.setFlag(ModuloBossEntity.FLAG_CHARGING, false);
		boss.setCharge(0);
	}

	@Override
	public void onRemoved(ModuloBossEntity boss, ServerLevel level) {
		DomainService.close(boss);
	}
}
