# Run by Magisk while installing the module.
MODEL=$(getprop ro.product.model)
BUILD=$(getprop ro.build.display.id)
ui_print "- RedMagic Astra Tuner, for the RedMagic Astra (NP05J)"
case "$MODEL$(getprop ro.product.name)$(getprop ro.product.device)" in
  *NP05J*|*np05j*) ;;
  *) abort "! This is not a RedMagic Astra (NP05J): $MODEL. Nothing was installed." ;;
esac
case "$BUILD" in
  REDMAGICOS11.0.7_NP05J_GB|REDMAGICOS11.0.8MR1_NP05J_GB) ;;
  *)
    ui_print "! Built and measured on RedMagicOS 11.0.7 and 11.0.8MR1 (global)."
    ui_print "! This is $BUILD: the governor will run, unmeasured;"
    ui_print "! display calibration will refuse to and stay at factory timing."
    ;;
esac

set_perm "$MODPATH/astrad2" 0 0 0755
set_perm "$MODPATH/panel-service" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/panel-table.sh" 0 0 0644
# ART only loads a DEX archive that is not writable.
set_perm "$MODPATH/panel-mode.jar" 0 0 0444

# Display calibration used to be a module of its own. Its running service takes
# the removal flag as a request for factory timing; this module takes over at
# the next boot.
OLD=/data/adb/modules/astra-panel
if [ -d "$OLD" ]; then
  touch "$OLD/remove"
  ui_print "- The separate Astra Display Calibration module is replaced by this one"
fi

# The settings app. Streamed, because the package manager cannot read Magisk's
# directories.
APK=$MODPATH/Astra.apk
if cat "$APK" | pm install -r -S "$(stat -c %s "$APK")" >/dev/null 2>&1; then
  ui_print "- Installed the app"
else
  ui_print "! Could not install the app. After the reboot, install"
  ui_print "!   /data/adb/modules/astra/Astra.apk by hand (as root)."
fi

ui_print "- Reboot to start. The governor and display calibration keep the"
ui_print "  settings they had; on a first install the governor is on and"
ui_print "  calibration is off until you switch it on in the app."
