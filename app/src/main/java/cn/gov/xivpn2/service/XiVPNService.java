package cn.gov.xivpn2.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cn.gov.xivpn2.LibXivpn;
import cn.gov.xivpn2.NotificationID;
import cn.gov.xivpn2.R;
import cn.gov.xivpn2.xrayconfig.Config;

public class XiVPNService extends VpnService {

    private final IBinder binder = new XiVPNBinder();
    private final String TAG = "XiVPNService";
    private VPNStatusListener listener;
    private Status status = Status.DISCONNECTED;
    private ParcelFileDescriptor fileDescriptor;
    public static final int SOCKS_PORT = 18964;

    public static enum Status {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
    }

    public static interface VPNStatusListener {
        void onStatusChanged(Status status);
        void onMessage(String msg);
    }

    public class XiVPNBinder extends Binder {

        public XiVPNService getService() {
            return XiVPNService.this;
        }

        public void setListener(VPNStatusListener listener) {
            XiVPNService.this.listener = listener;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public synchronized void startVPN(Config config) {
        if (status != Status.DISCONNECTED) return;

        status = Status.CONNECTING;
        if (listener != null) listener.onStatusChanged(status);

        // start foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "XiVPNService");
        builder.setContentText("XiVPN is running");
        builder.setSmallIcon(R.drawable.baseline_vpn_key_24);
        startForeground(NotificationID.getID(), builder.build());

        // establish vpn
        Builder vpnBuilder = new Builder();
        vpnBuilder.addRoute("0.0.0.0", 0);
        vpnBuilder.addRoute("[::]", 0);
        vpnBuilder.addAddress("10.89.64.1", 32);
        vpnBuilder.addDnsServer("8.8.8.8");
        vpnBuilder.addDnsServer("8.8.4.4");
        try {
            vpnBuilder.addDisallowedApplication(this.getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) {

        }
        fileDescriptor = vpnBuilder.establish();

        // start libxivpn
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String xrayConfig = gson.toJson(config);
        Log.i(TAG, "xray config: " + xrayConfig);
        String ret = LibXivpn.xivpn_start(xrayConfig, 18964, fileDescriptor.detachFd());

        status = Status.CONNECTED;
        if (listener != null) listener.onStatusChanged(status);

        if (!ret.isEmpty()) {
            stopVPN();
            if (listener != null) listener.onMessage("ERROR: " + ret);
        }
    }

    public synchronized void stopVPN() {
        if (status != Status.CONNECTED) return;

        stopForeground(true);
        LibXivpn.xivpn_stop();

        status = Status.DISCONNECTED;
        if (listener != null) listener.onStatusChanged(status);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public Status getStatus() {
        return status;
    }
}