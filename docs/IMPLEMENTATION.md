# Completion work, 2026-10-19

The supplied continuation brief is the acceptance checklist. Existing Phases 0 and 1
are retained. The work below is in progress; unchecked items are not release claims.

- [ ] Phase 2: budgeted world effects, readable impacts, accessibility and profiling.
- [ ] Phase 3: curse art, damage states, LOD, animations and expanded encounter roster.
- [ ] Phase 4: server-authoritative framed melee, guard, parry, dash and combat feedback.
- [ ] Phase 5: explicit awakening, clan/general pools, persistence, reveal and loadout usability.
- [ ] Phase 6: death choices, cursed objects, vessels, pacts and hardcore resolution.
- [ ] Phase 7: integration tests, migration/release documentation and playable build.

Player-reported problems: technique selection is unfinished and curses are too scarce.
Audit confirms automatic silent awakening, an icon-only loadout without scrolling,
Creative players excluded from natural encounters, a cap of eight curses, and one
spawn attempt every forty ticks. Twenty original curse types currently exist.

Dependency adjustment: establish the character/technique-selection state and encounter
spawning first, because combat eligibility, player overlays, death choices and regression
scenes depend on those states. This is an implementation order change, not a claim that
the later phases are complete. Existing saves and the Modulo expansion must remain usable.

Validation must distinguish automated checks from in-game visual/feel review, multiplayer
and renderer compatibility. No unperformed check is recorded as passed.

## Playable preview checkpoint

Implemented and server-tested: original Inherit/Random paths, family grade unlocks,
named paginated loadout, forty curse definitions, denser spawn defaults, framed combat,
Survival death choices, object identity and Vessel revival. Added client effects, core
rendering and generated curse damage/LOD assets are undergoing a client smoke check.

This is 1.2.0-preview, not full Phase 2-7 completion. Consult AWAKENING.md, COMBAT.md,
DEATH.md, VFX.md and CURSE_QA.md for explicit remaining acceptance work. In particular,
additional canon summon/domain mechanics, all pact/hardcore paths, bespoke art/animation,
four-player feel tests and renderer compatibility are not signed off. Idle Transfiguration's
family domain is reserved in data but its domain behavior is not implemented.

Canon references consulted: https://jujutsu-kaisen.fandom.com/wiki/Ratio_Technique
and https://jujutsu-kaisen.fandom.com/wiki/Crimson_Binding . The Ratio region uses
7:3 points; Crimson Binding uses the existing binding behavior as a baseline.

## 1.3.0-preview expansion checkpoint

The newer EXPANSION.md supersedes earlier descriptions of pending awakening/death,
Modulo gating and the reserved Idle Transfiguration domain. This checkpoint implements
63 techniques / 19 families, 15 separate attributes, spirit life cycles, fallen-host body
transfer, curse capture/release, Culling entry/personal Kogane, and the integrated Naunax
expedition path. All 140 server tests pass; focused client death/form/rebirth checks pass.
The wider original phase acceptance checklist still includes manual multiplayer, art,
combat balance and renderer work, plus more specialized canon simulations.
