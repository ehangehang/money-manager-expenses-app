package com.example.diglet.Configuration;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExpenseAccess {
    private SQLiteDatabase database;

    private ExpenseDb dbHelper;

    private String[] colsToReturn = { ExpenseDb.EXPENSE_ID, ExpenseDb.USER_ID,
            ExpenseDb.CATEGORY_ID, ExpenseDb.COST_COLUMN, ExpenseDb.DESCRIPTION_COLUMN,
            ExpenseDb.DAY_COLUMN, ExpenseDb.MONTH_COLUMN, ExpenseDb.YEAR_COLUMN };

    public ExpenseAccess(Context context) {
        dbHelper = new ExpenseDb(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Expense newExpense(BigDecimal cost, String description, int day, int mon, int yea, User us,
                              Category cat) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseDb.USER_ID, us.getId());
        cv.put(ExpenseDb.CATEGORY_ID, cat.getId());
        cv.put(ExpenseDb.COST_COLUMN, cost.movePointRight(2).longValueExact());
        cv.put(ExpenseDb.DESCRIPTION_COLUMN, description);
        cv.put(ExpenseDb.DAY_COLUMN, Integer.toString(day));
        cv.put(ExpenseDb.MONTH_COLUMN, Integer.toString(mon));
        cv.put(ExpenseDb.YEAR_COLUMN, Integer.toString(yea));
        long insertId = database.insert(ExpenseDb.EXPENSES_TABLE, null, cv);

        Cursor cur = database.query(ExpenseDb.EXPENSES_TABLE, colsToReturn,
                ExpenseDb.EXPENSE_ID + " = " + insertId, null, null, null, null);

        cur.moveToFirst();
        Expense ans = new Expense();
        ans.setId(cur.getInt(0)); // expense id
        ans.setUserId(cur.getInt(1)); // user id
        ans.setCategoryId(cur.getInt(2)); // category id
        ans.setCost(new BigDecimal(cur.getLong(3)).movePointLeft(2)); // cost from integer
        ans.setDescription(cur.getString(4)); // description
        ans.setDay(cur.getString(5)); // day
        ans.setMonth(cur.getString(6)); // month
        ans.setYear(cur.getString(7)); // year

        cur.close();

        return ans;
    }

    public Expense editExpense(Expense ex) {
        ContentValues cv = new ContentValues();
        cv.put(ExpenseDb.COST_COLUMN, ex.getCost().movePointRight(2).longValueExact());
        cv.put(ExpenseDb.DESCRIPTION_COLUMN, ex.getDescription());

        database.update(ExpenseDb.EXPENSES_TABLE, cv, ExpenseDb.EXPENSE_ID + " = '" +
                ex.getId() + "'", null);
        return ex;
    }

    public Expense deleteExpense(Expense exp) {
        // returns number of rows affected
        database.delete(ExpenseDb.EXPENSES_TABLE, ExpenseDb.EXPENSE_ID + " = '" + exp.getId()
                + "'", null);
        return exp;
    }

    public BigDecimal getTotalCost(User us, Category cat, int month, int year) {
        String[] cols = { ExpenseDb.COST_COLUMN };
        Cursor res = database.query(ExpenseDb.EXPENSES_TABLE, cols, ExpenseDb.USER_ID + " = '" +
                us.getId() + "' AND " + ExpenseDb.CATEGORY_ID + " = '" + cat.getId() + "' AND " +
                ExpenseDb.MONTH_COLUMN + " = '" + Integer.toString(month) + "' AND " +
                ExpenseDb.YEAR_COLUMN + " = '" + Integer.toString(year) + "'", null, null, null, null);

        BigDecimal totCost = new BigDecimal(0);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            totCost = totCost.add(new BigDecimal(res.getLong(0)));
            res.moveToNext();
        }
        res.close();

        return totCost.movePointLeft(2);
    }

    public BigDecimal getTotalCost(User us, int month, int year) {
        String[] cols = { ExpenseDb.COST_COLUMN };
        Cursor res = database.query(ExpenseDb.EXPENSES_TABLE, cols, ExpenseDb.USER_ID + " = '" +
                us.getId() + "' AND " + ExpenseDb.MONTH_COLUMN + " = '" + Integer.toString(month) +
                "' AND " + ExpenseDb.YEAR_COLUMN + " = '" + Integer.toString(year) + "'", null, null, null, null);

        BigDecimal totCost = new BigDecimal(0);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            totCost = totCost.add(new BigDecimal(res.getLong(0)));
            res.moveToNext();
        }
        res.close();

        // move decimal point
        return totCost.movePointLeft(2);
    }

    public List<Expense> getExpenses(User us, Category cat, int mon, int yea) {
        List<Expense> ans = new ArrayList<>();

        Cursor res = database.query(ExpenseDb.EXPENSES_TABLE, colsToReturn, ExpenseDb.USER_ID +
                " = '" + us.getId() + "' AND " + ExpenseDb.CATEGORY_ID + " = '" + cat.getId() +
                "' AND " + ExpenseDb.MONTH_COLUMN + " = '" + Integer.toString(mon) + "' AND " +
                ExpenseDb.YEAR_COLUMN + " = '" + Integer.toString(yea) + "'", null, null, null, null);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            Expense ex = new Expense();
            ex.setId(res.getInt(0)); // expense id
            ex.setUserId(res.getInt(1)); // user id
            ex.setCategoryId(res.getInt(2)); // category id
            ex.setCost(new BigDecimal(res.getLong(3)).movePointLeft(2)); // cost
            ex.setDescription(res.getString(4)); // description
            ex.setDay(res.getString(5)); // day
            ex.setMonth(res.getString(6)); // month
            ex.setYear(res.getString(7)); // year

            ans.add(ex);
            res.moveToNext();
        }

        res.close();
        return ans;
    }

    public List<Expense> getExpenses(User us, Category cat) {
        List<Expense> ans = new ArrayList<>();

        Cursor res = database.query(ExpenseDb.EXPENSES_TABLE, colsToReturn, ExpenseDb.USER_ID +
                " = '" + us.getId() + "' AND " + ExpenseDb.CATEGORY_ID + " = '" +
                cat.getId() + "'", null, null, null, null);

        res.moveToFirst();
        while (!res.isAfterLast()) {
            Expense ex = new Expense();
            ex.setId(res.getInt(0)); // expense id
            ex.setUserId(res.getInt(1)); // user id
            ex.setCategoryId(res.getInt(2)); // category id
            ex.setCost(new BigDecimal(res.getLong(3)).movePointLeft(2)); // cost
            ex.setDescription(res.getString(4)); // description
            ex.setDay(res.getString(5)); // day
            ex.setMonth(res.getString(6)); // month
            ex.setYear(res.getString(7)); // year

            ans.add(ex);
            res.moveToNext();
        }

        res.close();
        return ans;
    }
}
