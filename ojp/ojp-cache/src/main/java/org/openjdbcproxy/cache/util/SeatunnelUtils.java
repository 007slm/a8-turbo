package org.openjdbcproxy.cache.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class SeatunnelUtils {

    /**
     * 构建 JobName
     * 格式: ojp-cache-{connHashShort}-{slug}
     */
    public static String buildJobName(String connHash, String database, String table) {
        String connId = connHash;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(connId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            connId = hexString.toString().substring(0, 8); // Short hash
        } catch (Exception e) {
            connId = Integer.toHexString(connId.hashCode());
        }

        String slug = (database + "-" + table).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");

        return "ojp-cache-" + connId + "-" + slug;
    }
}
