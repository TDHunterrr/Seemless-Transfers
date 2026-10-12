package net.seemless.transfers.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TransferToken {
    public static final String COOKIE_NAMESPACE = "seemless_transfers";
    public static final String COOKIE_PATH = "fast_auth";
    private static final String DEFAULT_SECRET = "SeemLessTransfersSecretKeyChangeMe2026";

    // Minecraft world boundaries
    public static final double MIN_XZ = -29999984.0;
    public static final double MAX_XZ = 29999984.0;
    public static final double MIN_Y = -64.0;
    public static final double MAX_Y = 320.0;

    public record LocationTarget(double x, double y, double z, float yaw, float pitch) {}

    public static boolean isValidCoordinate(double x, double y, double z) {
        return x >= MIN_XZ && x <= MAX_XZ &&
               z >= MIN_XZ && z <= MAX_XZ &&
               y >= MIN_Y && y <= MAX_Y;
    }

    public static byte[] createToken(UUID playerUuid, String targetHost, double x, double y, double z, float yaw, float pitch, long timestamp) {
        try {
            String payload = playerUuid.toString() + ":" + targetHost + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch + ":" + timestamp;
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String tokenStr = payload + ":" + bytesToHex(signature);
            return tokenStr.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static LocationTarget parseAndVerifyToken(byte[] cookiePayload, UUID playerUuid) {
        try {
            if (cookiePayload == null || cookiePayload.length == 0) return null;
            String str = new String(cookiePayload, StandardCharsets.UTF_8);
            String[] parts = str.split(":");

            // Format with yaw/pitch: uuid:host:x:y:z:yaw:pitch:timestamp:signature (9 parts)
            if (parts.length == 9) {
                UUID uuid = UUID.fromString(parts[0]);
                String host = parts[1];
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                double z = Double.parseDouble(parts[4]);
                float yaw = Float.parseFloat(parts[5]);
                float pitch = Float.parseFloat(parts[6]);
                long timestamp = Long.parseLong(parts[7]);
                String hexSignature = parts[8];

                if (Math.abs(System.currentTimeMillis() - timestamp) > 300_000) return null;
                if (!uuid.equals(playerUuid)) return null;

                String payload = uuid + ":" + host + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch + ":" + timestamp;
                Mac hmac = Mac.getInstance("HmacSHA256");
                hmac.init(new SecretKeySpec(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] expectedSig = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

                if (MessageDigest.isEqual(expectedSig, hexToBytes(hexSignature))) {
                    return new LocationTarget(x, y, z, yaw, pitch);
                }
            } else if (parts.length == 7) {
                UUID uuid = UUID.fromString(parts[0]);
                String host = parts[1];
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                double z = Double.parseDouble(parts[4]);
                long timestamp = Long.parseLong(parts[5]);
                String hexSignature = parts[6];

                if (Math.abs(System.currentTimeMillis() - timestamp) > 300_000) return null;
                if (!uuid.equals(playerUuid)) return null;

                String payload = uuid + ":" + host + ":" + x + ":" + y + ":" + z + ":" + timestamp;
                Mac hmac = Mac.getInstance("HmacSHA256");
                hmac.init(new SecretKeySpec(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] expectedSig = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

                if (MessageDigest.isEqual(expectedSig, hexToBytes(hexSignature))) {
                    return new LocationTarget(x, y, z, 0.0f, 0.0f);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
