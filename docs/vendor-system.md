# The vendor's system

How RedMagicOS 11.0.7 (NP05J, global build `REDMAGICOS11.0.7_NP05J_GB`,
Android 16, kernel 6.6.92 GKI) decides clocks, heat limits, the fan and the
panel's refresh rate, as far as it was observed on one tablet. Everything here
was read from the running system or measured; where a behaviour is inferred
rather than seen, it says so.

[How it works](how-it-works.md) explains where the governor sits in this.

## Hardware the software controls

| Part | What the kernel exposes |
| --- | --- |
| CPU, six performance cores (cpu0-5) | `cpufreq/policy0`, 384 to 3532.8 MHz, `walt` governor |
| CPU, two prime cores (cpu6-7) | `cpufreq/policy6`, 1017.6 to 4320 MHz (the vendor keeps the minimum at 1017.6), `walt` |
| GPU, Adreno 830 | `kgsl-3d0`: 1100, 967, 900, 832, 734, 660, 607, 525, 443, 389, 342, 222, 160 MHz as power levels 0-12 (level 0 is the fastest); `min_pwrlevel`, `max_pwrlevel`, `thermal_pwrlevel` |
| Fan | `/sys/kernel/fan/`: `fan_enable`, `fan_speed_level` 1-5, `fan_speed_count` (rpm) |
| Panel | 2400x1504 OLED, refresh modes 60, 90, 120, 144 and 165 Hz; see [display.md](display.md) |
| Board temperature | thermal zone `xo-therm`: the sensor the vendor's heat limits act on |

## The layers, top to bottom

```
Game Space mode (Eco / Balance / Rise / Diablo, per game)
  └─ sets ceilings, where the heat ladder starts, and whether IFAS runs
IFAS (kernel module ifas): the vendor's frame governor, games only
  └─ lowers a CPU cap while frames are on time
walt (CPU) and KGSL devfreq (GPU): the stock governors
  └─ pick the actual clock between the minimum and maximum they are given
thermal-engine: cuts the maximums on the board sensor
```

Every limit is a separate request on the same sysfs node, and the lowest
maximum wins. A root process can add its own request; it cannot remove the
vendor's.

## Game Space and its modes

Game Space (`cn.nubia.gamelauncher`, with the in-game assistant
`cn.nubia.gameassist`) keeps a list of apps it treats as games, in an SQLite
table (`/data/user_de/0/cn.nubia.gamelauncher/databases/appadd.db`, table
`appadd`). Only an app with a row there gets a "game scene": a per-game mode,
the fan, the per-game refresh rate and IFAS. Editing the settings that mirror
the list is not enough.

The mode of each game is in `settings global NubiaperformanceMode`, a list of
`package+N00,` entries. The first digit is the mode:

| Digit | Mode | CPU ceilings (perf / prime) | GPU ceiling | First heat cut at (board) | IFAS |
| --- | --- | --- | --- | --- | --- |
| 1 | Eco | 1786 / 1402 MHz | 342 MHz | (not measured) | on |
| 2 | Balance | 2918 / 3072 MHz | 832 MHz | 41 C | on |
| 3 | Rise | 3532 / 4320 MHz (the hardware's top) | 1100 MHz | 49 C | on |
| 4 | Diablo | every clock pinned at the top | pinned | (ladder still applies) | off |

The GPU ceiling per mode is fixed: in 515 recorded runs it never moved during
a run. IFAS has a GPU policy of its own (`enable_gpu_policy`) that could move
it; the vendor ships it switched off. The other two digits of the setting do
not select the refresh rate (tried).

An app with no entry runs in Balance. An app that is not in the list at all is
not a game: IFAS stays off, both clusters keep their full ceilings and `walt`
alone picks the clocks.

**Diablo quirk.** Leaving Diablo for another mode, the vendor keeps Diablo's
GPU limits (floor 832 MHz, ceiling 1100 MHz in Balance; pinned at 1100 in
Rise) until the mode changes once more. Switching to Diablo, the vendor pins
the clocks first and reports the mode afterwards.

**Apps listed but skipped.** GravityMark is in the list and gets a game scene
(mode, fan, 60 Hz panel), but `ifas_enable` stays 0. Moonlight showed the same
on its list of computers; Artemis had IFAS on.

## IFAS, the vendor's frame governor

A kernel module (`/sys/module/ifas/parameters/`, about 35 parameters, and
`/sys/kernel/ifas/`). It runs only while a Game Space game is in front outside
Diablo; `/sys/kernel/ifas/ifas_enable` reads 1 then. That flag is the most
reliable "a game is in front" signal on the tablet.

Its tool is a cap on the CPU (`cpu_max_freq_limit_cluster*`), and it takes the
cap a long way down: with the vendor alone, light scenes were recorded with
ceilings of 749/1018 MHz in Balance and in Rise. It lowers the cap
after every 8 on-time frames (`normal_frame_keep_count`) and raises it only
when five frames average worse than about 56.6 fps. Between roughly 94% and
100% of the target rate it does nothing. The result is a game held on the edge
of its deadline: in the first measurements, Super Mario Galaxy's heavy scene
ran at 71-75% speed in Balance with the GPU at its lowest clock, 48% busy.

It does not set minimum clocks, and in this firmware it does not move the GPU
ceiling (its GPU policy is off; the ceiling is the mode's).

## The stock CPU and GPU governors

`walt` raises a cluster's clock on load, a frame late for a burst. The GPU's
devfreq governor looks only at utilisation. In an emulator the busiest thread
and the GPU take turns within a frame, so neither looks busy while the frame
is late: the GPU sat at 222 MHz, 48% busy, while the game ran at 65-71% speed.
With IFAS's cap removed, `walt` showed the same blind spot, parking the cluster
near 1 GHz with the render thread 68% busy.

## Heat: thermal-engine

`/vendor/etc/thermal-engine.conf` watches the board sensor `xo-therm` and cuts
the maximums in steps, giving each back two degrees lower. As the file has it
(the Balance ladder):

| Board | CPU ceilings (perf / prime) | GPU ceiling |
| --- | --- | --- |
| 41 C | 2400 / 2842 MHz | 832 MHz |
| 43 C | 2227 / 2650 | 607 |
| 45 C | 1997 / 2438 | |
| 47 C | 1786 / 2246 | 525 |
| 49 C | 1786 / 1958 | |
| 51 C | 1555 / 1690 | |

The mode decides where the ladder starts, not how much heat the mode makes. In
Rise the first cut came at 49.0 C (four cuts out of four), straight from
3532/4320 to 2400/2842 MHz. In Balance it is 41 C: Balance's lower ceilings
make less heat (7 W against 10 W in God of War II), but 7 W still takes the
board past 41 C in a quarter of an hour.

The GPU is capped separately (`thermal_pwrlevel`): in Rise, at 50.2 C the GPU
went to 832 MHz and came back at 48.1 C. Skin and junction temperatures do not
predict any of it; only the board sensor does.

Outside games the ladder is still in force and slow to let go: after a hot
game, ceilings of 1997/1690 MHz at 48.6 C were still 2227/1958 MHz six and a
half minutes later at 36.5 C. An ordinary app opened after a hot game runs on
about half its usual ceilings for several minutes.

## The fan

The cooling fan app is `cn.nubia.fan`. In a Game Space game the vendor switches
the fan on at level 2 (about 9,000 rpm), raises it to level 4 when the tablet
is hot (seen at a board temperature of 49 C) and switches it off when the game
leaves the screen. Its settings:

| Setting | Values |
| --- | --- |
| `system fan_state_of_mode` | 1 or absent: "Balanced heat dissipation" (the vendor's own choice of speed); 0: "Fast cooling" |
| `system fan_state_of_manual` | 100 / -100: switched on / off by the system for a game; 1 / 0: switched on / off by hand |
| `global game_fan_off_on` | 1 while the fan is on |
| `cn.nubia.fan` preference `switch_cooling_fan` | the "Cooling fan" switch |
| `/sys/kernel/fan/fan_enable`, `fan_speed_level`, `fan_speed_count` | what it is doing |

Running the fan faster than the vendor does changed nothing measurable in
three tests; see [results.md](results.md#heat-and-the-fan).

## Refresh rate

There are two separate settings.

- **The system setting** `settings system refresh_rate_mode`: 0 auto, 1 60,
  2 90, 3 120, 4 144, 5 165. On auto, ordinary apps and video players get
  120 Hz. On 165, ordinary apps get 165 Hz and video players and Chrome 120 Hz.
  It does not reach a Game Space app.
- **The per-game rate.** Game Space starts each listed app on a rate kept in
  the assistant's settings, `custom_defined_fps_<package>` in
  `/data/user_de/0/cn.nubia.gameassist/shared_prefs/data.xml`, and 60 Hz for an
  app with no entry, whatever the system setting says. The refresh button of
  the in-game overlay writes it. The assistant rewrites the file from memory,
  so it has to be stopped before the file is edited.

**The vendor lowers a game's rate when hot.** A third component,
recorded in the global setting `ScreenRefreshRateController_resultFps`, took a
165 Hz game to 120 Hz with the board (`xo-therm`) at 48.5-50 C, in four 30-minute
runs out of four on 2026-11-18; Android's display votes did not change. On
11-16 the same race held 165 Hz for 15-20 minutes at 50.1-50.5 C, so the
trigger is not the board sensor alone; what else it reads is not known. It
only drops to 120: a game set to 120 stays there.

Twice the system setting was found switched to 60 Hz by itself, once after a
sequence of mode changes including Diablo and once between two test sessions
with no game starting or stopping. The cause is not known.

## Other vendor behaviour that affects measurements

- **Chrome cap.** With Chrome in front the vendor lowers the CPU ceilings to
  1152/1402 MHz within a few seconds and again about ten seconds after each
  touch. No other app tried gets this; where it is configured was not found.
  It is not applied to Chrome when Chrome is a Game Space app, and it cost
  nothing in the web game and video tried.
- **Non-game clocks.** Outside games the six performance cores idle near
  570 MHz (minimum 384 MHz) and the prime cores are held at 1017.6 MHz.
- **Charge separation** (`com.zte.powersavemode`, `settings global
  charge_separation_switch`): runs the tablet from the charger without charging
  the battery. It switches itself off after five minutes with the screen
  locked unless that is turned off. When the tablet is warm and charging, the
  vendor puts up a page offering it, in front of the game.
- **Services** without diagnostic output: `performance`,
  `VendorPowerManagerService`, `scenedecision`, `gamecubeservice`,
  `zperfcubeservice`. Their Binder interfaces were not probed.

## Where these were measured

The per-mode GPU ceilings come from every recorded run (see
[data/](../data/README.md)); the heat ladder from `thermal-engine.conf` and the
`heat*` campaigns; the fan settings from switching each control by hand and
watching what changed; IFAS's behaviour from the first governor's
investigation, summarised in [history.md](history.md).
