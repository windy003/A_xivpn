package cn.gov.xivpn2.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProxyDao {
    @Query("SELECT * FROM proxy")
    List<Proxy> findAll();

    @Query("SELECT * FROM proxy WHERE subscription = :subscription")
    List<Proxy> findBySubscription(String subscription);

    @Query("SELECT * FROM proxy WHERE subscription = :subscription AND label = :label LIMIT 1")
    Proxy find(String label, String subscription);

    @Insert()
    void add(Proxy proxy);

    @Query("INSERT OR IGNORE INTO proxy (label, protocol, subscription, config) VALUES (\"Freedom\", \"freedom\", \"none\", \"{}\")")
    void addFreedom();

    @Query("SELECT count(*) FROM proxy WHERE label = :label AND subscription = :subscription LIMIT 1")
    int exists(String label, String subscription);

    @Query("UPDATE proxy SET config = :config WHERE label = :label AND subscription = :subscription")
    void updateConfig(String label, String subscription, String config);

    @Query("DELETE FROM proxy WHERE label = :label AND subscription = :subscription")
    void delete(String label, String subscription);

}
