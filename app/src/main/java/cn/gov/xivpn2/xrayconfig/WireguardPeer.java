package cn.gov.xivpn2.xrayconfig;

import java.util.ArrayList;
import java.util.List;

public class WireguardPeer {
    public String endpoint;
    public String publicKey;
    public String preSharedKey;
    public String[] allowedIPs = new String[0];
}
