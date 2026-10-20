# Performance evidence, 2026-10-19

The latest server GameTest stress scene ran forty curses, twelve NPC entrants and four
summons for 200 measured ticks: mean 10.33 ms/tick. This is a bounded automated scene,
not a worst-case server benchmark or a guarantee on other hardware. All 131 required
server GameTests passed in that run (43.66 seconds of test execution).

Client world effects default to a 4000-quad / 3 ms submission budget. Optional profiling
logs mean/peak submission CPU cost every 300 frames. Curses use a distance LOD beyond
32 blocks and damaged textures only when the resource exists. CPU effect submission
measurements do not include total GPU frame time.

Pending: a sustained ordinary world, four real players, remote server latency, shader
packs and full frame-time comparison on the user's Intel UHD 620.
