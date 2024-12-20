package cn.gov.xivpn2;

public class Utils {
    public static boolean isValidPort(String v) {
        if (v.isEmpty()) return false;
        try {
            int i = Integer.parseInt(v);
            return i <= 65535 && i >= 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
