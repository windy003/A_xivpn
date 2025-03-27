package cn.gov.xivpn2.ui;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.gov.xivpn2.R;

public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

    public final List<App> apps = new ArrayList<>();

    /**
     * onCheckListener(idx, selected);
     */
    public Consumer<Integer> onCheckListener = null;
    private final boolean checked;

    public InstalledAppsAdapter(boolean checked) {
        this.checked = checked;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new InstalledAppsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.checkBox.setOnCheckedChangeListener(null);

        holder.icon.setImageDrawable(apps.get(position).icon);
        holder.appName.setText(apps.get(position).appName);
        holder.packageName.setText(apps.get(position).packageName);
        holder.checkBox.setChecked(checked);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (onCheckListener != null) {
                onCheckListener.accept(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView appName;
        private final TextView packageName;
        private final CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            appName = itemView.findViewById(R.id.app_name);
            packageName = itemView.findViewById(R.id.package_name);
            checkBox = itemView.findViewById(R.id.checkbox);
        }

    }

    public static class App {
        public final String appName;
        public final Drawable icon;
        public final String packageName;

        public App(String appName, Drawable icon, String packageName) {
            this.appName = appName;
            this.icon = icon;
            this.packageName = packageName;
        }
    }
}
