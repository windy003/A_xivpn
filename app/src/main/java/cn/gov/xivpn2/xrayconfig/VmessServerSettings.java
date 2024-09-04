package cn.gov.xivpn2.xrayconfig;

import java.util.ArrayList;
import java.util.List;

public class VmessServerSettings {
    public String address;
    public int port;
    public List<VmessUser> users = new ArrayList<>();
}
