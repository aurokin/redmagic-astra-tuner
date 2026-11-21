# Results

Everything measured on the tablet, by scenario. Each section names the
campaigns it comes from; their per-run summaries are in
[`data/campaigns/`](../data/README.md) and `tools/results.py table CAMPAIGN`
prints them. How the numbers are made is in
[development.md](development.md#how-a-run-is-measured).

**Which firmware.** Every section but the last was measured on RedMagicOS
11.0.7; [RedMagicOS 11.0.8MR1](#redmagicos-1108mr1) repeats the main
comparisons on that build with the 0.4 and 0.4.1 releases and finds them
unchanged but for 30 fps games in Rise, which those two improve.

**Reading the numbers.** *Clean* is the share of seconds with no late frame
beyond what the panel itself forces. *Late frames* are extra one-refresh delays
per second. *W* is the whole tablet's draw. Power on the USB cable counts the
battery's share, but runs on the cable are not identical to runs on battery.
Most comparisons are one run per arm: differences under about half a watt, or
a few points of clean seconds, are within the spread between two runs of the
same thing.

## Summary

| Scenario | Vendor alone | Governor | Diablo |
| --- | --- | --- | --- |
| Mario Galaxy, heavy scene, Balance | 71% speed, 0% clean, 17 late frames/s | 100% speed, every second clean, 4.5 W | the same frames at 7.5 W |
| Emulator scenes, 60 Hz (16 scenes, Balance) | 7 of 16 below 90% clean | 15 of 16 at 88% clean or better | 1.5-2x the power; worse than the governor in two |
| SuperTuxKart, 165 Hz, 30 min | 1.35 late frames/s, 74% clean | 99.9% clean, 8.7 W | |
| Synthetic game, 165 Hz | 161 fps, 4.7 late frames/s | 166.1 fps, every second clean | the same frames at 10.6 W |
| Game streaming, 120 Hz | 3.7 W, decode 7.3 ms | 3.7 W, decode 5.8 ms | |
| 60 fps video, outside games | 58.7-58.9 fps | 59.8 fps, no measurable power | |
| GPU-limited game at 165 Hz | ~94 fps, 15.7 W | ~94 fps, 15-16 W | 91 fps, 19.3 W |

Where a game can reach its rate, the governor either removes late frames the
vendor lets through or matches the vendor; it never measured worse on frames.
It costs power where the vendor was starving the game, and up to 2 W for two
or three minutes after a launch.

## Emulators at 60 Hz

Campaign `real2` (2026-11-14): 22 scenes, a minute each, every arm on the same
running scene. "First governor" is the 2026-11-09 design; "Governor" is
the build of 2026-11-14, before the light, GPU-bound and thrifty rules (none of
which these scenes trigger except Soulcalibur and F-Zero GX).

| Scene | Vendor, Balance | First governor, Balance | Governor, Balance | Governor, Rise | Diablo |
| --- | --- | --- | --- | --- | --- |
| Mario Galaxy, castle gate (Wii) | 0% clean, 3.7 W, 71% speed | 100%, 4.3 W | 100%, 4.5 W | 100%, 4.5 W | 100%, 7.5 W |
| Mario Galaxy, festival (Wii) | 66%, 4.2 W | 100%, 4.8 W | 100%, 4.8 W | 100%, 4.7 W | 100%, 8.7 W |
| Xenoblade (Wii) | 73%, 4.0 W | 84%, 4.1 W | 97%, 4.5 W | 96%, 5.0 W | 79%, 9.4 W |
| F-Zero GX attract (GameCube) | 78%, 3.6 W | 83%, 4.4 W | 87%, 4.5 W | 94%, 5.8 W | 94%, 7.5 W |
| Wind Waker (GameCube) | 82%, 3.3 W | 91%, 3.6 W | 94%, 3.6 W | 91%, 3.7 W | 96%, 7.0 W |
| Harry Potter, 25 fps PAL (GameCube) | 90%, 3.7 W | 51%, 4.2 W | 94%, 3.9 W | 99%, 4.4 W | 99%, 7.5 W |
| God of War II (PS2) | 95%, 6.0 W | 83%, 6.4 W | 98%, 6.6 W | 100%, 8.6 W | 100%, 12.7 W |
| Shadow of the Colossus (PS2) | 97%, 6.7 W | 93%, 6.4 W | 100%, 7.4 W | 97%, 10.6 W | 66%, 12.3 W |
| Soulcalibur attract (Dreamcast) | 79%, 3.1 W | 80%, 3.5 W | 89%, 4.8 W | 89%, 5.2 W | 89%, 5.4 W |
| Super Mario Odyssey (Switch) | 100%, 3.9 W | 100%, 4.1 W | 100%, 4.2 W | 100%, 4.1 W | 100%, 6.9 W |
| A Link Between Worlds (3DS) | 100%, 3.3 W | 100%, 3.4 W | 100%, 3.3 W | 100%, 3.7 W | 100%, 5.9 W |
| Pokemon X (3DS) | 98%, 3.1 W | 100%, 3.2 W | 100%, 3.1 W | 100%, 3.2 W | 100%, 4.7 W |
| God of War: Chains of Olympus (PSP) | 100%, 3.1 W | 100%, 3.2 W | 100%, 3.1 W | 100%, 3.2 W | 100%, 5.0 W |
| Mario Kart DS (DS) | 100%, 3.2 W | 100%, 3.6 W | 100%, 3.7 W | 100%, 3.2 W | 100%, 5.6 W |
| Super Mario World, Metroid Fusion (RetroArch) | 100%, 2.7-2.8 W | 100% | 100% | 100% | 100%, 4.5 W |
| SuperTuxKart race on the grid (native) | 100%, 4.3 W | 100%, 4.7 W | 100%, 4.6 W | 100%, 4.9 W | 100%, 7.6 W |

- **Rise against Balance.** For heavy games (God of War II, F-Zero GX) Rise is
  better on every count but power; for light and medium games the two come
  out the same, because the governor picks low clocks in both. The vendor
  alone in Rise did not fix Mario Galaxy's gate (0% clean): the problem is the
  vendor's frame governor, not the mode's ceilings.
- **Diablo** gets every frame the clocks can buy at 1.5-2 times the power, and
  in Xenoblade and Shadow of the Colossus it measured worse than the governor
  (its own heat).
- The 25 and 30 fps games show pairs of one short and one long frame as their
  clock drifts past the panel's, in every arm; that counts against *clean*
  without saying anything about the clocks.

## Panel rates above 60 Hz

Game Space starts every listed app on its own panel rate, 60 Hz unless one is
set ([vendor-system.md](vendor-system.md#refresh-rate)).

**Synthetic game that can run at the panel's rate** (`panel2`, `panel3`, Rise):

| Panel | Governor | Vendor alone | Diablo |
| --- | --- | --- | --- |
| 90 Hz | 90.4 fps, every second clean, 7.0 W | 90.4 fps, clean, 5.1 W | |
| 120 Hz | 120.5 fps, every second clean, 6.8 W | 120.5 fps, clean, 5.0 W | 120.5 fps, 10.0 W |
| 144 Hz | 144.7 fps, every second clean, 6.6 W | 144.7 fps, clean, 5.6 W | |
| 165 Hz | 166.1 fps, every second clean, 7.7 W | 161.3 fps, 4.7 late frames/s, 5.7 W | 166.1 fps, 10.6 W |

At 120 Hz the vendor alone happens to hold it with the GPU 84% busy; the
governor wants the busy share under 80%, which costs 1.8 W for the same frames.

**SuperTuxKart, a race driven by scripted touches** (`native1`, `native3`, Rise):

| Panel | Governor | Vendor alone |
| --- | --- | --- |
| 120 Hz, 2 min | 120.54 fps, every second clean, 6.6 W | 120.50 fps, 98.4% clean, 6.3 W |
| 144 Hz, 90 s | 144.65 fps, every second clean, 7.8 W | 143.93 fps, 93.7% clean, 7.1 W |
| 165 Hz, 30 / 15 min | 166.05 fps, 99.9% clean, 8.7 W | 164.71 fps, 74.0% clean, 1.35 late/s, 8.2 W |
| 165 Hz, Balance, 2 min | 166.06 fps, every second clean, 9.4 W | 162.47 fps, 24% clean, 3.6 late/s, 8.4 W |

In the 30-minute run the board peaked at 39.8 C; the one late moment was the
race ending at 22 min 40 s.

**Geometry Dash Lite** (the only commercial game staged; `native3`): 166.06 fps,
every second clean, for 20 minutes at 4.0 W; the vendor alone the same at 3.9 W.
**Shattered Pixel Dungeon** (2D): 166 fps, 3.6 W either way.

**60 fps content on a faster panel** (Dolphin, Mario Galaxy): 59.94 fps at 120
and 165 Hz, 95-100% of seconds clean.

## A game the GPU cannot carry to the panel's rate

SuperTuxKart with every graphics option on, 165 Hz, Rise (`native1`-`native4`,
`fan2`): 90-105 fps with the GPU full at 1100 MHz.

- **Before the GPU-bound rule** the governor held the CPU at the top for the
  whole race: 19.1 W against the vendor's 16.4 W. With it: 15-16 W, level with
  the vendor.
- **The vendor's heat cap decides the frame rate.** The board reaches 50.2 C in
  two to four minutes; the vendor caps the GPU at 832 MHz until 48.1 C (about
  45 s), and the rate swings between 77-80 and 95-105 fps every minute and a
  half. Once the tablet is warm through it stays capped: 78-88 fps at 11 W.
- **Long pairs, order swapped** (15-20 min): the governor 87.8 and 94.6 fps,
  the vendor 95.2 and 97.6 fps; the governor's runs had the warmer starts, and
  two pairs neither show nor rule out a cause. Matched-start pairs of 8 minutes
  (`fan2`): 96.5 and 94.9 fps for the governor alone.
- **CPU maximums cost it nothing** (`ceil2`, `ceil3`: maximums switched every
  ten seconds on a fixed view): 94, 94, 93, 93, 91, 92, 92 fps, top and low in
  turn.
- **Diablo**: 90.9 fps at 19.3 W, already limited in the first minute.

A game like this wants its own frame cap.

## Game streaming

Artemis and Moonlight from a desktop host (`stream1`-`stream5`). Artemis is a
Game Space app, so it gets a game scene and both frame governors.

| Stream | Governor | Vendor alone |
| --- | --- | --- |
| 1080p60, 60 Hz panel | 3.13 W, decode 6.4 ms | 3.13 W, 8.0 ms |
| 1080p120, 120 Hz panel | 3.67 W, 5.8 ms | 3.70 W, 7.3 ms |
| 1440p120, 120 Hz panel | 3.67 W, 7.3 ms | 3.56 W, 8.4 ms |
| A game at 165 Hz, 20 min (Artemis) | 3.6-3.7 W, 4.8 ms, board flat at 31 C | 3.9 W, 5.7 ms |
| The same, Moonlight | 3.9 W, 3.9 ms | 3.8 W, 4.2 ms |

- The tablet drew every frame that arrived, in every arm; the host sent 51 of
  60, 91-94 of 120 and 116-124 of 165. Pacing figures describe the host.
- Balance is enough: Rise changed nothing measurable.
- Before the light rule, the governor sat at the top clocks for 4.0 W and no
  extra frame.

## Apps that are not games

`nongame1`, `nongame-165`, `nongame-auto`, `nongame2`, `chrome1`. Outside Game
Space both frame governors are off.

- **Video** in VLC: a 60 fps clip at 58.7-58.9 fps with the vendor's clocks;
  59.8 fps with a 1152 MHz minimum on the performance cores (749 MHz: 59.3;
  1555: 59.9). At 120 Hz late frames fell from 0.9-1.0 to 0.2-0.4 a second. No
  power cost: 3.36 against 3.32 W over six pairs, 2.35 against 2.40 W idle.
  This is the non-game floor the module runs with.
- **Scrolling** (Settings, a replayed finger): 0.2-0.3% of frames late at
  165 Hz and 0.3-0.8% at 60 Hz, no better with a floor.
- **Chrome**: a web game's animated menu (tibo.dog) at the panel's rate with
  every second clean at 60, 90 and 120 Hz under the vendor's Chrome cap;
  above 120 Hz Chrome needs to be a Game Space app (then 144.7 and 166.1 fps,
  clean, 3.7 and 4.1 W). A 60 fps video: 60.00 fps at every rate, with one
  long hold every 3.7 s at 120 Hz (the panel's real rate; see
  [display.md](display.md)).
- **YouTube**: 41 frames a second of a 60 fps upload in its page player, with
  and without the floor; not the clocks, cause not found.

## Heat and the fan

God of War II for 28 minutes (`heat1`-`heat6`), a synthetic GPU-heavy load for
65-75 minutes (`heat7`, `heat8`), the heavy SuperTuxKart race (`native5`,
`fan2`).

- **Rise, no heat control**: 10 W, the vendor's cut at 49 C after 18-26 minutes.
- **Rise, `-heat 48`**: no cut in 28 minutes, three runs of three; 8.8 W
  against 10.2 W, late frames unchanged (0.5-0.7 a minute). 71 minutes
  standing: no cut.
- **Balance**: cut after 14 minutes at 7 W, and its own ceilings already cost
  2.7-3.3 late frames a minute in this game's fights.
- **Heat from the GPU** (10.5 W, 65 minutes): the board levelled off at 49.7 C,
  the GPU was never limited and every frame was on time.
- **The fan**, three ways, no effect on any sensor, power or frame rate:
  - level 5 (20,000 rpm) against the vendor's level 2 (9,000) in God of
    War II;
  - an hour of one steady 10 W load with the fan switched between off, level 2
    and level 5 every six minutes (`fan1`): the GPU's junction stayed 35-36 C
    above the board in every block, the fan off included;
  - four matched-start runs of the heavy race (`fan2`): GPU capped from 2.4-3.1
    minutes, 94.5-96.5 fps, either level.
- **The GPU's top level given up first** (branch `gpu-first-heat`,
  `gpufirst1`-`gpufirst3`, 2026-11-18): the heavy race for 30 minutes at
  165 Hz, the heat control taking the GPU from 1100 to 967 MHz before any CPU
  step. One valid pair (`gpufirst2`); the other runs were spoilt by the
  vendor's drop to 120 Hz or a system-update notice over the game.

  | | Installed build | GPU-first |
  | --- | --- | --- |
  | Minutes 0-7 | ~99 fps, 15-17 W, GPU 1100 MHz | ~96 fps, 13-13.5 W, GPU 967 MHz |
  | Vendor's GPU cut (832 MHz) | from minute 4, 74% of the run | from minute 7, 74% of the run |
  | Whole run | 91.5 fps, 12.5 W | 88.6 fps, 11.6 W |

  The race is too hot to hold under the cut even at 967 MHz. The cut comes
  later because the GPU was already slower, so the step trades 2-3% of frames
  for 1-2.5 W, which a frame cap would also do. Not adopted. Starting skin
  temperatures were 26.8 and 31.2 C: weak evidence. The cap itself works:
  `devfreq/max_freq` held the GPU at 967 MHz throughout (`gpucap0`).
- **Unexplained cooling spells**: several times the board, skin and battery
  fell 2-5 C over 5-12 minutes at an unchanged load, fan speed and clocks.
  Room air moving across the tablet's back is a guess.

## Giving back unused GPU clock from the start

`thrifty1`, `thrifty2` (2026-11-16), each build from a fresh launch:

| Game | Without | With |
| --- | --- | --- |
| Soulcalibur | 6.84 / 3.77 W (GPU sometimes at 1060 MHz, 4% busy) | 4.26 / 3.93 W, GPU 250-260 MHz, 96.5% clean |
| God of War II | 9.58 / 8.64 W | 8.44 / 8.38 W |
| F-Zero GX | 5.22 / 5.50 W | 5.58 / 5.26 W |
| Xenoblade, Mario Galaxy, Mario Odyssey, Harry Potter | | 0.1-0.3 W less each |
| Shadow of the Colossus (30 fps, outside the rule) | 10.85 W | 10.87 W |

46.3 W against 50.4 W summed over eight games; no game's frames worse beyond
its usual spread. Installed 2026-11-17.

## RedMagicOS 11.0.8MR1

Everything above was measured on RedMagicOS 11.0.7. The tablet was updated to
11.0.8MR1 on 2026-11-19 (same kernel and display driver, a new GPU driver)
and the governor measured again, twice:

- `mr1-real1` (11-19 evening): the plan of `real2`, every scene a minute
  under each arm, with 0.3's control law on the new firmware.
- `mr2-real1` (11-20): the same plan on the 0.4 release build, installed from
  its zip, which adds the [count of frames held too
  long](control-law.md#when-a-frame-is-late) for 30 fps content. 82 runs on
  one boot; one run that did not start was repeated.
- `mr3-real1` (11-20): the plan once more on the 0.4.1 build, installed from
  its zip, which adds [a minimum for the performance cores in
  games](#games-on-the-prime-cores-in-rise-041). 82 runs on one boot; one
  scene that did not start was repeated.

Display calibration was off except where it says otherwise.

**The 0.3 results hold on 11.0.8MR1; 0.4 and 0.4.1 each change one of them
on purpose.** Scene for scene the four campaigns are within the spread
between two runs, except Xenoblade in Rise (0.4) and Shadow of the Colossus
in Rise (0.4.1), below.

| | 11.0.7, 0.3 (`real2`) | 11.0.8MR1, 0.3's law (`mr1-real1`) | 11.0.8MR1, 0.4 (`mr2-real1`) | 11.0.8MR1, 0.4.1 (`mr3-real1`) |
| --- | --- | --- | --- | --- |
| Emulator scenes below 90% clean, vendor in Balance | 7 of 16 | 6 of 16 | 7 of 16 | 6 of 16 |
| Emulator scenes at 88% clean or better, governor in Balance | 15 of 16 | 15 of 16 | 15 of 16 | 15 of 16 |
| The same, governor in Rise | | 14 of 16 | 15 of 16 | 15 of 16 |
| Mario Galaxy, heavy scene: vendor, governor, Diablo | 71% speed 3.7 W, 100% 4.5 W, 100% 7.5 W | 73% speed 3.7 W, 100% 4.3 W, 100% 7.2 W | 73% speed 3.8 W, 100% 4.5 W, 100% 8.2 W | 73% speed 3.6 W, 100% 4.2 W, 100% 7.5 W |
| 60 fps video outside games: vendor, governor | 58.8 fps, 59.8 fps | 58.7 fps, 59.8 fps | 58.8 fps, 59.8 fps | 59.0 fps, 59.9 fps |
| Power over the sixteen emulator scenes: vendor, governor in Balance, in Rise | | 59.5, 67.8, 73.1 W | 60.7, 68.2, 75.7 W | 60.1, 68.0, 73.7 W |

The scene left below 88% in Balance is F-Zero GX's attract loop each time
(86-88%, as in Diablo's 91-92% it never reaches full speed). The one left in
Rise in 0.4 is Shadow of the Colossus, below. In 0.4.1 it is Xenoblade, at
87.9% in that one-minute pass (89% in Balance); three 90 s runs of it in
each mode straight afterwards gave 97-99% and 99-100% (`mr3-thirty1`), so
this is the spread of a one-minute run and not a change. Diablo over the
sixteen: 114.8 W, 116.7 W and 115.8 W.

0.4, scene by scene (`mr2-real1`):

| Scene | Vendor, Balance | Governor, Balance | Governor, Rise | Diablo |
| --- | --- | --- | --- | --- |
| Mario Galaxy, castle gate (Wii) | 0%, 3.8 W, 73% speed | 100%, 4.5 W | 100%, 4.3 W | 100%, 8.2 W |
| Mario Galaxy, festival (Wii) | 65%, 4.3 W | 100%, 4.8 W | 100%, 4.9 W | 100%, 9.3 W |
| Xenoblade (Wii) | 68%, 3.8 W | 100%, 4.6 W | 96%, 5.8 W | 79%, 9.4 W |
| F-Zero GX attract (GameCube) | 75%, 3.8 W, 94% speed | 87%, 4.6 W, 95% speed | 91%, 6.0 W, 95% speed | 91%, 7.6 W, 95% speed |
| Wind Waker (GameCube) | 88%, 3.4 W | 100%, 3.9 W | 98%, 4.0 W | 91%, 6.7 W |
| Harry Potter, 25 fps PAL (GameCube) | 91%, 3.7 W | 94%, 3.9 W | 98%, 3.7 W | 99%, 7.5 W |
| God of War II (PS2) | 97%, 6.1 W | 95%, 6.9 W | 98%, 8.7 W | 100%, 12.2 W |
| Shadow of the Colossus (PS2) | 89%, 6.5 W | 96%, 7.6 W | 74%, 10.7 W | 67%, 12.2 W |
| Soulcalibur attract (Dreamcast) | 85%, 3.0 W, 96% speed | 96%, 3.8 W, 97% speed | 95%, 4.7 W, 97% speed | 88%, 5.0 W, 96% speed |
| Super Mario Odyssey (Switch) | 100%, 4.2 W | 100%, 4.5 W | 100%, 4.2 W | 100%, 7.6 W |
| A Link Between Worlds (3DS) | 100%, 3.3 W | 100%, 3.4 W | 100%, 3.4 W | 100%, 5.5 W |
| Pokemon X (3DS) | 100%, 3.1 W | 100%, 3.2 W | 100%, 3.2 W | 100%, 5.1 W |
| God of War: Chains of Olympus (PSP) | 100%, 3.0 W | 100%, 3.0 W | 100%, 3.1 W | 100%, 5.6 W |
| Mario Kart DS (DS) | 100%, 3.2 W | 100%, 3.8 W | 100%, 3.3 W | 100%, 6.2 W |
| Super Mario World, Metroid Fusion (RetroArch) | 100%, 2.7-2.8 W | 100%, 2.8 W | 100%, 2.8-3.0 W | 100%, 4.1-4.7 W |
| Shattered Pixel Dungeon (native) | 100%, 2.6 W | 100%, 2.7 W | 100%, 2.7 W | 100%, 3.8 W |

SuperTuxKart's row is left out: its Game Space entry is now 165 Hz, so in
these campaigns it ran at 165 Hz (every arm 100% clean, 9.4-10.6 W, Diablo
17.7 W) and is not the 60 Hz race of `real2`.

Video in VLC, outside games (the governor's part there is the non-game
floor): 60 fps at 1080p 58.8 fps and 66% clean under the vendor, 59.8 fps and
86% with the governor; at 2160p 58.6 fps and 73% against 59.8 fps and 87%;
30 fps 83% against 98%; 24 fps the same either way. Within 0.2 W in each.

### 30 fps games: late frames the rate does not show

With 0.3's law, Xenoblade in Rise came out at 61% clean in `mr1-real1` and at
96% and 70% in two more runs (`mr1-real2`). In both low runs the governor had
tried the GPU minimum all the way down to 222 MHz (`try-gpu389`,
`try-gpu342`, `try-gpu222` in its log) and kept it there: its own count
stayed at "30.1 fps of 30.1" while 0.6-0.9 frames a second came a refresh
late and the next made up for it. On 11.0.7 the same law gave Xenoblade
76-92% clean in runs of 150 s (`thrifty1`, `turns1`), so this was the law
and not the firmware.

0.4 counts those frames. `mr2-thirty1`: each scene 90 s, three times in each
mode, the governor left running throughout.

| Scene | Governor, Balance | Governor, Rise | 0.3's law, Rise |
| --- | --- | --- | --- |
| Xenoblade | 93, 100, 97% clean; 4.5-5.1 W | 94, 96, 99%; 5.4-7.3 W | 61, 96, 70%; 4.8-7.7 W |
| Wind Waker | 99, 99, 98%; 3.8-4.3 W | 98, 99, 99%; 3.9-4.1 W | 100%; 3.6 W |
| Shadow of the Colossus | 93, 98, 99%; 6.9-8.2 W | 73, 78, 76%; 10.5-10.6 W | 67, 67, 88%; 10.0-10.9 W |

- **Xenoblade**: in the first run the governor went to 222 MHz, counted the
  frames held too long within a few seconds, went back and stayed at 525 MHz
  for the five runs that followed.
- **Shadow of the Colossus was worse in Rise and in Diablo, and 0.4 did not
  change that.** Diablo gave 65-67%, the governor in Rise 67-88% with 5-13
  frames a minute held two refreshes too long, Balance 93-99% with none; on
  11.0.7 the governed Rise runs ranged 78-97%. Two causes were found
  afterwards, neither of them the clocks: [the performance cores' minimum in
  Rise](#games-on-the-prime-cores-in-rise-041), put right in 0.4.1, and the
  panel's rate, which is what is left in Diablo
  ([below](#an-emulator-on-the-ntsc-choice)).
- Seven minutes of Xenoblade in Balance from a fresh launch under 0.3's law
  (`mr1-xeno-long`) held 96% clean, minute by minute 100, 85, 100, 98, 95,
  100 and 93%, at 5.0 W.

### Games on the prime cores in Rise (0.4.1)

The vendor's Rise puts a heavy game's threads on the two prime cores. The
governor's CPU minimum follows them there, and the six performance cores got
only what the game's own threads on them called for: nothing, so 384 MHz,
under the 1152 MHz they are held at outside games. Shadow of the Colossus
then skipped frames: the emulator finished one 55-61 ms after the last and a
frame was shown for four refreshes instead of two, 3-19 times in 90 s
(`mr2-thirty1`, `mr2-real1`), with the GPU at its top clock, the prime cores
at 3.8-4.3 GHz and the hottest core at 104 C. It showed once the game's other
trouble, the panel's rate, was taken away
([below](#an-emulator-on-the-ntsc-choice)): Diablo, which holds the six cores
at 3.5 GHz and is as hot, was then perfect and Rise still skipped.

With the panel on NTSC, 90 s runs:

| Performance cores' minimum in Rise | Skipped frames per run | Clean | Power |
| --- | --- | --- | --- |
| 0.4 as released: 384-750 MHz (`mr2-sotc-ntsc1`) | 11, 11 | 89, 89% | 10.0, 10.9 W |
| 0.4, held by hand at 1152 MHz (`mr2-sotc-pin1152000`) | 0, 0 | 99, 99% | 10.0, 9.8 W |
| 0.4, held at 1555 MHz (`mr2-sotc-pin1555200`) | 0, 0 | 99, 100% | 10.6, 10.0 W |
| 0.4, held at 2227 MHz (`mr2-sotc-ntsc4`) | 0, 0 | 100, 100% | 10.5, 10.6 W |
| The new build with no minimum (`mr2-side1`) | 1, 12, 5 | 99, 88, 94% | 10.5, 10.8, 10.2 W |
| The new build, 1152 MHz (`mr2-side1`) | 0, 0, 0 | 99, 100, 100% | 10.2, 10.3, 10.5 W |
| The new build, 1555 MHz (`mr2-side1`) | 0, 0, 0 | 100, 100, 100% | 10.6, 10.4, 10.5 W |

The last three rows are one build with the minimum as a setting, the nine
runs taken in turn. 1152 MHz is what the cores already have outside games,
so 0.4.1 uses that: entering a game no longer lowers it.

On the factory panel, 0.4.1 as installed, three 90 s runs (`mr3-thirty1`) and
the one-minute pass of `mr3-real1`, against the same four for 0.4:

| Shadow of the Colossus | 0.4 | 0.4.1 |
| --- | --- | --- |
| Governor, Rise | 73, 78, 76 and 74% clean; 3-19 skipped frames a run; 10.5-10.7 W | 98, 92, 94 and 94%; none skipped; 10.0-10.1 W |
| Governor, Balance | 93, 98, 99 and 96%; 6.9-8.2 W | 94, 93, 97 and 96%; 7.8-8.3 W |

- **The other scenes whose performance cores sat under 1152 MHz in 0.4's
  Rise are unchanged**: God of War II 98% at 8.5 W (98%, 8.7 W), Mario Kart
  DS 100% at 3.4 W (3.3 W), SuperTuxKart at 165 Hz 100% at 10.4 W (10.5 W).
- **Xenoblade and Wind Waker in Rise do not go through the change** (their
  minimum there was already higher) and held 99, 99, 97% each. They drew
  more than in `mr2-thirty1`, 7.7-8.1 W and 4.5-5.8 W against 5.4-7.3 W and
  3.9-4.1 W: the governor spent these runs at ample clocks (`uneven`, late at
  the top and held against nothing), where last time an earlier probe had
  already brought it down. That is the law's known cost for [content late at
  ample clocks](history.md#open-problems), not this change.
- **What a frame waits for on those six cores was not found.** The
  emulator's three heavy threads were on the prime cores throughout.

### 165 Hz and the vendor's refresh setting

SuperTuxKart at 165 Hz in Rise for five minutes, `mr1-panel1` and, on the
0.4 and 0.4.1 releases, `mr2-panel1` and `mr3-panel1`:

| | Vendor | Governor |
| --- | --- | --- |
| 0.3's law | 164.3 fps, 75% clean, 1.79 late frames a second, 7.9 W | 166.05 fps, every second clean, 8.1 W |
| 0.4 | 164.5 fps, 69% clean, 1.54 late frames a second, 8.1 W | 166.06 fps, every second clean, 8.4 W |
| 0.4.1 | 163.7 fps, 70% clean, 2.35 late frames a second, 7.9 W | 166.04 fps, 99.3% clean, 9.6 W |

The governed 0.4.1 run drew 1.2 W more than 0.4's, with the GPU at 801 MHz
on average against 540 and the performance cores at 1251 MHz against 990.
The game's threads are on the prime cores here, so the new minimum is in
play; the other SuperTuxKart scene at 165 Hz in Rise drew 10.4 W against
10.5 W with it. One run each: how much of the 1.2 W is the minimum and how
much is where the governor happened to settle was not separated.

One run in each campaign is void and was repeated: the panel left 165 Hz
under the running game (to 60 Hz 95 seconds into a governed run the first
time, during a vendor run the second), with display calibration off both
times. The bench marks such runs ("panel at the end: NOT on rate"). This is
the system's refresh setting acting by itself, seen on 11.0.7 as well
([history](history.md#open-problems)).

### An emulator on the NTSC choice

Super Mario Galaxy's heavy scene for two minutes with display calibration on
NTSC, the panel at 59.940 Hz by Android's present times: `mr1-ntsc1`, and
`mr2-ntsc1` and `mr3-ntsc1` on the 0.4 and 0.4.1 releases with calibration
going through the display driver's command files.

| | Speed | Frames shown for one refresh | For two |
| --- | --- | --- | --- |
| Vendor, Balance | 73%, 73%, 73% | 3,428; 3,377; 3,471 | 2,023; 2,041; 2,036 |
| Governor, Balance | 100%, 100%, 100% | 7,470; 7,764; 7,393 | 2; 2; 4 |
| Governor, Balance, factory timing (`mr1-real1`, 67 s) | 100% | 4,012 | 22 |

With the governor keeping the game on time and the panel at the game's own
rate, two to four frames in two minutes were shown twice, three times of
three. On the factory panel the same governed scene shows one twice every
three seconds, which no clock setting can remove.

**A 29.97 fps game.** Shadow of the Colossus in 90 s runs, `mr2-sotc-ntsc1`
and `mr3-ntsc1`:

| | Factory panel | Panel on NTSC |
| --- | --- | --- |
| Governor, Balance | 93-99% clean | 100% five times of five; in three, every frame shown for exactly two refreshes |
| Governor, Rise (0.4.1) | 92-98% | 100, 98, 100% |
| Diablo | 59-67% | 100, 100, 7, 94, 100% |

On the factory panel the game's 29.97 frames a second and the panel's 30.136
pairs of refreshes slide past each other once every three seconds. Under the
governor that costs one frame held a refresh longer. In Diablo each crossing
is a burst of frames held three refreshes and one by turns (110 and 90 of
them in 97 s), which is what its 59-67% is made of.

With the rates matched there is no crossing, and the frames stay wherever
they fell when the scene started. In Diablo they twice fell on the edge of a
refresh: once for the first ten seconds of a run, and once for all of one
(491 frames held one refresh and 492 held three, where every other frame of
2,910 should have been two; the emulator's frames were finished 17.7 ms
before they were shown, against 23.6 and 30.6 ms in the runs that held).
Under the governor, in Balance or Rise, 25 runs on NTSC had 0 to 31 such
frames each, 3 in the middle one. So matching the panel takes away Diablo's
regular trouble with this game and leaves an occasional worse one. Why
Diablo's frames sit where they can flip and the governed modes' do not was
not looked into.

## Not measured

- A commercial 3D game (Real Racing 3 and Bullet Force need the Play Store
  signed in).
- Play on battery, unplugged, over Wi-Fi; every run so far was on the USB cable.
- A game at 120-165 Hz for longer than 30 minutes; a stream longer than 20.
- Input latency; a controller; Wi-Fi under load for streaming.
- Eco mode beyond one synthetic run.
