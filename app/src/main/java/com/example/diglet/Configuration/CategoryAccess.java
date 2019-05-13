package com.example.diglet.Configuration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class CategoryAccess {
    private SQLiteDatabase database;

    private ExpenseDb dbHelper;

    private String[] colsToReturn = { ExpenseDb.CATEGORY_ID, ExpenseDb.USER_ID,
            ExpenseDb.CATEGORY_NAME };

    public CategoryAccess(Context context) {
        dbHelper = new ExpenseDb(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public boolean exists(String category, User us) {
        Cursor res = database.query(ExpenseDb.CATEGORIES_TABLE, colsToReturn, ExpenseDb.CATEGORY_NAME +
                " = '" + category + "' AND " + ExpenseDb.USER_ID + " = '" + us.getId() + "'", null, null, null, null);
        int cnt = res.getCount();
        res.close();
        return cnt > 0;
    }

    public Category newCategory(String cat, User us) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseDb.CATEGORY_NAME, cat);
        cv.put(ExpenseDb.USER_ID, us.getId());
        long insertId = database.insert(ExpenseDb.CATEGORIES_TABLE, null, cv);

        Cursor cur = database.query(ExpenseDb.CATEGORIES_TABLE, colsToReturn,
                ExpenseDb.CATEGORY_ID + " = " + insertId, null, null, null, null);

        cur.moveToFirst();
        Category ans = new Category();
        ans.setId(cur.getInt(0));
        ans.setUserId(cur.getInt(1));
        ans.setCategory(cur.getString(2));
        cur.close();

        return ans;
    }

    public Category editCategory(Category cat) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseDb.CATEGORY_NAME, cat.getCategory());
        database.update(ExpenseDb.CATEGORIES_TABLE, cv, ExpenseDb.CATEGORY_ID + " = '" +
                cat.getId() + "'", null);
        return cat;
    }

    public Category deleteCategory(Category cat) {
        // delete expenses for the category
        database.delete(ExpenseDb.EXPENSES_TABLE, ExpenseDb.CATEGORY_ID + " = '" + cat.getId() + "'", null);
        // delete category
        database.delete(ExpenseDb.CATEGORIES_TABLE, ExpenseDb.CATEGORY_ID + " = '" + cat.getId() + "'", null);
        return cat;
    }

    public List<Category> getCategories(User us) {
        List<Category> ans = new ArrayList<>();

        Cursor res = database.query(ExpenseDb.CATEGORIES_TABLE, colsToReturn,
                ExpenseDb.USER_ID + " = '" + us.getId() + "'", null, null, null, null);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            Category cat = new Category();
            cat.setId(res.getInt(0));
            cat.setUserId(res.getInt(1));
            cat.setCategory(res.getString(2));
            ans.add(cat);
            res.moveToNext();
        }

        res.close();
        return ans;
    }
}
