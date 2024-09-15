package cn.gov.xivpn2.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Proxy.class, Subscription.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProxyDao proxyDao();

    public abstract SubscriptionDao subscriptionDao();

    private static AppDatabase instance;

    public static AppDatabase getInstance() {
        return instance;
    }

    public static void setInstance(AppDatabase instance) {
        AppDatabase.instance = instance;
    }
}