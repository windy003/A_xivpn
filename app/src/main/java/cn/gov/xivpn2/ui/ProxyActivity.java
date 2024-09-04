package cn.gov.xivpn2.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.ProxyDao;
import cn.gov.xivpn2.xrayconfig.HttpUpgradeSettings;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.QuicSettings;
import cn.gov.xivpn2.xrayconfig.ShadowsocksSettings;
import cn.gov.xivpn2.xrayconfig.SplitHttpSettings;
import cn.gov.xivpn2.xrayconfig.StreamSettings;
import cn.gov.xivpn2.xrayconfig.WsSettings;

public abstract class ProxyActivity<T> extends AppCompatActivity {

    private final static String TAG = "ProxyActivity";

    private RecyclerView recyclerView;
    private ProxyEditTextAdapter adapter;

    private String label;
    private String subscription;
    private String config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_proxy);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        label = getIntent().getStringExtra("LABEL");
        subscription = getIntent().getStringExtra("SUBSCRIPTION");
        config = getIntent().getStringExtra("CONFIG");

        getSupportActionBar().setTitle(label);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProxyEditTextAdapter();
        recyclerView.setAdapter(adapter);

        // initialize inputs
        initializeInputs(adapter);
        adapter.addInput("NETWORK", "Network", Arrays.asList("tcp", "ws", "quic", "httpupgrade", "splithttp"));

        adapter.setOnInputChangedListener((k, v) -> {
            onInputChanged(adapter, k, v);
        });

        // set existing values
        if (config != null) {
            Gson gson = new Gson();
            Outbound<T> outbound = gson.fromJson(config, getType());
            LinkedHashMap<String, String> initials = decodeOutboundConfig(outbound);
            initials.forEach((k, v) -> {
                adapter.setValue(k, v);
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.save) {

            // save

            // validation
            if (validate(adapter)) {
                Outbound<T> outbound = buildOutboundConfig();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(outbound);
                Log.d(TAG, json);

                ProxyDao proxyDao = AppDatabase.getInstance().proxyDao();
                if (proxyDao.exists(label, subscription) > 0) {
                    // update
                    proxyDao.updateConfig(label, subscription, json);
                } else {
                    // insert
                    Proxy proxy = new Proxy();
                    proxy.subscription = subscription;
                    proxy.label = label;
                    proxy.config = json;
                    proxy.protocol = getProtocolName();
                    proxyDao.add(proxy);
                }

                finish();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.proxy_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    abstract protected boolean validate(ProxyEditTextAdapter adapter);

    /**
     * @return type of T
     */
    abstract protected Type getType();

    /**
     * Build xray outbound object
     */
    private Outbound<T> buildOutboundConfig() {
        Outbound<T> outbound = new Outbound<>();
        outbound.protocol = getProtocolName();
        outbound.streamSettings = new StreamSettings();
        outbound.settings = buildProtocolSettings(adapter);

        String network = this.adapter.getValue("NETWORK");
        outbound.streamSettings.network = network;
        if (network.equals("ws")) {
            outbound.streamSettings.wsSettings = new WsSettings();
            outbound.streamSettings.wsSettings.path = adapter.getValue("NETWORK_WS_PATH");
            outbound.streamSettings.wsSettings.host = adapter.getValue("NETWORK_WS_HOST");
        } else if (network.equals("quic")) {
            outbound.streamSettings.quicSettings = new QuicSettings();
            outbound.streamSettings.quicSettings.header.type = adapter.getValue("NETWORK_QUIC_HEADER");
            outbound.streamSettings.quicSettings.security = adapter.getValue("NETWORK_QUIC_SECURITY");
            if (!outbound.streamSettings.quicSettings.security.equals("none")) {
                outbound.streamSettings.quicSettings.key = adapter.getValue("NETWORK_QUIC_KEY");
            }
        } else if (network.equals("httpupgrade")) {
            outbound.streamSettings.httpupgradeSettings = new HttpUpgradeSettings();
            outbound.streamSettings.httpupgradeSettings.path = adapter.getValue("NETWORK_HTTPUPGRADE_PATH");
            outbound.streamSettings.httpupgradeSettings.host = adapter.getValue("NETWORK_HTTPUPGRADE_HOST");
        } else if (network.equals("splithttp")) {
            outbound.streamSettings.splithttpSettings = new SplitHttpSettings();
            outbound.streamSettings.splithttpSettings.path = adapter.getValue("NETWORK_SPLITHTTP_PATH");
            outbound.streamSettings.splithttpSettings.host = adapter.getValue("NETWORK_SPLITHTTP_HOST");
            outbound.streamSettings.splithttpSettings.scMaxEachPostBytes = adapter.getValue("NETWORK_SPLITHTTP_MAX_POST");
            outbound.streamSettings.splithttpSettings.scMaxConcurrentPosts = adapter.getValue("NETWORK_SPLITHTTP_CONCURRENT_POSTS");
            outbound.streamSettings.splithttpSettings.scMinPostsIntervalMs = adapter.getValue("NETWORK_SPLITHTTP_MIN_POST_INTERVAL");
        }
        return outbound;
    }

    /**
     * Extract configuration values from outbound object
     */
    protected LinkedHashMap<String, String> decodeOutboundConfig(Outbound<T> outbound) {
        LinkedHashMap<String, String> initials = new LinkedHashMap<>();

        initials.put("NETWORK", outbound.streamSettings.network);
        if (outbound.streamSettings.network.equals("ws")) {
            initials.put("NETWORK_WS_PATH", outbound.streamSettings.wsSettings.path);
            initials.put("NETWORK_WS_HOST", outbound.streamSettings.wsSettings.host);
        } else if (outbound.streamSettings.network.equals("quic")) {
            initials.put("NETWORK_QUIC_HEADER", outbound.streamSettings.quicSettings.header.type);
            initials.put("NETWORK_QUIC_SECURITY", outbound.streamSettings.quicSettings.security);
            if (!outbound.streamSettings.quicSettings.security.equals("none")) {
                initials.put("NETWORK_QUIC_KEY", outbound.streamSettings.quicSettings.key);
            }
        } else if (outbound.streamSettings.network.equals("httpupgrade")) {
            initials.put("NETWORK_HTTPUPGRADE_PATH", outbound.streamSettings.httpupgradeSettings.path);
            initials.put("NETWORK_HTTPUPGRADE_HOST", outbound.streamSettings.httpupgradeSettings.host);
        } else if (outbound.streamSettings.network.equals("splithttp")) {
            initials.put("NETWORK_SPLITHTTP_PATH", outbound.streamSettings.splithttpSettings.path);
            initials.put("NETWORK_SPLITHTTP_HOST", outbound.streamSettings.splithttpSettings.host);
            initials.put("NETWORK_SPLITHTTP_MAX_POST", outbound.streamSettings.splithttpSettings.scMaxEachPostBytes);
            initials.put("NETWORK_SPLITHTTP_CONCURRENT_POSTS", outbound.streamSettings.splithttpSettings.scMaxConcurrentPosts);
            initials.put("NETWORK_SPLITHTTP_MIN_POST_INTERVAL", outbound.streamSettings.splithttpSettings.scMinPostsIntervalMs);
        }

        return initials;
    }

    /**
     * Build setting object for the proxy protocol
     */
    abstract protected T buildProtocolSettings(ProxyEditTextAdapter adapter);

    abstract protected String getProtocolName();

    abstract protected void initializeInputs(ProxyEditTextAdapter adapter);

    /**
     * Called when user changes a configuration field
     */
    protected void onInputChanged(ProxyEditTextAdapter adapter, String key, String value) {
        if (key.equals("NETWORK")) {
            adapter.removeInputByPrefix("NETWORK_");
            if (value.equals("ws")) {
                adapter.addInput("NETWORK_WS_PATH", "Websocket Path", "/");
                adapter.addInput("NETWORK_WS_HOST", "Websocket Host");
            } else if (value.equals("quic")) {
                adapter.addInput("NETWORK_QUIC_HEADER", "QUIC Header", Arrays.asList("none", "srtp", "utp", "wechat-video", "dtls", "wireguard"));
                adapter.addInput("NETWORK_QUIC_SECURITY", "QUIC Security", Arrays.asList("none", "aes-128-gcm", "chacha20-poly1305"));
            } else if (value.equals("httpupgrade")) {
                adapter.addInput("NETWORK_HTTPUPGRADE_PATH", "HttpUpgrade Path", "/");
                adapter.addInput("NETWORK_HTTPUPGRADE_HOST", "HttpUpgrade Host");
            } else if (value.equals("splithttp")) {
                adapter.addInput("NETWORK_SPLITHTTP_PATH", "SplitHttp Path", "/");
                adapter.addInput("NETWORK_SPLITHTTP_HOST", "SplitHttp Host");
                adapter.addInput("NETWORK_SPLITHTTP_MAX_POST", "SplitHttp Max Post (bytes)", "1000000", "The maximum size of upload chunks, in bytes.");
                adapter.addInput("NETWORK_SPLITHTTP_CONCURRENT_POSTS", "SplitHttp Concurrent Posts", "100", "The number of concurrent uploads");
                adapter.addInput("NETWORK_SPLITHTTP_MIN_POST_INTERVAL", "SplitHttp Post Interval (milliseconds)", "30");
            }
        } else if (key.equals("NETWORK_QUIC_SECURITY")) {
            if (value.equals("none")) {
                adapter.removeInput("NETWORK_QUIC_KEY");
            } else {
                adapter.addInput("NETWORK_QUIC_KEY", "QUIC Encryption Key");
            }
        }
    }
}