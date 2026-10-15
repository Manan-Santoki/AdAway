package org.adaway;

import android.app.Application;
import android.content.SharedPreferences;

import org.adaway.helper.NotificationHelper;
import org.adaway.helper.PreferenceHelper;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.source.SourceModel;
import org.adaway.model.source.SourceUpdateService;
import org.adaway.model.system.SystemApplyWorker;
import org.adaway.model.update.UpdateModel;
import org.adaway.util.AppExecutors;
import org.adaway.util.Constants;
import org.adaway.util.log.ApplicationLog;

/**
 * This class is a custom {@link Application} for AdAway app.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class AdAwayApplication extends Application {
    private static final String PREF_SYSTEM_SET_UP = "systemModeSetUp";
    /**
     * The common source model for the whole application.
     */
    private SourceModel sourceModel;
    /**
     * The common ad block model for the whole application.
     */
    private AdBlockModel adBlockModel;
    /**
     * The common update model for the whole application.
     */
    private UpdateModel updateModel;

    @Override
    public void onCreate() {
        // Delegate application creation
        super.onCreate();
        // Initialize logging
        ApplicationLog.init(this);
        // Create notification channels
        NotificationHelper.createNotificationChannels(this);
        // Create models
        this.sourceModel = new SourceModel(this);
        this.updateModel = new UpdateModel(this);
        // Built-in mode: first start sets everything up so blocking works out of the box
        if (PreferenceHelper.getAdBlockMethod(this) == AdBlockMethod.SYSTEM) {
            setUpSystemMode();
            // On every start, not just the first: an earlier apply may have failed
            AppExecutors.getInstance().diskIO().execute(() -> SystemApplyWorker.ensureApplied(this));
        }
    }

    private void setUpSystemMode() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_SYSTEM_SET_UP, false)) {
            return;
        }
        android.util.Log.i("AdAwaySystem", "first start in system mode: scheduling daily updates");
        SourceUpdateService.enable(this, PreferenceHelper.getUpdateOnlyOnWifi(this));
        prefs.edit().putBoolean(PREF_SYSTEM_SET_UP, true).apply();
    }

    /**
     * Get the source model.
     *
     * @return The common source model for the whole application.
     */
    public SourceModel getSourceModel() {
        return this.sourceModel;
    }

    /**
     * Get the ad block model.
     *
     * @return The common ad block model for the whole application.
     */
    public AdBlockModel getAdBlockModel() {
        // Check cached model
        AdBlockMethod method = PreferenceHelper.getAdBlockMethod(this);
        if (this.adBlockModel == null || this.adBlockModel.getMethod() != method) {
            this.adBlockModel = AdBlockModel.build(this, method);
        }
        return this.adBlockModel;
    }

    /**
     * Get the update model.
     *
     * @return Teh common update model for the whole application.
     */
    public UpdateModel getUpdateModel() {
        return this.updateModel;
    }
}
