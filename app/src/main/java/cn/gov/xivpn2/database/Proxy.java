package cn.gov.xivpn2.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        indices = {
                @Index(value = {"label", "subscription"}, unique = true),
        }
)
public class Proxy {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String subscription;
    public String protocol;
    public String label;
    public String config;

    @Ignore
    public int ping = 0;
}
