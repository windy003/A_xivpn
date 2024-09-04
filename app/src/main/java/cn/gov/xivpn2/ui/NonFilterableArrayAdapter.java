package cn.gov.xivpn2.ui;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.List;

public class NonFilterableArrayAdapter extends ArrayAdapter<String> {

    private final List<String> objects;

    public NonFilterableArrayAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.objects = objects;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence prefix) {
                // ignore prefix and display all options on the AutoCompleteTextView

                FilterResults filterResults = new FilterResults();
                filterResults.values = objects;
                filterResults.count = objects.size();
                return null;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

            }
        };
    }
}
