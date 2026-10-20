# Awakening

A persistent server attachment records path, family, clan, Six Eyes and roll time.
First-join HR is checked once. Pending choice survives relog without another HR roll.
Inherit uses weighted clan families; Random uses the general pool. An empty inherited
pool falls back to Random. Direct family choice is disabled by default and server-owned.
Invalid, repeated and rate-limited requests cannot reroll an awakened character.

Nine families live in data/jjkcraft/jjkcraft/innate_family. Their first move is usable
at Grade 4, with remaining moves unlocked by grade. Gojo has a separate 1% Six Eyes roll.
Old awakened characters are migrated from the existing innate technique and keep the original Grade 1 domain pool. New inherited families wait for their dedicated domain grade. Clan identity
is shown at reveal; a full profile history is still pending. Awakening Rites require a
nearby colony Vessel and the configured point cost.

WeightedRoll tests include one million seeded samples. Server tests cover both pools,
forged selection, one-shot behavior, relog pending state and Special Grade unlocks.
