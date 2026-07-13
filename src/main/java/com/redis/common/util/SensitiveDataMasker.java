package com.redis.common.util;

import java.util.regex.Pattern;

public class SensitiveDataMasker {

    private static final Pattern PASSWORD_JSON_PATTERN = Pattern.compile("(?i)(\"(?:password|securityAnswer|clientSecret|secret|token|apiKey|otp|creditCard)\"\\s*:\\s*\")[^\"]+(\")");
    private static final Pattern PASSWORD_FORM_PATTERN = Pattern.compile("(?i)((?:password|securityAnswer|clientSecret|secret|token|apiKey|otp|creditCard)=)[^&]+");
    private static final Pattern BEARER_HEADER_PATTERN = Pattern.compile("(?i)(Authorization\\s*:\\s*Bearer\\s+)[a-zA-Z0-9\\-_\\.]+");
    private static final Pattern API_KEY_HEADER_PATTERN = Pattern.compile("(?i)(X-API-Key\\s*:\\s*)[a-zA-Z0-9\\-_\\.]+");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    public static String mask(String input) {
        if (input == null) return null;
        
        String result = PASSWORD_JSON_PATTERN.matcher(input).replaceAll("$1******$2");
        result = PASSWORD_FORM_PATTERN.matcher(result).replaceAll("$1******");
        result = BEARER_HEADER_PATTERN.matcher(result).replaceAll("$1******");
        result = API_KEY_HEADER_PATTERN.matcher(result).replaceAll("$1******");
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("XXXX-XXXX-XXXX-XXXX");
        
        return result;
    }
}
