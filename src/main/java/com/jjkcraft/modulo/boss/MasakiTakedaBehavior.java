package com.jjkcraft.modulo.boss;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.modulo.Modulo;
import com.jjkcraft.modulo.entity.ModuloBossEntity;
import com.jjkcraft.modulo.entity.ModuloEntities;

/**
 * Masaki Takeda (mission 4, chapters 4 to 6): an elderly curse user whose technique, Marmalade Boy,
 * reshapes anything using human skin, himself and his guardian spirit included.
 * <ol>
 * <li>Disguised: he passes as a small boy until someone hits him or comes close.</li>
 * <li>Revealed: he throws needles that pin (a clear wind-up tell) while his skin-sewn guardian fights.</li>
 * <li>Below {@code hide_below} health he sews himself into the guardian: he cannot be hurt, and
 * every {@code tear_hits} blows on the guardian tear him out, stunned, for {@code stun_ticks}.</li>
 * </ol>
 * Params: {@code reveal_range} (4), {@code needle_interval} (70), {@code needle_windup} (14),
 * {@code needle_damage} (5), {@code hide_below} (0.5), {@code tear_hits} (3), {@code stun_ticks} (60),
 * {@code hide_ticks} (160).
 */
final class MasakiTakedaBehavior implements BossBehavior {
	static final Identifier GUARDIAN = Identifier.fromNamespaceAndPath(Modulo.NAMESPACE, "skin_guardian");

	private boolean disguised = true;
	private @Nullable ModuloBossEntity guardian;
	private int needleCooldown = 40;
	private int needleWindup = -1;
	private boolean hiding;
	private int hideTimer;
	private int tears;

	@Override
	public void onSpawn(ModuloBossEntity boss, ServerLevel level) {
		boss.setFlag(ModuloBossEntity.FLAG_DISGUISED, true);
		boss.getAttribute(Attributes.SCALE).setBaseValue(0.6);
	}

	@Override
	public float onHurt(ModuloBossEntity boss, ServerLevel level, DamageSource source, float amount) {
		if (disguised) {
			reveal(boss, level);
			return amount * 0.5f;
		}

		return hiding ? 0 : amount;
	}

	@Override
	public void tick(ModuloBossEntity boss, ServerLevel level) {
		if (disguised) {
			boss.getNavigation().stop();
			boss.setTarget(null);
			if (!BossBehavior.playersNear(boss, level, boss.param("reveal_range", 4)).isEmpty()) {
				reveal(boss, level);
			}

			return;
		}

		if (guardian != null && !guardian.isAlive()) {
			guardian = null;
			if (hiding) {
				tearOut(boss, level);
			}
		}

		if (!hiding && boss.getHealth() < boss.getMaxHealth() * boss.param("hide_below", 0.5) && guardian != null && hideTimer <= 0) {
			hide(boss, level);
		}

		if (hiding) {
			if (guardian != null) {
				boss.teleportTo(guardian.getX(), guardian.getY(), guardian.getZ());
			}

			if (--hideTimer <= 0 && guardian == null) {
				tearOut(boss, level);
			}

			return;
		}

		if (hideTimer > 0) {
			hideTimer--;
		}

		needles(boss, level);
	}

	private void reveal(ModuloBossEntity boss, ServerLevel level) {
		disguised = false;
		boss.setFlag(ModuloBossEntity.FLAG_DISGUISED, false);
		boss.getAttribute(Attributes.SCALE).setBaseValue(boss.definition().map(d -> d.scale()).orElse(1.0));
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, boss.getX(), boss.getY() + 1, boss.getZ(), 40, 0.5, 0.8, 0.5, 0.02);
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1f, 0.8f);
		for (ServerPlayer player : BossBehavior.playersNear(boss, level, 32)) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.masaki.reveal"), true);
		}

		guardian = ModuloEntities.spawnBoss(level, GUARDIAN, boss.blockPosition().offset(2, 0, 0), boss.site(), boss.mission(), boss.stage(), boss.fighters());
	}

	private void needles(ModuloBossEntity boss, ServerLevel level) {
		LivingEntity target = boss.getTarget();
		if (target == null || !target.isAlive()) {
			needleWindup = -1;
			return;
		}

		if (needleWindup > 0) {
			level.sendParticles(ParticleTypes.CRIT, boss.getX(), boss.getEyeY(), boss.getZ(), 2, 0.2, 0.2, 0.2, 0);
			if (--needleWindup == 0) {
				Vec3 from = boss.getEyePosition();
				Vec3 to = target.getEyePosition();
				BossBehavior.line(level, ParticleTypes.CRIT, from, to, 0.5);
				if (BossBehavior.clear(level, from, to, boss)) {
					target.hurtServer(level, level.damageSources().mobAttack(boss), (float) boss.param("needle_damage", 5));
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 4), boss);
				}

				needleCooldown = (int) boss.param("needle_interval", 70);
			}

			return;
		}

		if (--needleCooldown <= 0 && boss.hasLineOfSight(target) && boss.distanceTo(target) < 16) {
			needleWindup = (int) boss.param("needle_windup", 14);
			level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.HOSTILE, 1f, 1.6f);
		}
	}

	private void hide(ModuloBossEntity boss, ServerLevel level) {
		hiding = true;
		tears = 0;
		hideTimer = (int) boss.param("hide_ticks", 160);
		boss.setFlag(ModuloBossEntity.FLAG_HIDDEN, true);
		boss.setInvisible(true);
		boss.noPhysics = true;
		level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1f, 0.6f);
		for (ServerPlayer player : BossBehavior.playersNear(boss, level, 32)) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.masaki.hide"), true);
		}
	}

	/** A blow on the guardian while Masaki hides in it: enough of them tear him out. */
	void onGuardianHit(ModuloBossEntity boss, ServerLevel level) {
		if (hiding && ++tears >= (int) boss.param("tear_hits", 3)) {
			tearOut(boss, level);
		}
	}

	private void tearOut(ModuloBossEntity boss, ServerLevel level) {
		hiding = false;
		hideTimer = (int) boss.param("hide_ticks", 160);
		boss.setFlag(ModuloBossEntity.FLAG_HIDDEN, false);
		boss.setInvisible(false);
		boss.noPhysics = false;
		boss.addEffect(new MobEffectInstance(JjkEffects.STUNNED, (int) boss.param("stun_ticks", 60), 0));
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, boss.getX(), boss.getY() + 1, boss.getZ(), 12, 0.4, 0.6, 0.4, 0.1);
		for (ServerPlayer player : BossBehavior.playersNear(boss, level, 32)) {
			player.sendSystemMessage(Component.translatable("message.jjkcraft.modulo.masaki.torn"), true);
		}
	}

	@Override
	public void onRemoved(ModuloBossEntity boss, ServerLevel level) {
		if (guardian != null && guardian.isAlive()) {
			guardian.discard();
		}
	}

	/** Routes blows on any guardian to the Masaki it belongs to (called from the guardian's minion behavior). */
	static void guardianHit(ModuloBossEntity guardian, ServerLevel level) {
		for (ModuloBossEntity boss : level.getEntitiesOfClass(ModuloBossEntity.class, guardian.getBoundingBox().inflate(48))) {
			if (boss != guardian && boss.behavior() instanceof MasakiTakedaBehavior masaki && masaki.guardian == guardian) {
				masaki.onGuardianHit(boss, level);
			}
		}
	}
}
