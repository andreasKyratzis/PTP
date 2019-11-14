package com.andreaskyratzis.ptp.RoomDatabase;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {FavoritePlaces.class},version = 1, exportSchema = false)
public abstract class PlacesDataBase extends RoomDatabase{

    public abstract FavoritePlaceDao placeDao();
}
