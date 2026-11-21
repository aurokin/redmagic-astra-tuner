package dev.auro.astra;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.auro.astra.CalibrationStore.Choice;
import dev.auro.astra.CalibrationStore.Preset;

/**
 * The module's settings: the display calibration choice and the governor's
 * switch, each beside what its daemon reports. The app changes nothing on the
 * tablet itself; it saves a choice or asks the governor, and shows the result.
 */
public final class MainActivity extends Activity {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final String EXPERIMENTAL = "Experimental: a few days old and proven on one tablet. "
            + "After a restart by itself it stays off until you switch it off and on.";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::fetch;
    private final VsyncMeter vsync = new VsyncMeter();
    private final List<PresetRow> presets = new ArrayList<>();
    private Ui ui;
    private CalibrationStore store;

    private LinearLayout panes;
    private View paneRule;
    private TextView rate, rateNote, calibrationState, governorState, governorNote, board, battery;
    private TextView frameRate, lateFrames, cpuFloor, gpuFloor;
    private View gameFigures;
    private Switch calibrate, govern;

    // Calibration: what the owner chose, and what the service last said.
    private Choice desired = Choice.DEFAULT;
    private boolean loaded, saving, binding, visible;
    private String storageMessage;
    private JSONObject calibration;
    // Governor: its last status, and a switch change it has not confirmed yet.
    private JSONObject governor;
    private Boolean governorAsked;
    /** Advances whenever results still in flight stop being wanted. */
    private int generation;
    private int arrangedWidth = -1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // No system scrim behind the navigation buttons: the page's own colour runs to the edge.
        getWindow().setNavigationBarContrastEnforced(false);
        ui = new Ui(this);
        store = new CalibrationStore(this);

        LinearLayout page = ui.column();
        page.setPadding(ui.dp(28), ui.dp(20), ui.dp(28), ui.dp(24));
        panes = ui.row();
        paneRule = ui.rule();
        panes.addView(displayPane());
        panes.addView(paneRule);
        panes.addView(governorPane());
        page.addView(panes, Ui.fill());
        page.addView(ui.text("RedMagic Astra Tuner " + version() + ", for the RedMagic Astra (NP05J). Remove the module in Magisk "
                + "to return the tablet to stock.", 13, Ui.DIM, 400), ui.below(Ui.fill(), 28));

        // Wide enough for two panes side by side, or one above the other.
        FrameLayout frame = new FrameLayout(this);
        FrameLayout.LayoutParams pageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        frame.addView(page, pageParams);
        frame.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> arrange(r - l, pageParams));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        scroll.addView(frame);
        setContentView(scroll);

        calibrate.setOnCheckedChangeListener((v, on) -> {
            if (!binding) save(new Choice(on, desired.preset));
        });
        govern.setOnCheckedChangeListener((v, on) -> {
            if (!binding) askGovernor(on);
        });
        render();
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void arrange(int width, FrameLayout.LayoutParams pageParams) {
        boolean sideBySide = width >= ui.dp(860);
        int pageWidth = Math.min(width, ui.dp(sideBySide ? 1180 : 640));
        int orientation = sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
        if (arrangedWidth == pageWidth && panes.getOrientation() == orientation) return;
        arrangedWidth = pageWidth;
        pageParams.width = pageWidth;
        panes.setOrientation(orientation);
        int gap = ui.dp(28);
        for (int i = 0; i < panes.getChildCount(); i++) {
            View child = panes.getChildAt(i);
            LinearLayout.LayoutParams p;
            if (child == paneRule) {
                p = sideBySide ? new LinearLayout.LayoutParams(ui.dp(1), LinearLayout.LayoutParams.MATCH_PARENT)
                        : new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ui.dp(1));
                p.setMargins(sideBySide ? gap : 0, sideBySide ? 0 : gap, sideBySide ? gap : 0, sideBySide ? 0 : gap);
            } else {
                p = sideBySide ? Ui.share() : Ui.fill();
            }
            child.setLayoutParams(p);
        }
        // A layout pass is running; apply the new arrangement in the next one.
        panes.post(panes::requestLayout);
    }

    private View displayPane() {
        LinearLayout pane = ui.column();
        pane.addView(ui.heading("Display"));

        // The one large thing on the page: the panel's rate as it is right now.
        LinearLayout counter = ui.row();
        counter.setBaselineAligned(true);
        rate = ui.figure(76, Ui.AMBER, 300);
        rate.setTypeface(Ui.face("sans-serif-condensed", 300));
        rate.setIncludeFontPadding(false);
        counter.addView(rate);
        TextView unit = ui.text(" Hz", 26, Ui.DIM, 400);
        counter.addView(unit);
        pane.addView(counter, ui.below(Ui.fill(), 6));
        rateNote = ui.text("", 14, Ui.DIM, 400);
        pane.addView(rateNote, ui.below(Ui.fill(), 2));

        calibrate = ui.toggle("Calibrate refresh rates");
        pane.addView(ui.toggleRow("Calibrate refresh rates",
                "Corrects all five refresh modes after every boot and wake.", calibrate), ui.below(Ui.fill(), 16));
        calibrationState = ui.text("", 14, Ui.DIM, 400);
        calibrationState.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        pane.addView(calibrationState, ui.below(Ui.fill(), 6));
        pane.addView(ui.note(EXPERIMENTAL, Ui.CORAL), ui.below(Ui.fill(), 12));

        TextView group = ui.text("Match the 60 Hz mode to", 17, Ui.TEXT, 500);
        group.setAccessibilityHeading(true);
        pane.addView(group, ui.below(Ui.fill(), 20));
        for (Preset preset : Preset.values()) {
            PresetRow row = new PresetRow(ui, preset);
            row.setOnClickListener(v -> {
                if (desired.preset != preset) save(new Choice(desired.on, preset));
            });
            presets.add(row);
            pane.addView(row, ui.below(Ui.fill(), 2));
        }
        return pane;
    }

    private View governorPane() {
        LinearLayout pane = ui.column();
        pane.addView(ui.heading("Governor"));

        governorState = ui.text("", 30, Ui.TEXT, 300);
        governorState.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        pane.addView(governorState, ui.below(Ui.fill(), 14));
        governorNote = ui.text("", 14, Ui.DIM, 400);
        pane.addView(governorNote, ui.below(Ui.fill(), 4));

        govern = ui.toggle("Hold frame rates in games");
        pane.addView(ui.toggleRow("Hold frame rates in games",
                "Gives each game the lowest CPU and GPU clocks that keep its frames on time.", govern), ui.below(Ui.fill(), 22));

        LinearLayout game = ui.column();
        LinearLayout pacing = ui.row();
        frameRate = figure(pacing, "Frame rate");
        lateFrames = figure(pacing, "Late frames, last second");
        game.addView(pacing);
        LinearLayout floors = ui.row();
        cpuFloor = figure(floors, "CPU clocks held at or above");
        gpuFloor = figure(floors, "GPU clock held at or above");
        game.addView(floors, ui.below(Ui.fill(), 16));
        gameFigures = game;
        pane.addView(game, ui.below(Ui.fill(), 24));

        LinearLayout heat = ui.row();
        board = figure(heat, "Board");
        battery = figure(heat, "Battery");
        pane.addView(heat, ui.below(Ui.fill(), 24));

        TextView group = ui.text("To use it", 17, Ui.TEXT, 500);
        group.setAccessibilityHeading(true);
        pane.addView(group, ui.below(Ui.fill(), 28));
        for (String line : new String[]{
                "Add each game or emulator to Game Space.",
                "Set it to Balance there, or to Rise for the heaviest games.",
                "In Diablo the governor stands aside."}) {
            pane.addView(ui.text(line, 15, Ui.DIM, 400), ui.below(Ui.fill(), 8));
        }
        return pane;
    }

    /** A labelled value taking an equal share of its row; returns the value's view. */
    private TextView figure(LinearLayout row, String label) {
        LinearLayout cell = ui.column();
        cell.addView(ui.text(label, 13, Ui.DIM, 400));
        TextView value = ui.figure(20, Ui.TEXT, 400);
        cell.addView(value, ui.below(Ui.fill(), 2));
        row.addView(cell, Ui.share());
        return value;
    }

    @Override
    protected void onStart() {
        super.onStart();
        visible = true;
        vsync.start();
        int session = ++generation;
        loaded = false;
        calibration = governor = null;
        governorAsked = null;
        if (!getSystemService(UserManager.class).isSystemUser()) {
            // Device-protected storage is per user, and the service reads the owner's.
            storageMessage = "Open the app as the tablet's owner to change calibration.";
            render();
        } else {
            storageMessage = null;
            render();
            IO.execute(() -> {
                Choice saved = null;
                try {
                    saved = store.read();
                } catch (IOException e) {
                    // Reported below; the switch stays where the default puts it and saving is still possible.
                }
                Choice choice = saved;
                main.post(() -> {
                    if (session != generation) return;
                    desired = choice == null ? Choice.DEFAULT : choice;
                    storageMessage = choice == null ? "Couldn’t read the saved setting. Choosing again replaces it." : null;
                    loaded = true;
                    render();
                });
            });
        }
        fetch();
    }

    @Override
    protected void onStop() {
        visible = false;
        ++generation;
        main.removeCallbacks(poll);
        vsync.stop();
        super.onStop();
    }

    private void save(Choice choice) {
        if (!loaded || saving) {
            render();
            return;
        }
        Choice previous = desired;
        desired = choice;
        saving = true;
        storageMessage = null;
        render();
        IO.execute(() -> {
            boolean ok = true;
            try {
                store.write(choice);
            } catch (IOException e) {
                ok = false;
            }
            boolean saved = ok;
            main.post(() -> {
                saving = false;
                if (!saved) {
                    desired = previous;
                    storageMessage = "Couldn’t save the setting. Try again.";
                }
                render();
            });
        });
    }

    private void askGovernor(boolean on) {
        governorAsked = on;
        render();
        int session = generation;
        IO.execute(() -> {
            Daemons.setGovernor(on);
            JSONObject now = Daemons.governor();
            main.post(() -> {
                if (session != generation) return;
                governor = now;
                governorAsked = null;
                render();
            });
        });
    }

    /** Asks both daemons once a second while the app is on screen. */
    private void fetch() {
        if (!visible) return;
        int session = generation;
        IO.execute(() -> {
            JSONObject c = Daemons.calibration();
            JSONObject g = Daemons.governor();
            main.post(() -> {
                if (session != generation) return;
                calibration = c;
                governor = g;
                render();
                main.postDelayed(poll, 1000);
            });
        });
    }

    private void render() {
        binding = true;
        boolean canChoose = loaded && !saving;
        calibrate.setChecked(desired.on);
        calibrate.setEnabled(canChoose);
        for (PresetRow row : presets) {
            row.setChosen(row.preset == desired.preset);
            row.setEnabled(canChoose && desired.on);
        }
        boolean governorOn = governorAsked != null ? governorAsked : governor != null && governor.optBoolean("enabled");
        govern.setChecked(governorOn);
        govern.setEnabled(governor != null && governorAsked == null);
        binding = false;

        renderRate();
        renderCalibrationState();
        renderGovernor(governorOn);
    }

    private void renderRate() {
        double hz = vsync.hz();
        int mode = Math.round(getDisplay().getMode().getRefreshRate());
        setText(rate, hz > 0 ? String.format(Locale.US, "%.2f", hz) : "–");
        rate.setContentDescription(hz > 0 ? String.format(Locale.US, "Panel refresh rate %.2f hertz", hz) : "Measuring the panel");
        String note = "The panel’s real rate, measured now. Android calls this mode " + mode + " Hz.";
        if (mode != 60) note += " The choice below applies when Android selects 60 Hz.";
        setText(rateNote, note);
    }

    /** The line under the calibration switch: what is saved, then what the service says about it. */
    private void renderCalibrationState() {
        String text;
        int color = Ui.DIM;
        if (storageMessage != null) {
            text = storageMessage;
            color = Ui.CORAL;
        } else if (!loaded) {
            text = "Reading the saved setting…";
        } else if (saving) {
            text = "Saving…";
        } else if (calibration == null) {
            text = "The calibration service isn’t running. Check that the RedMagic Astra Tuner module is switched on in Magisk, then reboot.";
            color = Ui.CORAL;
        } else if (calibration.optBoolean("enabled") != desired.on || !desired.preset.value.equals(calibration.optString("rate"))) {
            // The service has not picked up the saved choice yet.
            text = "Waiting for the calibration service…";
        } else {
            switch (calibration.optString("state")) {
                case "off": text = "Factory timing."; break;
                case "active": text = "Calibrated."; color = Ui.TEAL; break;
                case "waiting": text = "Waiting for the display to settle. After a boot this takes about a minute."; break;
                case "applying": text = "Applying calibration…"; break;
                case "restoring": text = "Restoring factory timing…"; break;
                case "error":
                    String detail = calibration.optString("message");
                    text = detail.isEmpty() ? "Calibration needs attention." : detail;
                    color = Ui.CORAL;
                    break;
                default: text = "Waiting for the calibration service…";
            }
        }
        setText(calibrationState, text);
        calibrationState.setTextColor(color);
    }

    private void renderGovernor(boolean on) {
        JSONObject s = governor;
        boolean inGame = s != null && on && s.optBoolean("game");
        gameFigures.setVisibility(inGame ? View.VISIBLE : View.GONE);
        if (s == null) {
            setText(governorState, "Not running");
            setText(governorNote, "It starts about 15 seconds after the tablet boots. If this stays, check that the "
                    + "RedMagic Astra Tuner module is switched on in Magisk.");
            setText(board, "–");
            setText(battery, "–");
            return;
        }
        setText(board, String.format(Locale.US, "%.1f °C", s.optDouble("xo_c")));
        setText(battery, String.format(Locale.US, "%.1f °C", s.optDouble("batt_c")));
        if (!on) {
            setText(governorState, "Off");
            setText(governorNote, "Games run on the tablet’s own clock control.");
        } else if (!inGame) {
            setText(governorState, "Standing by");
            setText(governorNote, "It takes over when a Game Space game is in front.");
        } else {
            setText(governorState, "Holding " + appName(s.optString("package")));
            setText(governorNote, s.optInt("heat_level") > 0
                    ? "Giving up some CPU clock to stay under the tablet’s heat limit." : "Clocks follow what its frames need.");
            setText(frameRate, String.format(Locale.US, "%.1f of %.1f fps", s.optDouble("fps"), s.optDouble("target_fps")));
            setText(lateFrames, String.valueOf(s.optInt("long_frames")));
            JSONArray floors = s.optJSONArray("cpu_floors_mhz");
            setText(cpuFloor, floors == null ? "–" : Math.max(floors.optInt(0), floors.optInt(1)) + " MHz");
            setText(gpuFloor, s.optInt("gpu_floor_mhz") + " MHz");
        }
    }

    private String appName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg.isEmpty() ? "the game" : pkg.substring(pkg.lastIndexOf('.') + 1);
        }
    }

    /** Leaves an unchanged view alone, so a screen reader is not told the same thing every second. */
    private static void setText(TextView view, String value) {
        if (!value.contentEquals(view.getText())) view.setText(value);
    }
}
