package com.campus.lab.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * SHA-256 加盐哈希（演示级方案，生产环境建议更换 BCrypt）。
 * 存储格式：hex(salt) + "$" + hex(sha256(salt + raw))
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasswordUtil() {
    }

    public static String hash(String raw) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return toHex(salt) + "$" + toHex(digest(raw, salt));
    }

    public static boolean verify(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        int idx = stored.indexOf('$');
        if (idx <= 0) {
            return false;
        }
        try {
            byte[] salt = fromHex(stored.substring(0, idx));
            byte[] expect = fromHex(stored.substring(idx + 1));
            byte[] actual = digest(raw, salt);
            return MessageDigest.isEqual(expect, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] digest(String raw, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("摘要计算失败", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
