# Quality of life — 1.12.3-preview

This update builds on the reconciled GitHub and local reworks. Existing saves and
technique bindings continue to work.

## Finding and using abilities

Open a family's wheel with **R**, then choose **Abilities**. Search by displayed name
or move ID. Cycle **All**, **Learned**, **Ready** and **Untamed** to narrow the list.
Opening the browser from a selected wheel move jumps to that move's page.

The browser and wheel show cooldowns, grade requirements, CE availability, learned
status, stun and technique burnout. Ready is a client estimate; the server still
checks every cast, including personal vows and other contextual requirements. You
can equip a move while it is on cooldown. Search keeps its focus while the menu
refreshes, so typing is uninterrupted.

Each family's wheel remembers its selected move, plus the Limitless branch filter,
until disconnect. Use **Back to wheel** in the browser to return to the equipped
family. Instant moves and toggles preserve any separate ongoing channel.

## Infinity status

The selected-power HUD and Infinity tooltip show the saved toggle and actual field
state. The display distinguishes **OFF**, **ON**, waiting for CE, suppression and
startup. Upkeep comes from the server's personalized Infinity settings. Learning RCT
changes it to **no upkeep** without needing to toggle the field.

Infinity still starts enabled for eligible Limitless users. Its normal move toggles
the preference, which survives death and reconnect.

## Rituals and summons

The Ten Shadows wheel's **Subjugation** button opens the ability browser. Select an
untamed summon and choose **Subjugate** to start its hostile trial. Defeat it yourself
before its timer expires, stay alive in the same dimension and remain within 48
blocks. Outside combat intervention fails the trial.

A ritual HUD shows remaining time and distance, with warnings below 20 seconds or
beyond 40 blocks. Configure visibility, position, anchor, scale and opacity through
the **Ritual tracker** HUD settings. **End ritual** dismisses the trial beast without
unlocking it; spent CE is not refunded.

**Dismiss summons** on a summoning family's wheel, or **Dismiss (count)** in its
browser, dissolves that family's nearby owned summons, including fusion results.
It leaves other owners' summons alone. The count covers loaded summons in the
current dimension within the existing 64-block ownership query.

## Verification

The native client checks exercise search and typing focus, cooldown filtering,
remembered selections, Infinity toggling during an unrelated channel, live RCT
upkeep changes, opening a selected move's page, ritual countdown and cancellation,
and dismissal of owned normal and fusion summons while retaining another owner's.

The combined `qolRegression` scenario also checks the prior respawn, subjugation,
Infinity and equipment reconciliation. The separate `polish` scenario covers the
existing technique controls and Projection planner. Run with Java 25:

```powershell
.\gradlew.bat runClientGameTest -PclientTestFocus=qolRegression
.\gradlew.bat runClientGameTest -PclientTestFocus=polish
.\gradlew.bat test assemble
```

Validation on 22 October 2026: both native client scenarios passed; all 128 unit
tests passed with no failures or skips; the asset validator passed for 75 unique
creature rigs, 1,200 animation clips, 300 atlases and 296 technique icons. The
assembled release is `build/libs/jjkcraft-1.12.3-preview.jar`.

Full data generation remains blocked by existing missing item-model declarations
for equipment and the Divine Chalice. Its attempted output was backed up and
restored to preserve the checked-in resources; only the seven generated ritual
tracker setting labels were retained. Runtime asset validation and packaging pass.

Client and server should use the same preview version because menu and Infinity
status synchronization now carries additional fields.

The repository workflow in `AGENTS.md` records the requested release sequence:
fetch and preserve changes, verify and package, commit and push, then launch
Minecraft with `runClient -PclientRunDir=run/rework-preview`.
