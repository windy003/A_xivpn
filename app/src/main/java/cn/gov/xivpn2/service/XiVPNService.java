package cn.gov.xivpn2.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import cn.gov.xivpn2.LibXivpn;
import cn.gov.xivpn2.NotificationID;
import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.Rules;
import cn.gov.xivpn2.xrayconfig.Config;
import cn.gov.xivpn2.xrayconfig.Inbound;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.Routing;
import cn.gov.xivpn2.xrayconfig.RoutingRule;
import cn.gov.xivpn2.xrayconfig.Sniffing;

public class XiVPNService extends VpnService {

    public static final int SOCKS_PORT = 18964;
    private final IBinder binder = new XiVPNBinder();
    private final String TAG = "XiVPNService";
    private VPNStatusListener listener;
    private Status status = Status.DISCONNECTED;
    private ParcelFileDescriptor fileDescriptor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // https://developer.android.com/develop/connectivity/vpn#user_experience_2
        // https://developer.android.com/develop/connectivity/vpn#detect_always-on
        // We set always-on to false when the service is started by the app,
        // so we assume service started without always-on is started by the system.
        boolean alwaysOn = intent.getBooleanExtra("always-on", true);
        Log.i(TAG, "always on");

        // start vpn
        if ((intent.getAction() != null && intent.getAction().equals("cn.gov.xivpn2.START")) || alwaysOn) {
            Log.i(TAG, "start foreground");

            // start foreground service
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "XiVPNService");
            builder.setContentText("XiVPN is running");
            builder.setSmallIcon(R.drawable.baseline_vpn_key_24);
            startForeground(NotificationID.getID(), builder.build());

            // start
            startVPN(buildXrayConfig());
        }

        // stop vpn
        if (intent.getAction() != null && intent.getAction().equals("cn.gov.xivpn2.STOP")) {
            stopForeground(true);
            stopVPN();
        }
        return Service.START_NOT_STICKY;
    }

    public synchronized void startVPN(Config config) {
        if (status != Status.DISCONNECTED) return;

        status = Status.CONNECTING;
        if (listener != null) listener.onStatusChanged(status);

        // establish vpn
        Builder vpnBuilder = new Builder();
        vpnBuilder.addRoute("0.0.0.0", 0);
        vpnBuilder.addRoute("[::]", 0);
        vpnBuilder.addAddress("10.89.64.1", 32);
        vpnBuilder.addDnsServer("8.8.8.8");
        vpnBuilder.addDnsServer("8.8.4.4");
        try {
            vpnBuilder.addDisallowedApplication(this.getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) {

        }
        fileDescriptor = vpnBuilder.establish();

        // logging
        String logFile = "";
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("logs", false)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String datetime = sdf.format(new Date());
            logFile = getDataDir().getAbsolutePath() + "/logs/" + datetime + ".txt";
        }
        Log.i(TAG, "log file " + logFile);

        // start libxivpn
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String xrayConfig = gson.toJson(config);
        Log.i(TAG, "xray config: " + xrayConfig);
        String ret = LibXivpn.xivpn_start(xrayConfig, 18964, fileDescriptor.detachFd(), logFile);

        status = Status.CONNECTED;
        if (listener != null) listener.onStatusChanged(status);

        if (!ret.isEmpty()) { // error occurred
            Log.e(TAG, "libxivpn error: " + ret);
            stopVPN();
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "error stop vpn close", e);
            }
            if (listener != null) listener.onMessage("ERROR: " + ret);
        }

    }

    public synchronized void stopVPN() {
        if (status != Status.CONNECTED) return;

        LibXivpn.xivpn_stop();

        status = Status.DISCONNECTED;
        if (listener != null) listener.onStatusChanged(status);
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
                Log.d(TAG, "build xray config: add proxy: " + id + " | " + rule.outboundLabel + " | " + rule.outboundSubscription);
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
            Log.wtf(TAG, "build xray config", e);
        }

        return config;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public enum Status {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
    }

    public static interface VPNStatusListener {
        void onStatusChanged(Status status);

        void onMessage(String msg);
    }

    public class XiVPNBinder extends Binder {

        public Status getStatus() {
            return XiVPNService.this.status;
        }

        public void setListener(VPNStatusListener listener) {
            XiVPNService.this.listener = listener;
        }

    }
}