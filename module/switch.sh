#!/system/bin/sh
# Runs on the tablet as root, from the directory holding the files to install:
# stops the running daemons and their supervisor, copies the files into the
# module and starts the new supervisor.
set -e
M=/data/adb/modules/astra
T=${0%/*}
[ -f $M/module.prop ] || { echo "the astra module is not installed: install the zip from the Magisk app first (module/build-zip.sh)"; exit 1; }
if [ -e /data/adb/astra-panel-rate/table-session ]; then
  echo "display calibration is on: switch it off in the app and wait for Factory timing first"; exit 1
fi
# The supervisor goes first so it does not restart a daemon under us. The
# governor restores every setting it holds when it gets the signal; the
# calibration service holds nothing while calibration is off.
for p in $(ps -A -o PID,ARGS | grep "$M/service.sh" | grep -v grep | awk '{print $1}'); do kill $p; done
pkill -TERM -x astrad2 || true
pkill -TERM -x panel-service || true
# A second calibration service exits at once while the first still holds its
# lock, and the supervisor takes that as final: wait for the old one to go.
for i in 1 2 3 4 5 6 7 8 9 10; do
  sleep 1
  [ -z "$(pidof astrad2 panel-service)" ] && break
done
for f in astrad2 panel-service panel-mode.jar panel-table.sh service.sh uninstall.sh module.prop; do
  [ -f $T/$f ] && { rm -f $M/$f; cp $T/$f $M/$f; }
done
chmod 755 $M/service.sh $M/uninstall.sh $M/astrad2 $M/panel-service
chmod 644 $M/module.prop $M/panel-table.sh
chmod 444 $M/panel-mode.jar
nohup sh $M/service.sh >/dev/null 2>&1 &
