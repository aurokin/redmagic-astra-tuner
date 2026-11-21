# RedMagic Astra Tuner

Smooth frame rates on the RedMagic Astra without Diablo mode, and a screen
that refreshes at the rate it claims to.

One Magisk module with two parts and an app to switch them:

- **The governor.** The Astra's own frame governor saves power by letting
  games run right on the edge of their deadline, so emulators in Balance or
  Rise stutter and drop frames, and Diablo fixes it only by pinning every clock
  at the top. The governor watches every frame a game shows and gives it the
  lowest CPU and GPU clocks that keep it on time: Diablo's smoothness at a
  fraction of its power and heat.
- **Display calibration** (experimental, off until you switch it on). The
  panel's "60 Hz" is really 60.27 Hz, and its other modes are fast by the same
  share, so 60 fps video and every emulator repeat or drop a frame every few
  seconds. Calibration corrects all five modes, and lets you match the 60 Hz
  mode to what you are playing: 60.000, 59.94 (NTSC consoles), 59.73 (Game
  Boy) or 60.10 (NES).

| Super Mario Galaxy, heaviest scene | Smooth seconds | Speed | Power |
| --- | --- | --- | --- |
| Balance, stock | 0% | 71% | 3.7 W |
| Diablo | 100% | 100% | 7.5 W |
| Balance, with the governor | 100% | 100% | 4.5 W |

| Refresh mode | Stock panel | Calibrated |
| --- | ---: | ---: |
| 60 Hz | 60.272 Hz | 60.000 Hz (or 59.940, 59.727, 60.099) |
| 90 Hz | 90.408 Hz | 90.000 Hz |
| 120 Hz | 120.544 Hz | 120.000 Hz |
| 144 Hz | 144.653 Hz | 143.997 Hz |
| 165 Hz | 166.056 Hz | 164.999 Hz |

## Is it for you?

You need:

- a **RedMagic Astra (NP05J)** on the global **RedMagicOS 11.0.7** or
  **11.0.8MR1**; it was built and measured on one tablet, on those two builds;
- an **unlocked bootloader and root with Magisk** (20.4 or later).

The module refuses to install on any other device. On another RedMagicOS
build the governor runs, unmeasured, and display calibration stays at factory
timing: its values were measured at the panel on those two builds only.

## Install

1. Download `redmagic-astra-tuner-<version>.zip` from the repository's
   [Releases](https://github.com/aurokin/redmagic-astra-tuner/releases) page (or build
   it with `sh module/build-zip.sh`).
2. In the Magisk app: **Modules → Install from storage**, pick the zip. It
   installs the module and its app, both named **RedMagic Astra Tuner**.
3. Reboot. The governor starts about 15 seconds after the tablet has booted.

To update, install the new zip the same way. To remove everything, remove
**RedMagic Astra Tuner** in Magisk's Modules list and reboot: the governor, calibration and
the app all go, and the panel is back at factory timing. To pause both parts,
switch the module off there: within a few seconds the governor hands the
clocks back and the panel returns to factory timing. Switch it back on and
reboot to resume.

## Using the governor

It is on after installing. The app's **Governor** pane shows what it is doing
and has its switch, which is remembered across reboots.

- **Games must be in Game Space.** The governor only takes over in apps that
  Game Space treats as games, outside Diablo. Add an emulator or game to Game
  Space if it isn't there.
- **Pick Balance or Rise per game, not Diablo.** Balance is enough for most
  emulators, streaming and lighter games. Try Rise for the heaviest games (PS2,
  demanding GameCube and Wii scenes): it keeps more headroom and the vendor's
  heat limits start later. God of War II and F-Zero GX gained from it;
  Shadow of the Colossus is as good in either since 0.4.1. In Diablo the
  governor stands aside, and some 30 fps games pace worse there than in
  either governed mode.
- **Refresh rate per game.** Game Space starts each game at 60 Hz unless you
  change it with the refresh button in the in-game overlay. The governor holds
  120, 144 and 165 Hz games at full rate too.
- **Streaming** (Moonlight, Artemis): add the app to Game Space and use
  Balance. The governor recognises a stream and keeps the clocks low.
- **Outside games** it only keeps the performance cores from idling so low
  that video drops frames. No setup needed.

## Using display calibration

Open the app's **Display** pane. The large number is the panel's real refresh rate,
measured as you look at it. Switch on **Calibrate refresh rates** and choose
what the 60 Hz mode should match:

| Choice | Rate | For |
| --- | ---: | --- |
| Modern | 60.000 Hz | Android games, Switch, 60 fps video, game streaming |
| NTSC | 59.940 Hz | GameCube, Wii, PlayStation 2, PSP, Dreamcast, TV video |
| GBA | 59.727 Hz | Game Boy, Game Boy Color, Game Boy Advance |
| NES | 60.099 Hz | NES and Super NES |

The 90, 120, 144 and 165 Hz modes are corrected with any of them.

- **It is reapplied after every boot and every wake**, a few seconds after the
  screen comes on (about a minute after a boot). You may see one brief
  flicker as it takes effect.
- **The choice applies when Android selects 60 Hz.** If the system's refresh
  setting holds a game at 120 Hz, pick 60 Hz for that game in Game Space's
  overlay.
- **To go back**, switch it off with the screen on and wait for **Factory
  timing**. A reboot with it off also does it; nothing is stored in the panel.
- **Why it is experimental.** It is five days old and has run on one tablet.
  During development that tablet restarted while waking twice. The cause was
  found (a calibration packet reaching the panel as it powered down) and
  fixed in 0.4: some 2,170 calibration runs cut short on purpose by switching
  the screen off, 88 of them with a packet inside a millisecond of the
  panel's power-down, gave no restart, where the 18th had before. What that
  cannot show is other tablets, or months of ordinary use. If the tablet does
  restart by itself with calibration on, calibration stays off afterwards and
  the app says so, until you switch it off and on again (0.4.1). If that
  happens to you, please report it;
  [docs/display.md](docs/display.md#restarts-on-waking-found-and-fixed) has
  the whole story.

## What to expect

Measured on the tablet against the stock behaviour in the same mode
(details in [docs/results.md](docs/results.md)):

| | Stock | With the module |
| --- | --- | --- |
| Emulator scenes at 60 Hz, Balance | 7 of 16 scenes below 90% smooth seconds | 15 of 16 at 88% or better |
| SuperTuxKart at 165 Hz, 30 minutes | 1.35 late frames a second | 99.9% smooth seconds |
| Games at 120 and 144 Hz | occasional late frames | every second smooth |
| Game streaming | the same frames | the same frames, decoded up to 1.6 ms faster, same power |
| 60 fps video outside games | 58.8 fps | 59.8 fps, no measurable power cost |
| 60 fps video in VLC, 30 seconds, calibrated to Modern | 32-57 long frames | none in 8 of 12 captures |

Against Diablo, the same smoothness in nearly every game at half to two thirds
of the power.

## Good to know

- **The first two or three minutes after starting a game** use 1.5-2 W more
  than later, while the governor works down from ample clocks.
- **In Rise it stays under the vendor's heat cut** where it can: God of War II
  ran 28 minutes without the cut, three times out of three, at 1.4 W less than
  without this heat control (which reached the cut after 18-26 minutes).
- **A game too heavy for the GPU at your refresh rate** (an Android game at
  maximum settings at 165 Hz) still hits the tablet's heat limit; the governor
  cannot make the tablet cooler. Cap the game's frame rate or lower its
  settings.
- **Without calibration, a 59.94 fps emulator repeats one frame about every
  three seconds** in any mode: that is the 60.27 Hz panel, not the governor.
- **The system's Auto refresh setting can pull a 165 Hz game down to 120 Hz**
  on 11.0.7, when the Refresh Rate quick-settings tile refreshes. That is the
  vendor's software; see
  [docs/display.md](docs/display.md#the-vendors-refresh-setting).
- **Faster fan speeds don't help**: running the fan at full speed changed no
  temperature in three tests, so the governor leaves the fan to the system.

## Safety

**The governor** only sets clock limits that the kernel already exposes, and
only at runtime: a reboot clears every value. It never raises a clock above
the vendor's own limits, never touches thermal protection, the bootloader or
the firmware, and hands every setting back when a game ends, when you switch
to Diablo, or if it stops.

**Display calibration** writes ten timing values to the panel's controller
over the display link, the same link the tablet's own driver uses. They live
in the controller's working memory: the panel forgets them whenever it powers
down, and nothing is ever written to its flash. It only runs on the two
firmware builds it was measured on, checks the controller's identity first,
and stops until the next sleep and wake if anything reads back wrong. Its
packets go through the display driver's own command path, which the driver
locks against the panel powering down. It is the newer and less proven half;
see why it is experimental above.

Removing the module returns the tablet to stock behaviour after a reboot.

## More

[docs/](docs/README.md) explains how both parts work, how the RedMagic system
decides clocks and heat limits, the display, every measurement, and how to
develop and test changes.

## License

[MIT](LICENSE): use it, change it and ship it, in open or closed software,
as long as the copyright notice stays with it. That covers the code, the
documentation and the measurements. Bringing this to other devices is
welcome, and so is RedMagic taking whatever helps the stock software.

The one file that is not ours is
`module/zip/META-INF/com/google/android/update-binary`, the standard installer
stub every Magisk module carries, from the [Magisk](https://github.com/topjohnwu/Magisk)
project.
