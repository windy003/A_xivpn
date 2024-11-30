package cn.gov.xivpn2.ui;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;

import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.TrojanServerSettings;
import cn.gov.xivpn2.xrayconfig.TrojanSettings;

public class TrojanActivity extends ProxyActivity<TrojanSettings> {
    @Override
    protected boolean validate(IProxyEditor adapter) {
        return adapter.validate((k, v) -> {
            if (k.equals("PASSWORD") || k.equals("ADDRESS")) {
                return !v.isEmpty();
            }
            if (k.equals("PORT")) {
                if (v.isEmpty()) return false;
                try {
                    int i = Integer.parseInt(v);
                    return i <= 65535 && i >= 1;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        });
    }

    @Override
    protected Type getType() {
        return new TypeToken<Outbound<TrojanSettings>>() {
        }.getType();
    }

    @Override
    protected TrojanSettings buildProtocolSettings(IProxyEditor adapter) {
        TrojanSettings trojanSettings = new TrojanSettings();

        TrojanServerSettings server = new TrojanServerSettings();
        server.address = adapter.getValue("ADDRESS");
        server.port = Integer.parseInt(adapter.getValue("PORT"));
        server.password = adapter.getValue("PASSWORD");
        trojanSettings.servers.add(server);

        return trojanSettings;
    }

    @Override
    protected LinkedHashMap<String, String> decodeOutboundConfig(Outbound<TrojanSettings> outbound) {
        LinkedHashMap<String, String> hashMap = super.decodeOutboundConfig(outbound);

        TrojanServerSettings trojanServerSettings = outbound.settings.servers.get(0);
        hashMap.put("PASSWORD", trojanServerSettings.password);
        hashMap.put("PORT", String.valueOf(trojanServerSettings.port));
        hashMap.put("ADDRESS", trojanServerSettings.address);

        return hashMap;
    }

    @Override
    protected String getProtocolName() {
        return "trojan";
    }

    @Override
    protected void initializeInputs(IProxyEditor adapter) {
        adapter.addInput("ADDRESS", "Address");
        adapter.addInput("PORT", "Port");
        adapter.addInput("PASSWORD", "Password");
    }
}
