# The display

The Astra's panel refreshes slightly faster than each of its modes claims.
This page is what that means for games and video, how display calibration
corrects it, what was measured, and what is not yet settled.

## In one minute

- **The fault.** The five refresh modes run 0.45% fast (0.64% at 165 Hz):
  "60 Hz" is 60.272 Hz. Content at exactly 60 or 59.94 frames a second cannot
  keep step with it, so a frame is repeated or dropped every few seconds, in
  every performance mode.
- **The correction.** The panel's timing controller keeps one horizontal and
  one vertical timing value for each mode. Calibration writes ten corrected
  values after every boot and wake, then has Android switch modes for a
  moment so the controller takes them up. Nothing is written to the panel's
  flash, and a panel power-down clears it.
- **The result.** Every mode within 0.004 Hz of its nominal rate, measured at
  the panel's own timing signal, on both supported firmware builds. The 60 Hz
  mode can instead be matched to 59.94, 59.73 or 60.10 Hz for consoles that
  ran at those rates.
- **Restarts on waking, found and fixed.** Twice the development tablet
  restarted while waking with calibration in use. The cause was a calibration
  packet reaching the display link as the panel powered down; calibration now
  sends its packets through a path the driver locks against that. It stays
  off until switched on, and marked experimental: all of this is days of
  work on one tablet.

## The panel's real refresh rates

Measured at the controller's tearing-effect pin (kernel GPIO 598, one pulse
per refresh) against the tablet's own monotonic clock, with every edge fitted
over two five-second windows:

| Mode | Factory | Above nominal | Calibrated, 11.0.7 | Calibrated, 11.0.8MR1 |
| --- | ---: | ---: | ---: | ---: |
| 60 (Modern) | 60.2722 Hz | 0.453% | 59.99998 Hz | 59.99968 Hz |
| 90 | 90.4085 Hz | 0.453% | 89.99988 Hz | 89.99954 Hz |
| 120 | 120.5442 Hz | 0.453% | 119.99964 Hz | 119.99972 Hz |
| 144 | 144.6524 Hz | 0.453% | 143.99639 Hz | 143.99627 Hz |
| 165 | 166.0558 Hz | 0.640% | 164.99855 Hz | 164.99893 Hz |

The factory rate is the same in every vendor mode, Diablo included, in games
and on the home screen, on both firmware builds, and it does not wander.

The other three choices for the 60 Hz mode:

| Choice | Aimed at | Measured, 11.0.7 | Measured, 11.0.8MR1 |
| --- | ---: | ---: | ---: |
| NTSC | 59.94006 Hz (60000/1001) | 59.94006 Hz | 59.94018 Hz |
| GBA | 59.72750 Hz | 59.72711 Hz | 59.72696 Hz |
| NES | 60.09881 Hz | 60.09911 Hz | 60.09909 Hz |

**How good is the reference?** The pin is timed by the tablet's clock, which
ran 32-38 parts per million fast against the development Mac's. On the Mac's
clock the Modern rate is about 60.002 Hz. An audio clock or a video source
has its own small error of the same order, so the last digits above are
finer than anything content can be matched to.

## What that means for content

| Content | On the factory panel | Calibrated |
| --- | --- | --- |
| A native game paced by Android | runs at the panel's rate (60.27, 166.06...) | runs at 60.00, 165.00... |
| An emulator at 59.94 fps (GameCube, Wii, PS2) | one frame repeated about every 3 s | **NTSC**: in step. Modern: one repeat about every 17 s |
| Game Boy and GBA at 59.73 fps | one repeat every 1.8 s | **GBA**: in step |
| NES and Super NES at 60.10 fps | one repeat every 5.8 s | **NES**: in step |
| 60.000 fps video | at 60 Hz, 15-57 long frames in 30 s; at 120 Hz one long hold every 3.7 s | **Modern**: in step |
| 59.94 fps video | 14-18 long frames in 30 s | **NTSC**: in step |
| Game streaming at exactly 60 or 120 | drifts against 60.27 / 120.54 | **Modern**: in step with a host at 60.000 or 120.000 |
| 50 fps content (PAL) | uneven at every mode | still uneven: no mode is a multiple of 50 |

"In step" means the panel no longer forces a repeat; a game can still miss a
frame on its own. The video rows and the 59.94 fps emulator row were measured
as frames presented; the Game Boy and NES rows are arithmetic from the
measured rates ([what is not measured](#measured-and-not-measured)).

The governor judges lateness against the rate the panel is actually running
at, so it works the same with calibration on or off, and the test bench does
not count repeats the panel forces against a run.

## How calibration works

The panel's timing controller is a Himax HX8874 (device-tree node
`qcom,mdss_dsi_himax_hx8874_9p06_dsc_cmd`: command mode, dual DSI, display
stream compression). It times the refresh itself, on its own clock. The tablet
only asks it for one of five rate classes.

Inside the controller, block `0xE781` holds a horizontal and a vertical
timing word for each class, ten words in all. Three things had to be found
before changing them did anything, which is why the first three days of
investigation concluded that the rate could not be changed
([history](#how-it-was-found)):

1. **A timing word only reads back changed after two more writes**
   (`0xE781016C = 1`, then `0xE781017C = 0`). Without them the controller
   keeps the factory value.
2. **The controller only takes up a mode's words when that mode is selected.**
   The mode in use keeps its old timing until the tablet switches to another
   mode and back.
3. **The vertical word alone moves the rate in coarse steps.** The horizontal
   word, changed four counts at a time, gives the fine ones. Each rate is the
   closest pair found near the factory values.

So calibrating is: write the ten words, each with its two follow-up writes
and a read-back; then, if the mode on screen was among those changed, ask
SurfaceFlinger for the other of 60 and 120 Hz for as long as it takes to see
the mode change, and put the display policy back exactly as it was. Android's
saved refresh setting is never touched. The higher modes take up their words
whenever Android next selects them.

The controller forgets all of it when the panel powers down, which happens at
every screen-off. Calibration is therefore reapplied after every wake.

### The parts

| Part | What it does |
| --- | --- |
| `panel-service` (Go, root) | decides when: waits for boot and for a display that has been steadily on, runs the helper once per wake, and reports its state on `127.0.0.1:7879/calibration` (read-only) |
| `panel-table.sh` (root) | the only thing that talks to the controller: checks, writes and verifies the ten words through the display driver's command files (`tx_cmd` and `rx_cmd` of connector `DSI-1` in debugfs, which it mounts at `/dev/astra-panel-debug`), keeping a journal of what it owns |
| `panel-mode.jar` (`PanelMode.java`, run with `app_process`) | the momentary mode change through SurfaceFlinger, with a receipt so an interrupted one is undone |
| The app | saves the owner's choice in a file in its device-protected storage; the service reads it. The app has no access to the panel |

### When it runs

| Moment | What happens |
| --- | --- |
| Boot | waits for Android to finish booting, then 30 s, then for the display to have been on for 8 s; about 67 s from the reboot request to **Calibrated** in two measured boots |
| Wake | 2 s after the display is steadily on, the helper runs for 4-8 s (2-6 s before 0.4; the driver's command files are slower per packet); one helper run per wake |
| In use | nothing: no reads, no writes. Mode changes use the words already in the controller |
| Choice changed in the app | applied within about a second while the screen is on |
| Switched off in the app | the factory words are written back and verified, then nothing more |
| Module switched off or removed in Magisk | treated as "switched off": factory timing is restored while the tablet is still running |
| Screen off | nothing is sent to the panel |

### What keeps it safe

The first days of work on this controller cost several kernel panics, all
from register traffic at the wrong moment ([hazards](#hazards)). The service
is built around not repeating them:

- **Under the driver's own lock.** Every packet goes through the display
  driver's command files, which hold the lock the driver takes to power the
  panel up or down for the length of the transfer, and refuse a packet once
  the display link is off. A packet and a power-down cannot overlap
  ([what happened when they could](#restarts-on-waking-found-and-fixed)).
- **Only on a fully-on panel.** The helper also checks the display's power
  state before every single transfer and stops at the first one that is not
  "on".
- **Only on a known tablet and firmware**: model NP05J, builds
  `REDMAGICOS11.0.7_NP05J_GB` and `REDMAGICOS11.0.8MR1_NP05J_GB`. The timing
  words were measured on those; on anything else it leaves the panel alone
  and says so.
- **Only a controller that identifies itself** (chip ID `0x0008874B`), and
  only words it recognises: a value in the controller that is neither factory
  nor one of its own stops it.
- **A journal** records each word before it is written, so an interrupted run
  is finished or undone at the next wake rather than guessed at. Each update
  is on disk before it replaces the last, and a journal that cannot be read
  is dropped only if the tablet has restarted since it was written, when the
  controller is at factory timing whatever it said.
- **A guard file** is put down, synced, before the first transfer and removed
  after the last. If the tablet dies in between, the service finds the guard
  at the next start and does nothing until the screen has been fully off and
  on again.
- **After any error it stops** until the next full sleep and wake, rather
  than retrying against a display link that may be unwell.
- **After a restart by itself it stays off** (0.4.1). When Android's reason
  for the last boot is a kernel panic or a watchdog and calibration was
  switched on, the service leaves the panel at factory timing and the app
  says why, until calibration is switched off and on again or another rate
  is chosen. The service cannot tell whether calibration caused the restart;
  it assumes so once, so that a fault of its own costs one restart and not
  one at every wake. The two restarts calibration did cause read
  `kernel_panic` and `watchdog_apps_bark`. A reset forced with the power key
  reads as neither and does not trip it.
- **Four-byte reads only**, and only the registers named above.

## Measured and not measured

Measured, on both firmware builds unless noted:

- **Every rate at the pin**, above, and again after an ordinary wake.
- **Factory timing back** after switching off: 60.2721 Hz at the pin, all ten
  words read back as factory.
- **60.000 and 59.94 fps video in VLC, 30 s each.** On factory timing the two
  clips had 15-16 and 14-18 presentation intervals longer than 1.5 times the
  median; matched, each had none. In twelve later 60 fps captures on
  11.0.8MR1, eight had none against 32-57 on factory timing; two had 227 and
  430, with VLC reporting normal speed and the next capture on the same boot
  clean. The cause of those two stalls is not known.
- **Four sleep-wake cycles**: one helper run each, **Calibrated** each time,
  no display-link errors in the kernel log.
- **A game opened 1.0 and 3.5 s after waking** (SuperTuxKart at 165 Hz in
  Game Space, system setting on Auto): 165 Hz held, 164.998 Hz at the pin.
- **An interrupted run**: with the guard file left in place, the service
  stayed paused through a boot and resumed after one sleep and wake.
- **2,169 runs cut short on purpose** by switching the screen off at a
  random moment inside them: 170 on 2026-11-19 and 1,999 overnight on
  2026-11-20 with the 0.4.1 release (11.0.8MR1, one boot of nine hours). No
  restart, and no display-link, storage or interrupt fault in the kernel
  log. 88 of them had a packet requested within a millisecond of the panel's
  off command, 160 within two; 201 packets arrived after the link was off
  and were refused by the driver. A wake left alone after each test ended
  **Calibrated** at 60.000 Hz, and switching off gave factory timing back
  ([the comparison](#restarts-on-waking-found-and-fixed)).
- **The hold after a restart by itself** (0.4.1, 11.0.8MR1), with the boot
  reason set to `watchdog_apps_bark` by hand and calibration on: the service
  started in **error** with the reason in its message and sent nothing to the
  panel; stopped and started again in the same boot it still held; switched
  off it read **Factory timing**; switched on again it calibrated, and
  another start of the service in that boot did not hold it a second time.
  A real panic or watchdog restart was not provoked for it.
- **A 59.94 fps emulator on NTSC** (11.0.8MR1, campaign `mr1-ntsc1`): Super
  Mario Galaxy's heavy scene under the governor for two minutes showed 7,470
  frames for one refresh each and 2 for two. On factory timing the same scene
  repeats a frame every three seconds whatever the clocks do: 22 in 67
  seconds in `mr1-real1`. Android's present times put the panel at 59.940 Hz
  during the run.
- **A 29.97 fps emulator on NTSC** (0.4 and 0.4.1; `mr2-sotc-ntsc1`,
  `mr3-ntsc1`): Shadow of the Colossus under the governor, every second
  clean five times of five in Balance and 98-100% in Rise, against 92-99%
  on factory timing. In Diablo three runs of five were clean throughout and
  one had its frames on the edge of a refresh from start to end, 7%
  ([results](results.md#an-emulator-on-the-ntsc-choice)).
- **Through the app, on the first 0.4 zip** (the vendor's nodes): on, Modern,
  NTSC, a sleep and wake, and off again, with the app's own meter reading
  60.27, 60.00, 59.94 and 60.27 Hz and no display-link error in the kernel
  log.
- **On the released 0.4 zip** (the driver's command files), installed
  through Magisk and rebooted: the same sequence by the saved choice, with
  Android's measure of the refresh period at 60.000, 59.940, 59.940 after
  the wake, and 60.272 Hz; no display-link error. Then Super Mario Galaxy on
  NTSC for two minutes (`mr2-ntsc1`): the panel at 59.940 Hz, 2 frames shown
  twice.

Not measured:

- **Light from the panel.** The pin is the controller's timing signal, not a
  photodiode on the glass.
- **The GBA and NES choices with an emulator**, and the NTSC one with any
  game but the one above.
- **Battery cost**, which should be nil: nothing runs once the words are in.
- **Weeks of use.** Everything above is from five days, on one tablet.
- **The rates at the pin with the changed transfer path.** The ten words are
  the ones measured there; what was checked after the change is that they
  read back and that Android's own measure of the refresh period goes to
  60.000 Hz and back to 60.272.

## Restarts on waking: found and fixed

Twice the tablet restarted by itself while waking: once on 11.0.7 (2026-11-18)
and once on 11.0.8MR1 (2026-11-19). Both times it came back up by itself.
After the second, a game's settings file and one setting written seconds
before had to be put back; nothing else was lost. Until 0.4 calibration was
the suspect and nothing more: a matched test of 24 wakes with it and 24
without, and 16 more without, reproduced neither.

**The cause.** Until 0.4 the helper wrote through the vendor kernel's
`/sys/reg_debug` nodes. In the display driver (`msm_drm.ko`, the same file on
both builds) a write through them checks one "panel is initialised" flag and
sends the packet without taking any lock; the driver switching the panel off
takes its lock and does not wait for such a packet. The helper checked the
display's state before each packet, which leaves the same gap. A helper run
took 2-6 s after each wake, so a screen that went off again within seconds of
coming on could catch a packet in flight.

**Reproducing it.** With the screen switched off on purpose at a random moment
inside each helper run, the 18th such run put a packet into the driver 0.6 ms
before the panel's off command. Nothing was logged then. At the next wake, 30 s
later, the display's interrupt fired four million times in 15 s; one core did
nothing else; storage, the touchscreen and the charger timed out one after
another, and the watchdog restarted the tablet:

| Since screen-on | Kernel log |
| --- | --- |
| 0.57 s | an I2C transfer fails |
| 0.70 s | storage: power-mode command timed out, Hibern8 entry failed |
| 1.3-4.0 s | `dsi-ctrl-0: dma_tx done but irq not triggered`, three times |
| 3.7 s on | touchscreen and charger transfers time out |
| 14.96 s | `IRQ 403 [GICv3:msm_drm] - 4017678 times`, watchdog restart |

The restart of 11-19 has the same opening (storage timeout 0.62 s after
screen-on, then touch and charger) and the one of 11-18 the same ending;
both ended in `rpmh-rsc.c:1106`, a power request that timed out, before the
watchdog got there. Both boots had a helper run cut short by the screen
going off earlier.

**The fix.** The helper now sends every packet through the display driver's
own command files, `tx_cmd` and `rx_cmd` of the connector in debugfs. They
call `dsi_display_cmd_transfer` and `dsi_display_cmd_receive`, which hold the
display's lock for the whole transfer, the same lock `dsi_display_disable`
and `dsi_display_unprepare` take at entry, and refuse when the DSI host is no
longer initialised. Read from the driver's code and checked on the tablet:
with the screen off a write is refused and a read returns an error, with
nothing else in the kernel log.

**The same test against the fix.** 170 helper runs cut short the same way
that evening, and 1,999 more overnight on the 0.4.1 release
(`panel/interrupt-test.sh 2000 10`, nine hours on one boot): no restart, no
display-link error. In the first test one of the two packets within a
millisecond of the off command was followed by the hang; against the fix 88
packets came that close, the closest 4 microseconds before the off command
and 70 after it.

| | Vendor's nodes (before 0.4) | Driver's command files (0.4), 11-19 | The same, 0.4.1, overnight 11-20 |
| --- | --- | --- | --- |
| Helper runs cut short | 18 | 170 | 1,999 |
| A packet within 1 ms of the off command | 2 | 8 | 80 (52 before it, 28 after) |
| Within 2 ms | 2 | 15 | 145 |
| Packets refused because the link was off | none | 20 | 181 |
| Display-link, storage or interrupt faults in the kernel log | the hang | none | none |
| Restarts | 1 | 0 | 0 |

The kernel logged one warning that night, at 5.9 hours: the vendor's
scheduler objecting as the GPU driver's message thread woke another thread
(`android_rvh_try_to_wake_up` in `sched_walt`, called from `msm_kgsl`).
Nothing followed it and the test went on for three more hours. A calibration
run was in progress at that moment, as one was for about a quarter of the
night, and no display code is in the warning's call chain; it was not in the
kernel logs of the two earlier tests. What caused it is not known.

After each cut-short run the service reported the interruption, waited for
the next wake and started again, 1,999 times.

A second fault showed up with the restart: the journal was replaced by
renaming a file that had not reached the disk, the restart left it as 362
zero bytes, and the service then refused to run until it was deleted by
hand. Fixed as described under [what keeps it safe](#what-keeps-it-safe).

What this does not show: that these two restarts are the only way the
tablet can restart on waking (an owner of another Astra has reported
spontaneous restarts on stock firmware), or how calibration holds up over
weeks. For that reason the service from 0.4.1 keeps calibration off after
any restart Android puts down to a kernel panic or a watchdog, until it is
switched on again ([what keeps it safe](#what-keeps-it-safe)). If a tablet
goes on restarting by itself with calibration off, calibration was not the
cause.

## Hazards

Register traffic through the vendor's `/sys/reg_debug` nodes caused kernel
panics during the investigation when the display was off or powering down
(`BUG at drivers/soc/qcom/rpmh-rsc.c`, and the hang above), when a read asked
for more than 4 bytes (`BUG at lib/string_helpers.c`), and when unpopulated
blocks or the controller's RAM were read; some reads broke the display link
until a reboot. Nothing in this repository uses those nodes any more. The
driver's command files rule out the first of these; the others are about
what is asked of the controller and apply to any path. Android partition
recovery does not recover the panel controller's flash, which calibration
never writes. Any change to `panel-table.sh` should be read against the
[register rules](#where-the-rest-is) first.

## The vendor's refresh setting

Two behaviours of RedMagicOS that look like calibration faults and are not:

- **Auto can pull a 165 Hz game down to 120.** On 11.0.7 the Refresh Rate
  quick-settings tile rewrites the saved rate when it refreshes, every ten
  minutes, and Android then selects 120 Hz under a running game. With
  calibration off and factory timing confirmed at the pin it still happened.
  Removing the tile from the active tiles, or setting the system to a fixed
  165 Hz, held 165 for the eleven minutes measured. On 11.0.8MR1 the same
  tile left the game at 165, with and without calibration.
- **The preset only shows at 60 Hz.** If Auto holds an app at 120 Hz, it runs
  at a corrected 120.000 Hz and the 60 Hz choice is not in play.

## How it was found

From 2026-11-09 to 11-12 the controller's registers, Android's and the
vendor's display controls, the controller's flash image and a related Honor
tablet's firmware were all examined, and nothing moved the rate. The
conclusion then was that the timing was fixed by the controller's start-up
configuration in flash. That record, and its refuted attempts, are kept
[outside this repository](#where-the-rest-is).

On 11-18 the timing words in block `0xE781` were written again, this time
followed by the two writes the controller's own firmware makes after it sets
them, and then by a switch to 120 Hz and back. The pin moved from 60.2721 to
59.9941 Hz, held for a minute, and returned to 60.2721 Hz when the factory
word was put back. The same day the horizontal word gave finer steps, all
five modes were matched, and the service and app were written; the GBA and
NES choices followed. On 11-19 the helper stopped using Android's saved
refresh setting for the mode change (a game opened a second after waking
could lose its 165 Hz), the tablet was updated to 11.0.8MR1 and every rate was
measured again there.

## Where the rest is

Outside this repository, on the development Mac:

- `~/workspace/astra-root/governor/README.md`: the register map and the rules
  for talking to the controller, learned the hard way.
- `~/workspace/astra-root/governor/tcon-fw/`: the controller's firmware
  analysis from the first investigation.
- `~/workspace/astra-root/evidence/`: every capture behind this page.
  `astra-panel-apply-20261118` (the first change), `astra-all-rates-20261118`
  (all modes, the clock comparison, playback), `astra-auto-20261118` and
  `astra-reliability-20261118` (the service), `astra-migration-20261119`
  (11.0.8MR1), `astra-panic-20261119` and `astra-attribution-20261119` (the
  restarts).
