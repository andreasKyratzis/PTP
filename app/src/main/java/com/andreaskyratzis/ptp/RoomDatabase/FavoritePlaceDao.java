package com.andreaskyratzis.ptp.RoomDatabase;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface FavoritePlaceDao {

    @Insert
    long insertPlace(FavoritePlaces place);

    @Query("select * from FavoritePlaces")
    List<FavoritePlaces> getFavoritePlaces();

    @Delete
    void deletePlace(FavoritePlaces places);
}
