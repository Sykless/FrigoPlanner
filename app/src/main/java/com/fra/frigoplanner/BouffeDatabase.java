package com.fra.frigoplanner;

import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import android.content.Context;
import java.util.List;

@Entity(
    primaryKeys = {"name"}
)
class BouffeDico {
    @NonNull
    public String name;
    public double averagePrice;
    public int portions;
    public String ticketName;
    public String aisle;

    public BouffeDico(@NonNull String name, double averagePrice, int portions, String ticketName, String aisle) {
        this.name = name;
        this.averagePrice = averagePrice;
        this.portions = portions;
        this.ticketName = ticketName;
        this.aisle = aisle;
    }
}

@Entity(
    primaryKeys = {"year", "month", "rowNumber"},
    foreignKeys = @ForeignKey(
        entity = BouffeDico.class,
        parentColumns = "name",
        childColumns = "name",
        onDelete = ForeignKey.NO_ACTION,
        onUpdate = ForeignKey.CASCADE
    )
)
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

    @Query("SELECT EXISTS(SELECT 1 FROM Bouffe WHERE year = :year AND month = :month AND rowNumber = :rowNumber)")
    boolean exists(int year, int month, int rowNumber);

    @Query("DELETE FROM Bouffe")
    void clearAll();

    @Query("SELECT * FROM Bouffe ORDER BY year desc, month desc, rowNumber desc limit 1")
    Bouffe getLatestBouffe();

    @Query("SELECT price FROM Bouffe WHERE name = :name ORDER BY year desc, month desc, rowNumber desc limit 10")
    List<Double> getLatestPrices(String name);
}

@Dao
interface BouffeDicoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(BouffeDico entry);

    @Query("DELETE FROM BouffeDico")
    void clearAll();

    @Query("SELECT * FROM BouffeDico ORDER BY name")
    List<BouffeDico> getAll();

    @Query("SELECT * FROM BouffeDico WHERE name = :name")
    BouffeDico getByName(String name);

    @Query("UPDATE BouffeDico SET averagePrice = :averagePrice WHERE name = :name")
    void updateAveragePrice(String name, double averagePrice);

    @Query("UPDATE BouffeDico SET ticketName = :ticketName WHERE name = :name")
    int updateTicketName(String ticketName, String name);

    @Query("SELECT ticketName FROM BouffeDico WHERE ticketName IS NOT NULL ORDER BY ticketName")
    List<String> getAllTicketNames();
}

@Database(entities = {Bouffe.class, BouffeDico.class}, version = 9)
public abstract class BouffeDatabase extends RoomDatabase {
    public abstract BouffeDao productDao();
    public abstract BouffeDicoDao dicoDao();

    private static volatile BouffeDatabase INSTANCE;

    public static BouffeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BouffeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BouffeDatabase.class,
                            "bouffe.db"
                    )
                    .addMigrations(MIGRATION)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE BouffeDico ADD COLUMN ticketName TEXT");
        }
    };
}