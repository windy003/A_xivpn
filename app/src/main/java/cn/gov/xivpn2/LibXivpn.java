package cn.gov.xivpn2;

public class LibXivpn {

    public static native String xivpn_version();

    public static native String xivpn_start(String config, int socksPort, int fd, String logFile, String asset);

    public static native void xivpn_stop();

}
