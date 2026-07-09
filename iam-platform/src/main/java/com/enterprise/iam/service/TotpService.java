package com.enterprise.iam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@Service
public class TotpService {

    private static final Logger log = LoggerFactory.getLogger(TotpService.class);

    @Value("${app.mfa.app-name:EnterpriseIAM}")
    private String appName;

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] buffer = new byte[10];
        random.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    public String getQrCodeUrl(String secret, String username) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                appName, username, secret, appName);
    }

    public boolean verifyCode(String secret, String codeStr) {
        try {
            int code = Integer.parseInt(codeStr);
            byte[] decodedKey = decodeBase32(secret);
            long currentWindow = System.currentTimeMillis() / 1000L / 30L;
            
            for (int i = -1; i <= 1; i++) {
                if (getTOTPCode(decodedKey, currentWindow + i) == code) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid TOTP code format: {}", codeStr);
        } catch (Exception e) {
            log.error("Error verifying TOTP code: {}", e.getMessage());
        }
        return false;
    }

    private static int getTOTPCode(byte[] key, long timeIndex) throws Exception {
        byte[] data = new byte[8];
        long value = timeIndex;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0xF;
        int truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        return truncatedHash % 1000000;
    }

    private static String encodeBase32(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        int digit = 0;
        int currByte;
        for (int i = 0; i < bytes.length; i++) {
            currByte = bytes[i] & 0xFF;
            index = (index << 8) | currByte;
            digit += 8;
            while (digit >= 5) {
                sb.append(ALPHABET.charAt((index >> (digit - 5)) & 0x1F));
                digit -= 5;
            }
        }
        if (digit > 0) {
            sb.append(ALPHABET.charAt((index << (5 - digit)) & 0x1F));
        }
        return sb.toString();
    }

    private static byte[] decodeBase32(String base32) {
        base32 = base32.toUpperCase().replace("-", "");
        StringBuilder bits = new StringBuilder();
        for (int i = 0; i < base32.length(); i++) {
            char c = base32.charAt(i);
            if (c == '=') break;
            int val = ALPHABET.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("Invalid Base32 character: " + c);
            bits.append(String.format("%5s", Integer.toBinaryString(val)).replace(' ', '0'));
        }
        byte[] bytes = new byte[bits.length() / 8];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(bits.substring(i * 8, (i + 1) * 8), 2);
        }
        return bytes;
    }
}
