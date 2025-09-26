package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.TempProduct;

@Dao
public interface TempProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TempProduct tempProduct);

    @Query("SELECT EXISTS(SELECT 1 FROM TempProduct WHERE ticketFileName = :ticketFileName AND productId = :productId)")
    boolean exists(String ticketFileName, int productId);

    @Query("DELETE FROM TempProduct")
    void clearAll();
}
