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

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.ProxyDao;
import cn.gov.xivpn2.xrayconfig.HttpUpgradeSettings;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.QuicSettings;
import cn.gov.xivpn2.xrayconfig.RealitySettings;
import cn.gov.xivpn2.xrayconfig.ShadowsocksSettings;
import cn.gov.xivpn2.xrayconfig.SplitHttpSettings;
import cn.gov.xivpn2.xrayconfig.StreamSettings;
import cn.gov.xivpn2.xrayconfig.TLSSettings;
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
        if (hasStreamSettings()) {
            adapter.addInput("NETWORK", "Network", Arrays.asList("tcp", "ws", "quic", "httpupgrade", "splithttp"));
            adapter.addInput("SECURITY", "Security", Arrays.asList("none", "tls", "reality"));
        }

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

    /**
     * This method may be overridden to disable stream settings (network and security).
     */
    protected boolean hasStreamSettings() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.save) {

            // save

            // validation
            if (validate(adapter)) {
                Outbound<T> outbound = buildOutboundConfig(this.adapter);
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
    protected Outbound<T> buildOutboundConfig(ProxyEditTextAdapter adapter) {
        Outbound<T> outbound = new Outbound<>();
        outbound.protocol = getProtocolName();
        outbound.settings = buildProtocolSettings(adapter);

        if (!hasStreamSettings()) return outbound;

        outbound.streamSettings = new StreamSettings();
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

        String security = this.adapter.getValue("SECURITY");
        if (security.equals("tls")) {
            outbound.streamSettings.security = "tls";
            outbound.streamSettings.tlsSettings = new TLSSettings();
            outbound.streamSettings.tlsSettings.allowInsecure = adapter.getValue("SECURITY_TLS_INSECURE").equals("True");
            outbound.streamSettings.tlsSettings.fingerprint = adapter.getValue("SECURITY_TLS_FINGERPRINT");
            outbound.streamSettings.tlsSettings.serverName = adapter.getValue("SECURITY_TLS_SNI");
            outbound.streamSettings.tlsSettings.alpn = adapter.getValue("SECURITY_TLS_ALPN").split(",");
        } else if (security.equals("reality")) {
            outbound.streamSettings.security = "reality";
            outbound.streamSettings.realitySettings = new RealitySettings();
            outbound.streamSettings.realitySettings.shortId = adapter.getValue("SECURITY_REALITY_SHORTID");
            outbound.streamSettings.realitySettings.fingerprint = adapter.getValue("SECURITY_TLS_FINGERPRINT");
            outbound.streamSettings.realitySettings.serverName = adapter.getValue("SECURITY_TLS_SNI");
            outbound.streamSettings.realitySettings.publicKey = adapter.getValue("SECURITY_REALITY_PUBLIC_KEY");
        }

        return outbound;
    }

    /**
     * Extract configuration values from outbound object
     */
    protected LinkedHashMap<String, String> decodeOutboundConfig(Outbound<T> outbound) {
        LinkedHashMap<String, String> initials = new LinkedHashMap<>();

        if (!hasStreamSettings()) return initials;

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

        if (outbound.streamSettings.security == null || outbound.streamSettings.security.isEmpty()) {

        } else if (outbound.streamSettings.security.equals("tls")) {
            initials.put("SECURITY", "tls");
            initials.put("SECURITY_TLS_SNI", outbound.streamSettings.tlsSettings.serverName);
            initials.put("SECURITY_TLS_ALPN", String.join(",", outbound.streamSettings.tlsSettings.alpn));
            initials.put("SECURITY_TLS_INSECURE", outbound.streamSettings.tlsSettings.allowInsecure ? "True" : "False");
            initials.put("SECURITY_TLS_FINGERPRINT", outbound.streamSettings.tlsSettings.fingerprint);
        } else if (outbound.streamSettings.security.equals("reality")) {
            initials.put("SECURITY", "reality");
            initials.put("SECURITY_REALITY_SNI", outbound.streamSettings.realitySettings.serverName);
            initials.put("SECURITY_REALITY_FINGERPRINT", outbound.streamSettings.realitySettings.fingerprint);
            initials.put("SECURITY_REALITY_SHORTID", outbound.streamSettings.realitySettings.shortId);
            initials.put("SECURITY_REALITY_PUBLIC_KEY", outbound.streamSettings.realitySettings.publicKey);
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
        if (!hasStreamSettings()) return;

        if (key.equals("NETWORK")) {
            adapter.removeInputByPrefix("NETWORK_");
            if (value.equals("ws")) {
                adapter.addInputAfter("NETWORK", "NETWORK_WS_PATH", "Websocket Path", "/");
                adapter.addInputAfter("NETWORK", "NETWORK_WS_HOST", "Websocket Host");
            } else if (value.equals("quic")) {
                adapter.addInputAfter("NETWORK", "NETWORK_QUIC_HEADER", "QUIC Header", Arrays.asList("none", "srtp", "utp", "wechat-video", "dtls", "wireguard"));
                adapter.addInputAfter("NETWORK", "NETWORK_QUIC_SECURITY", "QUIC Security", Arrays.asList("none", "aes-128-gcm", "chacha20-poly1305"));
            } else if (value.equals("httpupgrade")) {
                adapter.addInputAfter("NETWORK", "NETWORK_HTTPUPGRADE_PATH", "HttpUpgrade Path", "/");
                adapter.addInputAfter("NETWORK", "NETWORK_HTTPUPGRADE_HOST", "HttpUpgrade Host");
            } else if (value.equals("splithttp")) {
                adapter.addInputAfter("NETWORK", "NETWORK_SPLITHTTP_PATH", "SplitHttp Path", "/");
                adapter.addInputAfter("NETWORK", "NETWORK_SPLITHTTP_HOST", "SplitHttp Host");
                adapter.addInputAfter("NETWORK", "NETWORK_SPLITHTTP_MAX_POST", "SplitHttp Max Post (bytes)", "1000000", "The maximum size of upload chunks, in bytes.");
                adapter.addInputAfter("NETWORK", "NETWORK_SPLITHTTP_CONCURRENT_POSTS", "SplitHttp Concurrent Posts", "100", "The number of concurrent uploads");
                adapter.addInputAfter("NETWORK", "NETWORK_SPLITHTTP_MIN_POST_INTERVAL", "SplitHttp Post Interval (milliseconds)", "30");
            }
        } else if (key.equals("NETWORK_QUIC_SECURITY")) {
            if (value.equals("none")) {
                adapter.removeInput("NETWORK_QUIC_KEY");
            } else {
                adapter.addInputAfter("NETWORK", "NETWORK_QUIC_KEY", "QUIC Encryption Key");
            }
        } else if (key.equals("SECURITY")) {
            adapter.removeInputByPrefix("SECURITY_");
            if (value.equals("tls")) {
                adapter.addInputAfter("SECURITY", "SECURITY_TLS_SNI", "TLS Server Name");
                adapter.addInputAfter("SECURITY", "SECURITY_TLS_ALPN", "TLS ALPN", "h2,http/1.1");
                adapter.addInputAfter("SECURITY", "SECURITY_TLS_INSECURE", "TLS Allow Insecure", List.of("False", "True"));
                adapter.addInputAfter("SECURITY", "SECURITY_TLS_FINGERPRINT", "TLS Fingerprint", List.of("None", "chrome", "firefox", "random", "randomized"));
            } else if (value.equals("reality")) {
                adapter.addInputAfter("SECURITY", "SECURITY_REALITY_SNI", "REALITY Server Name");
                adapter.addInputAfter("SECURITY", "SECURITY_REALITY_FINGERPRINT", "REALITY Fingerprint", List.of("None", "chrome", "firefox", "random", "randomized"));
                adapter.addInputAfter("SECURITY", "SECURITY_REALITY_SHORTID", "REALITY Short ID");
                adapter.addInputAfter("SECURITY", "SECURITY_REALITY_PUBLIC_KEY", "REALITY Public Key");
            }
        }
    }
}