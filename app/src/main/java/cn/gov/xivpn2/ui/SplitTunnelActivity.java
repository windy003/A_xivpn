package cn.gov.xivpn2.ui;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.gov.xivpn2.R;

public class SplitTunnelActivity extends AppCompatActivity {

    private static final String TAG = "SplitTunnelActivity";
    private Thread thread = null;
    private InstalledAppsAdapter selectedAdapter;
    private InstalledAppsAdapter unselectedAdapter;

    /**
     * Insert app in an already sorted list. Apps are sorted by appName.
     */
    private int insertSorted(List<InstalledAppsAdapter.App> apps, InstalledAppsAdapter.App app) {
        if (apps.isEmpty()) {
            apps.add(app);
            return 0;
        }
        if (apps.get(0).appName.compareToIgnoreCase(app.appName) >= 0) {
            apps.add(0, app);
            return 0;
        }
        if (apps.get(apps.size() - 1).appName.compareToIgnoreCase(app.appName) <= 0) {
            apps.add(app);
            return apps.size() - 1;
        }
        int i = 0;
        while (apps.get(i).appName.compareToIgnoreCase(app.appName) <= 0) {
            i++;
        }
        apps.add(i, app);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_split_tunnel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.split_tunnel);
        }

        SharedPreferences sp = getSharedPreferences("XIVPN", MODE_PRIVATE);
        Set<String> selectedPackageNames = sp.getStringSet("APP_LIST", new HashSet<>());

        // recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectedAdapter = new InstalledAppsAdapter(true);
        unselectedAdapter = new InstalledAppsAdapter(false);
        ConcatAdapter adapter = new ConcatAdapter(selectedAdapter, unselectedAdapter);

        recyclerView.setAdapter(adapter);

        unselectedAdapter.onCheckListener = (idx) -> {
            InstalledAppsAdapter.App app = unselectedAdapter.apps.remove(((int) idx));
            unselectedAdapter.notifyItemRemoved(idx);
            selectedAdapter.notifyItemInserted(insertSorted(selectedAdapter.apps, app));
        };
        selectedAdapter.onCheckListener = (idx) -> {
            InstalledAppsAdapter.App app = selectedAdapter.apps.remove(((int) idx));
            selectedAdapter.notifyItemRemoved(idx);
            unselectedAdapter.notifyItemInserted(insertSorted(unselectedAdapter.apps, app));
        };

        // get app list
        thread = new Thread(() -> {

            List<InstalledAppsAdapter.App> selected = selectedAdapter.apps;
            List<InstalledAppsAdapter.App> unselected = unselectedAdapter.apps;

            Log.d(TAG, "loading apps...");

            for (PackageInfo installedPackage : getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
                ApplicationInfo applicationInfo = installedPackage.applicationInfo;

                if (applicationInfo == null) {
                    continue;
                }
                if (applicationInfo.packageName.equals(getApplication().getPackageName())) {
                    continue;
                }

                Drawable drawable = applicationInfo.loadIcon(getPackageManager());
                String label = applicationInfo.loadLabel(getPackageManager()).toString();

                InstalledAppsAdapter.App app = new InstalledAppsAdapter.App(label, drawable, applicationInfo.packageName);

                if (selectedPackageNames.contains(applicationInfo.packageName)) {
                    selected.add(app);
                } else {
                    unselected.add(app);
                }
            }

            // sort
            selected.sort((a, b) -> a.appName.compareToIgnoreCase(b.appName));
            unselected.sort((a, b) -> a.appName.compareToIgnoreCase(b.appName));

            int size = unselected.size() + selected.size();
            Log.d(TAG, "loaded " + size + " apps");

            if (Thread.interrupted()) {
                return;
            }

            runOnUiThread(() -> {
                Log.d(TAG, "run on ui thread");

                unselectedAdapter.notifyItemRangeInserted(0, unselectedAdapter.getItemCount());
                selectedAdapter.notifyItemRangeInserted(0, selectedAdapter.getItemCount());

                // hide spinner
                CircularProgressIndicator progress = findViewById(R.id.progress);
                progress.hide();
            });
        });
        thread.start();

    }

    @Override
    protected void onDestroy() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.proxy_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.save) {

            SharedPreferences sp = getSharedPreferences("XIVPN", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            HashSet<String> apps = new HashSet<>();
            for (InstalledAppsAdapter.App app : selectedAdapter.apps) {
                apps.add(app.packageName);
            }
            editor.putStringSet("APP_LIST", apps);

            editor.apply();

            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}