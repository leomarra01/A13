/*
 *   Copyright (c) 2024 Stefano Marano https://github.com/StefanoMarano80017
 *   All rights reserved.

 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.g2.Game.GameModes;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.g2.Game.GameModes.Compile.CompileResult;
import com.g2.Interfaces.ServiceManager;

@JsonIgnoreProperties(ignoreUnknown = true)

public abstract class GameLogic implements Serializable {

    private static final long serialVersionUID = 1L;

    // Il serviceManager non deve essere serializzato (lo reinietteremo se necessario)
    @JsonIgnore
    private transient ServiceManager serviceManager;

    // IDs e attributi del gioco
    @JsonProperty("game_id")
    private int gameID;

    @JsonProperty("round_id")
    private int roundID;

    @JsonProperty("turn_id")
    private int turnID;

    @JsonProperty("player_id")
    private String playerID;

    @JsonProperty("class_ut")
    private String classeUT;

    @JsonProperty("type_robot")
    private String typeRobot;

    @JsonProperty("difficulty")
    private String difficulty;

    @JsonProperty("mode")
    private String gamemode;

    // Costruttore con ServiceManager (utilizzato in produzione)
    public GameLogic(ServiceManager serviceManager, String playerID, String classeUT,
            String typeRobot, String difficulty, String gamemode) {
        this.serviceManager = serviceManager;
        this.playerID = playerID;
        this.classeUT = classeUT;
        this.typeRobot = typeRobot;
        this.difficulty = difficulty;
        this.gamemode = gamemode;
    }

    // Costruttore senza argomenti (necessario per la deserializzazione JSON)
    public GameLogic() {
    }

    // Metodi astratti che ogni gioco deve implementare
    public abstract void NextTurn(int userScore, int robotScore);

    public abstract Boolean isGameEnd();

    public abstract int GetScore(CompileResult compileResult);

    public abstract Boolean isWinner();

    // Metodo per creare la partita
    public void CreateGame() {
        String time = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        this.gameID = serviceManager.handleRequest("T4", "CreateGame", Integer.class, time, this.difficulty, "name", this.gamemode, this.playerID);
        this.roundID = serviceManager.handleRequest("T4", "CreateRound", Integer.class, this.gameID, this.classeUT, time);
    }

    // Crea e gestisce un nuovo turno
    protected void CreateTurn(String Time, int userScore) {
        //Apro un nuovo turno
        String response = serviceManager.handleRequest("T4", "CreateTurn", String.class, this.playerID, this.roundID, Time);
        //Chiudo il turno
        JSONArray jsonArray = new JSONArray(response);
        this.turnID = jsonArray.getJSONObject(0).getInt("id");
        System.out.println("CReate turn id: " + this.turnID);
        String userScore_string = String.valueOf(userScore);
        String TurnID_string = String.valueOf(this.turnID);
        serviceManager.handleRequest("T4", "EndTurn", userScore_string, Time, TurnID_string);
    }

    // Conclude il round corrente
    public void EndRound() {
        String time = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        serviceManager.handleRequest("T4", "EndRound", time, this.roundID);
    }

    // Conclude la partita
    public void EndGame(int score) {
        Boolean winner = isWinner();
        String time = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        serviceManager.handleRequest("T4", "EndGame", this.gameID, time, score, winner);
    }

    // Getters e Setters
    public int getGameID() {
        return gameID;
    }

    public void setGameID(int gameID) {
        this.gameID = gameID;
    }

    public int getRoundID() {
        return roundID;
    }

    public void setRoundID(int roundID) {
        this.roundID = roundID;
    }

    public int getTurnID() {
        return turnID;
    }

    public void setTurnID(int turnID) {
        this.turnID = turnID;
    }

    public String getTypeRobot() {
        return typeRobot;
    }

    public void setTypeRobot(String typeRobot) {
        this.typeRobot = typeRobot;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getClasseUT() {
        return classeUT;
    }

    public void setClasseUT(String classeUT) {
        this.classeUT = classeUT;
    }

    public String getMode() {
        return gamemode;
    }

    public void setMode(String mode) {
        this.gamemode = mode;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    // Setter per reiniettare il serviceManager dopo deserializzazione, se necessario
    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }
}
