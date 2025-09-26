package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.TempProduct;

import java.util.List;

@Dao
public interface TempProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TempProduct tempProduct);

    @Query("SELECT EXISTS(SELECT 1 FROM TempProduct WHERE ticketFileName = :ticketFileName AND productId = :productId)")
    boolean exists(String ticketFileName, int productId);

    @Query("DELETE FROM TempProduct")
    void clearAll();

    @Query("SELECT * FROM TempProduct WHERE productName = :productName AND productType = :productType AND ABS(price - :price) < 0.001 ")
    List<TempProduct> getSimilarTempProducts(String productName, String productType, double price);

    @Query("SELECT COUNT(*) FROM TempProduct WHERE ticketFileName = :ticketFileName")
    int getTicketSize(String ticketFileName);

    @Query("DELETE FROM TempProduct WHERE ticketFileName = :ticketFileName")
    void deleteTicket(String ticketFileName);
}
