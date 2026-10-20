package com.jjkcraft.culling.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.energy.CursedEnergyUser;
import com.jjkcraft.grade.GradedEntity;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;
import com.jjkcraft.registry.LegacyIds;

/**
 * A Culling Game entrant: someone else caught in the game. Sorcerer entrants carry cursed energy
 * and a technique or two and are worth sorcerer points; the rest fight with whatever they brought.
 * They hunt participants of their own colony (players first, now and then each other) and are held
 * inside it like anyone else.
 */
public class EntrantEntity extends PathfinderMob implements CursedEnergyUser, GradedEntity {
	public static final int SKINS = 8;
	private static final EntityDataAccessor<Boolean> SORCERER = SynchedEntityData.defineId(EntrantEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(EntrantEntity.class, EntityDataSerializers.INT);

	/** Techniques sorcerer entrants draw from. */
	private static final List<String> TECHNIQUE_POOL = List.of("grudge_bolt", "cleave", "dismantle", "grave_chains", "ink_iron_palm",
			"seeking_grudge_bolt");
	private static final String[] GIVEN = {"Aoi", "Haru", "Kaede", "Ren", "Sora", "Mio", "Itsuki", "Nanami", "Rintaro", "Chiyo", "Sota", "Yae",
			"Kenji", "Hotaru", "Daichi", "Asuka"};
	private static final String[] FAMILY = {"Kurosawa", "Imai", "Hayashida", "Morioka", "Tsukishima", "Nagase", "Okabe", "Shirane", "Kagami",
			"Toyoda", "Minase", "Ueno"};

	private final List<Identifier> techniques = new ArrayList<>();
	private Identifier grade = JjkCraft.id("grade_4");
	private int colony = -1;
	private int castCooldown = 40;

	public EntrantEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 24)
				.add(Attributes.ATTACK_DAMAGE, 4)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 24)
				.add(Attributes.ARMOR, 2);
	}

	/** Sets the entrant up once, on spawn: kind, look, name, grade, techniques and weapon. */
	public void setup(boolean sorcerer, int colony, RandomSource random) {
		this.colony = colony;
		entityData.set(SORCERER, sorcerer);
		entityData.set(SKIN, random.nextInt(SKINS));
		setCustomName(Component.literal(GIVEN[random.nextInt(GIVEN.length)] + " " + FAMILY[random.nextInt(FAMILY.length)]));
		techniques.clear();
		if (sorcerer) {
			grade = JjkCraft.id(random.nextInt(3) == 0 ? "grade_2" : "grade_3");
			var registry=level().registryAccess().lookupOrThrow(com.jjkcraft.awakening.CharacterAwakening.FAMILIES);
			var families=registry.keySet().stream().sorted().filter(id->!id.getPath().equals("brain_transplant") && !id.getPath().equals("curse_manipulation")).toList();
			int pick=com.jjkcraft.core.progress.WeightedRoll.index(families.stream().map(id->Math.max(1,registry.getValue(id).inheritedWeight()+registry.getValue(id).randomWeight())).toList(),random.nextDouble());
			if(pick>=0) {
				int rank=com.jjkcraft.grade.Grades.rank(level().registryAccess(),grade);
				registry.getValue(families.get(pick)).techniques().stream().filter(id->Techniques.get(level().registryAccess(),id)
						.map(d->com.jjkcraft.grade.Grades.rank(level().registryAccess(),d.grade())<=rank).orElse(false)).limit(3).forEach(techniques::add);
			}

			getAttribute(Attributes.MAX_HEALTH).setBaseValue(32);
		} else {
			grade = JjkCraft.id("grade_4");
			ItemStack weapon = switch (random.nextInt(3)) {
				case 0 -> new ItemStack(Items.IRON_SWORD);
				case 1 -> new ItemStack(Items.STONE_AXE);
				default -> new ItemStack(Items.WOODEN_SWORD);
			};
			setItemSlot(EquipmentSlot.MAINHAND, weapon);
			setDropChance(EquipmentSlot.MAINHAND, 0f);
		}

		setHealth(getMaxHealth());
		CursedEnergy.fill(this);
	}

	public boolean isSorcerer() {
		return entityData.get(SORCERER);
	}

	public int skin() {
		return Mth.clamp(entityData.get(SKIN), 0, SKINS - 1);
	}

	public int colony() {
		return colony;
	}

	public List<Identifier> techniques() {
		return List.copyOf(techniques);
	}

	@Override
	public Identifier grade() {
		return grade;
	}

	@Override
	public boolean isCurse() {
		return false;
	}

	@Override
	public double maxCursedEnergy() {
		if (!isSorcerer()) {
			return 0;
		}

		return grade.getPath().equals("grade_2") ? 220 : 150;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SORCERER, false);
		builder.define(SKIN, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				(target, level) -> CullingGame.isRival(this, target)));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EntrantEntity.class, 200, true, false,
				(target, level) -> CullingGame.isRival(this, target)));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (tickCount % 5 == 0) {
			CullingGame.updatePresence(this);
		}

		LivingEntity target = getTarget();
		if (isSorcerer() && target != null && target.isAlive() && --castCooldown <= 0) {
			tryCast(level, target);
		}
	}

	/** Casts an affordable, ready technique in range, like a curse would. */
	private void tryCast(ServerLevel level, LivingEntity target) {
		double distance = distanceTo(target);
		List<Identifier> options = new ArrayList<>();
		long now = level.getGameTime();
		for (Identifier id : techniques) {
			TechniqueDefinition technique = Techniques.get(level.registryAccess(), id).orElse(null);
			if (technique == null || TechniqueService.readyAt(this, id) > now) {
				continue;
			}

			double range = technique.cast().range() > 0 ? technique.cast().range() : 4;
			if (distance <= range && CursedEnergy.current(this) >= TechniqueService.effectiveCost(this, id, technique) && hasLineOfSight(target)) {
				options.add(id);
			}
		}

		if (options.isEmpty()) {
			castCooldown = 15;
			return;
		}

		Vec3 to = target.getEyePosition().subtract(getEyePosition());
		float yaw = (float) (Mth.atan2(to.z, to.x) * 180 / Math.PI) - 90f;
		float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * 180 / Math.PI);
		setYRot(yaw);
		setXRot(pitch);
		yHeadRot = yaw;
		yBodyRot = yaw;
		CastFailure result = TechniqueService.cast(this, options.get(getRandom().nextInt(options.size())), -1);
		castCooldown = result == CastFailure.NONE ? 60 + getRandom().nextInt(50) : 20;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("jjk_colony", colony);
		output.putString("jjk_grade", grade.toString());
		output.putString("jjk_techniques", String.join(",", techniques.stream().map(Identifier::toString).toList()));
		output.putBoolean("jjk_sorcerer", isSorcerer());
		output.putInt("jjk_skin", entityData.get(SKIN));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		colony = input.getIntOr("jjk_colony", -1);
		Identifier parsed = Identifier.tryParse(input.getStringOr("jjk_grade", "jjkcraft:grade_4"));
		grade = parsed != null ? parsed : JjkCraft.id("grade_4");
		techniques.clear();
		for (String id : input.getStringOr("jjk_techniques", "").split(",")) {
			Identifier technique = id.isBlank() ? null : Identifier.tryParse(id);
			if (technique != null) {
				techniques.add(LegacyIds.technique(technique));
			}
		}

		entityData.set(SORCERER, input.getBooleanOr("jjk_sorcerer", false));
		entityData.set(SKIN, input.getIntOr("jjk_skin", 0));
	}

	/** Sorcerer entrants sometimes carry a scroll of one of their techniques. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (killedByPlayer && isSorcerer() && !techniques.isEmpty() && getRandom().nextFloat() < 0.25f) {
			spawnAtLocation(level, com.jjkcraft.item.TechniqueScrollItem.of(techniques.get(getRandom().nextInt(techniques.size()))));
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}
}
