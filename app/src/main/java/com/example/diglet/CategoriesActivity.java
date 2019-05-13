package com.example.diglet;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.diglet.Configuration.Category;
import com.example.diglet.Configuration.CategoryAccess;
import com.example.diglet.Configuration.ExpenseAccess;
import com.example.diglet.Configuration.IntentTags;
import com.example.diglet.Configuration.User;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

public class CategoriesActivity extends ListActivity {
    private CategoryAccess catSource;

    private ExpenseAccess exSource;

    private User curUser;

    private static Calendar date;

    private TextView acTitle;

    private ArrayAdapter<Category> adapter;

    public static final String[] MONTHS = { "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private ActionMode aMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        private AdapterView.OnItemClickListener iLstn;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_category, menu);

            ListView lv = getListView();
            iLstn = lv.getOnItemClickListener();
            lv.setOnItemClickListener(null);
            acTitle.setClickable(false);
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
                    editCategory();
                    mode.finish();
                    return true;
                case R.id.action_delete:
                    deleteCategory();
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

            getListView().setOnItemClickListener(iLstn);
            acTitle.setClickable(true);
        }
    };

    public static class DateSelector extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH);
            int day = date.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day);
            MainConfig gc = (MainConfig) getActivity().getApplication();
            gc.setCurrentDate(c);
            date = c;

            TextView title = (TextView) getActivity().findViewById(R.id.catMon);
            title.setText(MONTHS[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR));

            TextView total = (TextView) getActivity().findViewById(R.id.monYTot);
            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
                    ((CategoriesActivity) getActivity()).exSource.getTotalCost(gc.getCurrentUser(), month, year)));
            ((ArrayAdapter<Category>) ((CategoriesActivity) getActivity()).getListAdapter()).notifyDataSetChanged();
        }
    }

    private class GetCategories extends AsyncTask<Void, Void, List<Category>> {
        @Override
        protected List<Category> doInBackground(Void... params) {
            return catSource.getCategories(curUser);
        }

        @Override
        protected void onPostExecute(final List<Category> result) {
            adapter = new ArrayAdapter<Category>(CategoriesActivity.this,
                    R.layout.row_layout_category, R.id.catLabel, result) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = (TextView) view.findViewById(R.id.catLabel);
                    TextView text2 = (TextView) view.findViewById(R.id.catCost);
                    text1.setText(result.get(position).toString());
                    text2.setText(NumberFormat.getCurrencyInstance().format(
                            exSource.getTotalCost(curUser, result.get(position),
                                    date.get(Calendar.MONTH), date.get(Calendar.YEAR))));
                    return view;
                }
            };
            setListAdapter(adapter);

            final ListView lv = getListView();
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Category cat = adapter.getItem(i);

                    Intent intent = new Intent(CategoriesActivity.this, ExpenseActivity.class);
                    intent.putExtra(IntentTags.CURRENT_CATEGORY, cat);
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
                    aMode = CategoriesActivity.this.startActionMode(mActionModeCallback);
                    return true;
                }
            });
        }
    }

    /**
     * Class to asynchronously add new category to database.
     */
    private class AddCategory extends AsyncTask<String, Void, Category> {
        @Override
        protected Category doInBackground(String... params) {
            return catSource.newCategory(params[0], curUser);
        }

        @Override
        protected void onPostExecute(Category result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Category> adapter = (ArrayAdapter<Category>) getListAdapter();
            adapter.add(result);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Class to asynchronously edit a category name in database.
     */
    private class EditCategory extends AsyncTask<Category, Void, Category> {
        @Override
        protected Category doInBackground(Category... params) {
            return catSource.editCategory(params[0]);
        }

        @Override
        protected void onPostExecute(Category result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
            aa.notifyDataSetChanged();
        }
    }

    /**
     * Class to asynchronously delete a category from database.
     */
    private class DeleteCategory extends AsyncTask<Category, Void, Category> {
        @Override
        protected Category doInBackground(Category... params) {
            return catSource.deleteCategory(params[0]);
        }

        @Override
        protected void onPostExecute(Category result) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
            aa.remove(result);
            aa.notifyDataSetChanged();
            TextView total = (TextView) findViewById(R.id.monYTot);
            total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
                    exSource.getTotalCost(curUser, date.get(Calendar.MONTH), date.get(Calendar.YEAR))));
        }
    }

    private void populateCategories() {
        final List<Category> result = catSource.getCategories(curUser);

        adapter = new ArrayAdapter<Category>(CategoriesActivity.this,
                R.layout.row_layout_category, R.id.catLabel, result) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(R.id.catLabel);
                TextView text2 = (TextView) view.findViewById(R.id.catCost);
                text1.setText(result.get(position).toString());
                text2.setText(NumberFormat.getCurrencyInstance().format(
                        exSource.getTotalCost(curUser, result.get(position),
                                date.get(Calendar.MONTH), date.get(Calendar.YEAR))));
                return view;
            }
        };
        setListAdapter(adapter);

        final ListView lv = getListView();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Category cat = adapter.getItem(i);

                Intent intent = new Intent(CategoriesActivity.this, ExpenseActivity.class);
                intent.putExtra(IntentTags.CURRENT_CATEGORY, cat);
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
                aMode = CategoriesActivity.this.startActionMode(mActionModeCallback);
                return true;
            }
        });
    }

    /**
     * Method to add a new category. Called when Add button in action bar is clicked.
     */
    private void addCategory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create category");
        builder.setMessage("Please enter a category name.");

        final EditText enterCat = new EditText(this);
        enterCat.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        enterCat.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        builder.setView(enterCat);

        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();

        enterCat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
                String catName = enterCat.getText().toString().trim();

                if (catName.equals("")) {
                    enterCat.setError("Please enter a name.");
                } else if (catSource.exists(catName, curUser)) {
                    enterCat.setError("This category already exists.");
                } else {
                    new AddCategory().execute(catName);
                    dia.dismiss();
                }
            }
        });
    }

    /**
     * Method to edit a category title, called when the Edit button in the context menu is clicked.
     */
    private void editCategory() {
        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
        final Category catToEdi = aa.getItem(lv.getCheckedItemPosition());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit category");
        builder.setMessage("Please enter a new category name.");

        final EditText enterName = new EditText(this);
        enterName.setText(catToEdi.getCategory());
        enterName.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        enterName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
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
                String catName = enterName.getText().toString().trim();

                if (catName.equals("")) {
                    enterName.setError("Please enter a name.");
                } else if (catSource.exists(catName, curUser)) {
                    enterName.setError("This category already exists.");
                } else {
                    catToEdi.setCategory(catName);
                    new EditCategory().execute(catToEdi);
                    dia.dismiss();
                }
            }
        });
    }

    /**
     * Method to delete a category, called when the Delete button in the context menu is clicked.
     */
    private void deleteCategory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete category");
        builder.setMessage("Are you sure? All expenses from this category will be deleted.");

        builder.setPositiveButton(R.string.conf, null);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dia = builder.create();
        dia.show();

        ListView lv = getListView();
        @SuppressWarnings("unchecked")
        final ArrayAdapter<Category> aa = (ArrayAdapter<Category>) getListAdapter();
        final Category catToDel = aa.getItem(lv.getCheckedItemPosition());

        dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DeleteCategory().execute(catToDel);
                dia.dismiss();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        MainConfig settings = (MainConfig) getApplication();
        curUser = settings.getCurrentUser();
        date = settings.getCurrentDate();

        acTitle = (TextView) findViewById(R.id.catMon);
        acTitle.setText(MONTHS[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR));
        acTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DateSelector().show(getFragmentManager(), "configDatePicker");
            }
        });

        exSource = new ExpenseAccess(this);
        exSource.open();
        catSource = new CategoryAccess(this);
        catSource.open();

        TextView total = (TextView) findViewById(R.id.monYTot);
        total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
                exSource.getTotalCost(curUser, date.get(Calendar.MONTH), date.get(Calendar.YEAR))));


        populateCategories();
    }

    @Override
    protected void onResume() {
        catSource.open();
        exSource.open();

        @SuppressWarnings("unchecked")
        ArrayAdapter<Category> aa = ((ArrayAdapter<Category>) getListAdapter());
        aa.notifyDataSetChanged();

        TextView total = (TextView) findViewById(R.id.monYTot);
        total.setText("Total: " + NumberFormat.getCurrencyInstance().format(
                exSource.getTotalCost(curUser, date.get(Calendar.MONTH), date.get(Calendar.YEAR))));
        super.onResume();
    }

    @Override
    protected void onPause() {
        catSource.close();
        exSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_categories, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            addCategory();
            return true;
        } else if (id == R.id.switch_user) {
            Intent intent = new Intent(this, UserActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
