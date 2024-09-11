package cn.gov.xivpn2.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import cn.gov.xivpn2.R;

public class ProxyEditTextAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final static String TAG = "ProxyEditTextAdapter";

    private final ArrayList<Input> inputs;
    private BiConsumer<String, String> onInputChanged;

    public ProxyEditTextAdapter() {
        inputs = new ArrayList<>();
    }

    public void setOnInputChangedListener(BiConsumer<String, String> onInputChanged) {
        this.onInputChanged = onInputChanged;
    }

    @Override
    public int getItemViewType(int position) {
        return inputs.get(position).isSelect() ? 0 : 1;
    }

    /**
     * Add the input if no input with the same key exists.
     */
    public void addInput(Input input) {
        boolean found = false;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).key.equals(input.key)) {
                found = true;
                break;
            }
        }
        if (!found) {
            int size = this.inputs.size();
            inputs.add(input);
            this.notifyItemInserted(size);
        }
    }

    public void addInputAfter(String key, Input input) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).key.equals(key)) {
                inputs.add(i + 1, input);
                this.notifyItemInserted(i + 1);
                break;
            }
            if (inputs.get(i).key.equals(input.key)) {
                // skip if input with same key exists
                break;
            }
        }
    }

    public void addInput(String key, String label) {
        this.addInput(new Input(key, label, "", Collections.emptyList()));
    }

    public void addInput(String key, String label, String defaultValue) {
        this.addInput(new Input(key, label, "", Collections.emptyList(), defaultValue));
    }

    public void addInput(String key, String label, String defaultValue, String helperText) {
        this.addInput(new Input(key, label, helperText, Collections.emptyList(), defaultValue));
    }

    public void addInput(String key, String label, List<String> selections) {
        this.addInput(new Input(key, label, "", selections));
    }

    public void addInputAfter(String after, String key, String label) {
        this.addInputAfter(after, new Input(key, label, "", Collections.emptyList()));
    }

    public void addInputAfter(String after, String key, String label, String defaultValue) {
        this.addInputAfter(after, new Input(key, label, "", Collections.emptyList(), defaultValue));
    }

    public void addInputAfter(String after, String key, String label, String defaultValue, String helperText) {
        this.addInputAfter(after, new Input(key, label, helperText, Collections.emptyList(), defaultValue));
    }

    public void addInputAfter(String after, String key, String label, List<String> selections) {
        this.addInputAfter(after, new Input(key, label, "", selections));
    }

    /**
     * Remove input with the key. Does nothing if the key does not exist.
     */
    public void removeInput(String key) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).key.equals(key)) {
                inputs.remove(i);
                this.notifyItemRemoved(i);
                break;
            }
        }
    }

    public void removeInputByPrefix(String prefix) {
        Iterator<Input> iterator = inputs.iterator();
        int i = 0;
        int removed = 0;
        while (iterator.hasNext()) {
            Input next = iterator.next();
            if (next.key.startsWith(prefix)) {
                iterator.remove();
                Log.d(TAG, "notifyItemRemoved " + (i - removed));
                this.notifyItemRemoved(i - removed);
                removed++;
            }
            i++;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dropdown, parent, false);
            return new DropdownViewHolder(view);
        } else if (viewType == 1) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edittext, parent, false);
            return new EditTextViewHolder(view);
        } else {
            throw new IllegalArgumentException("view type " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        Input input = inputs.get(position);

        if (h instanceof EditTextViewHolder) {
            EditTextViewHolder holder = (EditTextViewHolder) h;

            holder.setOnTextChangedListener(null);

            holder.setText(input.getText());
            holder.setHint(input.getHint());
            holder.setHelperText(input.getHelperText());
            holder.setError(input.isValidated());

            holder.setOnTextChangedListener(() -> {
                input.setText(holder.getText());
                if (onInputChanged != null) {
                    onInputChanged.accept(input.key, input.text);
                }
            });
        }

        if (h instanceof DropdownViewHolder) {
            DropdownViewHolder holder = (DropdownViewHolder) h;
            holder.setOnTextChangedListener(null);

            holder.setDropdown(input.getSelect());
            holder.setText(input.getText());
            holder.setHint(input.getHint());
            holder.setHelperText(input.getHelperText());
            holder.setContentDescription(input.getHint());

            holder.setOnTextChangedListener(() -> {
                input.setText(holder.getText());
                if (onInputChanged != null) {
                    onInputChanged.accept(input.key, input.text);
                }
            });
        }


    }

    public String getValue(String key) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).key.equals(key)) {
                return inputs.get(i).text;
            }
        }
        return "";
    }

    public void setValue(String key, String value) {
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).key.equals(key)) {
                inputs.get(i).text = value;
                if (onInputChanged != null) onInputChanged.accept(key, value);
            }
        }
    }

    @Override
    public int getItemCount() {
        return inputs.size();
    }

    /**
     * validate user input values
     * @param consumer a function that takes a key-value pair and return true if
     *                 the user-provided value if valid
     * @return true if all values are valid
     */
    public boolean validate(BiFunction<String, String, Boolean> consumer) {
        boolean valid = true;
        for (int i = 0; i < inputs.size(); i++) {
            Boolean v = consumer.apply(inputs.get(i).key, inputs.get(i).text);
            if (v != inputs.get(i).validated) {
                this.notifyItemChanged(i);
            }
            inputs.get(i).setValidated(v);
            if (!v) valid = false;
        }
        return valid;
    }

    public static class DropdownViewHolder extends RecyclerView.ViewHolder implements TextWatcher {
        private final TextInputLayout layout;
        private final AutoCompleteTextView editText;
        private Runnable onTextChanged;

        public DropdownViewHolder(@NonNull View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.layout);
            editText = itemView.findViewById(R.id.edittext);
            editText.addTextChangedListener(this);
        }

        public void setOnTextChangedListener(Runnable onTextChanged) {
            this.onTextChanged = onTextChanged;
        }

        public void setHint(String s) {
            layout.setHint(s);
        }

        public void setDropdown(List<String> strings) {
            editText.setAdapter(new NonFilterableArrayAdapter(editText.getContext(), R.layout.list_item, strings));
        }

        public void setText(String s) {
            editText.setText(s);
        }

        public void setHelperText(String s) {
            layout.setHelperText(s);
        }

        public String getText() {
            return Objects.requireNonNull(editText.getText()).toString();
        }

        public void setContentDescription(String s) {
            layout.setEndIconContentDescription(s);
        }


        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (onTextChanged != null) {
                onTextChanged.run();
            }
        }
    }

    public static class EditTextViewHolder extends RecyclerView.ViewHolder implements TextWatcher {

        private final TextInputLayout layout;
        private final TextInputEditText editText;
        private Runnable onTextChanged;

        public EditTextViewHolder(@NonNull View itemView) {
            super(itemView);

            layout = itemView.findViewById(R.id.layout);
            editText = itemView.findViewById(R.id.edittext);

            editText.addTextChangedListener(this);
        }

        public void setHint(String s) {
            layout.setHint(s);
        }

        public void setText(String s) {
            editText.setText(s);
        }

        public void setHelperText(String s) {
            layout.setHelperText(s);
        }

        public void setError(boolean v) {
            if (v) {
                editText.setError(null);
            } else {
                editText.setError("Invalid value");
            }
        }

        public String getText() {
            return Objects.requireNonNull(editText.getText()).toString();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (onTextChanged != null) {
                onTextChanged.run();
            }
        }

        public void setOnTextChangedListener(Runnable onTextChanged) {
            this.onTextChanged = onTextChanged;
        }
    }

    public static class Input {
        private final String hint;
        private final String key;
        private final String helperText;
        private final List<String> select;
        private String text = "";
        private boolean validated = true;
        private String error;

        public Input(String key, String hint, String helperText, List<String> select) {
            this.hint = hint;
            this.helperText = helperText;
            this.key = key;
            this.select = select;
            if (!select.isEmpty()) {
                text = select.get(0);
            }
        }

        public Input(String key, String hint, String helperText, List<String> select, String defaultValue) {
            this(key, hint, helperText, select);
            this.text = defaultValue;
        }

        public void setValidated(boolean validated) {
            this.validated = validated;
        }

        public boolean isValidated() {
            return validated;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public boolean isSelect() {
            return !select.isEmpty();
        }

        public List<String> getSelect() {
            return select;
        }

        public String getHelperText() {
            return helperText;
        }

        public String getHint() {
            return hint;
        }

        public String getText() {
            return text;
        }

        public String getKey() {
            return key;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

}
