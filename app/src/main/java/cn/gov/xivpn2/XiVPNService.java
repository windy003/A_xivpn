package cn.gov.xivpn2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.logging.Logger;

public class XiVPNService extends VpnService {

    private final IBinder binder = new XiVPNBinder();
    private final String TAG = "XiVPNService";
    private VPNStatusListener listener;
    private Status status = Status.DISCONNECTED;
    private ParcelFileDescriptor fileDescriptor;

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

    public synchronized void startVPN() {
        if (status != Status.DISCONNECTED) return;

        status = Status.CONNECTING;
        if (listener != null) listener.onStatusChanged(status);

        // start foreground service
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "XiVPNService");
        builder.setContentText("XiVPN is running");
        builder.setSmallIcon(R.drawable.baseline_vpn_key_24);
        startForeground(1, builder.build());

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
        LibXivpn.xivpn_start("{\"log\":{\"loglevel\":\"info\"},\"inbounds\":[{\"port\":18964,\"listen\":\"10.89.64.1\",\"protocol\":\"socks\",\"settings\":{\"udp\":true}}],\"outbounds\":[{\"protocol\":\"freedom\"}]}", 18964, fileDescriptor.detachFd());

        status = Status.CONNECTED;
        if (listener != null) listener.onStatusChanged(status);
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