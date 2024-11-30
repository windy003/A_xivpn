package cn.gov.xivpn2.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.Subscription;

public class SubscriptionsAdapter extends RecyclerView.Adapter<SubscriptionsAdapter.ViewHolder> {

    private final List<Subscription> subscriptions = new ArrayList<>();
    private Consumer<Subscription> onClickListener;

    public void setOnClickListener(Consumer<Subscription> onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void addSubscriptions(List<Subscription> s) {
        int size = subscriptions.size();
        subscriptions.addAll(s);
        notifyItemRangeInserted(size, subscriptions.size());
    }

    public void clear() {
        int size = subscriptions.size();
        subscriptions.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subscription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.label.setText(subscriptions.get(position).label);
        holder.cardView.setOnClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.accept(subscriptions.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return subscriptions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView label;
        public MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.label);
            cardView = itemView.findViewById(R.id.card);
        }
    }
}
