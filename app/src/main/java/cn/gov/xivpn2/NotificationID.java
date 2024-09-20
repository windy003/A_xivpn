package cn.gov.xivpn2;

public class NotificationID {
    private static int id = 1;

    public synchronized static int getID() {
        int i = id;
        id++;
        return i;
    }
}
