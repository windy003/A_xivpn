package cn.gov.xivpn2.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.util.ArrayList;
import java.util.HashMap;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.service.XiVPNService;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.xrayconfig.Config;
import cn.gov.xivpn2.xrayconfig.Inbound;
import cn.gov.xivpn2.xrayconfig.Outbound;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btn;
    private MaterialSwitch aSwitch;
    private TextView textView;
    private XiVPNService.XiVPNBinder binder;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (XiVPNService.XiVPNBinder) service;

            updateSwitch(binder.getService().getStatus());

            binder.setListener(new XiVPNService.VPNStatusListener() {
                @Override
                public void onStatusChanged(XiVPNService.Status status) {
                    updateSwitch(status);
                }

                @Override
                public void onMessage(String msg) {
                    textView.setText(msg);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    };
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    @Override
    protected void onStart() {
        super.onStart();
        // bind and start vpn service
        bindService(new Intent(this, XiVPNService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        textView.setText("");
        SharedPreferences sp = getSharedPreferences("XIVPN", MODE_PRIVATE);
        btn.setText(sp.getString("SELECTED_LABEL", "Freedom"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // bind views
        textView = findViewById(R.id.textview);
        aSwitch = findViewById(R.id.vpn_switch);
        btn = findViewById(R.id.btn_report);

        onCheckedChangeListener = (compoundButton, b) -> {
            if (b) {
                // start vpn

                // request vpn permission
                Intent intent = XiVPNService.prepare(this);
                if (intent != null) {
                    aSwitch.setChecked(false);
                    startActivityForResult(intent, 1);
                    return;
                }

                // build xray config
                Config config = buildXrayConfig();

                // start vpn service

                if (binder.getService().startVPN(config)) {
                    Intent intent2 = new Intent(this, XiVPNService.class);
                    intent2.setAction("cn.gov.xivpn2.START");
                    startForegroundService(intent2);
                }


            } else {
                // stop
                binder.getService().stopVPN();
            }
        };
        aSwitch.setOnCheckedChangeListener(onCheckedChangeListener);

        // request notification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        2
                );
            }
        }

        btn.setOnClickListener(v -> {
            startActivity(new Intent(this, ProxiesActivity.class));
        });

        // show privacy policy
        if (!getSharedPreferences("XIVPN", MODE_PRIVATE).getBoolean("NEVER_SHOW_PRIVACY_POLICY", false)) {
            TextView textView = new TextView(this);
            textView.setTextIsSelectable(true);
            textView.setText(Html.fromHtml(getString(R.string.privacy_policy_content), Html.FROM_HTML_MODE_COMPACT));

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.privacy_policy)
                    .setView(textView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.never_show_again, (dialog, which) -> {
                        SharedPreferences sp = getSharedPreferences("XIVPN", MODE_PRIVATE);
                        sp.edit().putBoolean("NEVER_SHOW_PRIVACY_POLICY", true).apply();
                    })
                    .show();
        }

    }

    /**
     * update switch based on the status of vpn
     */
    private void updateSwitch(XiVPNService.Status status) {
        // set listener to null so setChecked will not trigger the listener
        aSwitch.setOnCheckedChangeListener(null);

        if (status == XiVPNService.Status.CONNECTING) {
            aSwitch.setChecked(false);
            aSwitch.setEnabled(false);
            aSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
            return;
        }
        aSwitch.setEnabled(true);
        aSwitch.setChecked(status == XiVPNService.Status.CONNECTED);
        aSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.subscriptions) {
            startActivity(new Intent(this, SubscriptionsActivity.class));
        } else if (item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, PreferenceActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private Config buildXrayConfig() {
        Config config = new Config();
        config.inbounds = new ArrayList<>();
        config.outbounds = new ArrayList<>();

        // socks5 inbound
        Inbound socks5Inbound = new Inbound();
        socks5Inbound.protocol = "socks";
        socks5Inbound.port = XiVPNService.SOCKS_PORT;
        socks5Inbound.listen = "10.89.64.1";
        socks5Inbound.settings = new HashMap<>();
        socks5Inbound.settings.put("udp", true);
        config.inbounds.add(socks5Inbound);

        // outbound
        SharedPreferences sp = getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
        String label = sp.getString("SELECTED_LABEL", "Freedom");
        String subscription = sp.getString("SELECTED_SUBSCRIPTION", "none");
        Proxy proxy = AppDatabase.getInstance().proxyDao().find(label, subscription);

        Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
        Outbound<?> outbound = gson.fromJson(proxy.config, Outbound.class);

        config.outbounds.add(outbound);

        return config;
    }

}