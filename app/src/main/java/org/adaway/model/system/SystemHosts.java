package org.adaway.model.system;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

/**
 * The interface a ROM provides for the system ad block method.
 * <p>
 * A ROM that supports it sets {@code ro.adaway.system_hosts=1}, creates {@link #DIRECTORY},
 * and at boot copies {@link #BLOCKLIST} into {@link #LIVE_HOSTS} and bind mounts that over
 * {@code /system/etc/hosts}. The app writes both files directly: the blocklist so it survives
 * reboots, the live file so it takes effect at once. The ROM's SELinux rules only let
 * platform-signed apps write them, so the app must be signed with the ROM's platform key.
 */
public final class SystemHosts {
    public static final File DIRECTORY = new File("/data/misc/adaway");
    public static final File BLOCKLIST = new File(DIRECTORY, "blocklist");
    public static final File LIVE_HOSTS = new File(DIRECTORY, "hosts");
    private static final String PROP_AVAILABLE = "ro.adaway.system_hosts";
    static final String TAG = "AdAwaySystem";

    private SystemHosts() {
    }

    /**
     * @return {@code true} when running on a ROM that provides system hosts support.
     */
    public static boolean isAvailable() {
        String value = getProperty(PROP_AVAILABLE);
        Log.i(TAG, PROP_AVAILABLE + "=" + value);
        return "1".equals(value) || "true".equals(value);
    }

    @SuppressLint("PrivateApi")
    private static String getProperty(String key) {
        try {
            Method get = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            return (String) get.invoke(null, key);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e(TAG, "Failed to read property " + key, e);
            return "";
        }
    }
}
