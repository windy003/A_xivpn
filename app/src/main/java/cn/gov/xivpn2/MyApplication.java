package cn.gov.xivpn2;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import cn.gov.xivpn2.database.AppDatabase;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        System.loadLibrary("xivpn");

        NotificationChannel channel = new NotificationChannel("XiVPNService", "Xi Vpn Service", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Xi Vpn Background Service");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "xivpn")
                .setQueryCallback((s, list) -> {
                    Log.d("ROOM", s + list);
                }, Executors.newSingleThreadExecutor())
                .allowMainThreadQueries()
                .build();
        AppDatabase.setInstance(db);

        db.proxyDao().addFreedom();
    }
}
