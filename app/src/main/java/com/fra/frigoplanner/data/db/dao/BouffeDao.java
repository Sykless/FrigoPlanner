package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.Bouffe;

import java.util.List;

@Dao
public interface BouffeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Bouffe product);

    @Query("SELECT EXISTS(SELECT 1 FROM Bouffe WHERE year = :year AND month = :month AND rowNumber = :rowNumber)")
    boolean exists(int year, int month, int rowNumber);

    @Query("DELETE FROM Bouffe")
    void clearAll();

    @Query("SELECT * FROM Bouffe ORDER BY year desc, month desc, rowNumber desc limit 1")
    Bouffe getLatestBouffe();

    @Query("SELECT price FROM Bouffe WHERE name = :name ORDER BY year desc, month desc, rowNumber desc limit 10")
    List<Double> getLatestPrices(String name);
}
