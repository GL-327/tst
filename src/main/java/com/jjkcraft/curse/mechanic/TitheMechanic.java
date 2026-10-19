package com.jjkcraft.curse.mechanic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.curse.CurseEntity;

/**
 * Tithe Burrower (Grade 2): a thief with a price. It wants one thing (an item from the
 * {@code jjkcraft:tithe_offerings} tag, shown as flecks over its head and in its name). Its bite
 * steals a random stack from your inventory, then it burrows and surfaces somewhere nearby. Drop
 * the item it wants near it and it takes the tithe, gives back everything it stole and leaves.
 * Kill it instead and its stash spills out.
 *
 * MECH_A: 1 while burrowed. MECH_B: the wanted item's numeric id.
 */
public final class TitheMechanic implements CurseMechanic {
	public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, JjkCraft.id("tithe_offerings"));

	@Override
	public void onSpawn(CurseEntity curse, ServerLevel level) {
		List<Item> offerings = new ArrayList<>();
		for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(OFFERINGS)) {
			offerings.add(holder.value());
		}

		Item wanted = offerings.isEmpty() ? Items.GOLD_INGOT : offerings.get(curse.getRandom().nextInt(offerings.size()));
		setWanted(curse, wanted);
		curse.setMechA(0);
	}

	public static void setWanted(CurseEntity curse, Item item) {
		curse.setMechB(BuiltInRegistries.ITEM.getId(item));
		curse.setCustomName(Component.translatable("curse.jjkcraft.tithe_burrower.wants",
				Component.translatable("curse.jjkcraft.tithe_burrower"), new ItemStack(item).getHoverName()));
	}

	public static Item wanted(CurseEntity curse) {
		Item item = BuiltInRegistries.ITEM.byId(Math.max(0, curse.mechB()));
		return item == null || item == Items.AIR ? Items.GOLD_INGOT : item;
	}

	@Override
	public void tick(CurseEntity curse, ServerLevel level) {
		if (curse.mechA() == 1) {
			burrowed(curse, level);
			return;
		}

		Item wanted = wanted(curse);
		if (curse.tickCount % 10 == 0) {
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, wanted), curse.getX(), curse.getY() + curse.getBbHeight() + 0.4,
					curse.getZ(), 1, 0.15, 0.05, 0.15, 0.01);
		}

		if (curse.tickCount % 5 == 0) {
			double radius = curse.parameter("pay_radius", 4);
			for (ItemEntity offering : level.getEntitiesOfClass(ItemEntity.class, curse.getBoundingBox().inflate(radius),
					item -> item.isAlive() && item.getItem().is(wanted))) {
				paid(curse, level, offering);
				return;
			}
		}
	}

	/** The tithe is paid: it takes one, gives everything back where the offering lay, and leaves. */
	private void paid(CurseEntity curse, ServerLevel level, ItemEntity offering) {
		offering.getItem().shrink(1);
		if (offering.getItem().isEmpty()) {
			offering.discard();
		}

		Vec3 at = offering.position();
		for (ItemStack stack : curse.stash()) {
			ItemEntity returned = new ItemEntity(level, at.x, at.y + 0.3, at.z, stack);
			returned.setDefaultPickUpDelay();
			level.addFreshEntity(returned);
		}

		curse.stash().clear();
		level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5f, 1.2f);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, curse.getX(), curse.getY() + 0.8, curse.getZ(), 12, 0.5, 0.4, 0.5, 0.05);
		dirt(curse, level, 30);
		curse.discard();
	}

	@Override
	public void onAttack(CurseEntity curse, ServerLevel level, LivingEntity target) {
		if (!(target instanceof ServerPlayer player) || curse.stash().size() >= curse.parameter("max_stash", 6)) {
			return;
		}

		Inventory inventory = player.getInventory();
		List<Integer> full = new ArrayList<>();
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			if (!inventory.getItem(slot).isEmpty()) {
				full.add(slot);
			}
		}

		if (full.isEmpty()) {
			return;
		}

		int slot = full.get(curse.getRandom().nextInt(full.size()));
		ItemStack stolen = inventory.removeItemNoUpdate(slot);
		inventory.setChanged();
		curse.stash().add(stolen);
		player.sendSystemMessage(Component.translatable("message.jjkcraft.tithe_stolen", stolen.getHoverName(), new ItemStack(wanted(curse)).getHoverName()), true);
		burrow(curse, level);
	}

	/** Goes underground for a while. Public for tests. */
	public static void burrow(CurseEntity curse, ServerLevel level) {
		curse.setMechA(1);
		curse.setInvisible(true);
		curse.getNavigation().stop();
		MechanicUtil.set(curse, "burrow", (int) curse.parameter("burrow_ticks", 60));
		dirt(curse, level, 24);
		level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
	}

	private void burrowed(CurseEntity curse, ServerLevel level) {
		curse.setDeltaMovement(Vec3.ZERO);
		if (MechanicUtil.countDown(curse, "burrow") > 0) {
			if (curse.tickCount % 6 == 0) {
				dirt(curse, level, 3);
			}

			return;
		}

		// Surfaces somewhere nearby on solid ground.
		double min = curse.parameter("resurface_min", 6);
		double max = curse.parameter("resurface_max", 12);
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = curse.getRandom().nextDouble() * Math.PI * 2;
			double distance = min + curse.getRandom().nextDouble() * (max - min);
			BlockPos column = BlockPos.containing(curse.getX() + Math.cos(angle) * distance, curse.getY(), curse.getZ() + Math.sin(angle) * distance);
			if (!level.isLoaded(column)) {
				continue;
			}

			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (Math.abs(ground.getY() - curse.getY()) <= 6) {
				curse.teleportTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
				break;
			}
		}

		curse.setMechA(0);
		curse.setInvisible(false);
		dirt(curse, level, 24);
		level.playSound(null, curse.getX(), curse.getY(), curse.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2f, 1.1f);
	}

	private static void dirt(CurseEntity curse, ServerLevel level, int count) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(curse.blockPosition().below())), curse.getX(), curse.getY() + 0.2,
				curse.getZ(), count, 0.5, 0.2, 0.5, 0.1);
	}

	@Override
	public boolean canAct(CurseEntity curse) {
		return curse.mechA() != 1;
	}

	@Override
	public float onHurt(CurseEntity curse, ServerLevel level, DamageSource source, float amount) {
		return curse.mechA() == 1 ? 0 : amount;
	}

	@Override
	public void onRemoved(CurseEntity curse) {
		curse.setInvisible(false);
	}
}
