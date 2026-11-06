package com.onyx.decoled;

import android.util.Log;
import java.io.OutputStream;

public class InteractiveRootSession {
    private static final String TAG = "RootSession";
    private static Process process;
    private static OutputStream os;

    public static synchronized void ensureOpen() {
        if (process == null) {
            try {
                process = Runtime.getRuntime().exec("su");
                os = process.getOutputStream();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start su process", e);
                process = null;
                os = null;
            }
        }
    }

    public static synchronized void writeCommand(String cmd) {
        ensureOpen();
        if (os != null) {
            try {
                os.write((cmd + "\n").getBytes());
                os.flush();
            } catch (Exception e) {
                Log.e(TAG, "Failed to write command, restarting session", e);
                close();
                ensureOpen();
                if (os != null) {
                    try {
                        os.write((cmd + "\n").getBytes());
                        os.flush();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public static synchronized void close() {
        if (os != null) {
            try {
                os.write("exit\n".getBytes());
                os.flush();
                os.close();
            } catch (Exception ignored) {}
            os = null;
        }
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ignored) {}
            process = null;
        }
    }
}
