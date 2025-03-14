package com.g2.Game.GameModes.Compile;

public class CoverageResult {
    private final int covered;
    private final int missed;
    private final double percentage;
    private final String errorMessage;

    public CoverageResult(int covered, int missed) {
        this.covered = covered;
        this.missed = missed;
        this.percentage = calculatePercentage(covered, missed);
        this.errorMessage = null;
    }

    public CoverageResult(String errorMessage) {
        this.covered = 0;
        this.missed = 0;
        this.percentage = 0.0;
        this.errorMessage = errorMessage;
    }

    private double calculatePercentage(int covered, int missed) {
        if (covered + missed == 0) {
            return 0.0; // No coverage data available
        }
        return (double) covered / (covered + missed) * 100;
    }

    public int getCovered() {
        return covered;
    }

    public int getMissed() {
        return missed;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
