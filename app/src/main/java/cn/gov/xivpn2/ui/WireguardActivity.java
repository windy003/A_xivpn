package cn.gov.xivpn2.ui;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.WireguardPeer;
import cn.gov.xivpn2.xrayconfig.WireguardSettings;

public class WireguardActivity extends ProxyActivity<WireguardSettings> {

    private static final Pattern RESERVED_PATTERN = Pattern.compile("^\\d{1,3},\\d{1,3},\\d{1,3}$");

    @Override
    protected boolean validate(IProxyEditor adapter) {
        return adapter.validate((k, v) -> {
            if (k.equals("PRIVATE_KEY") || k.equals("PEER_ENDPOINT") || k.equals("PEER_PUBLIC_KEY")) {
                return !v.isEmpty();
            }
            if (k.equals("RESERVED")) {
                Matcher matcher = RESERVED_PATTERN.matcher(v);
                return matcher.find();
            }
            return true;
        });
    }

    @Override
    protected Type getType() {
        return new TypeToken<Outbound<WireguardSettings>>() {
        }.getType();
    }

    @Override
    protected WireguardSettings buildProtocolSettings(IProxyEditor adapter) {
        WireguardSettings wireguardSettings = new WireguardSettings();

        String address1 = adapter.getValue("ADDRESS1");
        String address2 = adapter.getValue("ADDRESS2");
        if (!address1.isEmpty()) wireguardSettings.address.add(address1);
        if (!address2.isEmpty()) wireguardSettings.address.add(address2);

        wireguardSettings.secretKey = adapter.getValue("PRIVATE_KEY");
        String[] splits = adapter.getValue("RESERVED").split(",");
        wireguardSettings.reserved[0] = Integer.parseInt(splits[0]);
        wireguardSettings.reserved[1] = Integer.parseInt(splits[1]);
        wireguardSettings.reserved[2] = Integer.parseInt(splits[2]);

        WireguardPeer peer = new WireguardPeer();
        peer.endpoint = adapter.getValue("PEER_ENDPOINT");
        peer.publicKey = adapter.getValue("PEER_PUBLIC_KEY");
        peer.preSharedKey = adapter.getValue("PEER_PRE_SHARED_KEY");
        peer.allowedIPs = adapter.getValue("PEER_ALLOWED_IPS").split(",");
        wireguardSettings.peers.add(peer);

        return wireguardSettings;
    }

    @Override
    protected LinkedHashMap<String, String> decodeOutboundConfig(Outbound<WireguardSettings> outbound) {
        // wireguard does not support stream settings
        // so we are not calling super.decodeOutboundConfig
        LinkedHashMap<String, String> hashMap = new LinkedHashMap<>();

        if (!outbound.settings.address.isEmpty()) {
            hashMap.put("ADDRESS1", outbound.settings.address.get(0));
        }
        if (outbound.settings.address.size() >= 2) {
            hashMap.put("ADDRESS2", outbound.settings.address.get(1));
        }
        hashMap.put("PRIVATE_KEY", outbound.settings.secretKey);
        hashMap.put("RESERVED", outbound.settings.reserved[0] + "," + outbound.settings.reserved[1] + "," + outbound.settings.reserved[2]);
        hashMap.put("PEER_ENDPOINT", outbound.settings.peers.get(0).endpoint);
        hashMap.put("PEER_PUBLIC_KEY", outbound.settings.peers.get(0).publicKey);
        hashMap.put("PEER_PRE_SHARED_KEY", outbound.settings.peers.get(0).preSharedKey);
        hashMap.put("PEER_ALLOWED_IPS", String.join(",", outbound.settings.peers.get(0).allowedIPs));

        return hashMap;
    }

    @Override
    protected String getProtocolName() {
        return "wireguard";
    }

    @Override
    protected void initializeInputs(IProxyEditor adapter) {
        adapter.addInput("ADDRESS1", "Address 1", "", "Local Address (IPv4 CIDR)");
        adapter.addInput("ADDRESS2", "Address 2", "", "Local Address (IPv6 CIDR)");
        adapter.addInput("PRIVATE_KEY", "Private Key");
        adapter.addInput("RESERVED", "Reserved", "0,0,0", "Format: 0,0,0");
        adapter.addInput("PEER_ENDPOINT", "Peer Endpoint", "", "Example: engage.cloudflareclient.com:2408");
        adapter.addInput("PEER_PUBLIC_KEY", "Peer Public Key");
        adapter.addInput("PEER_PRE_SHARED_KEY", "Peer Pre Shared Key");
        adapter.addInput("PEER_ALLOWED_IPS", "Allowed IPs", "0.0.0.0/0,::/0");
    }

    /**
     * Wireguard does not support stream settings
     */
    @Override
    protected boolean hasStreamSettings() {
        return false;
    }
}
