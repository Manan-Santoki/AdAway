package org.adaway.model.system;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.SystemClock;

import java.io.File;
import java.lang.reflect.Method;

import timber.log.Timber;

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

    private SystemHosts() {
    }

    /**
     * @return {@code true} when running on a ROM that provides system hosts support.
     */
    public static boolean isAvailable() {
        return "1".equals(getProperty(PROP_AVAILABLE));
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
        setProperty(PROP_OWNER, Integer.toString(Process.myUid()));
        for (int i = 0; i < 50 && !DIRECTORY.canWrite(); i++) {
            SystemClock.sleep(100);
        }
        return DIRECTORY.canWrite();
    }

    /**
     * Ask init to copy the blocklist into the live hosts file.
     */
    public static void requestApply() {
        setProperty(PROP_APPLY, Long.toString(System.currentTimeMillis()));
    }

    /**
     * Hide the launcher icon and the quick settings tile: on such a ROM the app is opened from
     * Settings, and blocking stays on by default.
     */
    public static void setBuiltInComponents(Context context) {
        PackageManager pm = context.getPackageManager();
        String pkg = context.getPackageName();
        for (String name : new String[]{".ui.home.LauncherAlias", ".tile.AdBlockingTileService"}) {
            pm.setComponentEnabledSetting(new ComponentName(pkg, pkg + name),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    @SuppressLint("PrivateApi")
    private static String getProperty(String key) {
        try {
            Method get = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            return (String) get.invoke(null, key);
        } catch (ReflectiveOperationException e) {
            Timber.w(e, "Failed to read property %s", key);
            return "";
        }
    }

    @SuppressLint("PrivateApi")
    private static void setProperty(String key, String value) {
        try {
            Method set = Class.forName("android.os.SystemProperties").getMethod("set", String.class, String.class);
            set.invoke(null, key, value);
        } catch (ReflectiveOperationException e) {
            Timber.e(e, "Failed to set property %s", key);
        }
    }
}
