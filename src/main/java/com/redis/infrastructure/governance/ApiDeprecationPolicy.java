package com.redis.infrastructure.governance;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ApiDeprecationPolicy {

    public static class DeprecationDetail {
        private final ZonedDateTime deprecationDate;
        private final ZonedDateTime sunsetDate;
        private final String docLink;

        public DeprecationDetail(ZonedDateTime deprecationDate, ZonedDateTime sunsetDate, String docLink) {
            this.deprecationDate = deprecationDate;
            this.sunsetDate = sunsetDate;
            this.docLink = docLink;
        }

        public String getDeprecationHeaderValue() {
            return deprecationDate != null ? deprecationDate.format(DateTimeFormatter.RFC_1123_DATE_TIME) : "true";
        }

        public String getSunsetHeaderValue() {
            return sunsetDate != null ? sunsetDate.format(DateTimeFormatter.RFC_1123_DATE_TIME) : "";
        }

        public String getLinkHeaderValue() {
            return docLink != null ? "<" + docLink + ">; rel=\"deprecation\"" : "";
        }
    }

    private static final Map<String, DeprecationDetail> POLICIES = new HashMap<>();

    static {
        POLICIES.put("v1", new DeprecationDetail(
                ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneId.of("GMT")),
                ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneId.of("GMT")),
                "https://api.ecommerce.com/docs/deprecation/v1"
        ));
    }

    public static DeprecationDetail getPolicy(String version) {
        if (version == null) return null;
        return POLICIES.get(version.toLowerCase().trim());
    }
}
