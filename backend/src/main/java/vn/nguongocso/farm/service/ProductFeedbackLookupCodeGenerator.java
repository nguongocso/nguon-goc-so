package vn.nguongocso.farm.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** Sinh và băm mã tra cứu phản ánh công khai. */
@Component
public class ProductFeedbackLookupCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int RANDOM_CHARACTER_COUNT = 16;
    private static final int GROUP_SIZE = 4;
    private static final String PREFIX = "PA";

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedLookupCode generate() {
        StringBuilder compactCode = new StringBuilder(PREFIX);
        for (int index = 0; index < RANDOM_CHARACTER_COUNT; index++) {
            compactCode.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }

        String normalizedCode = compactCode.toString();
        return new GeneratedLookupCode(format(normalizedCode), sha256(normalizedCode));
    }

    public String hash(String lookupCode) {
        return sha256(normalize(lookupCode));
    }

    private String normalize(String lookupCode) {
        if (lookupCode == null) {
            throw new IllegalArgumentException("Mã tra cứu không hợp lệ");
        }

        String normalized = lookupCode.trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "");
        if (normalized.length() != PREFIX.length() + RANDOM_CHARACTER_COUNT
                || !normalized.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Mã tra cứu không hợp lệ");
        }

        for (int index = PREFIX.length(); index < normalized.length(); index++) {
            if (ALPHABET.indexOf(normalized.charAt(index)) < 0) {
                throw new IllegalArgumentException("Mã tra cứu không hợp lệ");
            }
        }
        return normalized;
    }

    private String format(String normalizedCode) {
        String randomPart = normalizedCode.substring(PREFIX.length());
        StringBuilder displayCode = new StringBuilder(PREFIX);
        for (int index = 0; index < randomPart.length(); index += GROUP_SIZE) {
            displayCode.append('-')
                    .append(randomPart, index, index + GROUP_SIZE);
        }
        return displayCode.toString();
    }

    private String sha256(String normalizedCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    public record GeneratedLookupCode(String displayValue, String hash) {
    }
}
