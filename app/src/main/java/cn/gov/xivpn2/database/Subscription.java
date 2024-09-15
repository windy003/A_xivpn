package cn.gov.xivpn2.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        indices = {
                @Index(value = {"label"}, unique = true),
        }
)
public class Subscription {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String label;
    public String url;
    public int autoUpdate;
}
