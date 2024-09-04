package cn.gov.xivpn2.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;

public class ProxiesActivity extends AppCompatActivity {


    private RecyclerView recyclerView;
    private ProxiesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_proxies);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.proxies);

        recyclerView = findViewById(R.id.recycler_view);

        this.adapter = new ProxiesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // on list item clicked
        adapter.setOnClickListener((view, proxy, i) -> {

            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.inflate(R.menu.proxies_popup);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.edit) {

                    // edit
                    if (proxy.protocol.equals("shadowsocks")) {
                        Intent intent = new Intent(this, ShadowsocksActivity.class);
                        intent.putExtra("LABEL", proxy.label);
                        intent.putExtra("SUBSCRIPTION", proxy.subscription);
                        intent.putExtra("CONFIG", proxy.config);
                        startActivity(intent);
                    } else if (proxy.protocol.equals("vmess")) {
                        Intent intent = new Intent(this, VmessActivity.class);
                        intent.putExtra("LABEL", proxy.label);
                        intent.putExtra("SUBSCRIPTION", proxy.subscription);
                        intent.putExtra("CONFIG", proxy.config);
                        startActivity(intent);
                    }

                } else if (item.getItemId() == R.id.delete) {

                    // delete
                    AppDatabase.getInstance().proxyDao().delete(proxy.label, proxy.subscription);
                    refresh();

                } else if (item.getItemId() == R.id.select) {

                    // select
                    SharedPreferences sp = getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putString("SELECTED_LABEL", proxy.label);
                    edit.putString("SELECTED_SUBSCRIPTION", proxy.subscription);
                    edit.apply();

                    adapter.setSelectedProxy(proxy.label, proxy.subscription);

                }
                return true;
            });
            popupMenu.show();


        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        adapter.clear();
        adapter.addProxies(AppDatabase.getInstance().proxyDao().findAll());

        SharedPreferences sp = getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
        String label = sp.getString("SELECTED_LABEL", "Freedom");
        String subscription = sp.getString("SELECTED_SUBSCRIPTION", "none");
        adapter.setSelectedProxy(label, subscription);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        } else if (item.getItemId() == R.id.shadowsocks || item.getItemId() == R.id.vmess) {
            EditText editText = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.label)
                    .setView(editText)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        if (item.getItemId() == R.id.shadowsocks) {
                            Intent intent = new Intent(this, ShadowsocksActivity.class);
                            intent.putExtra("LABEL", editText.getText().toString());
                            intent.putExtra("SUBSCRIPTION", "none");
                            startActivity(intent);
                        } else if (item.getItemId() == R.id.vmess) {
                            Intent intent = new Intent(this, VmessActivity.class);
                            intent.putExtra("LABEL", editText.getText().toString());
                            intent.putExtra("SUBSCRIPTION", "none");
                            startActivity(intent);
                        }
                    }).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.proxies_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }


}