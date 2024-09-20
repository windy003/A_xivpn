package cn.gov.xivpn2;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.service.SubscriptionWork;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
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
                    Log.d("ROOM", s + list);
                }, Executors.newSingleThreadExecutor())
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstance(db);

        db.proxyDao().addFreedom();

        // background work
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueueUniquePeriodicWork(
                "SUBSCRIPTION",
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                new PeriodicWorkRequest.Builder(SubscriptionWork.class, Duration.ofHours(1))
                        .build()
        );
    }
}
