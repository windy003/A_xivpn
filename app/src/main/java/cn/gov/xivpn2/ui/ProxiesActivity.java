package cn.gov.xivpn2.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
        adapter.setOnLongClickListener((view, proxy, i) -> {
            if (proxy.protocol.equals("freedom")) {
                // freedom is not removable and editable
                return;
            }

            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.inflate(R.menu.proxies_popup);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.edit) {

                    // edit
                    Class<? extends AppCompatActivity> cls = null;
                    if (proxy.protocol.equals("shadowsocks")) {
                        cls = ShadowsocksActivity.class;
                    } else if (proxy.protocol.equals("vmess")) {
                        cls = VmessActivity.class;
                    } else if (proxy.protocol.equals("vless")) {
                        cls = VlessActivity.class;
                    } else if (proxy.protocol.equals("trojan")) {
                        cls = TrojanActivity.class;
                    } else if (proxy.protocol.equals("wireguard")) {
                        cls = WireguardActivity.class;
                    }

                    Intent intent = new Intent(this, cls);
                    intent.putExtra("LABEL", proxy.label);
                    intent.putExtra("SUBSCRIPTION", proxy.subscription);
                    intent.putExtra("CONFIG", proxy.config);
                    startActivity(intent);

                } else if (item.getItemId() == R.id.delete) {

                    // delete
                    AppDatabase.getInstance().proxyDao().delete(proxy.label, proxy.subscription);

                    // check if selected subscription is deleted
                    // if so, set to default
                    SharedPreferences sp = getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
                    String selectedLabel = sp.getString("SELECTED_LABEL", "No Proxy (Bypass Mode)");
                    String selectedSubscription = sp.getString("SELECTED_SUBSCRIPTION", "none");
                    if (AppDatabase.getInstance().proxyDao().exists(selectedLabel, selectedSubscription) == 0) {
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putString("SELECTED_LABEL", "No Proxy (Bypass Mode)");
                        edit.putString("SELECTED_SUBSCRIPTION", "none");
                        edit.commit();
                    }

                    refresh();

                }

                return true;
            });
            popupMenu.show();
        });

        adapter.setOnClickListener((view, proxy, i) -> {

            // select
            SharedPreferences sp = getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = sp.edit();
            edit.putString("SELECTED_LABEL", proxy.label);
            edit.putString("SELECTED_SUBSCRIPTION", proxy.subscription);
            edit.apply();

            adapter.setSelectedProxy(proxy.label, proxy.subscription);
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
        String label = sp.getString("SELECTED_LABEL", "No Proxy (Bypass Mode)");
        String subscription = sp.getString("SELECTED_SUBSCRIPTION", "none");
        adapter.setSelectedProxy(label, subscription);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        } else if (item.getItemId() == R.id.shadowsocks || item.getItemId() == R.id.vmess || item.getItemId() == R.id.vless || item.getItemId() == R.id.trojan || item.getItemId() == R.id.wireguard) {

            // add

            View view = LayoutInflater.from(this).inflate(R.layout.label_edit_text, null);
            TextInputEditText editText = ((TextInputEditText) view.findViewById(R.id.edit_text));

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.label)
                    .setView(view)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {

                        String label = String.valueOf(editText.getText());
                        if (label.isEmpty() || AppDatabase.getInstance().proxyDao().exists(label, "none") > 0) {
                            Toast.makeText(this, getResources().getText(R.string.conflict_label), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Class<? extends AppCompatActivity> cls = null;
                        if (item.getItemId() == R.id.shadowsocks) {
                            cls = ShadowsocksActivity.class;
                        } else if (item.getItemId() == R.id.vmess) {
                            cls = VmessActivity.class;
                        } else if (item.getItemId() == R.id.vless) {
                            cls = VlessActivity.class;
                        } else if (item.getItemId() == R.id.trojan) {
                            cls = TrojanActivity.class;
                        } else if (item.getItemId() == R.id.wireguard) {
                            cls = WireguardActivity.class;
                        }

                        Intent intent = new Intent(this, cls);
                        intent.putExtra("LABEL", label);
                        intent.putExtra("SUBSCRIPTION", "none");
                        startActivity(intent);

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