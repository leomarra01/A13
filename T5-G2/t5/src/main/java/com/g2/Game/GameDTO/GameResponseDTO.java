package com.g2.Game.GameDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g2.Game.GameModes.Coverage.CompileResult;

public class GameResponseDTO {

    @JsonProperty("outCompile")
    private String outCompile;

    @JsonProperty("coverage")
    private String coverage;

    @JsonProperty("robotScore")
    private int robotScore;

    @JsonProperty("userScore")
    private int userScore;

    @JsonProperty("GameOver")
    private boolean gameOver;

    @JsonProperty("coverageDetails")
    private CoverageDetails coverageDetails;

    @JsonProperty("isWinner")
    private Boolean isWinner;

    public GameResponseDTO() {
        // Costruttore vuoto 
    }

    /**
     * Costruttore che converte un CompileResult in un GameResponseDTO.
     * Se uno dei risultati di coverage è nullo, vengono usati dei valori di fallback (0).
     */
    public GameResponseDTO(CompileResult compileResult,
                           Boolean gameFinished,
                           int robotScore,
                           int userScore,
                           Boolean isWinner) {
        this.outCompile = compileResult.getCompileOutput();
        this.coverage  = compileResult.getXML_coverage();
        this.robotScore = robotScore;
        this.userScore = userScore;
        this.gameOver = gameFinished;
        this.isWinner = isWinner;

        // Inizializza coverageDetails usando valori di fallback (0) se qualche parte è null.
        CoverageDetail lineDetail;
        try {
            if (compileResult.getLineCoverage() != null) {
                lineDetail = new CoverageDetail(
                        compileResult.getLineCoverage().getCovered(),
                        compileResult.getLineCoverage().getMissed()
                );
            } else {
                System.err.println("[GameResponseDTO] getLineCoverage() è null; uso fallback 0.");
                lineDetail = new CoverageDetail(0, 0);
            }
        } catch(Exception e) {
            System.err.println("[GameResponseDTO] Eccezione in getLineCoverage(): " + e.getMessage());
            lineDetail = new CoverageDetail(0, 0);
        }

        CoverageDetail branchDetail;
        try {
            if (compileResult.getBranchCoverage() != null) {
                branchDetail = new CoverageDetail(
                        compileResult.getBranchCoverage().getCovered(),
                        compileResult.getBranchCoverage().getMissed()
                );
            } else {
                System.err.println("[GameResponseDTO] getBranchCoverage() è null; uso fallback 0.");
                branchDetail = new CoverageDetail(0, 0);
            }
        } catch(Exception e) {
            System.err.println("[GameResponseDTO] Eccezione in getBranchCoverage(): " + e.getMessage());
            branchDetail = new CoverageDetail(0, 0);
        }

        CoverageDetail instructionDetail;
        try {
            if (compileResult.getInstructionCoverage() != null) {
                instructionDetail = new CoverageDetail(
                        compileResult.getInstructionCoverage().getCovered(),
                        compileResult.getInstructionCoverage().getMissed()
                );
            } else {
                System.err.println("[GameResponseDTO] getInstructionCoverage() è null; uso fallback 0.");
                instructionDetail = new CoverageDetail(0, 0);
            }
        } catch(Exception e) {
            System.err.println("[GameResponseDTO] Eccezione in getInstructionCoverage(): " + e.getMessage());
            instructionDetail = new CoverageDetail(0, 0);
        }

        this.coverageDetails = new CoverageDetails();
        this.coverageDetails.setLine(lineDetail);
        this.coverageDetails.setBranch(branchDetail);
        this.coverageDetails.setInstruction(instructionDetail);
    }

    // Getters e setters

    public String getOutCompile() {
        return outCompile;
    }
    public void setOutCompile(String outCompile) {
        this.outCompile = outCompile;
    }
    public String getCoverage() {
        return coverage;
    }
    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }
    public int getRobotScore() {
        return robotScore;
    }
    public void setRobotScore(int robotScore) {
        this.robotScore = robotScore;
    }
    public int getUserScore() {
        return userScore;
    }
    public void setUserScore(int userScore) {
        this.userScore = userScore;
    }
    public boolean isGameOver() {
        return gameOver;
    }
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
    public CoverageDetails getCoverageDetails() {
        return coverageDetails;
    }
    public void setCoverageDetails(CoverageDetails coverageDetails) {
        this.coverageDetails = coverageDetails;
    }
    public Boolean getIsWinner() {
        return isWinner;
    }
    public void setIsWinner(Boolean isWinner) {
        this.isWinner = isWinner;
    }

    public static class CoverageDetails {
        @JsonProperty("line")
        private CoverageDetail line;
        @JsonProperty("branch")
        private CoverageDetail branch;
        @JsonProperty("instruction")
        private CoverageDetail instruction;

        public CoverageDetail getLine() {
            return line;
        }
        public void setLine(CoverageDetail line) {
            this.line = line;
        }
        public CoverageDetail getBranch() {
            return branch;
        }
        public void setBranch(CoverageDetail branch) {
            this.branch = branch;
        }
        public CoverageDetail getInstruction() {
            return instruction;
        }
        public void setInstruction(CoverageDetail instruction) {
            this.instruction = instruction;
        }
    }

    public static class CoverageDetail {
        @JsonProperty("covered")
        private int covered;
        @JsonProperty("missed")
        private int missed;

        public CoverageDetail(int covered, int missed) {
            this.covered = covered;
            this.missed = missed;
        }
        public int getCovered() {
            return covered;
        }
        public void setCovered(int covered) {
            this.covered = covered;
        }
        public int getMissed() {
            return missed;
        }
        public void setMissed(int missed) {
            this.missed = missed;
        }
    }
}
