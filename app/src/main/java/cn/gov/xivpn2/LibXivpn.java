package cn.gov.xivpn2;

import cn.gov.xivpn2.service.SocketProtect;

public class LibXivpn {

    public static native String xivpn_version();

    public static native String xivpn_start(String config, int socksPort, int fd, String logFile, String asset, SocketProtect protect);

    public static native void xivpn_stop();

    public static native void xivpn_init();

}
