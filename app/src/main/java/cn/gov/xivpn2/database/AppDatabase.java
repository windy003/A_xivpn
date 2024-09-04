package cn.gov.xivpn2.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Proxy.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProxyDao proxyDao();

    private static AppDatabase instance;

    public static AppDatabase getInstance() {
        return instance;
    }

    public static void setInstance(AppDatabase instance) {
        AppDatabase.instance = instance;
    }
}