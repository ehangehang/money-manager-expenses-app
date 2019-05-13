package com.example.diglet;

import android.app.Application;

import com.example.diglet.Configuration.User;
import com.example.diglet.Configuration.UserAccess;

import java.util.Calendar;
import java.util.List;

public class MainConfig extends Application {
    private User currentUser;

    private Calendar currentDate;

    public Calendar getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(Calendar date) {
        this.currentDate = date;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setCurrentDate(Calendar.getInstance());
        UserAccess uSource = new UserAccess(this);
        uSource.open();
        List<User> lu = uSource.getAllUsers();

        if (lu.size() == 1) {
            User soleUser = lu.get(0);
            setCurrentUser(soleUser);
        }

        uSource.close();
    }
}
