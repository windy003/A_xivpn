package cn.gov.xivpn2;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.room.Room;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.Executors;

import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.service.SubscriptionWork;
import cn.gov.xivpn2.ui.CrashActivity;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // crash
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH", "uncaught exception handler", throwable);

            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            Intent intent = new Intent(this, CrashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("EXCEPTION", exceptionAsString);
            startActivity(intent);
            System.exit(1);
        });

        System.loadLibrary("xivpn");

        // notification
        NotificationChannel channelVpnService = new NotificationChannel("XiVPNService", "Xi VPN Service", NotificationManager.IMPORTANCE_DEFAULT);
        channelVpnService.setDescription("Xi VPN Background Service");
        NotificationChannel channelSubscriptions = new NotificationChannel("XiVPNSubscriptions", "Xi VPN Subscription Update", NotificationManager.IMPORTANCE_DEFAULT);
        channelSubscriptions.setDescription("Xi VPN Subscription Update Worker");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channelVpnService);
        notificationManager.createNotificationChannel(channelSubscriptions);

        // database
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "xivpn")
                .setQueryCallback((s, list) -> {
                    // Log.d("ROOM", s + list);
                }, Executors.newSingleThreadExecutor())
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstance(db);

        db.proxyDao().addFreedom();
        db.proxyDao().addBlackhole();

        // background work
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueueUniquePeriodicWork(
                "SUBSCRIPTION",
                ExistingPeriodicWorkPolicy.UPDATE,
                new PeriodicWorkRequest.Builder(SubscriptionWork.class, Duration.ofHours(1))
                        .build()
        );

        // default routing rules
        File file = new File(getFilesDir(), "rules.json");
        if (!file.exists()) {
            Log.i("MyApplication", "copy default rules to " + file.getAbsolutePath());

            try {
                AssetManager assets = getAssets();
                InputStream inputStream = assets.open("default_rules.json");
                byte[] bytes = IOUtils.toByteArray(inputStream);
                inputStream.close();
                FileUtils.writeByteArrayToFile(file, bytes);
            } catch (IOException e) {
                Log.e("MyApplication", "copy default rules", e);
                throw new RuntimeException(e);
            }
        }

    }
}
