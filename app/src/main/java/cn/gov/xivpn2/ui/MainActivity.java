package cn.gov.xivpn2.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.Rules;
import cn.gov.xivpn2.service.XiVPNService;
import cn.gov.xivpn2.xrayconfig.Config;
import cn.gov.xivpn2.xrayconfig.Inbound;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.Routing;
import cn.gov.xivpn2.xrayconfig.RoutingRule;
import cn.gov.xivpn2.xrayconfig.Sniffing;

public class MainActivity extends AppCompatActivity {

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private MaterialSwitch aSwitch;
    private TextView textView;
    private XiVPNService.XiVPNBinder binder;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BlackBackground.apply(this);

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
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navView);

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
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        2
                );
            }
        }

        // drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_menu_24);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.proxies) {
                startActivity(new Intent(this, ProxiesActivity.class));
            }
            if (item.getItemId() == R.id.subscriptions) {
                startActivity(new Intent(this, SubscriptionsActivity.class));
            }
            if (item.getItemId() == R.id.settings) {
                startActivity(new Intent(this, PreferenceActivity.class));
            }
            if (item.getItemId() == R.id.rules) {
                startActivity(new Intent(this, RulesActivity.class));
            }
            drawerLayout.close();
            return false;
        });
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        // drawer
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isOpen()) {
                drawerLayout.close();
            } else {
                drawerLayout.open();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private Config buildXrayConfig() {
        Config config = new Config();
        config.inbounds = new ArrayList<>();
        config.outbounds = new ArrayList<>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // logs
        config.log.loglevel = preferences.getString("log_level", "warning");

        // socks5 inbound
        Inbound socks5Inbound = new Inbound();
        socks5Inbound.protocol = "socks";
        socks5Inbound.port = XiVPNService.SOCKS_PORT;
        socks5Inbound.listen = "10.89.64.1";
        socks5Inbound.settings = new HashMap<>();
        socks5Inbound.settings.put("udp", true);

        socks5Inbound.sniffing = new Sniffing();
        socks5Inbound.sniffing.enabled = preferences.getBoolean("sniffing", true);
        socks5Inbound.sniffing.destOverride = List.of("http", "tls");
        socks5Inbound.sniffing.routeOnly = preferences.getBoolean("sniffing_route_only", true);

        config.inbounds.add(socks5Inbound);

        try {

            // routing
            List<RoutingRule> rules = Rules.readRules(getFilesDir());

            config.routing = new Routing();
            config.routing.rules = rules;

            // outbound
            HashSet<Long> proxyIds = new HashSet<>();

            for (RoutingRule rule : rules) {
                long id = AppDatabase.getInstance().proxyDao().find(rule.outboundLabel, rule.outboundSubscription).id;
                Log.d("MainActivity", "build xray config: add proxy: " + id + " | " + rule.outboundLabel + " | " + rule.outboundSubscription);
                proxyIds.add(id);

                rule.outboundTag = String.format("#%d %s (%s)", id, rule.outboundLabel, rule.outboundSubscription);
                if (rule.domain.isEmpty()) rule.domain = null;
                if (rule.ip.isEmpty()) rule.ip = null;
                if (rule.port.isEmpty()) rule.port = null;
                if (rule.protocol.isEmpty()) rule.protocol = null;
                rule.outboundLabel = null;
                rule.outboundSubscription = null;
                rule.label = null;
            }

            for (Long id : proxyIds) {
                Proxy proxy = AppDatabase.getInstance().proxyDao().findById(id);
                Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
                Outbound<?> outbound = gson.fromJson(proxy.config, Outbound.class);
                outbound.tag = String.format("#%d %s (%s)", id, proxy.label, proxy.subscription);
                config.outbounds.add(outbound);
            }
        } catch (IOException e) {
            Log.wtf("MainActivity", "build xray config", e);
        }

        return config;
    }

}