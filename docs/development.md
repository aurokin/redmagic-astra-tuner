# Development

Building, installing, checking and measuring a change.

## Build and test

```sh
sh module/build.sh       # test and build everything into build/
sh module/build-zip.sh   # the same, then build/redmagic-astra-tuner-<version>.zip
```

`build.sh` needs Go, Java 17 or later, Python 3 and the Android SDK's
build-tools 36.0.0 and platform 36 (`ANDROID_HOME`, by default
`/opt/homebrew/share/android-commandlinetools`). No Gradle. The same commit
and signing key give the same zip, byte for byte.

The parts on their own:

```sh
go test ./... && go vet ./...                   # the governor: control law, heat control and fan decision against models
(cd panel && go test ./... && go vet ./...)     # calibration service: readiness, retries, the preference file
python3 panel/test_panel_table.py               # calibration helper against an emulated controller
sh app/build.sh                                 # the app, build/app/astra.apk
```

The tests are the control law's specification: each encodes a behaviour that
was measured on the tablet (a starved game found and economised, a stream not
held at ample clocks, bursts met by the floor...) and names it. A change to
`control.go` that fails one needs a reason, not a new expected value. Most were
checked by breaking the rule they cover and seeing them fail.

`panel/test_panel_table.py` runs the real `panel-table.sh` with the
controller's registers replaced by files: every preset from every other,
a write that fails half-way, a mode change that is interrupted, a value in
the controller that nobody owns. It is the only way to exercise the helper
without sending anything to a panel, and it should pass before a changed
helper goes near one ([hazards](display.md#hazards)).

**The app's signing key is not in the repository.** `app/build.sh` reads it
from `$KEYSTORE` (by default `~/workspace/astra-root/governor/app/debug.keystore`
on the development Mac). Without one it signs with a throwaway key, and
Android will refuse to install that build over an app signed with the real
one.

## Install on the tablet

```sh
sh module/install.sh            # build, copy both daemons into the installed module, restart them; no reboot
sh module/build-zip.sh          # build/redmagic-astra-tuner-<version>.zip, for a fresh install from the Magisk app
SERIAL=... sh module/install.sh # another device
adb install -r build/app/astra.apk   # the app alone
```

`install.sh` needs the module to be installed already, and calibration
switched off in the app (it will not replace the service while the panel
holds calibrated timing); a fresh device takes the zip. To install the zip
from a computer exactly as the Magisk app would:

```sh
adb push build/redmagic-astra-tuner-0.4.2.zip /data/local/tmp/ && adb shell su -c 'magisk --install-module /data/local/tmp/redmagic-astra-tuner-0.4.2.zip'
```

`panel-service` takes SIGTERM as the module being switched off: it exits 0
and `service.sh` does not start it again. After stopping it by hand, start
`/data/adb/modules/astra/panel-service` by hand or reboot.

On the tablet:

```sh
adb shell su -c 'curl -s 127.0.0.1:7878/status'       # governor: action, target, floors, temperatures
adb shell su -c 'curl -s 127.0.0.1:7878/disable'      # off, and remembered, until /enable
adb shell su -c 'tail -f /data/adb/astra/astrad2.log'
adb shell su -c 'curl -s 127.0.0.1:7879/calibration'  # calibration: enabled, rate, state, message
adb shell su -c 'tail -f /data/adb/astra-panel-rate/service.log'
```

The log names every action with the sample that caused it, for example
`down-gpu607: 60.3 fps (target 60.3) cpu floor 1785 MHz gpu floor 607 MHz,
busy 0.36+0.29 at 1996/525 MHz`.

## Checking a build on the tablet

The test bench lives outside this repository, in
`~/workspace/astra-root/lab` on the development Mac (git-tracked, local only;
about 8 GB with its staged games and raw recordings). It finds this
repository at `~/code/redmagic-astra-tuner`, or wherever `$ASTRA_TUNER` points.

After any change to the daemon, before installing it:

```sh
python3 recorder/handover.py
```

runs the new build from a temporary path and checks, with a game in front:
minimums held in Rise; a heat ceiling in place; Diablo's pins surviving a
switch to Diablo; minimums back after returning to Rise; a new instance
restoring floors and ceilings left by a killed one; the non-game floor going
with the screen and coming back. It restores everything afterwards.

To compare a change against the installed build on real games:

```sh
ARMS=v2rise+heat=48,v2rise+heat=48+bin=astrad2-new CAMPAIGN=name sh recorder/sweeps/builds.sh [scene ...]
```

builds.sh runs each arm from a fresh launch of each scene (14 by default),
150 s from 15 s after the start, from a skin temperature of 33 C or less.
`+bin=NAME` runs `build/NAME` instead of `build/astrad2`; other `+flag=value`
parts become daemon flags.

## How a run is measured

`recorder/astrarec` (Go, on the tablet, as root) only observes:

- every presented frame of the app's layer, from SurfaceFlinger's per-layer
  history, polled four times a second;
- clocks, limits and GPU levels ten times a second;
- temperatures, power (battery and USB current and voltage), fan, busiest
  threads and per-core load once a second.

`recorder/analyze.py` turns a recording into the figures in
[results.md](results.md):

| Figure | Meaning |
| --- | --- |
| fps, % of target | throughput against the content's own rate (or the panel's) |
| clean | share of seconds with no late frame beyond what the panel forces |
| long / late frames | extra one-refresh delays per second |
| hitch | delays of two refreshes or more, per minute |
| worst | the longest gap between frames |
| W | the whole tablet: USB input less the battery's charge, or the battery's draw |

A 59.94 fps game on a 60.27 Hz panel must repeat a frame every three seconds;
`analyze.py` works out what the panel forces and does not count it.

### What confounds a comparison

- **Heat.** The vendor's cuts change everything. Compare runs that start
  equally warm, judged by the battery temperature: skin and board fall five to
  ten degrees within three minutes of a load stopping and say little about how
  warm the tablet is inside. Cool down with the game closed.
- **Time in the scene.** God of War II drops frames at the same moments on
  every run. Restart such scenes before every arm (`--fresh`).
- **Launch.** The first two or three minutes after a launch cost more than the
  settled state.
- **25 and 30 fps content** shows short-long frame pairs as its clock drifts
  past the panel's, in every arm.
- **Charging.** A run on the cable that also charges the battery is warmer.
  The harness switches on the vendor's charge separation before each arm.
- **One run each.** The spread between two runs of the same thing is up to
  half a watt and several points of clean seconds; Soulcalibur's attract loop
  varies by much more between launches.
- **The vendor's surprises.** The system refresh setting has switched itself to
  60 Hz twice, and the vendor's charging page has come to the front during a
  game; the harness puts the setting back and stops scripted touches when the
  game loses the screen.

### Limits of the measurement

Present times come from SurfaceFlinger, not a light sensor on the panel.
Polling SurfaceFlinger costs a `dumpsys` fork four times a second (the daemon
does it twice). Power on the cable is the tablet's draw only while the battery
is not charging.

## Working on the app

The app is plain Java against the platform, no libraries, built by
`app/build.sh` in a few seconds. It can be looked at without the tablet: in
an emulator, with the two daemons stood in for by any HTTP server answering
`/status` and `/calibration` on the host and `adb reverse tcp:7878 ...` and
`tcp:7879 ...` pointing the emulator at it. The emulator has no calibration
service, so saving works and the state line reports whatever the stand-in
says.

| File | What is in it |
| --- | --- |
| `MainActivity.java` | the one screen: both panes, what each shows in each state |
| `CalibrationStore.java` | the calibration choice: the four presets and the file the service reads |
| `Daemons.java` | the two loopback endpoints |
| `VsyncMeter.java` | the panel's real rate from Android's vsync timestamps |
| `Ui.java`, `PresetRow.java` | colours, type and the view shapes |

## Checking calibration against a screen-off

Any change to how `panel-table.sh` reaches the controller, or to when the
service runs it, has to survive the screen going off in the middle of a run:
that is what [restarted the tablet](display.md#restarts-on-waking-found-and-fixed)
before 0.4, and it took the 18th cut-short run to show. Against the fix,
2,169 such runs have shown nothing ([display.md](display.md#restarts-on-waking-found-and-fixed)).

```sh
adb push panel/interrupt-test.sh /data/local/tmp/
adb exec-out su -c "dmesg -w" > kernel.log &          # the only record if it restarts
adb shell su -c "'nohup setsid sh /data/local/tmp/interrupt-test.sh 200 10 > /data/local/tmp/interrupt.log 2>&1 < /dev/null &'"
```

with calibration switched on in the app. It wakes the screen, waits for the
helper to start, sends the sleep key at a random moment inside its run, and
repeats. 200 cycles take about 70 minutes. Afterwards: the boot ID is the one
it started on; the kernel log has no `transfer failed`, `msm-dsi-err`,
`dma_tx done but irq not triggered` or storage timeout; a wake left alone
ends **Calibrated**; switching off returns factory timing. Lines saying
`Command xfer attempted while device is in suspend state` are the driver
refusing a late packet, which is the point.

## Checking a firmware update

A new RedMagicOS build needs both halves looked at again before the module
says it supports it.

- **The governor** runs on any build with the same kernel interfaces.
  `recorder/handover.py`, then `sweeps/real.sh` against stock, as in
  [results.md](results.md#redmagicos-1108mr1).
- **Calibration refuses a build it does not know**, in two places:
  `supported()` in `panel/main.go` and the `case` at the top of
  `panel/panel-table.sh`. Before adding a build to both: measure the five
  factory rates at the pin with the module off, read the ten factory words,
  prove one mode's change and its restoration, then measure every rate with
  the service. Check too that the display driver still has its command files
  (`tx_cmd` and `rx_cmd` under `dri/0/DSI-1` in debugfs) and that they still
  take the display's lock: the helper depends on both. The 11.0.8MR1 record is
  `~/workspace/astra-root/evidence/astra-migration-20261119/`, and the
  procedure for updating a rooted tablet without losing root is
  `~/workspace/astra-root/governor/OS-UPDATE.md`.

## Releasing

1. `sh module/build.sh`: every test passes.
2. `recorder/handover.py` on the tablet.
3. A `builds.sh` comparison against the installed build on the scenes the
   change could affect, and on the default set if it touches the control law.
   For a change to calibration: every rate at the pin, a boot, four wakes and
   switching off, as in [display.md](display.md#measured-and-not-measured),
   and the [screen-off test](#checking-calibration-against-a-screen-off).
4. Bump `version` and `versionCode` in `module/module.prop` and
   `app/AndroidManifest.xml`, commit, then `sh module/install.sh`.
5. Record what was measured in [results.md](results.md) and, if a rule
   changed, in [control-law.md](control-law.md).
6. Publish from a clean, pushed tree: `sh module/build-zip.sh`, then
   `gh release create v<version> build/redmagic-astra-tuner-<version>.zip` with notes on what
   changed and what was measured. Both scripts build with `-trimpath` from the
   same commit, so the release's daemons are byte for byte the ones installed
   in step 4 (compare `sha256sum /data/adb/modules/astra/astrad2`).
7. Install that zip on the tablet from the Magisk app, reboot, and check both
   panes of the app.
