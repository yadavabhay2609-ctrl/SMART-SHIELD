package com.smartshield.util;

import java.time.Year;

import org.springframework.stereotype.Component;

/**
 * Generates server-side deterministic, collision-resistant tracking IDs for grievances
 * in the format SS-YYYY-XXXX (e.g. SS-2026-0001, SS-2026-0042, SS-2026-1234).
 */
@Component
public class TrackingIdGenerator {

    private static final String PREFIX = "SS";

    /**
     * Generates a tracking ID using the current calendar year and database ID.
     *
     * @param id the persisted grievance database primary key
     * @return tracking ID in format SS-YYYY-XXXX
     */
    public String generate(Long id) {
        int currentYear = Year.now().getValue();
        return generate(id, currentYear);
    }

    /**
     * Generates a tracking ID using a specific year and database ID.
     *
     * @param id the persisted grievance database primary key
     * @param year the 4-digit calendar year
     * @return tracking ID in format SS-YYYY-XXXX
     */
    public String generate(Long id, int year) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Grievance database ID must be a positive non-null number");
        }
        String formattedNumber = (id < 10000) ? String.format("%04d", id) : String.valueOf(id);
        return String.format("%s-%d-%s", PREFIX, year, formattedNumber);
    }
}
