package org.adaway.model.system;

import android.annotation.SuppressLint;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

/**
 * The interface a ROM provides for the system ad block method.
 * <p>
 * A ROM that supports it sets {@code ro.adaway.system_hosts=1} and, from its init scripts:
 * <ul>
 * <li>creates {@link #DIRECTORY},</li>
 * <li>at boot, copies {@link #BLOCKLIST} into {@link #LIVE_HOSTS} and bind mounts that
 * over {@code /system/etc/hosts},</li>
 * <li>hands the directory to the app when it sets {@link #PROP_OWNER} to its uid,</li>
 * <li>copies {@link #BLOCKLIST} into the live file again whenever {@link #PROP_APPLY} is set.</li>
 * </ul>
 * The app must be signed with the ROM's platform key, which is what its SELinux rules allow.
 */
public final class SystemHosts {
    public static final File DIRECTORY = new File("/data/misc/adaway");
    public static final File BLOCKLIST = new File(DIRECTORY, "blocklist");
    public static final File LIVE_HOSTS = new File(DIRECTORY, "hosts");
    private static final String PROP_AVAILABLE = "ro.adaway.system_hosts";
    private static final String PROP_OWNER = "sys.adaway.owner";
    private static final String PROP_APPLY = "sys.adaway.apply";
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

    /**
     * Ask init to give the hosts directory to this app, and wait until it is writable.
     *
     * @return {@code true} if the directory is writable.
     */
    public static boolean claimDirectory() {
        if (DIRECTORY.canWrite()) {
            return true;
        }
        Log.i(TAG, "asking for " + DIRECTORY + " as uid " + Process.myUid());
        setProperty(PROP_OWNER, Integer.toString(Process.myUid()));
        for (int i = 0; i < 50 && !DIRECTORY.canWrite(); i++) {
            SystemClock.sleep(100);
        }
        boolean writable = DIRECTORY.canWrite();
        Log.i(TAG, DIRECTORY + (writable ? " is writable" : " is still not writable"));
        return writable;
    }

    /**
     * Ask init to copy the blocklist into the live hosts file.
     */
    public static void requestApply() {
        // An int property: seconds fit, milliseconds don't
        setProperty(PROP_APPLY, Long.toString(System.currentTimeMillis() / 1000));
        Log.i(TAG, "asked to apply " + BLOCKLIST.length() + " bytes");
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

    @SuppressLint("PrivateApi")
    private static void setProperty(String key, String value) {
        try {
            Method set = Class.forName("android.os.SystemProperties").getMethod("set", String.class, String.class);
            set.invoke(null, key, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e(TAG, "Failed to set property " + key, e);
        }
    }
}
