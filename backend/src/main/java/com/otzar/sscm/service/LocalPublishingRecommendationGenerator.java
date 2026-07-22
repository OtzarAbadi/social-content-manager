package com.otzar.sscm.service;

import com.otzar.sscm.models.PublishingRecommendationContentType;
import com.otzar.sscm.models.PublishingRecommendationInput;
import com.otzar.sscm.models.PublishingRecommendationRequest;
import com.otzar.sscm.models.PublishingRecommendationResponse;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LocalPublishingRecommendationGenerator implements PublishingRecommendationGenerator {
    static final ZoneId ZONE = ZoneId.of("Asia/Jerusalem");
    static final String PROVIDER = "LOCAL_RULES";
    static final String RULE_VERSION = "v1";

    private static final List<String> PROMOTIONAL_TERMS = Arrays.asList(
            "מבצע", "הנחה", "קופון", "חדש", "sale", "discount");
    private static final List<String> EDUCATIONAL_TERMS = Arrays.asList(
            "טיפ", "מדריך", "מידע", "איך", "guide", "tips");
    private static final List<String> EVENT_TERMS = Arrays.asList(
            "אירוע", "הרשמה", "היום", "מחר", "event", "deadline");

    private static final Map<PublishingRecommendationContentType, List<WeeklyWindow>> WINDOWS = windows();

    private final Clock clock;

    public LocalPublishingRecommendationGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PublishingRecommendationResponse generate(PublishingRecommendationRequest request) {
        LocalDateTime minimum = ZonedDateTime.now(clock).withZoneSameInstant(ZONE)
                .plusHours(24).toLocalDateTime();
        LocalDateTime existing = request.getExistingPlannedPublishDate();
        LocalDateTime anchor = existing != null && existing.isAfter(minimum) ? existing : minimum;

        List<LocalDateTime> candidates = WINDOWS.get(request.getContentType()).stream()
                .map(window -> nextOccurrence(anchor, window))
                .sorted()
                .collect(Collectors.toList());

        Category category = category(request);
        LocalDateTime recommendation;
        String rationale;
        switch (category) {
            case EVENT:
                recommendation = candidates.get(0);
                rationale = "זוהה תוכן הקשור לאירוע או למועד, ולכן נבחר חלון הפרסום הקרוב ביותר לפי כללי הסימולטור המקומי.";
                break;
            case PROMOTIONAL:
                recommendation = candidates.get(candidates.size() - 1);
                rationale = "זוהה תוכן שיווקי, ולכן נבחר חלון הפרסום המאוחר מבין החלונות השבועיים לפי כללי הסימולטור המקומי.";
                break;
            case EDUCATIONAL:
                recommendation = candidates.get(0);
                rationale = "זוהה תוכן לימודי או מידע שימושי, ולכן נבחר חלון הפרסום המוקדם מבין החלונות השבועיים לפי כללי הסימולטור המקומי.";
                break;
            case GENERAL:
            default:
                recommendation = candidates.get(Math.floorMod(request.getClientId(), candidates.size()));
                rationale = "לא זוהתה קטגוריה מיוחדת; חלון הפרסום נבחר באופן דטרמיניסטי לפי סוג התוכן ומזהה הלקוח.";
                break;
        }

        return new PublishingRecommendationResponse(
                recommendation, ZONE.getId(), PROVIDER, true, RULE_VERSION, rationale, inputsUsed(request));
    }

    private LocalDateTime nextOccurrence(LocalDateTime anchor, WeeklyWindow window) {
        LocalDate date = anchor.toLocalDate().with(TemporalAdjusters.nextOrSame(window.day));
        LocalDateTime candidate = LocalDateTime.of(date, window.time);
        if (candidate.isBefore(anchor)) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate.atZone(ZONE).toLocalDateTime();
    }

    private Category category(PublishingRecommendationRequest request) {
        String searchable = (request.getTitle() + " " + String.join(" ",
                request.getKeywords() == null ? Collections.emptyList() : request.getKeywords()))
                .toLowerCase(Locale.ROOT);
        if (containsAny(searchable, EVENT_TERMS)) return Category.EVENT;
        if (containsAny(searchable, PROMOTIONAL_TERMS)) return Category.PROMOTIONAL;
        if (containsAny(searchable, EDUCATIONAL_TERMS)) return Category.EDUCATIONAL;
        return Category.GENERAL;
    }

    private boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }

    private List<PublishingRecommendationInput> inputsUsed(PublishingRecommendationRequest request) {
        List<PublishingRecommendationInput> inputs = new ArrayList<>();
        inputs.add(PublishingRecommendationInput.CONTENT_TYPE);
        inputs.add(PublishingRecommendationInput.TITLE_KEYWORDS);
        inputs.add(PublishingRecommendationInput.CLIENT);
        if (request.getExistingPlannedPublishDate() != null) {
            inputs.add(PublishingRecommendationInput.EXISTING_PLANNED_DATE);
        }
        return inputs;
    }

    private static Map<PublishingRecommendationContentType, List<WeeklyWindow>> windows() {
        Map<PublishingRecommendationContentType, List<WeeklyWindow>> result =
                new EnumMap<>(PublishingRecommendationContentType.class);
        result.put(PublishingRecommendationContentType.IMAGE, Arrays.asList(
                new WeeklyWindow(DayOfWeek.TUESDAY, LocalTime.of(12, 30)),
                new WeeklyWindow(DayOfWeek.THURSDAY, LocalTime.of(12, 30))));
        result.put(PublishingRecommendationContentType.VIDEO, Arrays.asList(
                new WeeklyWindow(DayOfWeek.SUNDAY, LocalTime.of(19, 0)),
                new WeeklyWindow(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0))));
        result.put(PublishingRecommendationContentType.TEXT, Arrays.asList(
                new WeeklyWindow(DayOfWeek.MONDAY, LocalTime.of(9, 30)),
                new WeeklyWindow(DayOfWeek.THURSDAY, LocalTime.of(9, 30))));
        return result;
    }

    private enum Category { PROMOTIONAL, EDUCATIONAL, EVENT, GENERAL }

    private static final class WeeklyWindow {
        private final DayOfWeek day;
        private final LocalTime time;

        private WeeklyWindow(DayOfWeek day, LocalTime time) {
            this.day = day;
            this.time = time;
        }
    }
}
