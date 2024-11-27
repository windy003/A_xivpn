package cn.gov.xivpn2.ui;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;

import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.ShadowsocksServerSettings;
import cn.gov.xivpn2.xrayconfig.ShadowsocksSettings;
import cn.gov.xivpn2.xrayconfig.XHttpSettings;
import cn.gov.xivpn2.xrayconfig.XHttpStream;

public class XHttpStreamActivity extends ProxyActivity<XHttpSettings> {

    @Override
    protected XHttpSettings buildProtocolSettings(ProxyEditTextAdapter adapter) {
        return new XHttpSettings();
    }

    @Override
    protected boolean validate(ProxyEditTextAdapter adapter) {
        return super.validate(adapter);
    }

    @Override
    protected LinkedHashMap<String, String> decodeOutboundConfig(Outbound<XHttpSettings> outbound) {
        return super.decodeOutboundConfig(outbound);
    }

    @Override
    protected Type getType() {
        return new TypeToken<Outbound<XHttpStream>>() { }.getType();
    }

    @Override
    protected String getProtocolName() {
        return "xhttpstream";
    }

    @Override
    protected void initializeInputs(ProxyEditTextAdapter adapter) {
    }

    @Override
    protected void afterInitializeInputs(ProxyEditTextAdapter adapter) {
        adapter.removeInputByPrefix("NETWORK");
    }

    @Override
    protected void onInputChanged(ProxyEditTextAdapter adapter, String key, String value) {
        super.onInputChanged(adapter, key, value);
    }

}
