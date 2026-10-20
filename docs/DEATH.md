# Current death rules

See PLAYTEST.md and EXPANSION.md for 1.3.0-preview. Both Survival and Hardcore offer
cursed-spirit rebirth; exorcism starts a new character with a different technique.
Survival also offers vanilla respawn points. Colony reincarnation requires a prior
Kenjaku pact and its revival cost. Hardcore free bed respawn is unavailable. The notes
below describe the earlier 1.2 implementation and its object/pact internals.

# Death, objects and pacts preview

Survival uses actual vanilla death/respawn, followed by a server-authoritative choice.
Reincarnation resets Culling points and returns the sorcerer to a colony. Cursed-object
state retains owner identity and token metadata, spectates the object location, and has
a configurable twenty-minute window. The first token can appear as a placed reliquary;
others are item objects. Destruction and expiration are tracked across owner disconnects.
A token is consumed once at revival, restoring 25% health with temporary weakness.

Colony Vessels offer revival, a point-priced Awakening Rite, the Vessel trait, and
Kenjaku/Sukuna pacts with registry-driven costs. Hardcore interception requires a pact;
without one, normal hardcore death remains. Sukuna's repeated revival threshold causes
a short hostile episode. Contract state, expiration and life state persist as attachments.

Automated checks cover state/item serialization, placed-object metadata, spectator state,
revival and duplicate-token rejection. Full hardcore world recovery, multiplayer item
trading/offline destruction, all pact branches and long-running expiry need manual review.
Vessels currently reuse the Kogane visual and appear in colonies; village Vessels and
full possession body control remain unfinished. Do not call this a fully reviewed death
system or the full Phase 6 acceptance checklist.
