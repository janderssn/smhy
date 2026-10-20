package smhy.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * JSON document consumed by the web UI and intended for a future native client.
 * Schema 1. Numbers come from the active forecast provider only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {
    private int schema;
    private Instant generatedAt;
    private String timezone;
    private String activeSource;
    private String updatedTime;
    private String updatedDate;
    private String rangeLabel;
    /** Region used by the Stockholm-style nav item, for example {@code se/stockholm}. */
    private String regionView;
    private String regionName;
    private List<String> defaultFavorites;
    private List<String> notes;
    private List<Source> sources;
    private List<Day> days;
    private List<Site> sites;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String id;
        private String name;
        /** {@code live} for the provider that supplied the numbers. */
        private String status;
        private String kind;
        private String url;
        private String license;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Day {
        private String date;
        private int dayOfMonth;
        private String weekday;
        private String weekdayLong;
        private String month;
        private boolean weekend;
        private boolean today;
        private int leadDays;
        /** 1–3. Lead-time heuristic, not model spread. */
        private int confidence;
        private String confidenceLabel;
        private String confidenceBasis;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Site {
        private String id;
        private String name;
        private String region;
        private String regionName;
        private double latitude;
        private double longitude;
        private int altitude;
        private Launch launch;
        private List<Sun> sun;
        private List<Window> windows;
        private List<Hour> hours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Launch {
        private List<String> good;
        private List<String> ok;
        private Double windMin;
        private Double windMax;
        private Double gustMax;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sun {
        private String date;
        private String rise;
        private String set;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Window {
        private String date;
        /** First flyable local hour, inclusive, already trimmed to daylight. */
        private int fromHour;
        /** Last flyable local hour, inclusive. */
        private int toHour;
        private int durationHours;
        /** 1–10, same scale as the badges. */
        private int score;
        private double scoreRaw;
        private Double wind;
        private Double gust;
        private Double direction;
        private Double temp;
        private Double dew;
        private Double precipMin;
        private Double precipMax;
        private Double precipProb;
        private Double pressure;
        private Double humidity;
        /** Metres above the site, from temperature minus dew point. Not a forecast field. */
        private Integer cloudBase;
        private boolean cloudBaseCalculated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hour {
        private String time;
        private String date;
        private int hour;
        private Double wind;
        private Double gust;
        private Double direction;
        private Double temp;
        private Double dew;
        private Double precip;
        private Double precipMin;
        private Double precipMax;
        private Double precipProb;
        private Double pressure;
        private Double humidity;
        /** 1–10 when the hour matches a launch rule, otherwise null. */
        private Integer score;
        private boolean daylight;
        /** True when the provider had no timestep here and the previous hour was repeated. */
        private boolean repeated;
    }
}
