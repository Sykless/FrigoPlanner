package com.fra.frigoplanner.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fra.frigoplanner.data.db.entity.BouffeDico;

import java.util.List;

@Dao
public interface BouffeDicoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(BouffeDico entry);

    @Query("DELETE FROM BouffeDico")
    void clearAll();

    @Query("SELECT * FROM BouffeDico ORDER BY name")
    List<BouffeDico> getAll();

    @Query("SELECT * FROM BouffeDico WHERE name = :name")
    BouffeDico getByName(String name);

    @Query("SELECT * FROM BouffeDico WHERE ticketName = :ticketName")
    BouffeDico getByTicketName(String ticketName);

    @Query("UPDATE BouffeDico SET averagePrice = :averagePrice WHERE name = :name")
    void updateAveragePrice(String name, double averagePrice);

    @Query("UPDATE BouffeDico SET ticketName = :ticketName WHERE name = :name")
    int updateTicketName(String ticketName, String name);

    @Query("SELECT ticketName FROM BouffeDico WHERE ticketName IS NOT NULL ORDER BY ticketName")
    List<String> getAllTicketNames();
}
