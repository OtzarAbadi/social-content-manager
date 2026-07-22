package com.otzar.sscm;

import com.otzar.sscm.models.PublishingRecommendationContentType;
import com.otzar.sscm.models.PublishingRecommendationRequest;
import com.otzar.sscm.models.PublishingRecommendationResponse;
import com.otzar.sscm.service.LocalPublishingRecommendationGenerator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalPublishingRecommendationGeneratorTests {
    private static final ZoneId JERUSALEM = ZoneId.of("Asia/Jerusalem");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-20T06:00:00Z"), JERUSALEM);
    private final LocalPublishingRecommendationGenerator generator =
            new LocalPublishingRecommendationGenerator(FIXED_CLOCK);

    @Test
    void identicalInputAndFixedClockReturnIdenticalResponse() {
        PublishingRecommendationRequest request = request(PublishingRecommendationContentType.IMAGE, 7L, "תוכן רגיל");
        PublishingRecommendationResponse first = generator.generate(request);
        PublishingRecommendationResponse second = generator.generate(request);

        assertEquals(first.getRecommendedPlannedPublishDate(), second.getRecommendedPlannedPublishDate());
        assertEquals(first.getRationale(), second.getRationale());
        assertEquals("LOCAL_RULES", first.getProvider());
        assertEquals("v1", first.getRuleVersion());
        assertEquals("Asia/Jerusalem", first.getTimezone());
        assertTrue(first.isGenerated());
    }

    @Test
    void everyContentTypeUsesOnlyItsDocumentedWindowsAndLeadTime() {
        assertWindow(PublishingRecommendationContentType.IMAGE,
                List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 12, 30);
        assertWindow(PublishingRecommendationContentType.VIDEO,
                List.of(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY), 19, 0);
        assertWindow(PublishingRecommendationContentType.TEXT,
                List.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), 9, 30);
    }

    @Test
    void categoriesChooseLaterEarlierAndNearestCandidate() {
        LocalDateTime promotional = generator.generate(
                request(PublishingRecommendationContentType.IMAGE, 2L, "מבצע קיץ")).getRecommendedPlannedPublishDate();
        LocalDateTime educational = generator.generate(
                request(PublishingRecommendationContentType.IMAGE, 2L, "מדריך קיץ")).getRecommendedPlannedPublishDate();
        LocalDateTime event = generator.generate(
                request(PublishingRecommendationContentType.IMAGE, 2L, "אירוע קיץ")).getRecommendedPlannedPublishDate();

        assertTrue(promotional.isAfter(educational));
        assertEquals(educational, event);
    }

    @Test
    void keywordCategoriesAreRecognizedAndEventHasPriority() {
        PublishingRecommendationRequest request = request(PublishingRecommendationContentType.VIDEO, 2L, "כותרת");
        request.setKeywords(List.of("discount", "deadline"));
        PublishingRecommendationResponse response = generator.generate(request);

        assertTrue(response.getRationale().contains("אירוע"));
    }

    @Test
    void clientDistributionIsStableAndCanSelectBothEquivalentWindows() {
        LocalDateTime evenFirst = generator.generate(
                request(PublishingRecommendationContentType.TEXT, 2L, "כללי")).getRecommendedPlannedPublishDate();
        LocalDateTime evenSecond = generator.generate(
                request(PublishingRecommendationContentType.TEXT, 2L, "כללי")).getRecommendedPlannedPublishDate();
        LocalDateTime odd = generator.generate(
                request(PublishingRecommendationContentType.TEXT, 3L, "כללי")).getRecommendedPlannedPublishDate();

        assertEquals(evenFirst, evenSecond);
        assertFalse(evenFirst.equals(odd));
    }

    @Test
    void futureExistingDateAnchorsRecommendationAndPastDateCannotBypassLeadTime() {
        PublishingRecommendationRequest future = request(PublishingRecommendationContentType.IMAGE, 1L, "אירוע");
        future.setExistingPlannedPublishDate(LocalDateTime.of(2026, 8, 14, 14, 0));
        LocalDateTime futureResult = generator.generate(future).getRecommendedPlannedPublishDate();
        assertFalse(futureResult.isBefore(future.getExistingPlannedPublishDate()));

        PublishingRecommendationRequest past = request(PublishingRecommendationContentType.IMAGE, 1L, "אירוע");
        past.setExistingPlannedPublishDate(LocalDateTime.of(2020, 1, 1, 0, 0));
        LocalDateTime pastResult = generator.generate(past).getRecommendedPlannedPublishDate();
        LocalDateTime minimum = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), JERUSALEM).plusHours(24);
        assertFalse(pastResult.isBefore(minimum));
    }

    @Test
    void jerusalemDstTransitionProducesAValidDocumentedWindow() {
        Clock dstClock = Clock.fixed(Instant.parse("2026-03-26T08:00:00Z"), JERUSALEM);
        PublishingRecommendationResponse response = new LocalPublishingRecommendationGenerator(dstClock)
                .generate(request(PublishingRecommendationContentType.VIDEO, 5L, "אירוע"));
        LocalDateTime recommended = response.getRecommendedPlannedPublishDate();

        assertEquals(19, recommended.getHour());
        assertEquals(0, recommended.getMinute());
        assertEquals(1, JERUSALEM.getRules().getValidOffsets(recommended).size());
    }

    private void assertWindow(PublishingRecommendationContentType type, List<DayOfWeek> days,
                              int hour, int minute) {
        LocalDateTime result = generator.generate(request(type, 4L, "אירוע")).getRecommendedPlannedPublishDate();
        LocalDateTime minimum = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), JERUSALEM).plusHours(24);
        assertTrue(days.contains(result.getDayOfWeek()));
        assertEquals(hour, result.getHour());
        assertEquals(minute, result.getMinute());
        assertFalse(result.isBefore(minimum));
    }

    private PublishingRecommendationRequest request(PublishingRecommendationContentType type,
                                                     Long clientId, String title) {
        PublishingRecommendationRequest request = new PublishingRecommendationRequest();
        request.setContentType(type);
        request.setClientId(clientId);
        request.setTitle(title);
        request.setKeywords(List.of());
        return request;
    }
}
