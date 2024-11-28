package cn.gov.xivpn2.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.gov.xivpn2.NotificationID;
import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.Subscription;
import cn.gov.xivpn2.xrayconfig.HttpUpgradeSettings;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.ShadowsocksServerSettings;
import cn.gov.xivpn2.xrayconfig.ShadowsocksSettings;
import cn.gov.xivpn2.xrayconfig.XHttpSettings;
import cn.gov.xivpn2.xrayconfig.StreamSettings;
import cn.gov.xivpn2.xrayconfig.TLSSettings;
import cn.gov.xivpn2.xrayconfig.TrojanServerSettings;
import cn.gov.xivpn2.xrayconfig.TrojanSettings;
import cn.gov.xivpn2.xrayconfig.VmessServerSettings;
import cn.gov.xivpn2.xrayconfig.VmessSettings;
import cn.gov.xivpn2.xrayconfig.VmessShare;
import cn.gov.xivpn2.xrayconfig.VmessUser;
import cn.gov.xivpn2.xrayconfig.WsSettings;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SubscriptionWork extends Worker {
    private static final String TAG = "SubscriptionWorker";

    public SubscriptionWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "doWork");

        // start foreground service

        Notification foregroundNotification = new Notification.Builder(getApplicationContext(), "XiVPNService")
                .setContentText(getApplicationContext().getString(R.string.subscription_updating))
                .setOngoing(true)
                .setSmallIcon(R.drawable.baseline_refresh_24)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setForegroundAsync(new ForegroundInfo(NotificationID.getID(), foregroundNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE));
        } else {
            setForegroundAsync(new ForegroundInfo(NotificationID.getID(), foregroundNotification));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "sleep", e);
            return Result.success();
        }

        OkHttpClient client = new OkHttpClient();
        for (Subscription subscription : AppDatabase.getInstance().subscriptionDao().findAll()) {

            // update subscription

            Log.i(TAG, "begin update: " + subscription.label + ", " + subscription.url);

            Response response = null;
            try {

                Request request = new Request.Builder()
                        .url(subscription.url)
                        .build();

                response = client.newCall(request).execute();


                if (response.body() == null) continue;

                // delete old proxies
                AppDatabase.getInstance().proxyDao().deleteBySubscription(subscription.label);

                // parse subscription and add proxies
                String body = response.body().string();
                parse(body, subscription.label);

                Notification notification = new Notification.Builder(getApplicationContext(), "XiVPNSubscriptions")
                        .setContentTitle(getApplicationContext().getString(R.string.subscription_updated) + subscription.label)
                        .setSmallIcon(R.drawable.outline_info_24)
                        .build();
                getApplicationContext().getSystemService(NotificationManager.class).notify(NotificationID.getID(), notification);

            } catch (Exception e) {

                Log.e(TAG, "update " + subscription.label, e);

                // post error message
                Notification notification = new Notification.Builder(getApplicationContext(), "XiVPNSubscriptions")
                        .setContentTitle(getApplicationContext().getString(R.string.subscription_error) + subscription.label)
                        .setContentText(e.getClass().getSimpleName() + ": " + e.getMessage())
                        .setSmallIcon(R.drawable.baseline_error_24)
                        .build();
                getApplicationContext().getSystemService(NotificationManager.class).notify(NotificationID.getID(), notification);

            } finally {
                if (response != null) {
                    try {
                        response.close();
                    } catch (Exception e) {
                        Log.e(TAG, "close response", e);
                    }
                }
            }

        }

        // check if selected subscription is deleted
        // if so, set to default
        SharedPreferences sp = getApplicationContext().getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
        String selectedLabel = sp.getString("SELECTED_LABEL", "No Proxy (Bypass Mode)");
        String selectedSubscription = sp.getString("SELECTED_SUBSCRIPTION", "none");
        if (AppDatabase.getInstance().proxyDao().exists(selectedLabel, selectedSubscription) == 0) {
            SharedPreferences.Editor edit = sp.edit();
            edit.putString("SELECTED_LABEL", "No Proxy (Bypass Mode)");
            edit.putString("SELECTED_SUBSCRIPTION", "none");
            edit.apply();
        }

        Log.i(TAG, "doWork finish");
        return Result.success();
    }

    /**
     * Parse subscription text and add proxies
     *
     * @param text base64 encoded, one line per proxy
     */
    private void parse(String text, String label) throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
        // decode base64
        String textDecoded = new String(Base64.decode(text, Base64.DEFAULT), StandardCharsets.UTF_8);

        String[] lines = textDecoded.split("\\r?\\n");

        for (String line : lines) {
            Log.i(TAG, "parse " + line);

            Proxy proxy = null;

            try {
                if (line.startsWith("ss://")) {
                    proxy = parseShadowsocks(line);
                } else if (line.startsWith("vmess://")) {
                    proxy = parseVmess(line);
                } else if (line.startsWith("trojan://")) {
                    proxy = parseTrojan(line);
                }
            } catch (Exception e) {
                Log.e(TAG, "parse " + label + " " + line, e);
            }

            if (proxy == null) continue;

            proxy.subscription = label;

            int n = 2;
            String proxyLabel = proxy.label;
            while (AppDatabase.getInstance().proxyDao().find(proxy.label, proxy.subscription) != null) {
                // add number to label if already exists
                proxy.label = proxyLabel + " " + n;
                n++;
            }
            AppDatabase.getInstance().proxyDao().add(proxy);
        }
    }

    /**
     * Parse shadowsocks URI
     */
    private Proxy parseShadowsocks(String line) throws UnsupportedEncodingException {
        Proxy proxy = new Proxy();
        proxy.protocol = "shadowsocks";

        URI uri = URI.create(line);

        String userInfo = uri.getRawUserInfo();
        String hostname = uri.getHost();
        String port = String.valueOf(uri.getPort());
        String plugin = uri.getRawQuery();
        String label = uri.getRawFragment();

        Outbound<ShadowsocksSettings> outbound = new Outbound<>();
        outbound.settings = new ShadowsocksSettings();
        outbound.protocol = "shadowsocks";

        // network
        outbound.streamSettings = new StreamSettings();
        outbound.streamSettings.network = "tcp";
        outbound.streamSettings.security = "none";

        // shadowssocks server

        ShadowsocksServerSettings server = new ShadowsocksServerSettings();
        outbound.settings.servers.add(server);

        if (!userInfo.contains(":")) {
            userInfo = new String(Base64.decode(userInfo, Base64.URL_SAFE), StandardCharsets.UTF_8);
        } else {
            // userinfo is percent encoded if it is not base64 encoded
            userInfo = URLDecoder.decode(userInfo, "UTF-8");
        }

        String[] parts = userInfo.split(":");
        server.method = parts[0];
        server.password = parts[1];

        server.address = hostname;
        server.port = Integer.parseInt(port);
        server.out = false;

        proxy.label = URLDecoder.decode(label, "UTF-8");
        proxy.config = new Gson().toJson(outbound);

        return proxy;
    }

    private Proxy parseVmess(String line) {
        if (!line.startsWith("vmess://")) {
            throw new IllegalArgumentException("invalid vmess " + line);
        }

        line = line.substring(8);

        String json = new String(Base64.decode(line, Base64.URL_SAFE), StandardCharsets.UTF_8);

        Gson gson = new GsonBuilder().create();
        VmessShare vmessShare = gson.fromJson(json, VmessShare.class);

        Proxy proxy = new Proxy();
        proxy.label = vmessShare.ps;
        proxy.protocol = "vmess";

        Outbound<VmessSettings> outbound = new Outbound<>();
        outbound.protocol = "vmess";
        outbound.settings = new VmessSettings();

        VmessServerSettings server = new VmessServerSettings();
        outbound.settings.vnext.add(server);
        server.address = vmessShare.add;
        server.port = Integer.parseInt(vmessShare.port);

        VmessUser vmessUser = new VmessUser();
        vmessUser.security = vmessShare.security;
        vmessUser.id = vmessShare.id;
        server.users.add(vmessUser);

        outbound.streamSettings = new StreamSettings();
        outbound.streamSettings.network = vmessShare.network;
        if (vmessShare.network.equals("ws")) {
            outbound.streamSettings.wsSettings = new WsSettings();
            outbound.streamSettings.wsSettings.path = vmessShare.path;
            outbound.streamSettings.wsSettings.host = vmessShare.host;
        }
        if (vmessShare.network.equals("httpupgrade")) {
            outbound.streamSettings.httpupgradeSettings = new HttpUpgradeSettings();
            outbound.streamSettings.httpupgradeSettings.host = vmessShare.host;
            outbound.streamSettings.httpupgradeSettings.path = vmessShare.path;
        }
        if (vmessShare.network.equals("splithttp")) {
            outbound.streamSettings.splithttpSettings = new XHttpSettings();
            outbound.streamSettings.httpupgradeSettings.host = vmessShare.host;
            outbound.streamSettings.httpupgradeSettings.path = vmessShare.path;
        }
        if (!vmessShare.type.equals("none") && !vmessShare.type.equals("auto") && !vmessShare.type.isEmpty()) {
            throw new IllegalArgumentException("unsupported type " + vmessShare.type);
        }
        outbound.streamSettings.security = vmessShare.tls;
        if (outbound.streamSettings.security.equals("tls")) {
            outbound.streamSettings.tlsSettings = new TLSSettings();
            outbound.streamSettings.tlsSettings.allowInsecure = false;
            outbound.streamSettings.tlsSettings.serverName = vmessShare.sni;
            outbound.streamSettings.tlsSettings.alpn = vmessShare.alpn.split(",");
            outbound.streamSettings.tlsSettings.fingerprint = vmessShare.fingerprint;
        }

        proxy.config = gson.toJson(outbound);

        return proxy;
    }

    private Proxy parseTrojan(String line) throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
        if (!line.startsWith("trojan://")) {
            throw new IllegalArgumentException("invalid trojan " + line);
        }

        URI uri = new URI(line);
        Map<String, String> query = splitQuery(uri.getRawQuery());

        Proxy proxy = new Proxy();
        proxy.label = URLDecoder.decode(uri.getFragment(), "UTF-8");
        proxy.protocol = "trojan";

        Outbound<TrojanSettings> outbound = new Outbound<>();
        outbound.protocol = "trojan";
        outbound.settings = new TrojanSettings();

        TrojanServerSettings trojanServerSettings = new TrojanServerSettings();
        trojanServerSettings.address = uri.getHost();
        trojanServerSettings.port = uri.getPort();
        trojanServerSettings.password = uri.getUserInfo();
        outbound.settings.servers.add(trojanServerSettings);

        outbound.streamSettings = new StreamSettings();
        outbound.streamSettings.network = "tcp";
        outbound.streamSettings.security = "tls";
        outbound.streamSettings.tlsSettings = new TLSSettings();
        outbound.streamSettings.tlsSettings.allowInsecure = !query.containsKey("allowInsecure") && query.get("allowInsecure").equals("1");
        outbound.streamSettings.tlsSettings.serverName = query.getOrDefault("sni", uri.getHost());
        outbound.streamSettings.tlsSettings.alpn = !query.containsKey("alpn") ? new String[]{"h2", "http/1.1"} : query.get("alpn").split(",");
        outbound.streamSettings.tlsSettings.fingerprint = query.get("fp");

        proxy.config = new Gson().toJson(outbound);

        return proxy;

    }

    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
}
