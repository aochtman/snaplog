package com.example.postoffline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "posts.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_DATES = "dates";
    private static final String COLUMN_DATE_ID = "id";
    private static final String COLUMN_DATE = "date";

    private static final String TABLE_POSTS = "posts";
    private static final String COLUMN_POST_ID = "id";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_PHOTO = "photo";
    private static final String COLUMN_DATE_FOREIGN_KEY = "date_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_DATES_TABLE = "CREATE TABLE " + TABLE_DATES + "("
                + COLUMN_DATE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DATE + " TEXT UNIQUE)";
        db.execSQL(CREATE_DATES_TABLE);

        String CREATE_POSTS_TABLE = "CREATE TABLE " + TABLE_POSTS + "("
                + COLUMN_POST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_WEIGHT + " TEXT,"
                + COLUMN_DATE_FOREIGN_KEY + " INTEGER,"
                + COLUMN_PHOTO + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_DATE_FOREIGN_KEY + ") REFERENCES " + TABLE_DATES + "(" + COLUMN_DATE_ID + "))";
        db.execSQL(CREATE_POSTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATES);
        onCreate(db);
    }

    public boolean addDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        long result = db.insertWithOnConflict(TABLE_DATES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result != -1;
    }

    public int getDateId(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES + " WHERE " + COLUMN_DATE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{date});
        int dateId = -1;
        if (cursor.moveToFirst()) {
            dateId = cursor.getInt(cursor.getColumnIndex(COLUMN_DATE_ID));
        }
        cursor.close();
        db.close();
        return dateId;
    }
    private boolean isDateAlreadyAdded(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DATES, new String[]{COLUMN_DATE},
                COLUMN_DATE + " = ?", new String[]{date}, null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return exists;
    }
    public boolean addPost(String weight, String date, String photoPath) {
        int dateId;
        if (!isDateAlreadyAdded(date)) {
            addDate(date); // Ensure the date is added to the dates table
        }
        dateId = getDateId(date);

        System.out.println("date and id=" + date + "   id=" + dateId);

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_DATE_FOREIGN_KEY, dateId);
        values.put(COLUMN_PHOTO, photoPath != null ? photoPath : ""); // Handling null or empty photo path

        long result = db.insert(TABLE_POSTS, null, values);
        db.close();
        return result != -1;
    }

    public Cursor getPostsByDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT d." + COLUMN_DATE + ", p." + COLUMN_WEIGHT + ", p." + COLUMN_PHOTO + ", p." + COLUMN_POST_ID + " " +
                "FROM " + TABLE_POSTS + " p " +
                "JOIN " + TABLE_DATES + " d ON p." + COLUMN_DATE_FOREIGN_KEY + " = d." + COLUMN_DATE_ID + " " +
                "ORDER BY d." + COLUMN_DATE + " DESC, p." + COLUMN_POST_ID + " ASC";
        return db.rawQuery(query, null);
    }

    public String getFirstNonEmptyWeight(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY +
                " = (SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES + " WHERE " +
                COLUMN_DATE + " = ?) ORDER BY " + COLUMN_POST_ID + " ASC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{date});
        String firstNonEmptyWeight = null;
        if (cursor.moveToFirst()) {
            firstNonEmptyWeight = cursor.getString(cursor.getColumnIndex(COLUMN_WEIGHT));
        }
        cursor.close();
        db.close();
        return firstNonEmptyWeight;
    }
    public String getDateOfFirstRecord() {
        SQLiteDatabase db = this.getReadableDatabase();
        String date = null;

        // Query to get the earliest date
        String query = "SELECT " + COLUMN_DATE + " FROM " + TABLE_DATES +
                " ORDER BY " + COLUMN_DATE + " ASC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
        }
        cursor.close();
        db.close();
        return date;
    }
    public boolean isFirstWeightEntryForDate(String date, String weight) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get the first weight entry for the given date
        String query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY + " = (" +
                "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                " WHERE " + COLUMN_DATE + " = ?) " +
                "ORDER BY " + COLUMN_POST_ID + " ASC LIMIT 1";

        Cursor cursor = db.rawQuery(query, new String[]{date});
        String firstWeightEntry = null;
        if (cursor.moveToFirst()) {
            firstWeightEntry = cursor.getString(cursor.getColumnIndex(COLUMN_WEIGHT));
        }
        cursor.close();
        db.close();

        // Compare the given weight with the first weight entry
        return weight.equals(firstWeightEntry);
    }

    public String getLastWeightOfPreviousRecentDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        String weight = "";

        // Step 1: Get the second most recent date
        String queryDate = "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                " ORDER BY " + COLUMN_DATE + " DESC LIMIT 1 OFFSET 1";
        Cursor cursorDate = db.rawQuery(queryDate, null);
        int dateId = -1;

        if (cursorDate.moveToFirst()) {
            dateId = cursorDate.getInt(cursorDate.getColumnIndex(COLUMN_DATE_ID));
            Log.d("SecondMostRecentDate", "Date ID: " + dateId);
        } else {
            Log.d("SecondMostRecentDate", "No second most recent date found.");
        }
        cursorDate.close();

        // Step 2: Get the last weight record for the second most recent date
        if (dateId != -1) {
            String queryWeight = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                    " WHERE " + COLUMN_DATE_FOREIGN_KEY + " = ? " +
                    " ORDER BY " + COLUMN_POST_ID + " DESC LIMIT 1";
            Cursor cursorWeight = db.rawQuery(queryWeight, new String[]{String.valueOf(dateId)});

            if (cursorWeight.moveToFirst()) {
                weight = cursorWeight.getString(cursorWeight.getColumnIndex(COLUMN_WEIGHT));
            }
            cursorWeight.close();
        }

        db.close();
        return weight;
    }

    public String getLastWeightOfProvidedDate(String providedDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String weight = "";

        // Step 1: Get the ID of the provided date
        String queryDate = "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                " WHERE " + COLUMN_DATE + " = ?";
        Cursor cursorDate = db.rawQuery(queryDate, new String[]{providedDate});
        int dateId = -1;

        if (cursorDate.moveToFirst()) {
            dateId = cursorDate.getInt(cursorDate.getColumnIndex(COLUMN_DATE_ID));
            Log.d("ProvidedDate", "Date ID: " + dateId);
        } else {
            Log.d("ProvidedDate", "Provided date not found in the database.");
        }
        cursorDate.close();

        // Step 2: Get the last weight record for the provided date
        if (dateId != -1) {
            String queryWeight = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                    " WHERE " + COLUMN_DATE_FOREIGN_KEY + " = ? " +
                    " ORDER BY " + COLUMN_POST_ID + " DESC";
            Cursor cursorWeight = db.rawQuery(queryWeight, new String[]{String.valueOf(dateId)});

            // Iterate through the cursor to find the last non-empty weight
            while (cursorWeight.moveToNext()) {
                weight = cursorWeight.getString(cursorWeight.getColumnIndex(COLUMN_WEIGHT));
                if (!weight.isEmpty()) {
                    break;
                }
            }
            cursorWeight.close();
        }

        db.close();
        return weight;
    }

    public List<String> getAllDates() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to select all distinct dates
        String query = "SELECT DISTINCT " + COLUMN_DATE + " FROM " + TABLE_DATES;

        Cursor cursor = db.rawQuery(query, null);

        // Iterate through the cursor to get all dates
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE));
                dates.add(date);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return dates;
    }

    public String getSecondNonEmptyWeight(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY +
                " = (SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES + " WHERE " +
                COLUMN_DATE + " = ?) ORDER BY " + COLUMN_POST_ID + " ASC LIMIT 1 OFFSET 1";
        Cursor cursor = db.rawQuery(query, new String[]{date});
        String secondNonEmptyWeight = null;
        if (cursor.moveToFirst()) {
            secondNonEmptyWeight = cursor.getString(cursor.getColumnIndex(COLUMN_WEIGHT));
        }
        cursor.close();
        db.close();
        return secondNonEmptyWeight;
    }


    public String getSecondToLastNonEmptyWeight(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY + " = (" +
                "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                " WHERE " + COLUMN_DATE + " = ?) " +
                "ORDER BY " + COLUMN_POST_ID + " DESC LIMIT 1 OFFSET 1";
        Cursor cursor = db.rawQuery(query, new String[]{date});
        String secondToLastNonEmptyWeight = null;
        if (cursor.moveToFirst()) {
            secondToLastNonEmptyWeight = cursor.getString(cursor.getColumnIndex(COLUMN_WEIGHT));
        }
        cursor.close();
        db.close();
        return secondToLastNonEmptyWeight;
    }

    public String getPreviousNonEmptyWeight(String date, String currentWeight) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Subquery to get the ID of the current weight
        String subQuery = "(SELECT " + COLUMN_POST_ID + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_DATE_FOREIGN_KEY + " = (" +
                "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                " WHERE " + COLUMN_DATE + " = ?) AND " + COLUMN_WEIGHT + " = ?)";

        // Query to get the weight before the current weight
        String query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY + " = (" +
                "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                " WHERE " + COLUMN_DATE + " = ?) AND " + COLUMN_POST_ID + " < " + subQuery +
                " ORDER BY " + COLUMN_POST_ID + " DESC LIMIT 1";



        Cursor cursor = db.rawQuery(query, new String[]{date, currentWeight});
        String previousNonEmptyWeight = "";
        if (cursor.moveToFirst()) {
            previousNonEmptyWeight = cursor.getString(cursor.getColumnIndex(COLUMN_WEIGHT));
            Log.d("Query", "Previous Weight Query: " + previousNonEmptyWeight);
        } else {
            // If no weight is found before the current weight, get the next weight
            query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                    " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY + " = (" +
                    "SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES +
                    " WHERE " + COLUMN_DATE + " = ?) AND " + COLUMN_POST_ID + " > " + subQuery +
                    " ORDER BY " + COLUMN_POST_ID + " ASC LIMIT 1";
          //  Log.d("Query", "Next Weight Query: " + query);
            cursor = db.rawQuery(query, new String[]{date, currentWeight});
            if (cursor.moveToFirst()) {
                previousNonEmptyWeight = cursor.getString(cursor.getColumnIndex(COLUMN_WEIGHT));
                Log.d("Query", "Previous Weight Query2: " + previousNonEmptyWeight);
            }
        }
        cursor.close();
        db.close();
        return previousNonEmptyWeight;
    }



    public String getLastNonEmptyWeight(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_WEIGHT + " FROM " + TABLE_POSTS +
                " WHERE " + COLUMN_WEIGHT + " != '' AND " + COLUMN_DATE_FOREIGN_KEY +
                " = (SELECT " + COLUMN_DATE_ID + " FROM " + TABLE_DATES + " WHERE " +
                COLUMN_DATE + " = ?) ORDER BY " + COLUMN_POST_ID + " DESC LIMIT 1";
        Log.d("SQLQuery", query); // Log the query for debugging
        Cursor cursor = db.rawQuery(query, new String[]{date});
        String lastNonEmptyWeight = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int weightIndex = cursor.getColumnIndex(COLUMN_WEIGHT);
                if (weightIndex != -1) {
                    lastNonEmptyWeight = cursor.getString(weightIndex);
                    Log.d("LastNonEmptyWeight", "Weight: " + lastNonEmptyWeight); // Log the result
                }
            } else {
                Log.d("LastNonEmptyWeight", "No non-empty weight found");
            }
            cursor.close();
        }
        db.close();
        return lastNonEmptyWeight;
    }

    public String getPhotoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_PHOTO + " FROM " + TABLE_POSTS + " WHERE " + COLUMN_POST_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});

        String photo = "";
        if (cursor.moveToFirst()) {
            photo = cursor.getString(cursor.getColumnIndex(COLUMN_PHOTO));
        }
        cursor.close();
        db.close();
        return photo;
    }

    public boolean updateItem(int id, String weight, String date, String photo) {
        int dateId = getDateId(date);
        if (dateId == -1) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_WEIGHT, weight);
        contentValues.put(COLUMN_DATE_FOREIGN_KEY, dateId);
        contentValues.put(COLUMN_PHOTO, photo);

        int result = db.update(TABLE_POSTS, contentValues, COLUMN_POST_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }

    public boolean deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_POSTS, COLUMN_POST_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }
}
