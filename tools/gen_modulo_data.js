// Writes the Modulo expansion's data files and its English lang source. Usage: node tools/gen_modulo_data.js <projectRoot>
// Lines are original paraphrases of the story's events (docs/MODULO.md section 1), not quotations.
const fs = require('fs');
const path = require('path');
const root = process.argv[2];
const RES = path.join(root, 'src/main/resources');
const lang = [];
const L = (key, text) => { lang.push([key, text]); return key; };
function write(rel, obj) {
	const file = path.join(RES, rel);
	fs.mkdirSync(path.dirname(file), { recursive: true });
	fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
}

// ------------------------------------------------------------ dimensions
const noBeds = { can_set_spawn: 'never', can_sleep: 'never', error_message: { translate: 'message.jjkcraft.modulo.no_sleep' } };
write('data/modulo/dimension_type/naunax.json', {
	ambient_light: 0.35,
	attributes: {
		'minecraft:gameplay/bed_rule': noBeds,
		'minecraft:gameplay/straw_bed_rule': noBeds,
		'minecraft:gameplay/respawn_anchor_works': false,
		'minecraft:visual/ambient_light_color': '#3a6b52',
		'minecraft:visual/fog_color': '#16241e',
		'minecraft:visual/sky_color': '#0b1411',
		'minecraft:visual/sky_light_factor': 0.0,
	},
	cardinal_light: 'nether',
	coordinate_scale: 1.0,
	has_ceiling: true,
	has_ender_dragon_fight: false,
	has_fixed_time: true,
	has_skylight: false,
	height: 128,
	infiniburn: '#minecraft:infiniburn_overworld',
	logical_height: 128,
	min_y: 0,
	monster_spawn_block_light_limit: 0,
	monster_spawn_light_level: 0,
	skybox: 'none',
	timelines: '#minecraft:in_nether',
});
write('data/modulo/dimension_type/tokyo_2086.json', {
	ambient_light: 0.0,
	attributes: {
		'minecraft:gameplay/bed_rule': noBeds,
		'minecraft:gameplay/straw_bed_rule': noBeds,
		'minecraft:gameplay/respawn_anchor_works': false,
		'minecraft:visual/ambient_light_color': '#0a0a0a',
		'minecraft:visual/cloud_color': '#ccdcd6e0',
		'minecraft:visual/cloud_height': 192.33,
		'minecraft:visual/fog_color': '#b9c4d6',
		'minecraft:visual/sky_color': '#7f9ed8',
	},
	coordinate_scale: 1.0,
	default_clock: 'minecraft:overworld',
	has_ceiling: false,
	has_ender_dragon_fight: false,
	has_skylight: true,
	height: 256,
	infiniburn: '#minecraft:infiniburn_overworld',
	logical_height: 256,
	min_y: 0,
	monster_spawn_block_light_limit: 0,
	monster_spawn_light_level: { type: 'minecraft:uniform', max_inclusive: 7, min_inclusive: 0 },
	timelines: '#minecraft:in_overworld',
});
write('data/modulo/dimension/naunax.json', {
	type: 'modulo:naunax',
	generator: { type: 'minecraft:flat', settings: { biome: 'modulo:naunax_hold', features: true, lakes: false,
		layers: [{ block: 'minecraft:bedrock', height: 1 }, { block: 'jjkcraft:naunax_hull', height: 62 }, { block: 'jjkcraft:naunax_deck_plate', height: 1 }],
		structure_overrides: [] } },
});
write('data/modulo/dimension/tokyo_2086.json', {
	type: 'modulo:tokyo_2086',
	generator: { type: 'minecraft:flat', settings: { biome: 'modulo:tokyo_2086', features: true, lakes: false,
		layers: [{ block: 'minecraft:bedrock', height: 1 }, { block: 'minecraft:stone', height: 59 }, { block: 'minecraft:dirt', height: 3 }, { block: 'minecraft:smooth_stone', height: 1 }],
		structure_overrides: [] } },
});
const noSpawns = { argument: { spawn_costs: {}, spawns_by_category: { ambient: [], axolotls: [], creature: [], misc: [], monster: [], underground_water_creature: [], water_ambient: [], water_creature: [] } }, modifier: 'overlay' };
const steps = (place) => { const f = [[], [], [], [], [], [], [], [], [], [], []]; for (const [i, id] of place) f[i].push(id); return f; };
write('data/modulo/worldgen/biome/naunax_hold.json', {
	attributes: { 'minecraft:gameplay/natural_mob_spawns': noSpawns, 'minecraft:visual/sky_color': '#0b1411' },
	carvers: [], downfall: 0.0, effects: { water_color: '#3fe48f' },
	features: steps([[6, 'modulo:ore_mul'], [9, 'modulo:naunax_deck']]), has_precipitation: false, temperature: 0.6,
});
write('data/modulo/worldgen/biome/tokyo_2086.json', {
	attributes: { 'minecraft:gameplay/natural_mob_spawns': noSpawns, 'minecraft:visual/sky_color': '#7f9ed8' },
	carvers: [], downfall: 0.4, effects: { water_color: '#3f76e4' },
	features: steps([[9, 'modulo:city_block']]), has_precipitation: true, temperature: 0.6,
});
write('data/modulo/worldgen/feature/naunax_deck.json', { type: 'jjkcraft:naunax_deck', floor_y: 63, ceiling_y: 76, spacing: 12 });
write('data/modulo/worldgen/feature/city_block.json', { type: 'jjkcraft:city_block', ground_y: 63, kyoto_boundary_x: 0, max_height: 40 });
write('data/modulo/worldgen/feature/ore_mul.json', {
	type: 'minecraft:ore', discard_chance_on_air_exposure: 0.0, size: 7,
	targets: [{ state: 'jjkcraft:mul_ore', target: { predicate_type: 'minecraft:block_match', block: 'jjkcraft:naunax_hull' } }],
});
write('data/modulo/worldgen/placed_feature/naunax_deck.json', { feature: 'modulo:naunax_deck', placement: [{ type: 'minecraft:biome' }] });
write('data/modulo/worldgen/placed_feature/city_block.json', { feature: 'modulo:city_block', placement: [{ type: 'minecraft:biome' }] });
write('data/modulo/worldgen/placed_feature/ore_mul.json', { feature: 'modulo:ore_mul', placement: [
	{ type: 'minecraft:count', count: 6 }, { type: 'minecraft:in_square' },
	{ type: 'minecraft:height_range', height: { type: 'minecraft:uniform', max_inclusive: { absolute: 60 }, min_inclusive: { absolute: 4 } } },
	{ type: 'minecraft:biome' }] });

// ------------------------------------------------------------ sites
const NAUNAX = 'modulo:naunax', TOKYO = 'modulo:tokyo_2086';
function site(id, name, dim, center, radius, layout, npcs = {}, markers = {}) {
	write(`data/modulo/jjkcraft/modulo_site/${id}.json`, {
		dimension: dim, center, radius, layout, name: L(`site.jjkcraft.modulo.${id}`, name),
		npcs: Object.entries(npcs).map(([character, offset]) => ({ character, offset })), markers,
	});
}
site('naunax_deck', 'Naunax: Arrival Hall', NAUNAX, [0, 64, 0], 14, 'deck_hall', {}, { arrival: [0, 0, 5], gate_earth: [-9, 0, 0], gate_tokyo: [9, 0, 0] });
site('naunax_bridge', 'Naunax: Envoys\' Hall', NAUNAX, [0, 64, -40], 12, 'meeting_room', { maru: [0, 0, -3], jabaloma: [3, 0, -4] });
site('naunax_core', 'Naunax: Mul Core', NAUNAX, [40, 64, 0], 12, 'mul_core', { jabaloma: [0, 0, 8], mino: [3, 0, 8] });
site('kyoto_jujutsu_high', 'Kyoto Jujutsu High', TOKYO, [-120, 64, 0], 22, 'plaza',
	{ usami: [-3, 0, -6], yuka: [3, 0, -6], tsurugi: [6, 0, -4], kyoko_tomoe: [-7, 0, -3], mino: [-5, 0, -8] },
	{ arrival: [0, 0, 10], gate_ship: [8, 0, 14] });
site('kyoto_alley', 'Kyoto: The Guiding Road', TOKYO, [-200, 64, 70], 20, 'alley', { yuka: [-16, 0, 1] },
	{ path_1: [-10, 0, 0], path_2: [0, 0, 0], path_3: [12, 0, 0] });
site('kyoto_warehouse', 'Kyoto: Talisman Warehouse', TOKYO, [-230, 64, 140], 16, 'warehouse', {}, { boss: [0, 0, 0] });
site('gifu_river', 'Gifu: The Dry River', TOKYO, [-300, 64, -170], 28, 'river', { osada: [-8, 0, 0] }, { boss: [0, 0, 14] });
site('shuei_elementary', 'Shuei Elementary', TOKYO, [-110, 64, -150], 26, 'school', { masayoshi: [0, 0, 2], cross: [10, 0, -2] },
	{ boss: [-17, 0, 7], pool: [9, 0, 10] });
site('kyoto_beach', 'The Beach', TOKYO, [-330, 64, 200], 24, 'beach', { tsurugi: [-16, 0, 3] });
site('tokyo_barrier', 'Tokyo: The Barrier Line', TOKYO, [60, 64, -60], 18, 'ruins', { usami: [-3, 0, 0], dabura: [4, 0, 0] });
site('inhuman_makyo', 'The Inhuman Makyo', TOKYO, [120, 64, 40], 32, 'ruins', { yuka: [-20, 0, -18] }, { boss: [0, 0, 0] });
site('chaos_arena', 'Shinjuku Ruins: The Duel', TOKYO, [140, 64, 170], 20, 'arena', { tsurugi: [-8, 0, 0] }, { boss: [6, 0, 0] });

// ------------------------------------------------------------ bosses
function boss(id, obj) {
	obj.name = L(`entity.jjkcraft.modulo.${id}`, obj.name);
	write(`data/modulo/jjkcraft/modulo_boss/${id}.json`, obj);
}
boss('trafficker', { name: 'The Trafficker', behavior: 'trafficker', skin: 'trafficker', health: 90, armor: 4, damage: 6, speed: 0.3, color: 'purple',
	params: { teleport_interval: 100, hammer_damage: 7 } });
boss('toranosuke', { name: 'Toranosuke', behavior: 'toranosuke', skin: 'toranosuke', health: 110, armor: 4, damage: 7, speed: 0.36, color: 'yellow',
	params: { pounce_interval: 90, windup: 16, pounce_damage: 8 } });
boss('masaki_takeda', { name: 'Masaki Takeda', behavior: 'masaki_takeda', skin: 'masaki_takeda', health: 170, armor: 6, damage: 6, speed: 0.3, color: 'red',
	params: { reveal_range: 4, needle_interval: 70, needle_windup: 14, needle_damage: 5, hide_below: 0.5, tear_hits: 3, stun_ticks: 60, hide_ticks: 160 } });
boss('skin_guardian', { name: 'Skin-Sewn Guardian', behavior: 'minion', skin: 'skin_guardian', health: 60, armor: 6, damage: 6, speed: 0.28, scale: 1.3, color: 'red' });
boss('maru_duel', { name: 'Maru (Chaos)', behavior: 'maru_duel', skin: 'maru', health: 240, armor: 10, damage: 8, speed: 0.34, color: 'green',
	params: { inversion_interval: 220, inversion_tell: 40, inversion_ticks: 80, arena_radius: 22, gaze_interval: 110, yield_below: 0.15 } });
boss('dabura', { name: 'Dabura Karaba', behavior: 'dabura', skin: 'dabura', health: 520, armor: 14, damage: 12, speed: 0.32, scale: 1.15, color: 'white',
	params: { beam_interval: 70, beam_windup: 20, beam_damage: 9, beam_range: 32, pillar_below: 0.75, pillar_count: 5, pin_ticks: 70, domain_below: 0.5,
		darkness_below: 0.3, darkness_charge: 100, darkness_damage: 22, darkness_radius: 18, darkness_interval: 300, yield_below: 0.05, arena_radius: 28 } });
boss('dabura_scene', { name: 'Dabura Karaba', behavior: 'mahoraga_scene', skin: 'dabura', health: 1000, armor: 20, damage: 0, speed: 0.0, scale: 1.15, color: 'white',
	params: { beam_interval: 140, beam_damage: 24, distance: 10 } });
boss('mahoraga', { name: 'Eight-Handled Sword Divergent Sila Divine General Mahoraga', behavior: 'minion', skin: 'mahoraga', health: 400, armor: 12,
	damage: 14, speed: 0.32, scale: 1.6, color: 'white', adaptation: { key: 'technique', first: 0.2, per_hit: 0.1, max: 0.95, full_adaptation: 0.9 } });

// ------------------------------------------------------------ Dabura's domain
write('data/modulo/jjkcraft/domain/reverse_transcendence.json', {
	name: L('domain.modulo.reverse_transcendence', 'Otherworld Between Darkness and Light: Reverse Transcendence'),
	description: L('domain.modulo.reverse_transcendence.desc', 'Dabura Karaba\'s domain. Its sure-hit effect is never shown in the story; here, light falls on everyone inside.'),
	grade: 'jjkcraft:special_grade', cast_cost: 200, upkeep: 0, radius: 16, max_duration: 400, clash_priority: 5,
	behaviors: [{ type: 'jjkcraft:sure_hit_damage', interval: 20, damage: 2.0 }, { type: 'jjkcraft:apply_effect', effect: 'minecraft:slowness', duration: 40, amplifier: 0 }],
	visuals: { color: '#F6F1D3', secondary: '#1A1A1A', dome: '#0D0C0A', ambience: 'rings' },
	innate_weight: 0,
});

// ------------------------------------------------------------ techniques and tools
write('data/jjkcraft/jjkcraft/technique/batto_sword_drawing.json', {
	name: L('technique.jjkcraft.batto_sword_drawing', 'New Shadow Style: Batto Sword Drawing'),
	description: L('technique.jjkcraft.batto_sword_drawing.desc', 'The fastest draw of the New Shadow Style. With Honoyagi the blade leaves its scabbard in sparks and the cut sweeps out as fire.'),
	category: 'strike', grade: 'jjkcraft:grade_3', cast: { cost: 30, cooldown: 60, range: 5 },
	behavior: { type: 'jjkcraft:arc_slash', damage: 9, arc: 120, ce_scaling: 0.2, knockback: 0.6 },
	visuals: { color: '#FF7A2E', particle: 'minecraft:flame', sound: 'jjkcraft:technique.slash', shake: 0.3, hit_stop: 2, trail: 'slash', hit_sound: 'jjkcraft:technique.strike' },
	icon: 'jjkcraft:textures/gui/technique/batto_sword_drawing.png', innate_weight: 0, mastery_uses: 50,
});
write('data/jjkcraft/jjkcraft/technique/mul_lance.json', {
	name: L('technique.jjkcraft.mul_lance', 'Mul Lance'),
	description: L('technique.jjkcraft.mul_lance.desc', 'A Rumelian blade throws a lance of Mul energy. Paid in Mul energy, not cursed energy.'),
	category: 'projectile', grade: 'jjkcraft:grade_3', cast: { cost: 0, cooldown: 30, range: 40, mul_cost: 25 },
	behavior: { type: 'jjkcraft:projectile', damage: 10, speed: 1.8, size: 0.3, lifetime: 30, pierce: true },
	visuals: { color: '#7CF2A8', particle: 'jjkcraft:cursed_spark', sound: 'jjkcraft:technique.bolt_fire', shake: 0.2, hit_stop: 1, trail: 'beam', hit_sound: 'jjkcraft:technique.bolt_hit' },
	icon: 'jjkcraft:textures/gui/technique/mul_lance.png', innate_weight: 0, mastery_uses: 50,
});
write('data/jjkcraft/jjkcraft/cursed_tool/honoyagi.json', {
	item: 'jjkcraft:honoyagi', kind: 'weapon', grade: 'jjkcraft:grade_2', technique: 'jjkcraft:batto_sword_drawing', cost_multiplier: 1.0,
	on_hit: 'ignite', amount: 3, description: L('item.jjkcraft.honoyagi.desc', 'Tsurugi Okkotsu\'s katana. Sparks between scabbard and guard set its blade alight. A medium: it pays for its technique with its own durability.'),
	medium: true, durability_per_ce: 0.1,
});
write('data/jjkcraft/jjkcraft/cursed_tool/rumelian_blade.json', {
	item: 'jjkcraft:rumelian_blade', kind: 'weapon', grade: 'jjkcraft:grade_3', technique: 'jjkcraft:mul_lance', cost_multiplier: 1.0,
	description: L('item.jjkcraft.rumelian_blade.desc', 'A forked Rumelian sword with Mul set in its hilt. Lends Mul Lance, paid in Mul energy.'),
});
{
	const f = path.join(RES, 'data/jjkcraft/jjkcraft/cursed_tool/split_soul_katana.json');
	const j = JSON.parse(fs.readFileSync(f, 'utf8'));
	j.medium = true; j.durability_per_ce = 0.1;
	fs.writeFileSync(f, JSON.stringify(j, null, 2) + '\n');
}

// ------------------------------------------------------------ advancements and tags
write('data/jjkcraft/advancement/modulo/root.json', {
	criteria: { boarded: { trigger: 'minecraft:changed_dimension', conditions: { to: 'modulo:naunax' } } },
	display: { announce_to_chat: true, background: 'minecraft:gui/advancements/backgrounds/stone', description: { translate: L('advancements.jjkcraft.modulo.root.description', 'Board the Naunax during the Naunax Descent') },
		icon: { id: 'jjkcraft:mul' }, show_toast: true, title: { translate: L('advancements.jjkcraft.modulo.root.title', 'Special Grade Incident') } },
	requirements: [['boarded']],
});
write('data/jjkcraft/advancement/modulo/epilogue.json', {
	parent: 'jjkcraft:modulo/root',
	criteria: { epilogue: { trigger: 'minecraft:impossible' } },
	display: { announce_to_chat: true, description: { translate: L('advancements.jjkcraft.modulo.epilogue.description', 'See the Modulo story to its end') },
		frame: 'challenge', icon: { id: 'jjkcraft:yuji_cursed_object' }, show_toast: true, title: { translate: L('advancements.jjkcraft.modulo.epilogue.title', 'A Bright Future') } },
	requirements: [['epilogue']],
});
write('data/minecraft/tags/block/mineable/pickaxe.json', { replace: false, values: ['jjkcraft:mul_ore', 'jjkcraft:naunax_hull', 'jjkcraft:naunax_deck_plate', 'jjkcraft:mul_conduit', 'jjkcraft:hard_light'] });
write('data/minecraft/tags/block/needs_iron_tool.json', { replace: false, values: ['jjkcraft:mul_ore'] });

// ------------------------------------------------------------ missions
const line = (speaker, key, text) => ({ speaker, text: L(key, text) });
let mission = '';
const K = (s) => `dialogue.jjkcraft.modulo.${mission}.${s}`;
const O = (s, text) => L(`objective.jjkcraft.modulo.${mission}.${s}`, text);
function missionFile(id, order, title, summary, chapters, stages, rewards) {
	write(`data/modulo/jjkcraft/modulo_mission/${id}.json`, {
		order, title: L(`mission.jjkcraft.modulo.${id}`, title), summary: L(`mission.jjkcraft.modulo.${id}.summary`, summary), chapters, stages, rewards,
	});
}
const S = (id) => `modulo:${id}`;

mission = 'special_grade_incident';
missionFile(mission, 1, 'Special Grade Incident', 'A ship of Simurians arrives, and an inspector joins two young sorcerers.', '1-2', [
	{ type: 'travel', objective: O('board', 'Board the Naunax while it hangs above the badlands'), site: S('naunax_deck'), marker: 'arrival', radius: 10,
		intro: [line('narrator', K('intro_1'), '2086, sixty-eight years after the Culling Game. A mothership carrying nearly fifty thousand Simurians hangs high above Nevada.'),
			line('narrator', K('intro_2'), 'The United States has left all dealings with them to Japan. The Jujutsu Inspector General calls it a special grade incident.')] },
	{ type: 'talk', objective: O('maru', 'Meet the Simurian inspector in the envoys\' hall'), site: S('naunax_bridge'), npc: 'maru', lines: [
		line('maru', K('maru_1'), 'You must be one of Japan\'s sorcerers. I\'m Marulu Val Vol Yelvori. Maru is fine.'),
		line('maru', K('maru_2'), 'My people want to know whether we can live beside you. I\'m to go with your sorcerers and see for myself.'),
		line('jabaloma', K('maru_3'), 'The inspector will neither help nor hinder your work. Those are his orders.'),
		line('maru', K('maru_4'), 'Take the gate down to Kyoto. I\'ll follow.')] },
	{ type: 'travel', objective: O('kyoto', 'Take the ship\'s gate down to Kyoto Jujutsu High'), site: S('kyoto_jujutsu_high'), radius: 14 },
	{ type: 'talk', objective: O('usami', 'Hear Usami\'s briefing'), site: S('kyoto_jujutsu_high'), npc: 'usami', lines: [
		line('usami', K('usami_1'), 'Children are vanishing in Kyoto. Japanese citizens are taken and sold, because their cursed energy can be burned for power.'),
		line('usami', K('usami_2'), 'Yuka and Tsurugi Okkotsu will look into it. The Simurian inspector goes with you; he is here under the Security Treaty.'),
		line('usami', K('usami_3'), 'It is an investigation, not a fight. Try to remember that.')] },
	{ type: 'talk', objective: O('yuka', 'Talk to Yuka Okkotsu'), site: S('kyoto_jujutsu_high'), npc: 'yuka', lines: [
		line('yuka', K('yuka_1'), 'An inspector? Fine. Just don\'t slow us down.'),
		line('tsurugi', K('yuka_2'), 'I work faster alone.'),
		line('yuka', K('yuka_3'), 'Ignore my brother. Maru, you\'re with us. Let\'s find those kids.')] },
], { companion: true, grade_xp: 300, items: [{ item: 'jjkcraft:mul', count: 4 }, { item: 'jjkcraft:naunax_beacon', count: 1 }] });

mission = 'kyoto_abductions';
missionFile(mission, 2, 'The Kyoto Abductions', 'A road that steers children into a barrier, and the man who sells them.', '1', [
	{ type: 'travel', objective: O('path_1', 'Walk the guiding road: first lantern'), site: S('kyoto_alley'), marker: 'path_1', radius: 2.5,
		intro: [line('yuka', K('intro_1'), 'This road is under a gaze-manipulation curse. It nudges whoever walks it down one path.'),
			line('yuka', K('intro_2'), 'Walk it in the order it wants, and we\'ll see where it leads.')] },
	{ type: 'travel', objective: O('path_2', 'Walk the guiding road: second lantern'), site: S('kyoto_alley'), marker: 'path_2', radius: 2.5 },
	{ type: 'travel', objective: O('path_3', 'Walk the guiding road: third lantern'), site: S('kyoto_alley'), marker: 'path_3', radius: 2.5,
		intro: [line('narrator', K('hands'), 'At the end of the road, hands reach out of the dark: a barrier technique built to carry people away.')] },
	{ type: 'travel', objective: O('warehouse', 'Find the talisman warehouse'), site: S('kyoto_warehouse'), radius: 12 },
	{ type: 'defeat', objective: O('fight', 'Defeat the trafficker'), site: S('kyoto_warehouse'), boss: 'modulo:trafficker', allies: ['yuka', 'tsurugi'],
		intro: [line('tsurugi', K('fight'), 'He moves through his talismans. Burn them, or watch your back.')] },
	{ type: 'talk', objective: O('report', 'Report to Usami'), site: S('kyoto_jujutsu_high'), npc: 'usami', lines: [
		line('usami', K('report_1'), 'The children are alive. Good work.'),
		line('usami', K('report_2'), 'He says he was quitting the trade, because the aliens will soon show the world how weak our sorcerers have become. Keep that to yourself.')] },
], { grade_xp: 400, items: [{ item: 'jjkcraft:mul', count: 2 }] });

mission = 'swimming_through_the_forest';
missionFile(mission, 3, 'Swimming Through the Forest', 'A tiger comes down from the mountain after its river runs dry.', '3', [
	{ type: 'travel', objective: O('river', 'Go to the dry river in Gifu'), site: S('gifu_river'), radius: 14,
		intro: [line('tsurugi', K('intro'), 'Villagers in the mountains of Gifu keep being attacked. Something big came down the river valley.')] },
	{ type: 'talk', objective: O('osada', 'Talk to Osada'), site: S('gifu_river'), npc: 'osada', lines: [
		line('osada', K('osada_1'), 'I was a sorcerer once. The killer isn\'t a curse. It\'s a tiger. Toranosuke.'),
		line('osada', K('osada_2'), 'They restored the solar plant on the mountain, and the heat dried his river. He came down for water and found people.'),
		line('osada', K('osada_3'), 'I wanted to save him. Keep him far from town.'),
		line('tsurugi', K('osada_4'), 'A beast that has eaten people has to be put down.')] },
	{ type: 'defeat', objective: O('tiger', 'Stop Toranosuke'), site: S('gifu_river'), boss: 'modulo:toranosuke', allies: ['tsurugi'] },
	{ type: 'deliver', objective: O('cub', 'Bring the tiger cub to Osada'), site: S('gifu_river'), item: 'jjkcraft:tiger_cub', count: 1, npc: 'osada', lines: [
		line('tsurugi', K('cub_1'), 'Raise this one well. Don\'t let it become another Toranosuke.'),
		line('osada', K('cub_2'), 'I will. I promise.')] },
], { grade_xp: 400, items: [{ item: 'jjkcraft:high_purity_mul', count: 1 }] });

mission = 'the_boy_who_kept_repeating';
missionFile(mission, 4, 'The Boy Who Kept Repeating', 'A first-grader who never grows older, and the curse user wearing a child\'s skin.', '4-6', [
	{ type: 'talk', objective: O('mino', 'Hear Mino\'s report'), site: S('kyoto_jujutsu_high'), npc: 'mino', lines: [
		line('mino', K('mino_1'), 'Every year the same boy enrols at Shuei Elementary under a new name. Masayoshi, then Masaaki, now Masaki. His face never changes.'),
		line('mino', K('mino_2'), 'Each year he moves away with his family before second grade. Each year he comes back.')] },
	{ type: 'travel', objective: O('school', 'Go to Shuei Elementary'), site: S('shuei_elementary'), radius: 16 },
	{ type: 'talk', objective: O('boy', 'Talk to the boy'), site: S('shuei_elementary'), npc: 'masayoshi', lines: [
		line('masayoshi', K('boy_1'), 'I found my mother dead three years ago. I begged her to let me stay in first grade forever.'),
		line('masayoshi', K('boy_2'), 'Something has looked after me ever since. It never hurts me.'),
		line('maru', K('boy_3'), 'Then let us be your neighbors. Someone who understands why your feelings matter.'),
		line('yuka', K('boy_4'), 'Tsurugi, listen: there are five bodies buried at Masaki Takeda\'s house. The real Masaki Takeda is an old man.')] },
	{ type: 'defeat', objective: O('fight', 'Defeat Masaki Takeda'), site: S('shuei_elementary'), boss: 'modulo:masaki_takeda', allies: ['yuka', 'tsurugi'],
		intro: [line('tsurugi', K('fight_1'), 'Marmalade Boy: he sews with human skin. Himself, his guardian, anything.'),
			line('tsurugi', K('fight_2'), 'If he hides inside that guardian, tear him out of it.')] },
], { grade_xp: 600, items: [{ item: 'jjkcraft:mul_ward', count: 1 }] });

mission = 'berserk';
missionFile(mission, 5, 'Berserk', 'Maru\'s third eye opens, and Chaos answers before he does.', '5-6', [
	{ type: 'travel', objective: O('pool', 'Find Maru by the school pool'), site: S('shuei_elementary'), marker: 'pool', radius: 6,
		intro: [line('narrator', K('intro_1'), 'Water lifts out of a cup and hangs in the air. Across town, an unconscious Maru opens his third eye.'),
			line('narrator', K('intro_2'), 'His technique, Chaos, bends the laws of order: gravity, heat, the path of a thrown stone.')] },
	{ type: 'talk', objective: O('maru', 'Call Maru back'), npc: 'maru', lines: [
		line('narrator', K('maru_1'), 'Maru turns his third eye on you. Your thoughts slide sideways.'),
		line('narrator', K('maru_2'), 'You remind him what he said to the boy: that you would be neighbors.'),
		line('maru', K('maru_3'), '...Neighbors. Did I... hurt anyone?')] },
	{ type: 'talk', objective: O('cross', 'Talk to the Simurian on the roof'), site: S('shuei_elementary'), npc: 'cross', lines: [
		line('cross', K('cross_1'), 'I am Cross, his brother. Our technique is shared; when he uses it without thinking, I feel it.'),
		line('cross', K('cross_2'), 'Some of us would have welcomed a reason for war today. Don\'t give them one.')] },
	{ type: 'talk', objective: O('usami', 'Report to Usami'), site: S('kyoto_jujutsu_high'), npc: 'usami', lines: [
		line('usami', K('usami_1'), 'So the inspector is an alien. The secrecy is over, then.'),
		line('usami', K('usami_2'), 'One of their envoys could conquer Japan alone. His name is Dabura Karaba. Remember it.')] },
], { grade_xp: 400 });

mission = 'cultural_exchange';
missionFile(mission, 6, 'Cultural Exchange', 'A takoyaki party, and the octopus that has the flavor of all eight tentacles in one.', '11', [
	{ type: 'travel', objective: O('beach', 'Go to the beach'), site: S('kyoto_beach'), radius: 14,
		intro: [line('tsurugi', K('intro'), 'The Simurians want to make takoyaki themselves. We need wancha octopus: one tentacle, all eight flavors. Watch the jellyfish.')] },
	{ type: 'deliver', objective: O('catch', 'Catch a wancha octopus and show Tsurugi (%s/%s)'), site: S('kyoto_beach'), item: 'jjkcraft:wancha_octopus', count: 1, npc: 'tsurugi',
		spawns: [{ entity: 'jjkcraft:wancha_octopus', count: 4, radius: 9 }], lines: [
		line('tsurugi', K('catch_1'), 'Not bad. Maru came up with a bigger one, covered in stings. He refuses a rash guard.')] },
	{ type: 'talk', objective: O('party', 'Join the takoyaki party at Kyoto Jujutsu High'), site: S('kyoto_jujutsu_high'), npc: 'kyoko_tomoe', lines: [
		line('kyoko_tomoe', K('party_1'), 'I\'m the nutritionist and I\'m not cooking the main course? Fine. What on earth is this octopus?'),
		line('maru', K('party_2'), 'Hot! It\'s hot! It\'s wonderful!'),
		line('kyoko_tomoe', K('party_3'), 'Take some with you. There is far too much.')] },
], { grade_xp: 300, items: [{ item: 'jjkcraft:takoyaki', count: 6 }, { item: 'jjkcraft:gravity_harness', count: 1 }] });

mission = 'inhuman_makyo';
missionFile(mission, 7, 'Inhuman Makyo', 'The ship runs on cursed energy, curses look like Kalyans, and a duel is demanded.', '11-13', [
	{ type: 'talk', objective: O('core', 'Visit the Naunax\'s Mul core'), site: S('naunax_core'), npc: 'jabaloma', lines: [
		line('jabaloma', K('core_1'), 'This is Mul, the ore our conflict was fought over. It turns rolloluca into any kind of energy.'),
		line('mino', K('core_2'), 'Rolloluca is their word for cursed energy. This ship runs on cursed energy. If that gets out, people will be hunted for it again.'),
		line('jabaloma', K('core_3'), 'Try it yourself: pour your cursed energy into a piece of Mul.')] },
	{ type: 'make_mul_energy', objective: O('mul', 'Make Mul energy at the core (%s/%s)'), site: S('naunax_core'), amount: 40 },
	{ type: 'travel', objective: O('barrier', 'Go to Tokyo\'s barrier line'), site: S('tokyo_barrier'), radius: 12,
		intro: [line('narrator', K('intro_1'), 'In Tokyo, a Simurian weeps from his third eye: sorcerers are exorcising cursed spirits, and to Rumelians they look like Kalyans, the sacred beasts.'),
			line('narrator', K('intro_2'), 'In the confusion, Cross is shot.')] },
	{ type: 'talk', objective: O('usami', 'Hear what happened'), site: S('tokyo_barrier'), npc: 'usami', lines: [
		line('usami', K('usami_1'), 'The Rumelians demand Tokyo, as a nation where cursed spirits are protected. We cannot stop exorcising them; they would spill out of Tokyo.'),
		line('usami', K('usami_2'), 'They have challenged us to a duel, the Deskunte way. Japan has chosen its champion: Yuka Okkotsu.')] },
	{ type: 'talk', objective: O('dabura', 'Face Dabura'), site: S('tokyo_barrier'), npc: 'dabura', lines: [
		line('dabura', K('dabura_1'), 'I fight for the Rumel tribe. On Simuria, a dispute is settled by one champion against another.'),
		line('dabura', K('dabura_2'), 'Bring your champion to the ruins of your old capital.')] },
], { grade_xp: 500, items: [{ item: 'jjkcraft:high_purity_mul', count: 2 }, { item: 'jjkcraft:rumelian_blade', count: 1 }] });

mission = 'forbidden_technique';
missionFile(mission, 8, 'The Forbidden Technique Unleashed', 'Yuka summons Mahoraga against Dabura, and it begins to adapt.', '15-19', [
	{ type: 'travel', objective: O('makyo', 'Go to the Inhuman Makyo'), site: S('inhuman_makyo'), radius: 20,
		intro: [line('narrator', K('intro_1'), 'Yuka forms the seal of the Ten Shadows and calls the shikigami no one in her line has ever tamed: Mahoraga.'),
			line('narrator', K('intro_2'), 'Ten Shadows\' strongest shikigami. Every time something hurts it, its wheel turns, and it adapts.')] },
	{ type: 'defend', objective: O('hold', 'Hold off the cursed spirits fleeing Tokyo while Mahoraga adapts'), site: S('inhuman_makyo'), seconds: 300,
		boss: 'modulo:dabura_scene', protect: 'yuka',
		spawns: [{ entity: 'jjkcraft:hollow_hymnal', count: 3, radius: 24 }, { entity: 'jjkcraft:wick_moth', count: 3, radius: 24 }, { entity: 'jjkcraft:pale_stilt', count: 1, radius: 24 }] },
	{ type: 'talk', objective: O('yuka', 'Check on Yuka'), site: S('inhuman_makyo'), npc: 'yuka', lines: [
		line('yuka', K('yuka_1'), 'It adapted to his Light. He can\'t touch it now.'),
		line('yuka', K('yuka_2'), 'I\'m fine. I\'m fine. Go and find my brother.')] },
], { grade_xp: 800 });

mission = 'chaos_and_harmony';
missionFile(mission, 9, 'Chaos and Harmony', 'Two older brothers duel: Tsurugi, and Maru with both his techniques.', '14, 16-21', [
	{ type: 'travel', objective: O('arena', 'Go to the duel in the Shinjuku ruins'), site: S('chaos_arena'), radius: 14,
		intro: [line('tsurugi', K('intro'), 'I asked Maru to take his people away so Yuka wouldn\'t have to die fighting. He said no. So we fight.')] },
	{ type: 'choice', objective: O('choose', 'Duel Maru yourself, or let Tsurugi fight?'), site: S('chaos_arena'), npc: 'tsurugi', flag: 'chaos_and_harmony',
		options: ['duel', 'stand_aside'] },
	{ type: 'defeat', objective: O('duel', 'Duel Maru: Chaos turns gravity over'), site: S('chaos_arena'), boss: 'modulo:maru_duel', when: { flag: 'chaos_and_harmony', value: 'duel' },
		intro: [line('maru', K('duel'), 'Chaos and Harmony. Both of them, then.')] },
	{ type: 'defend', objective: O('watch', 'Watch Tsurugi and Maru fight (%s s)'), site: S('chaos_arena'), seconds: 45, when: { flag: 'chaos_and_harmony', value: 'stand_aside' },
		intro: [line('narrator', K('watch'), 'Gravity tips over. Rika rises behind Tsurugi. At the last moment he lets go of his katana and is cut down.')] },
	{ type: 'talk', objective: O('after', 'Talk to Tsurugi'), site: S('chaos_arena'), npc: 'tsurugi', lines: [
		line('tsurugi', K('after_1'), 'A future where I won didn\'t feel right. I can\'t explain it better than that.'),
		line('tsurugi', K('after_2'), 'Take Honoyagi for now. Its own energy pays for its cuts, so even I can draw it.')] },
], { grade_xp: 800, items: [{ item: 'jjkcraft:honoyagi', count: 1 }] });

mission = 'ritual_of_harmony';
missionFile(mission, 10, 'Ritual of Harmony', 'Dabura reaches the summit of jujutsu; Maru looks for a world without curses.', '20-23', [
	{ type: 'travel', objective: O('makyo', 'Return to the Inhuman Makyo'), site: S('inhuman_makyo'), radius: 20,
		intro: [line('narrator', K('intro'), 'Dabura understands Reverse Cursed Technique by watching it, builds a barrier by watching one, and pours positive energy into his Light.')] },
	{ type: 'defeat', objective: O('dabura', 'Defeat Dabura Karaba (bring up to four sorcerers)'), site: S('inhuman_makyo'), boss: 'modulo:dabura', allies: ['tsurugi'],
		intro: [line('dabura', K('fight_1'), 'Light, and its reversal. Let us see which of us is the warrior.'),
			line('tsurugi', K('fight_2'), 'When the black mass grows in his hands, get something solid between you and him.')] },
	{ type: 'talk', objective: O('ritual', 'Talk to Maru'), npc: 'maru', lines: [
		line('maru', K('ritual_1'), 'My Harmony took Dabura home. He has a sister to free and a friend to bury.'),
		line('maru', K('ritual_2'), 'I meant to make a world where cursed spirits are never born. Itadori said it cannot be done by taking cursed energy from Japan alone.'),
		line('maru', K('ritual_3'), 'Rumelians, Kalyans and cursed spirits share one color of soul. That is why we mistook them. I will scatter it.')] },
], { grade_xp: 1500, mul_energy: 100 });

mission = 'a_bright_future';
missionFile(mission, 11, 'A Bright Future', 'The ritual ends. Tokyo\'s cursed spirits rest, and two peoples learn to live together.', '24-25', [
	{ type: 'travel', objective: O('kyoto', 'Go to Kyoto Jujutsu High'), site: S('kyoto_jujutsu_high'), radius: 14 },
	{ type: 'sequence', objective: O('watch', 'A bright future'), sequence: 'epilogue', seconds: 32 },
	{ type: 'talk', objective: O('kyoko', 'Talk to Kyoko Tomoe'), site: S('kyoto_jujutsu_high'), npc: 'kyoko_tomoe', lines: [
		line('kyoko_tomoe', K('kyoko_1'), 'Yuka\'s headaches are gone. Completely gone.'),
		line('kyoko_tomoe', K('kyoko_2'), 'Itadori came by and left something with the school. He asked that it go to someone who will keep it safe.')] },
], { advancement: 'jjkcraft:modulo/epilogue', items: [{ item: 'jjkcraft:yuji_cursed_object', count: 1 }] });

// The epilogue sequence's text, shown by the client.
const EPI = [
	'The Ritual of Harmony ends. Across Tokyo, cursed spirits come apart into motes of light and rise.',
	'Yuka wakes as Mahoraga\'s wheel falls and melts into shadow. The pain in her head is gone.',
	'On Simuria, Dabura and Spejo walk into the chieftain\'s hall. The Deskunte kneel to a new chief, and Dura is buried as a hero.',
	'Aboard the Naunax, the Rumelians vote to keep living beside the Kalyans: they are family.',
	'No new sorcerers will be born. Yuji Itadori decides that when he dies, he will become a cursed object, so the energy can be taken back if it is ever needed.',
	'In Kyoto, sorcerers and Simurians play catch.',
];
EPI.forEach((t, i) => L(`epilogue.jjkcraft.modulo.${i + 1}`, t));

// Characters, idle lines and other strings.
const NPCS = { maru: ['Maru', 'Shall I bring you to the ship? Sneak and ask. My Harmony can find the way.'], cross: ['Cross', 'My brother trusts too easily.'],
	yuka: ['Yuka Okkotsu', 'I\'ll get that ring back one day. You\'ll see.'], tsurugi: ['Tsurugi Okkotsu', 'I need to get stronger. Strong enough to give it back.'],
	usami: ['Kou Usami', 'War with them must be avoided at any cost.'], mino: ['Mino', 'Please tell me someone read the treaty.'],
	kyoko_tomoe: ['Kyoko Tomoe', 'Eat properly. Sorcerers never do.'], osada: ['Osada', 'Don\'t call me old.'], masayoshi: ['Masayoshi', 'Are you really going to be my neighbor?'],
	jabaloma: ['Jabaloma', 'We came as refugees, not as invaders.'], dabura: ['Dabura Karaba', 'Words are cheap. A duel is not.'] };
for (const [id, [name, idle]] of Object.entries(NPCS)) {
	L(`npc.jjkcraft.modulo.${id}`, name);
	L(`npc.jjkcraft.modulo.${id}.idle`, idle);
}
L('npc.jjkcraft.modulo.narrator', '');
L('option.jjkcraft.modulo.chaos_and_harmony.duel', 'I\'ll duel Maru myself.');
L('option.jjkcraft.modulo.chaos_and_harmony.stand_aside', 'It\'s your fight, Tsurugi.');

// The lang source for datagen.
const java = ['package com.jjkcraft.datagen;', '', 'import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;', '',
	'/** English text of the Modulo data files: missions, sites, bosses, lines. Written by tools/gen_modulo_data.js; edit there, not here. */',
	'final class ModuloDataLang {', '	private ModuloDataLang() {', '	}', '', '	static void add(TranslationBuilder b) {'];
for (const [k, v] of lang) java.push(`		b.add(${JSON.stringify(k)}, ${JSON.stringify(v)});`);
java.push('	}', '}', '');
fs.writeFileSync(path.join(root, 'src/datagen/java/com/jjkcraft/datagen/ModuloDataLang.java'), java.join('\n'));
console.log(lang.length, 'lang entries');
