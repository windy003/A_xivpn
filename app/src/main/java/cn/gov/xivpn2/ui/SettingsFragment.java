package cn.gov.xivpn2.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.method.LinkMovementMethodCompat;
import androidx.preference.PreferenceFragmentCompat;

import cn.gov.xivpn2.BuildConfig;
import cn.gov.xivpn2.LibXivpn;
import cn.gov.xivpn2.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference("feedback").setOnPreferenceClickListener(preference -> {
            openUrl("https://github.com/Exclude0122/xivpn/issues/new");
            return true;
        });

        findPreference("privacy_policy").setOnPreferenceClickListener(preference -> {
            openUrl("https://exclude0122.github.io/docs/privacy-policy.html");
            return true;
        });

        findPreference("source_code").setOnPreferenceClickListener(preference -> {
            openUrl("https://github.com/Exclude0122/xivpn");
            return true;
        });

        findPreference("open_source_licenses").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), LicensesActivity.class));
            return true;
        });

        findPreference("xray_version").setSummary(LibXivpn.xivpn_version());

        findPreference("black_background").setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(getContext(), R.string.restart_to_apply, Toast.LENGTH_SHORT).show();
            return true;
        });

        findPreference("app_version").setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");

        findPreference("geoip_geosite").setOnPreferenceClickListener(preference -> {
            Spanned html = Html.fromHtml(getString(R.string.geoip_geosite_summary), Html.FROM_HTML_MODE_COMPACT);

            TextView textView = new TextView(requireContext());
            textView.setText(html);
            textView.setMovementMethod(LinkMovementMethodCompat.getInstance());

            FrameLayout frameLayout = new FrameLayout(requireContext());
            frameLayout.addView(textView);

            frameLayout.setPadding(dp2px(24f), dp2px(24f), dp2px(24f), dp2px(24f));

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.geoip_geosite)
                    .setView(frameLayout)
                    .setPositiveButton(R.string.ok, null)
                    .create();
            dialog.show();

            return true;
        });
    }

    private void openUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("SettingsFragment", "open browser", e);
        }
    }

    public int dp2px(float dp) {
        return (int) (dp * ((float) requireContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
