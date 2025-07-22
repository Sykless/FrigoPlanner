package com.fra.frigoplanner;

import androidx.room.*;
import android.content.Context;
import java.util.List;

@Entity(primaryKeys = {"year", "month", "rowNumber"})
class Bouffe {
    public int year;
    public int month;
    public int rowNumber;
    public String name;
    public String type;
    public double price;
    public String expirationDate;
    public boolean eaten;

    public Bouffe(int year, int month, int rowNumber, String name, String type, double price) {
        this.year = year;
        this.month = month;
        this.rowNumber = rowNumber;
        this.name = name;
        this.type = type;
        this.price = price;
        this.expirationDate = null;
        this.eaten = false;
    }
}

@Dao
interface BouffeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Bouffe product);

    @Query("SELECT * FROM Bouffe WHERE type = :type")
    List<Bouffe> getBouffeByType(String type);

    @Query("SELECT * FROM Bouffe ORDER BY year desc, month desc, rowNumber desc limit 1")
    Bouffe getLatestBouffe();

    @Query("DELETE FROM Bouffe")
    void clearAll();
}

@Database(entities = {Bouffe.class}, version = 4)
public abstract class BouffeDatabase extends RoomDatabase {
    public abstract BouffeDao productDao();

    private static volatile BouffeDatabase INSTANCE;

    public static BouffeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BouffeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BouffeDatabase.class,
                            "bouffe.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
