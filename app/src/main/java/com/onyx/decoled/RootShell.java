package com.onyx.decoled;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RootShell {
    private static final String TAG = "RootShell";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onComplete(boolean success, String output);
    }

    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitCode = process.waitFor();
            return exitCode == 0 && line != null && line.contains("uid=0");
        } catch (Exception e) {
            Log.e(TAG, "Root check failed", e);
            return false;
        }
    }

    public static boolean runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to run command: " + command, e);
            return false;
        }
    }

    public static void runCommandAsync(final String command, final Callback callback) {
        executor.execute(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                os.writeBytes(command + "\n");
                os.writeBytes("exit\n");
                os.flush();

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                int exitCode = process.waitFor();
                boolean success = (exitCode == 0);
                if (callback != null) {
                    callback.onComplete(success, sb.toString().trim());
                }
            } catch (Exception e) {
                Log.e(TAG, "Async command error: " + command, e);
                if (callback != null) {
                    callback.onComplete(false, e.getMessage());
                }
            }
        });
    }

    public static void runQuick(final String command) {
        executor.execute(() -> runCommand(command));
    }
}
