package com.example.diglet.Configuration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class UserAccess {
    private SQLiteDatabase database;

    private ExpenseDb dbHelper;

    private String[] colsToReturn = { ExpenseDb.USER_ID,
            ExpenseDb.USER_NAME };

    public UserAccess(Context context) {
        dbHelper = new ExpenseDb(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public boolean exists(String user) {
        Cursor res = database.query(ExpenseDb.USERS_TABLE, colsToReturn, ExpenseDb.USER_NAME +
                " = '" + user + "'", null, null, null, null);
        int cnt = res.getCount();
        res.close();
        return cnt > 0;
    }

    public User newUser(String name) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseDb.USER_NAME, name);

        try {
            long insertId = database.insert(ExpenseDb.USERS_TABLE, null, cv);

            Cursor cursor = database.query(ExpenseDb.USERS_TABLE, colsToReturn, ExpenseDb.USER_ID +
                    " = " + insertId, null, null, null, null);

            if (insertId > 0) {
                cursor.moveToFirst();
                User us = new User();
                us.setId(cursor.getInt(0));
                us.setName(cursor.getString(1));
                cursor.close();
                return us;
            } else {
                return null;
            }
        } catch (SQLiteConstraintException ce) {
            return null;
        }
    }

    public User editUser(User name) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseDb.USER_NAME, name.getName());
        database.update(ExpenseDb.USERS_TABLE, cv, ExpenseDb.USER_ID + " = '" +
                name.getId() + "'", null);
        return name;
    }

    public User deleteUser(User user) {
        database.delete(ExpenseDb.EXPENSES_TABLE, ExpenseDb.USER_ID +
                " = '" + user.getId() + "'", null);
        database.delete(ExpenseDb.CATEGORIES_TABLE, ExpenseDb.USER_ID +
                " = '" + user.getId() + "'", null);
        database.delete(ExpenseDb.USERS_TABLE, ExpenseDb.USER_ID +
                " = '" + user.getId() + "'", null);
        return user;
    }

    public List<User> getAllUsers() {
        List<User> ans = new ArrayList<>();

        // query db and get all users
        Cursor res = database.query(ExpenseDb.USERS_TABLE, colsToReturn,
                null, null, null, null, null);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            User u = new User();
            u.setId(res.getInt(0));
            u.setName(res.getString(1));
            ans.add(u);
            res.moveToNext();
        }

        res.close();
        return ans;
    }
}
