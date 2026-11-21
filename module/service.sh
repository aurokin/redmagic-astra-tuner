#!/system/bin/sh
# Starts the two daemons once Android has booted and restarts either if it
# exits. It runs late and outside the boot sequence, so a broken daemon cannot
# hold up boot.
MODDIR=${0%/*}
until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 2; done
mkdir -p /data/adb/astra
off() { [ -e "$MODDIR/disable" ] || [ -e "$MODDIR/remove" ]; }

# Display calibration. It does nothing until it is switched on in the app.
# It exits 0 once the module is switched off or removed and factory
# timing is back, and 73 when another instance is already running.
(
  while ! off; do
    "$MODDIR/panel-service"
    case "$?" in 0|73) exit 0 ;; esac
    sleep 5
  done
) &

# The governor. Switching the module off or removing it in Magisk stops it
# within a few seconds; it hands every clock back when it gets the signal.
sleep 15
while ! off; do
  # -heat 48: in Rise, stay a degree under the board temperature at which the vendor cuts the CPU ceilings.
  # -base 1152: outside games, keep the six performance cores from idling below 1152 MHz while the
  #   screen is on, which is what makes video miss frames. Nothing else is done outside games.
  #   In a game that runs on the two prime cores, the other six keep the same minimum.
  # -state: the on/off switch in the app is remembered across restarts and reboots.
  "$MODDIR/astrad2" -listen 127.0.0.1:7878 -log /data/adb/astra/astrad2.log -heat 48 -base 1152 -thrifty -state /data/adb/astra/governor &
  PID=$!
  while kill -0 $PID 2>/dev/null; do
    if off; then
      kill $PID
      wait $PID
      exit 0
    fi
    sleep 5
  done
  sleep 5
done
