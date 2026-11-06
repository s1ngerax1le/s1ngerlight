package com.onyx.decoled;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import java.util.Locale;

public class LedController {
    private static final String TAG = "LedController";
    public static final String SYSFS_LED = "/sys/class/leds/aw21024_led";
    public static final String BOOT_SCRIPT = "/data/adb/service.d/disable_deco_led.sh";
    public static final String PREFS_NAME = "deco_led_prefs";

    public static final int EFFECT_STATIC = 0;
    public static final int EFFECT_BREATH = 1;
    public static final int EFFECT_BLINK = 2;
    public static final int EFFECT_STROBE = 3;

    private static Thread animThread = null;
    private static volatile boolean isAnimating = false;

    private final SharedPreferences prefs;

    public LedController(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isMasterEnabled() {
        return prefs.getBoolean("master_enabled", false);
    }

    public void setMasterEnabled(boolean enabled) {
        prefs.edit().putBoolean("master_enabled", enabled).apply();
    }

    public int getBrightness() {
        return prefs.getInt("brightness", 180);
    }

    public void setBrightness(int val) {
        prefs.edit().putInt("brightness", val).apply();
    }

    public int getColor() {
        return prefs.getInt("color", Color.parseColor("#00E5FF"));
    }

    public void setColor(int color) {
        prefs.edit().putInt("color", color).apply();
    }

    public int getEffect() {
        return prefs.getInt("effect", EFFECT_STATIC);
    }

    public void setEffect(int effect) {
        prefs.edit().putInt("effect", effect).apply();
    }

    public int getSpeed() {
        return prefs.getInt("speed", 1500);
    }

    public void setSpeed(int speed) {
        prefs.edit().putInt("speed", speed).apply();
    }

    public boolean isShieldMode() {
        return prefs.getBoolean("shield_mode", true);
    }

    public void setShieldMode(boolean shield) {
        prefs.edit().putBoolean("shield_mode", shield).apply();
    }

    public boolean isBootSync() {
        return prefs.getBoolean("boot_sync", true);
    }

    public void setBootSync(boolean sync) {
        prefs.edit().putBoolean("boot_sync", sync).apply();
    }

    public synchronized void applyState() {
        boolean enabled = isMasterEnabled();
        int brightness = getBrightness();
        int color = getColor();
        int effect = getEffect();
        int speed = getSpeed();
        boolean shield = isShieldMode();
        boolean bootSync = isBootSync();

        stopAnimation();

        // 1. Make sure permissions are writeable
        InteractiveRootSession.writeCommand("chmod 666 " + SYSFS_LED + "/* 2>/dev/null;");

        if (!enabled) {
            // Master OFF
            String offCmd = "echo 0 > " + SYSFS_LED + "/run; " +
                    "echo 0 > " + SYSFS_LED + "/brightness; ";
            if (shield) {
                offCmd += "chown root:root " + SYSFS_LED + "/*; chmod 444 " + SYSFS_LED + "/*; ";
            }
            InteractiveRootSession.writeCommand(offCmd);
        } else {
            // Master ON
            int hex24 = 0xFFFFFF & color;
            String colorHex = String.format(Locale.US, "0x%06x", hex24);

            String baseOn = "echo 1 > " + SYSFS_LED + "/hwen; " +
                    "echo 1 > " + SYSFS_LED + "/effect; " +
                    "echo 1 > " + SYSFS_LED + "/run; " +
                    "echo " + colorHex + " > " + SYSFS_LED + "/color 2>/dev/null; ";
            InteractiveRootSession.writeCommand(baseOn);

            if (effect == EFFECT_STATIC) {
                InteractiveRootSession.writeCommand("echo " + brightness + " > " + SYSFS_LED + "/brightness;");
                if (shield) {
                    InteractiveRootSession.writeCommand("chown root:root " + SYSFS_LED + "/*; chmod 444 " + SYSFS_LED + "/*;");
                }
            } else {
                // Dynamic Animation Engine (Breath / Blink / Strobe)
                startAnimation(effect, speed, brightness);
            }
        }

        if (bootSync) {
            updateBootScript(enabled, brightness, color, effect, speed, shield);
        }
    }

    private synchronized void startAnimation(final int effect, final int speed, final int maxBrightness) {
        stopAnimation();
        isAnimating = true;

        animThread = new Thread(() -> {
            InteractiveRootSession.ensureOpen();
            long startTime = System.currentTimeMillis();
            int minB = Math.max(3, maxBrightness / 12);

            while (isAnimating) {
                try {
                    if (effect == EFFECT_BREATH) {
                        long elapsed = (System.currentTimeMillis() - startTime) % speed;
                        double phase = (elapsed / (double) speed) * 2 * Math.PI;
                        double factor = (1.0 - Math.cos(phase)) / 2.0;
                        int b = (int) (minB + factor * (maxBrightness - minB));
                        InteractiveRootSession.writeCommand("echo " + b + " > " + SYSFS_LED + "/brightness");
                        Thread.sleep(35); // ~28 fps
                    } else if (effect == EFFECT_BLINK) {
                        int half = Math.max(80, speed / 2);
                        InteractiveRootSession.writeCommand("echo " + maxBrightness + " > " + SYSFS_LED + "/brightness");
                        Thread.sleep(half);
                        if (!isAnimating) break;
                        InteractiveRootSession.writeCommand("echo 0 > " + SYSFS_LED + "/brightness");
                        Thread.sleep(half);
                    } else if (effect == EFFECT_STROBE) {
                        // High energy electro-strobe: 60ms ON, 60ms OFF
                        InteractiveRootSession.writeCommand("echo " + maxBrightness + " > " + SYSFS_LED + "/brightness");
                        Thread.sleep(60);
                        if (!isAnimating) break;
                        InteractiveRootSession.writeCommand("echo 0 > " + SYSFS_LED + "/brightness");
                        Thread.sleep(60);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in anim loop", e);
                }
            }
        }, "LedAnimThread");

        animThread.start();
    }

    public synchronized void stopAnimation() {
        isAnimating = false;
        if (animThread != null) {
            animThread.interrupt();
            animThread = null;
        }
    }

    public void fullBlackout() {
        setMasterEnabled(false);
        setShieldMode(true);
        stopAnimation();

        String blackoutCmd = "chmod 664 " + SYSFS_LED + "/* 2>/dev/null; " +
                "echo 0 > " + SYSFS_LED + "/run; " +
                "echo 0 > " + SYSFS_LED + "/brightness; " +
                "chown root:root " + SYSFS_LED + "/*; " +
                "chmod 444 " + SYSFS_LED + "/*;";
        InteractiveRootSession.writeCommand(blackoutCmd);
        updateBootScript(false, 0, 0, 0, 0, true);
    }

    private void updateBootScript(boolean enabled, int brightness, int color, int effect, int speed, boolean shield) {
        StringBuilder script = new StringBuilder();
        script.append("#!/system/bin/sh\n");
        script.append("while [ \"$(getprop sys.boot_completed)\" != \"1\" ]; do sleep 2; done\n");
        script.append("sleep 2\n");
        script.append("LED_DIR=\"").append(SYSFS_LED).append("\"\n");
        script.append("if [ -d \"$LED_DIR\" ]; then\n");
        script.append("    chmod 664 $LED_DIR/* 2>/dev/null\n");

        if (!enabled) {
            script.append("    echo 0 > $LED_DIR/run\n");
            script.append("    echo 0 > $LED_DIR/brightness\n");
            if (shield) {
                script.append("    chown root:root $LED_DIR/*\n");
                script.append("    chmod 444 $LED_DIR/*\n");
            }
        } else {
            int hex24 = 0xFFFFFF & color;
            String colorHex = String.format(Locale.US, "0x%06x", hex24);
            script.append("    echo 1 > $LED_DIR/hwen\n");
            script.append("    echo 1 > $LED_DIR/effect\n");
            script.append("    echo 1 > $LED_DIR/run\n");
            script.append("    echo ").append(colorHex).append(" > $LED_DIR/color 2>/dev/null\n");
            script.append("    echo ").append(brightness).append(" > $LED_DIR/brightness\n");
            if (shield && effect == EFFECT_STATIC) {
                script.append("    chown root:root $LED_DIR/*\n");
                script.append("    chmod 444 $LED_DIR/*\n");
            }
        }
        script.append("fi\n");

        String writeCmd = "cat << 'EOF' > " + BOOT_SCRIPT + "\n" + script.toString() + "EOF\nchmod 755 " + BOOT_SCRIPT;
        RootShell.runQuick(writeCmd);
    }
}
