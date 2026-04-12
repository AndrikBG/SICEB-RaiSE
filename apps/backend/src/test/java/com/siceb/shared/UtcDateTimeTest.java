package com.siceb.shared;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtcDateTimeTest {

    @Test
    void nowIsInUtc() {
        UtcDateTime now = UtcDateTime.now();
        assertNotNull(now.value());
        assertEquals(ZoneId.of("UTC"), now.toUtcZoned().getZone());
    }

    @Test
    void fromInstant() {
        Instant instant = Instant.parse("2026-03-23T12:00:00Z");
        UtcDateTime dt = UtcDateTime.of(instant);
        assertEquals(instant, dt.toInstant());
    }

    @Test
    void fromZonedDateTimeConvertsToUtc() {
        ZonedDateTime mexicoTime = ZonedDateTime.of(
                2026, 3, 23, 12, 0, 0, 0,
                ZoneId.of("America/Mexico_City")
        );
        UtcDateTime dt = UtcDateTime.of(mexicoTime);
        // Mexico City is UTC-6 in March (no DST yet)
        assertEquals(Instant.parse("2026-03-23T18:00:00Z"), dt.toInstant());
    }

    @Test
    void conversionToMexicoCity() {
        UtcDateTime dt = UtcDateTime.of(Instant.parse("2026-03-23T18:00:00Z"));
        ZonedDateTime mexicoTime = dt.toMexicoCity();
        assertEquals(ZoneId.of("America/Mexico_City"), mexicoTime.getZone());
        assertEquals(12, mexicoTime.getHour());
    }

    @Test
    void beforeAndAfter() {
        UtcDateTime earlier = UtcDateTime.of(Instant.parse("2026-01-01T00:00:00Z"));
        UtcDateTime later = UtcDateTime.of(Instant.parse("2026-12-31T23:59:59Z"));
        assertTrue(earlier.isBefore(later));
        assertTrue(later.isAfter(earlier));
    }

    @Test
    void equalityByValue() {
        Instant instant = Instant.parse("2026-06-15T10:30:00Z");
        UtcDateTime a = UtcDateTime.of(instant);
        UtcDateTime b = UtcDateTime.of(instant);
        assertEquals(a, b);
    }

    @Test
    void nullRejected() {
        assertThrows(NullPointerException.class, () -> UtcDateTime.of((Instant) null));
    }

    @Test
    void toStringIsIso8601() {
        UtcDateTime dt = UtcDateTime.of(Instant.parse("2026-03-23T15:30:00Z"));
        assertEquals("2026-03-23T15:30:00Z", dt.toString());
    }
}
