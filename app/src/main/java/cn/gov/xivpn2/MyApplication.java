package cn.gov.xivpn2;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        System.loadLibrary("xivpn");

        NotificationChannel channel = new NotificationChannel("XiVPNService", "Xi Vpn Service", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Xi Vpn Background Service");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}
