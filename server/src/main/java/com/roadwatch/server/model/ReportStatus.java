package com.roadwatch.server.model;

import java.util.EnumSet;
import java.util.Set;

/** Lifecycle of a pothole report. Each state knows which states may follow it. */
public enum ReportStatus {
    REPORTED, UNDER_REVIEW, IN_PROGRESS, RESOLVED, REJECTED;

    public Set<ReportStatus> allowedNext() {
        return switch (this) {
            case REPORTED -> EnumSet.of(UNDER_REVIEW, IN_PROGRESS, REJECTED);
            case UNDER_REVIEW -> EnumSet.of(IN_PROGRESS, REJECTED);
            case IN_PROGRESS -> EnumSet.of(RESOLVED);
            case RESOLVED -> EnumSet.of(IN_PROGRESS);
            case REJECTED -> EnumSet.of(UNDER_REVIEW);
        };
    }

    public boolean canMoveTo(ReportStatus next) {
        return allowedNext().contains(next);
    }
}
