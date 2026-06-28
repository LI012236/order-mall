package com.example.mall.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 模拟支付回调验签。
 *
 * 真实项目：第三方用商户私钥/RSA 或平台公钥签名，这里用共享密钥 + SHA-256 简化，
 * 但"验签"这件事的工程意义是一样的：防止别人伪造回调把订单刷成已支付。
 */
public final class PaySignUtil {

    /** 演示用密钥；真实项目放配置中心或环境变量 */
    public static final String SECRET = "mall-pay-secret-2026";

    private PaySignUtil() {
    }

    public static String sign(String orderNo, String tradeNo) {
        return sha256(orderNo + "|" + tradeNo + "|" + SECRET);
    }

    public static boolean verify(String orderNo, String tradeNo, String sign) {
        if (orderNo == null || tradeNo == null || sign == null) {
            return false;
        }
        return sign(orderNo, tradeNo).equalsIgnoreCase(sign);
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
