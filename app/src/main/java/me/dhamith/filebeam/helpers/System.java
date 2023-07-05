package me.dhamith.filebeam.helpers;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class System {
    public static List<String> getLocalIPs() {
        List<String> ips = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.getDisplayName().contains("wlan") || networkInterface.getDisplayName().contains("eth")) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress inetAddress = addresses.nextElement();
                        if (inetAddress.isSiteLocalAddress() || (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress())) {
                            ips.add(inetAddress.getHostAddress());
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("error", e.toString());
        }

        return ips;
    }

    public static List<List<Integer>> getLocalIPsAsInt() {
        List<String> ips = getLocalIPs();
        List<List<Integer>> output = new ArrayList<>();

        for (String ip : ips) {
            try {
                String[] ipOctetArr = ip.split("\\.");
                List<Integer> ipOctetIntArr = new ArrayList<>();
                for (String octet : ipOctetArr) {
                    ipOctetIntArr.add(Integer.parseInt(octet));
                }
                output.add(ipOctetIntArr);
            } catch (Exception ignored) { }
        }

        return output;
    }

    public static boolean isUp(String host) {
        GRPCClient client = GRPCClient.create(host);
        return client.hello();
    }

    public static String generateRandomString(int length) {
        String charset = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ0123456789";
        StringBuilder randomString = new StringBuilder(length);
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(charset.length());
            char randomChar = charset.charAt(randomIndex);
            randomString.append(randomChar);
        }

        return randomString.toString();
    }

    public static float byteToKilobyte(long input) { return (float) (input / 1024); }

    public static float byteToMegabyte(long input) { return (float) input / 1024 / 1024; }

    public static float byteToGigabyte(long input) { return (float) input / 1024 / 1024 / 1024; }

    public static String getSimplifiedFileSize(long byteSize) {
        if (byteSize >= 1098907648) {
            return String.format(new Locale("en", "us"), "%.02f GB", byteToGigabyte(byteSize));
        } else if (byteSize > 1048576) {
            return String.format(new Locale("en", "us"),"%.02f MB", byteToMegabyte(byteSize));
        } else if (byteSize >= 1024) {
            return String.format(new Locale("en", "us"),"%.02f KB", byteToKilobyte(byteSize));
        }
        return byteSize +" B";
    }

    public static float getSpeed(long startTime, long endTime, long completedBytes) {
        float diff = endTime - startTime;

        if (diff > 0)
            return (float) completedBytes / diff;

        return 0;
    }

    public static String convertToHumanReadableTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds - (hours * 3600)) / 60;
        seconds = seconds - (hours * 3600) - (minutes * 60);

        if (hours != 0) {
            if (minutes != 0) {
                return hours + "h " + minutes + "m " + String.format(new Locale("en", "us"), "%.2f", (float)seconds) + "s";
            } else {
                return hours + "h " + String.format(new Locale("en", "us"), "%.2f", (float)seconds) + "s";
            }
        }

        if (minutes != 0) {
            return minutes + "m " + String.format(new Locale("en", "us"), "%.2f", (float)seconds) + "s";
        }

        return String.format(new Locale("en", "us"), "%.2f", (float)seconds) + "s";
    }

    public static String handleTimeString(long fileSize, long completedBytes, long startTime, long endTime, float speed) {
        String eta = convertToHumanReadableTime((int) ((fileSize - completedBytes) / speed));
        int diff = (int) (endTime - startTime);
        String elapsed = convertToHumanReadableTime(diff);
        return String.format(new Locale("en", "us"), "ETA: %s | Spent: %s", eta, elapsed) ;
    }
}
