package cn.gov.xivpn2.ui;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import cn.gov.xivpn2.R;

public class BlackBackground {
    /**
     * Set pure black background if enabled in settings
     */
    public static void apply(AppCompatActivity activity) {

        try {

            boolean blackBackground = PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("black_background", false);
            if (blackBackground) {
                activity.getTheme().applyStyle(R.style.Theme_XiVPN_Black, true);
            }
        } catch (Exception e) {
            Log.e("DarkBackground", "apply black background", e);
        }

    }
}
