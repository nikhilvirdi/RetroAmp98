package io.github.nikhilvirdi.retroamp98.engine;

import java.time.Duration;

/**
 * Manages A-B repeat loop points.
 * Completely decoupled from UI for standalone unit testing.
 */
public class AbLoop {

    private Duration pointA = null;
    private Duration pointB = null;

    public void setA(Duration position) {
        this.pointA = (position != null) ? position : Duration.ZERO;
        if (pointB != null && pointB.compareTo(pointA) <= 0) {
            pointB = null;
        }
    }

    public void setB(Duration position) {
        if (position == null) return;
        if (pointA == null) {
            this.pointA = position;
        } else if (position.compareTo(pointA) < 0) {
            Duration temp = pointA;
            this.pointA = position;
            this.pointB = temp;
        } else if (position.compareTo(pointA) > 0) {
            this.pointB = position;
        }
    }

    public void clear() {
        this.pointA = null;
        this.pointB = null;
    }

    public boolean isActive() {
        return pointA != null && pointB != null;
    }

    public Duration getPointA() {
        return pointA;
    }

    public Duration getPointB() {
        return pointB;
    }

    /**
     * Checks if current position has reached or exceeded point B.
     * If so, returns pointA to seek back to; otherwise returns null.
     */
    public Duration checkLoop(Duration currentPosition) {
        if (isActive() && currentPosition != null) {
            if (currentPosition.compareTo(pointB) >= 0) {
                return pointA;
            }
        }
        return null;
    }

    public String getStatusIndicator() {
        if (isActive()) {
            return "A-B";
        } else if (pointA != null) {
            return "A-...";
        }
        return "";
    }
}
