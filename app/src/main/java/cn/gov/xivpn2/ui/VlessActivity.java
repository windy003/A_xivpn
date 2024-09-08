package cn.gov.xivpn2.ui;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;

import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.VlessServerSettings;
import cn.gov.xivpn2.xrayconfig.VlessSettings;
import cn.gov.xivpn2.xrayconfig.VlessUser;
import cn.gov.xivpn2.xrayconfig.VmessServerSettings;
import cn.gov.xivpn2.xrayconfig.VmessSettings;
import cn.gov.xivpn2.xrayconfig.VmessUser;

public class VlessActivity extends ProxyActivity<VlessSettings>{
    @Override
    protected boolean validate(ProxyEditTextAdapter adapter) {
        return adapter.validate((k, v) -> {
            if (k.equals("PORT") || k.equals("ADDRESS") || k.equals("UUID")) {
                return !v.isEmpty();
            }
            return true;
        });
    }

    @Override
    protected Type getType() {
        return new TypeToken<Outbound<VmessSettings>>() { }.getType();
    }

    @Override
    protected VlessSettings buildProtocolSettings(ProxyEditTextAdapter adapter) {
        VlessSettings vlessSettings = new VlessSettings();

        VlessServerSettings vnext = new VlessServerSettings();
        vnext.address = adapter.getValue("ADDRESS");
        vnext.port = Integer.parseInt(adapter.getValue("PORT"));

        VlessUser user = new VlessUser();
        user.id = adapter.getValue("UUID");
        String flow = adapter.getValue("FLOW");
        if (!flow.equals("None")) {
            user.flow = flow;
        }
        vnext.users.add(user);

        vlessSettings.vnext.add(vnext);

        return vlessSettings;
    }

    @Override
    protected LinkedHashMap<String, String> decodeOutboundConfig(Outbound<VlessSettings> outbound) {
        LinkedHashMap<String, String> hashMap = super.decodeOutboundConfig(outbound);
        VlessServerSettings vnext = outbound.settings.vnext.get(0);
        hashMap.put("ADDRESS", vnext.address);
        hashMap.put("PORT", String.valueOf(vnext.port));
        String flow = vnext.users.get(0).flow;
        if (flow == null || flow.isEmpty()) {
            hashMap.put("FLOW", "");
        } else {
            hashMap.put("FLOW", flow);
        }
        hashMap.put("UUID", vnext.users.get(0).id);
        return hashMap;
    }

    @Override
    protected String getProtocolName() {
        return "vmess";
    }

    @Override
    protected void initializeInputs(ProxyEditTextAdapter adapter) {
        adapter.addInput("ADDRESS", "Address");
        adapter.addInput("PORT", "Port");
        adapter.addInput("FLOW", "Flow", List.of("none", "xtls-rprx-vision", "xtls-rprx-vision-udp443"));
        adapter.addInput("UUID", "UUID");
    }
}
