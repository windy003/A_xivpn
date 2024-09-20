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
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.model.WorkSpec;

import com.google.common.net.PercentEscaper;
import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.gov.xivpn2.NotificationID;
import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.Subscription;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.ShadowsocksServerSettings;
import cn.gov.xivpn2.xrayconfig.ShadowsocksSettings;
import cn.gov.xivpn2.xrayconfig.StreamSettings;
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

        OkHttpClient client = new OkHttpClient();
        for (Subscription subscription : AppDatabase.getInstance().subscriptionDao().findAll()) {

            // update subscription

            Log.i(TAG, subscription.label + " " + subscription.url);

            Request request = new Request.Builder()
                    .url(subscription.url)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (response.body() == null) continue;

                // delete old proxies
                AppDatabase.getInstance().proxyDao().deleteBySubscription(subscription.label);

                // parse subscription and add proxies
                String body = response.body().string();
                parse(body, subscription.label);

                Notification notification = new Notification.Builder(getApplicationContext(), "XiVPNSubscriptions")
                        .setContentTitle(getApplicationContext().getString(R.string.subscription_updated) + subscription.label)
                        .setSmallIcon(R.drawable.baseline_error_24)
                        .build();
                getApplicationContext().getSystemService(NotificationManager.class).notify(NotificationID.getID(), notification);


            } catch (Exception e) {
                Log.e(TAG, "update " + subscription.label, e);

                // post error message
                Notification notification = new Notification.Builder(getApplicationContext(), "XiVPNSubscriptions")
                        .setContentTitle(getApplicationContext().getString(R.string.subscription_error) + subscription.label)
                        .setContentText(e.getMessage())
                        .setSmallIcon(R.drawable.baseline_error_24)
                        .build();
                getApplicationContext().getSystemService(NotificationManager.class).notify(NotificationID.getID(), notification);
            }
        }

        // check if selected subscription is deleted
        // if so, set to default
        SharedPreferences sp = getApplicationContext().getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
        String selectedLabel = sp.getString("SELECTED_LABEL", "Freedom");
        String selectedSubscription = sp.getString("SELECTED_SUBSCRIPTION", "none");
        if (AppDatabase.getInstance().proxyDao().exists(selectedLabel, selectedSubscription) == 0) {
            SharedPreferences.Editor edit = sp.edit();
            edit.putString("SELECTED_LABEL", "Freedom");
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
    private void parse(String text, String label) throws MalformedURLException, UnsupportedEncodingException {
        // decode base64
        String textDecoded = new String(Base64.decode(text, Base64.DEFAULT), StandardCharsets.UTF_8);

        String[] lines = textDecoded.split("\\r?\\n");

        for (String line : lines) {
            Log.i(TAG, "parse " + line);

            Proxy proxy = null;

            if (line.startsWith("ss://")) {
                proxy = parseShadowsocks(line);
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

        Pattern pattern = Pattern.compile("^ss:\\/\\/([0-9A-Za-z-_%:]+)@([\\w\\.-]+):(\\d+)\\/?(?:\\?([\\w-=\\&]+))?(?:#([\\w-%\\.]+))?$");
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid ss " + line);
        }

        String userInfo = matcher.group(1);
        String hostname = matcher.group(2);
        String port = matcher.group(3);
        String plugin = matcher.group(4);
        String label = matcher.group(5);

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
}
