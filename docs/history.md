# History and decisions

How the design got here, what was tried and dropped, and what is still open.
Dates are 2026.

## Timeline

| Date | What happened |
| --- | --- |
| 11-08 | Tablet unlocked and rooted with Magisk 31.1 on stock firmware; a full partition backup kept |
| 11-09 to 11-10 | **First governor** (`astrad`): bottom-up control of the GPU minimum and the CPU governor's boost, from a once-a-second frame histogram. Super Mario Galaxy's heavy scene from 71% speed to 100% in Balance. The panel's real rates measured |
| 11-10 to 11-12 | Display timing investigated at length; no safe correction ([display.md](display.md)) |
| 11-13 | Test bench, synthetic load app and 22 staged scenes. The first governor found to settle starved games on a clean half rate. **The present governor** (`astrad2`) written: top-down, every frame, CPU and GPU minimums |
| 11-14 | First full comparison (`real2`); the present governor installed as the tablet's governor; hand-over checks |
| 11-15 | Heat control (`-heat 48`); the thrifty GPU rule near the heat target; non-game floor (`-base 1152`); streams rested on low clocks; three faults fixed |
| 11-16 | Panel rates above 60 Hz; native games; Chrome; GPU-bound rule; long runs at 165 Hz; fan tests; the repository split out of the test bench |
| 11-17 | The thrifty GPU rule from the start (`-thrifty`); IFAS's GPU policy held off. Release 0.3, the governor alone |
| 11-18 | **The panel's timing corrected**: the controller's timing words take effect when followed by two more writes and a mode change. All five modes matched; a service and an app to reapply them after every wake ([display.md](display.md#how-it-was-found)). A restart on waking that evening |
| 11-19 | GBA and NES choices; the mode change made through SurfaceFlinger instead of the saved setting. Tablet updated to RedMagicOS 11.0.8MR1 keeping root; every rate measured again. A second restart on waking; a matched comparison could not reproduce it. The governor measured again on the new build. **One module, one app**: the governor's switch remembered, calibration off until switched on. That night the restart was reproduced by switching the screen off inside calibration runs, its cause found, and calibration moved to the display driver's locked command path |
| 11-20 | Frames held too long counted for 30 fps content; everything measured again on the final build. Release 0.4 |
| 11-20 | Why Shadow of the Colossus paced worse in Rise and Diablo: the panel's rate, and in Rise the six performance cores left at 384 MHz. A minimum for them in games; calibration held off after a restart by itself; everything measured again. Release 0.4.1 |
| 11-21 | Calibration cut short 1,999 more times overnight on 0.4.1 with no restart ([display.md](display.md#restarts-on-waking-found-and-fixed)). The package named **RedMagic Astra Tuner**: the repository (it was `astra-governor`), the module and the app (they were Astra) and the zip. The daemons are 0.4.1's byte for byte |

## Why the first governor was replaced

The first governor started from the vendor's low clocks and raised them when
frames were late. On the synthetic loads of 11-13 (`sweep1`, `sweep2`) it held
a GPU-heavy load at exactly half its rate (50.3% speed, 0% clean) and a 30 fps
load with a share of every second late. A starved game that settles on a clean
fraction of the panel's rate looks like a healthy game at that rate, and
nothing raises the clocks. The present governor learns the target at ample clocks
first and works down; on the same loads (`sweep2`, `sweep3`) it held 100% with
97-100% of seconds clean.

Other changes in the same rewrite:

- **Every frame, not a histogram**: SurfaceFlinger's per-layer present times
  twice a second, so one late frame is one event.
- **Minimums for both CPU and GPU** instead of the CPU governor's boost, so a
  step down can be predicted for either.
- **The other cluster** gets only what its own busiest game thread needs, so
  six idle cores are not held at a high clock because two busy ones need it.
  (Since 0.4.1 the six never get less than their minimum outside games: idle
  of the game's own threads turned out not to mean idle.)

## Faults found and fixed

| Date | Fault | Fix |
| --- | --- | --- |
| 11-14 | a game's own late frames held against the floors, blocking every setting | a late frame at the top is held against nothing (`uneven`) |
| 11-14 | bursts the busy share does not show (God of War II) | follow how high the stock CPU governor reaches (`demand`) |
| 11-15 | a target counted 1-2.5% high was never corrected; the floors stayed at the top | replace it after eight seconds at the top (`roughFor`) |
| 11-15 | streams held at the top clocks for a watt and no frame | the light rule |
| 11-15 | the governor followed a streaming app's once-a-second overlay instead of its video | follow the layer with the most frames |
| 11-15 | the non-game floor took back a minimum it had not set | only take back its own |
| 11-16 | a game the GPU limits kept the CPU at the top (19.1 W against 16.4) | the GPU-bound rule |
| 11-16 | GPU clock held on suspicion in games that drop frames by themselves (Soulcalibur at 1060 MHz, 4% busy) | the thrifty rule from the start |
| 11-19 | the tablet restarted while waking, twice in two days, with calibration in use: a calibration packet sent through the vendor's unlocked debug nodes met the panel's off command, and the display's interrupt fired without end at the next wake | calibration sends its packets through the display driver's own command files, which the driver locks against a power-down ([display.md](display.md#restarts-on-waking-found-and-fixed)) |
| 11-19 | that restart left calibration's journal as a file of zeros, and the service refused to run | the journal reaches the disk before it replaces the last; an unreadable one from before a restart is dropped |
| 11-20 | a 30 fps game left on clocks that made a frame a second late, because the next frame made up for it and no rate dipped (Xenoblade in Rise, 61-70% clean in two runs of three) | count frames held a refresh too long, beyond the panel's own (`heldLate`) |
| 11-20 | in Rise, with a game's heavy threads all on the two prime cores, the six performance cores fell to 384 MHz, under their minimum outside games, and Shadow of the Colossus skipped 3-19 frames in 90 s (73-78% clean) | they keep that minimum, 1152 MHz, in games ([results](results.md#games-on-the-prime-cores-in-rise-041)) |
| 11-20 | nothing limited the cost of being wrong about the restart fix: a tablet that still restarted with calibration on would do so again at a later wake | after a restart Android puts down to a kernel panic or a watchdog, calibration stays off until switched on again ([display.md](display.md#what-keeps-it-safe)) |

## Tried and not adopted

- **Faster descent after a launch or rescue**, three ways (11-16). Going to the
  predicted end in one move, and a step every five seconds while nothing is
  late, each failed the control law's own tests: the CPU minimum went under a
  game's bursts before the stock governor had shown them, and the GPU minimum
  stepped on a busy share that had not caught up. CPU and GPU steps
  alternating five seconds apart passed the tests and ran on 14 scenes
  (`turns1`, branch `descent-in-turns`): no change beyond the run-to-run
  spread, because a launch's loading nearly always produces the late frame that
  ends the faster pace.
- **The fan faster than the vendor runs it** (`-fan`, `-fanhot`). No effect on
  any sensor, power or frame rate in three tests; built, not used.
- **Holding the vendor's own knobs fixed** (the `staticN` arms, 11-13: the
  CPU-cap ratchet plus the GPU governor's load gain). Fixed a CPU-heavy load,
  took a GPU-heavy one from 45% to 70% of its rate, and left the balanced,
  stepped and 30 fps loads as starved as the vendor alone (`sweep1`).
- **Pinning the GPU in Rise**, or lifting Balance's GPU ceiling to 1100 MHz
  (discussed 11-16). Governed Rise games ran with the GPU at a median of
  512 MHz, and in 171 Balance runs the 832 MHz ceiling never held a governed
  game back: no frames to gain, only heat.
- **The GPU's top level first in the heat control** (11-18, branch
  `gpu-first-heat`): 1100 to 967 MHz before any CPU step, kept until the
  board is well clear. In the heavy race it delayed the vendor's cut by about
  three minutes only by running slower, for 1-2.5 W less and 2-3% fewer
  frames; the race is too hot even at 967 MHz. See
  [results](results.md#heat-and-the-fan).
- **Rejected without a trial**: writing the panel controller's flash.
- **Withdrawn**: the rule, written here on 11-17, that no further panel
  register traffic should be tried. The correction of 11-18 came from trying
  the timing words once more with the writes that follow them in the
  controller's own firmware. The caution behind the rule stands
  ([hazards](display.md#hazards)).
- **A storage clock-gating change against the restarts on waking** (11-19):
  the storage controller's software clock gating switched off for one boot,
  after the second panic showed that controller timing out first. It ran
  without fault, proved nothing about the panic, and is not in the module.

## Open problems

- **A 29.97 fps game in Diablo crosses the panel's refresh badly.** On the
  factory panel Shadow of the Colossus is 59-67% clean in Diablo against
  92-99% under the governor: each time the game's frames slide past a refresh
  (every three seconds) Diablo shows a burst of late and early frames where
  the governed modes show one late frame. With the panel on NTSC it is 100%
  in three runs of five, but once the frames fell on the edge of a refresh
  and stayed there for the whole run (7%). Why Diablo's frames sit where they
  can flip is not known, and Diablo is outside what the governor does
  ([results](results.md#an-emulator-on-the-ntsc-choice)).
- **What a frame waits for on the performance cores** when a game's heavy
  threads are all on the prime cores was not found; 0.4.1 holds those cores
  at 1152 MHz without knowing which thread needs it, on the evidence of one
  game.
- **No readout over a game.** The app shows what the governor is doing only
  while the app is in front, or beside a game in split screen. The first
  governor's app had an overlay; it was not carried over.
- **Content late at ample clocks with modest work** (Soulcalibur's attract
  loop in some launches): the minimums stay at the top for 1.5-3.6 W more than
  the vendor, though far more frames are on time. A mode of its own is
  sketched: content late at the top again and again, whose busy shares at the
  resting clock fall between the light rule's limits and a real game's, would
  step down regardless and take a timed look at ample clocks to confirm they
  still give no more frames. The light rule's wider limit is not it:
  Soulcalibur's busiest thread is 31-43% busy at the resting clock, and its
  bursts would take it in and out every few seconds.
- **The launch cost**: 1.5-2 W for two or three minutes after each launch.
- **A GPU ceiling of our own** just under the vendor's heat cap (967 or
  900 MHz) would turn the heavy game's 77-105 fps swing into a steadier
  ~90 fps; not built.
- **The system refresh setting** switching itself to 60 Hz, twice; cause
  unknown.
- **Something raises the GPU floor early in boot**: twice, the daemon found
  `min_pwrlevel` above the base level when it started 15 s after boot and put
  it back (its start-up check assumes only a killed instance of its own leaves
  it raised). What raised it, and for how long it would have stayed, was not
  looked into; a guess is a vendor boot boost.
- **Unexplained cooling spells**: board, skin and battery falling 2-5 C at an
  unchanged load.
