package cn.gov.xivpn2;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fab;
    private MaterialSwitch aSwitch;
    private TextView textView;
    private TextView textViewVersion;
    private XiVPNService.XiVPNBinder binder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (XiVPNService.XiVPNBinder) service;

            updateSwitch(binder.getService().getStatus());

            binder.setListener(new XiVPNService.VPNStatusListener() {
                @Override
                public void onStatusChanged(XiVPNService.Status status) {
                    updateSwitch(status);
                }

                @Override
                public void onMessage(String msg) {
                    textView.setText(msg);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    };
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, XiVPNService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        textView.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fab = findViewById(R.id.fab);
        textView = findViewById(R.id.textview);
        aSwitch = findViewById(R.id.vpn_switch);
        textViewVersion = findViewById(R.id.version);

        textViewVersion.setText(LibXivpn.xivpn_version());
        fab.hide();

        onCheckedChangeListener = (compoundButton, b) -> {
            if (b) {
                Intent intent = XiVPNService.prepare(this);

                if (intent != null) {
                    // request vpn permission
                    aSwitch.setChecked(false);
                    startActivityForResult(intent, 1);
                }

                startForegroundService(new Intent(this, XiVPNService.class));
                binder.getService().startVPN();
            } else {
                binder.getService().stopVPN();
            }
        };
        aSwitch.setOnCheckedChangeListener(onCheckedChangeListener);

        // request notification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        2
                );
            }

        }
    }

    /**
     * update switch based on the status of vpn
     */
    private void updateSwitch(XiVPNService.Status status) {
        aSwitch.setOnCheckedChangeListener(null);
        if (status == XiVPNService.Status.CONNECTING) {
            aSwitch.setChecked(false);
            aSwitch.setEnabled(false);
            aSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
            return;
        }
        aSwitch.setEnabled(true);
        aSwitch.setChecked(status == XiVPNService.Status.CONNECTED);
        aSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

}