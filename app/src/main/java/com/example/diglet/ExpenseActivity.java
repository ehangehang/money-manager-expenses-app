package com.example.diglet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.diglet.Configuration.Category;
import com.example.diglet.Configuration.Expense;
import com.example.diglet.Configuration.ExpenseAccess;
import com.example.diglet.Configuration.IntentTags;
import com.example.diglet.Configuration.User;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class ExpenseActivity extends ListActivity {

    private ExpenseAccess exSource;

    private User curUser;

    private static Calendar date;

    private Category curCat;

    private BigDecimal categoryTotal;

    private ActionMode aMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        private TextView title;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_expenses, menu);

            title = (TextView) findViewById(R.id.exCat);
            title.setClickable(false);
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
                    editExpense();
                    mode.finish();
                    return true;
                case R.id.action_delete:
                    deleteExpense();
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

            title.setClickable(true);
        }
    };

    private class GetExpenses extends AsyncTask<Void, Void, List<Expense>> {
        @Override
        protected List<Expense> doInBackground(Void... params) {
            return exSource.getExpenses(curUser, curCat, date.get(Calendar.MONTH), date.get(Calendar.YEAR));
        }

        @Override
        protected void onPostExecute(final List<Expense> result) {
            ArrayAdapter<Expense> aa = new ArrayAdapter<>(ExpenseActivity.this,
                    android.R.layout.simple_list_item_activated_1, result);
            setListAdapter(aa);

            final ListView lv = getListView();
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> aView, View view, int i, long l) {
                    if (aMode != null) {
                        return false;
                    }
                    lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    lv.setItemChecked(i, true);
                    aMode = ExpenseActivity.this.startActionMode(mActionModeCallback);
                    return true;
                }
            });
        }
    }

    private class AddExpense extends AsyncTask<String, Void, Expense> {
        @Override
        protected Expense doInBackground(String... params) {
            return exSource.newExpense(new BigDecimal(params[0]), params[1],
                    date.get(Calendar.DATE), date.get(Calendar.MONTH),
                    date.get(Calendar.YEAR), curUser, curCat);
        }

        @Override
        protected void onPostExecute(Expense result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
            aa.add(result);
            aa.notifyDataSetChanged();

            categoryTotal = categoryTotal.add(result.getCost());
            TextView total = (TextView) findViewById(R.id.exTotal);
            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(categoryTotal));
        }
    }

    private class EditExpense extends AsyncTask<Expense, Void, Expense> {
        @Override
        protected Expense doInBackground(Expense... params) {
            return exSource.editExpense(params[0]);
        }

        @Override
        protected void onPostExecute(Expense result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
            aa.notifyDataSetChanged();

            categoryTotal = categoryTotal.add(result.getCost());
            TextView total = (TextView) findViewById(R.id.exTotal);
            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(categoryTotal));
        }
    }

    private class DeleteExpense extends AsyncTask<Expense, Void, Expense> {
        @Override
        protected Expense doInBackground(Expense... params) {
            return exSource.deleteExpense(params[0]);
        }

        @Override
        protected void onPostExecute(Expense result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
            aa.remove(result);
            aa.notifyDataSetChanged();

            categoryTotal = categoryTotal.subtract(result.getCost());
            TextView total = (TextView) findViewById(R.id.exTotal);
            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(categoryTotal));
        }
    }

    private void addExpense() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record expense");
        builder.setMessage("Please enter expense details.");

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        final EditText enterCost = new EditText(this);
        final EditText enterDesc = new EditText(this);
        enterCost.setHint("Cost");
        enterDesc.setHint("Description (optional)");
        enterCost.setInputType(InputType.TYPE_CLASS_NUMBER);
        enterCost.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        enterDesc.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        enterDesc.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        ll.addView(enterCost);
        ll.addView(enterDesc);
        builder.setView(ll);

        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();

        enterDesc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
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
                String cost = enterCost.getText().toString().trim();
                String desc = enterDesc.getText().toString().trim();

                if (cost.equals("")) {
                    enterCost.setError("Please enter a dollar amount.");
                } else if (!Pattern.matches("^(\\d{1,10})?(\\.\\d{0,2})?$", cost)) {
                    enterCost.setError("Please enter a valid dollar amount.");
                } else {
                    new AddExpense().execute(cost, desc);
                    dia.dismiss();
                }
            }
        });
    }

    private void editExpense() {
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
        final Expense exToEdi = aa.getItem(lv.getCheckedItemPosition());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit expense");
        builder.setMessage("Please enter expense details.");

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        final EditText enterCost = new EditText(this);
        final EditText enterDesc = new EditText(this);
        enterCost.setText(exToEdi.getCost().toString());
        enterDesc.setText(exToEdi.getDescription());
        enterCost.setInputType(InputType.TYPE_CLASS_NUMBER);
        enterCost.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        enterDesc.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        enterDesc.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        ll.addView(enterCost);
        ll.addView(enterDesc);
        builder.setView(ll);

        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();

        enterDesc.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
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
                String cost = enterCost.getText().toString().trim();
                String desc = enterDesc.getText().toString().trim();

                if (cost.equals("")) {
                    enterCost.setError("Please enter a dollar amount.");
                } else if (!Pattern.matches("^(\\d{1,10})?(\\.\\d{0,2})?$", cost)) {
                    enterCost.setError("Please enter a valid dollar amount.");
                } else {
                    categoryTotal = categoryTotal.subtract(exToEdi.getCost());
                    exToEdi.setCost(new BigDecimal(cost));
                    exToEdi.setDescription(desc);
                    new EditExpense().execute(exToEdi);
                    dia.dismiss();
                }
            }
        });
    }

    private void deleteExpense() {
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        ArrayAdapter<Expense> aa = (ArrayAdapter<Expense>) getListAdapter();
        int pos = lv.getCheckedItemPosition();
        Expense del = aa.getItem(pos);
        new DeleteExpense().execute(del);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        MainConfig settings = (MainConfig) getApplication();
        curUser = settings.getCurrentUser();
        date = settings.getCurrentDate();
        curCat = (Category) getIntent().getSerializableExtra(IntentTags.CURRENT_CATEGORY);

        TextView title = (TextView) findViewById(R.id.exCat);
        title.setText(curCat.getCategory());
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(ExpenseActivity.this, CategoriesActivity.class);
                startActivity(it);
            }
        });

        exSource = new ExpenseAccess(this);
        exSource.open();

        categoryTotal = exSource.getTotalCost(curUser, curCat, date.get(Calendar.MONTH), date.get(Calendar.YEAR));
        TextView total = (TextView) findViewById(R.id.exTotal);
        total.setText("Total: " + NumberFormat.getCurrencyInstance().format(categoryTotal));

        new GetExpenses().execute();
    }

    @Override
    protected void onResume() {
        exSource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        exSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_expenses, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            addExpense();
            return true;
        } else if (id == R.id.switch_user) {
            Intent intent = new Intent(this, UserActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
