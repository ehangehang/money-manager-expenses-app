package com.example.diglet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.diglet.Configuration.CategoryAccess;
import com.example.diglet.Configuration.User;
import com.example.diglet.Configuration.UserAccess;

import java.util.List;

public class UserActivity extends ListActivity {

    private UserAccess uSource;

    private ActionMode aMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        private AdapterView.OnItemClickListener lstn;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_user, menu);

            ListView lv = getListView();
            lstn = lv.getOnItemClickListener();
            lv.setOnItemClickListener(null);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_edit:
                    editUser();
                    mode.finish();
                    return true;
                case R.id.action_delete:
                    deleteUser();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            final ListView lv = getListView();
            lv.clearChoices();
            lv.setItemChecked(lv.getCheckedItemPosition(), false);
            lv.post(new Runnable() {
                @Override
                public void run() {
                    lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
                }
            });
            aMode = null;

            getListView().setOnItemClickListener(lstn);
        }
    };

    private class GetUsers extends AsyncTask<Void, Void, List<User>> {
        @Override
        protected List<User> doInBackground(Void... params) {
            return uSource.getAllUsers();
        }

        @Override
        protected void onPostExecute(List<User> result) {
            final ArrayAdapter<User> adapter = new ArrayAdapter<>(UserActivity.this,
                    android.R.layout.simple_list_item_activated_1, result);
            setListAdapter(adapter);

            final ListView lv = getListView();
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    User us = adapter.getItem(i);

                    MainConfig gc = (MainConfig) getApplication();
                    gc.setCurrentUser(us);

                    Intent intent = new Intent(UserActivity.this, CategoriesActivity.class);
                    startActivity(intent);
                }
            });

            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> aView, View view, int i, long l) {
                    if (aMode != null) {
                        return false;
                    }
                    lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    lv.setItemChecked(i, true);
                    aMode = UserActivity.this.startActionMode(mActionModeCallback);
                    return true;
                }
            });

            if (result.size() == 0) {
                addUser();
            }
        }
    }

    private class AddUser extends AsyncTask<String, Void, User> {
        @Override
        protected User doInBackground(String... params) {
            User newU = uSource.newUser(params[0]);

            CategoryAccess cSource = new CategoryAccess(UserActivity.this);
            cSource.open();
            cSource.newCategory("Rent", newU);
            cSource.newCategory("Bills", newU);
            cSource.newCategory("Groceries", newU);
            cSource.newCategory("Eating out", newU);
            cSource.newCategory("Shopping", newU);
            cSource.newCategory("Gas", newU);
            cSource.close();

            return newU;
        }

        @Override
        protected void onPostExecute(User result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<User> adapter = (ArrayAdapter<User>) getListAdapter();
            adapter.add(result);
            adapter.notifyDataSetChanged();

            int pos = adapter.getPosition(result);
            getListView().performItemClick(null, pos, adapter.getItemId(pos));
        }
    }

    private class EditUser extends AsyncTask<User, Void, User> {
        @Override
        protected User doInBackground(User... params) {
            return uSource.editUser(params[0]);
        }

        @Override
        protected void onPostExecute(User result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
            aa.notifyDataSetChanged();
        }
    }

    private class DeleteUser extends AsyncTask<User, Void, User> {
        @Override
        protected User doInBackground(User... params) {
            return uSource.deleteUser(params[0]);
        }

        @Override
        protected void onPostExecute(User result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
            aa.remove(result);
            aa.notifyDataSetChanged();
        }
    }

    private void addUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create user");
        builder.setMessage("Please enter your name.");

        final EditText enterName = new EditText(this);
        enterName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        enterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        builder.setView(enterName);

        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();

        enterName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dia.getButton(Dialog.BUTTON_POSITIVE).performClick();
                    handled = true;
                }
                return handled;
            }
        });

        dia.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dia.show();

        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = enterName.getText().toString().trim();

                if (username.equals("")) {
                    enterName.setError("Please enter a name.");
                } else if (uSource.exists(username)) {
                    enterName.setError("This user already exists.");
                } else {
                    new AddUser().execute(username);
                    dia.dismiss();
                }
            }
        });
    }

    private void editUser() {
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
        final User userToEdi = aa.getItem(lv.getCheckedItemPosition());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit user");
        builder.setMessage("Please enter a new name.");

        final EditText enterName = new EditText(this);
        enterName.setText(userToEdi.getName());
        enterName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        enterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        builder.setView(enterName);

        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();

        enterName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dia.getButton(Dialog.BUTTON_POSITIVE).performClick();
                    handled = true;
                }
                return handled;
            }
        });

        dia.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        dia.show();

        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = enterName.getText().toString().trim();

                if (username.equals("")) {
                    enterName.setError("Please enter a name.");
                } else if (uSource.exists(username)) {
                    enterName.setError("This user already exists.");
                } else {
                    userToEdi.setName(username);
                    new EditUser().execute(userToEdi);
                    dia.dismiss();
                }
            }
        });
    }

    private void deleteUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete user");
        builder.setMessage("Are you sure? All expenses for this user will be deleted.");

        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();

        dia.show();

        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<User> aa = (ArrayAdapter<User>) getListAdapter();
        final User userToDel = aa.getItem(lv.getCheckedItemPosition());

        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DeleteUser().execute(userToDel);
                dia.dismiss();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        uSource = new UserAccess(this);
        uSource.open();
        new GetUsers().execute();
    }

    @Override
    protected void onResume() {
        uSource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        uSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_new) {
            addUser();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
