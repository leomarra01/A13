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
package com.g2.Interfaces;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.g2.Model.Game;
import com.g2.Model.StatisticProgress;

@Service
public class T4Service extends BaseService {

    // Costante che definisce l'URL di base per le richieste REST
    private static final String BASE_URL = "http://t4-g18-app-1:3000";

    // Costruttore della classe, inizializza il servizio con il RestTemplate e l'URL
    // di base
    public T4Service(RestTemplate restTemplate) {
        // Inizializzazione del servizio base con RestTemplate e URL specificato
        super(restTemplate, BASE_URL);

        registerAction("getGames", new ServiceActionDefinition(
                params -> getGames((int) params[0]),
                Integer.class
        ));

        registerAction("getStatisticsProgresses", new ServiceActionDefinition(
                params -> getStatisticsProgresses((int) params[0]),
                Integer.class
        ));

        registerAction("getHashStatisticsProgresses", new ServiceActionDefinition(
            params -> getHashStatisticsProgresses((int) params[0]),
            Integer.class
        ));

        registerAction("updateStatisticProgress", new ServiceActionDefinition(
                params -> updateStatisticProgress((int) params[0], (String) params[1], (float) params[2]),
                Integer.class, String.class, Float.class));

        registerAction("CreateGame", new ServiceActionDefinition(
                params -> CreateGame((String) params[0], (String) params[1], (String) params[2], (String) params[3],
                        (String) params[4]),
                String.class, String.class, String.class, String.class, String.class));

        registerAction("EndGame", new ServiceActionDefinition(
                params -> EndGame((int) params[0], (String) params[1], (int) params[2], (Boolean) params[3]),
                Integer.class, String.class, Integer.class, Boolean.class));

        registerAction("CreateRound", new ServiceActionDefinition(
                params -> CreateRound((int) params[0], (String) params[1], (String) params[2]),
                Integer.class, String.class, String.class));

        registerAction("EndRound", new ServiceActionDefinition(
                params -> EndRound((String) params[0], (int) params[1]),
                String.class, Integer.class));

        registerAction("CreateTurn", new ServiceActionDefinition(
                params -> CreateTurn((String) params[0], (int) params[1], (String) params[2]),
                String.class, Integer.class, String.class));

        registerAction("EndTurn", new ServiceActionDefinition(
                params -> EndTurn((String) params[0], (String) params[1], (String) params[2]),
                String.class, String.class, String.class));

        registerAction("CreateScalata", new ServiceActionDefinition(
                params -> CreateScalata((String) params[0], (String) params[1], (String) params[2], (String) params[3]),
                String.class, String.class, String.class, String.class));

        registerAction("GetRisultati", new ServiceActionDefinition(
                params -> GetRisultati((String) params[0], (String) params[1], (String) params[2]),
                String.class, String.class, String.class));
    }

    // usa /games per ottenere una lista di giochi
    private List<Game> getGames(int playerId) {
        final String endpoint = "/games/player/" + playerId;
        return callRestGET(endpoint, null, new ParameterizedTypeReference<List<Game>>() {
        });
    }

    private List<StatisticProgress> getStatisticsProgresses(int playerID) {
        Map<String, String> formData = new HashMap<>();
        formData.put("pid", String.valueOf(playerID));

        String endpoint = "/phca/" + playerID;

        List<StatisticProgress> response = callRestGET(endpoint, formData, new ParameterizedTypeReference<List<StatisticProgress>>() {
        });
        return response;
    }

    private Set<StatisticProgress> getHashStatisticsProgresses(int playerID) {
        Map<String, String> formData = new HashMap<>();
        formData.put("pid", String.valueOf(playerID));
        String endpoint = "/phca/" + playerID;
        // Recupera la risposta come una lista
        List<StatisticProgress> response = callRestGET( endpoint, 
                                                        formData, 
                                                        new ParameterizedTypeReference<List<StatisticProgress>>() {
                                                       });
        // Converti la lista in un HashSet per rimuovere eventuali duplicati
        Set<StatisticProgress> responseSet = new HashSet<>(response);
        return responseSet;
    }

    private String updateStatisticProgress(int playerID, String statisticID, float progress) {
        JSONObject obj = new JSONObject();
        obj.put("playerId", playerID);
        obj.put("statistic", statisticID);
        obj.put("progress", progress);

        String endpoint = "/phca/" + playerID + "/" + statisticID;
        String response = callRestPut(endpoint, obj, null, null, String.class);
        return response;
    }

    /*
    private String updateStatisticProgress(int playerID, String statisticID, float progress) {
        try {
            MultiValueMap<String, String> jsonMap = new LinkedMultiValueMap<>();
            jsonMap.put("playerId", Collections.singletonList(String.valueOf(playerID)));
            jsonMap.put("statistic", Collections.singletonList(statisticID));
            jsonMap.put("progress", Collections.singletonList(String.valueOf(progress)));

            String endpoint = "/phca/" + playerID + "/" + statisticID;

            String response = callRestPut(endpoint, jsonMap, new HashMap<>(), String.class);

            return response;
        } catch (Exception e) {
            System.out.println("[updateStatisticProgress] Errore nell'update delle statistiche: " + e.getMessage());
            return "errore UPDATESTATISTICPROGRESS";
        }
    }
     */
    // usa /robots per ottenere dati
    private String GetRisultati(String className, String robot_type, String difficulty) {
        Map<String, String> formData = new HashMap<>();
        formData.put("testClassId", className); // Nome della classe
        formData.put("type", robot_type); // Tipo di robot
        formData.put("difficulty", difficulty); // Livello di difficoltà corrente

        String response = callRestGET("/robots", formData, String.class);
        return response;
    }

    private int CreateGame(String Time, String difficulty, String name, String description, String id) {
        final String endpoint = "/games";
        JSONObject obj = new JSONObject();
        obj.put("difficulty", difficulty);
        obj.put("name", name);
        /*
         * Stiamo usando descrizione per salvare la modalità di gioco 
         */
        obj.put("description", description);
        obj.put("startedAt", Time);
        JSONArray playersArray = new JSONArray();
        playersArray.put(String.valueOf(id));
        obj.put("players", playersArray);
        // Questa chiamata in risposta dà anche i valori che hai fornito, quindi faccio
        // parse per avere l'id
        String respose = callRestPost(endpoint, obj, null, null, String.class);
        // Parsing della stringa JSON
        JSONObject jsonObject = new JSONObject(respose);
        // Estrazione del valore di id
        return jsonObject.getInt("id");
    }

    private String EndGame(int gameid, String closedAt, int Score, Boolean isWinner) {
        final String endpoint = "/games/" + String.valueOf(gameid);
        /*
         * La chiamata sovrascrive perchè è una put, devi ridare tutti i dati 
            CurrentRound int        `json:"currentRound"`
            Name         string     `json:"name"`
            Username     string     `json:"username"`
            Description  string     `json:"description"`
            Score        float64    `json:"score"`
            IsWinner     bool       `json:"isWinner"`
            StartedAt    *time.Time `json:"startedAt,omitempty"`
            ClosedAt     *time.Time `json:"closedAt,omitempty"`
         */
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("closedAt", closedAt);
        jsonObject.put("score", Score);
        jsonObject.put("isWinner", isWinner);

        System.out.println(jsonObject);
        String respose = callRestPut(endpoint, jsonObject, null, null, String.class);
        return respose;
    }

    /*
    private String EndGame(int gameid, String username, String closedAt, int Score, Boolean isWinner){
        final String endpoint = "/games/" + String.valueOf(gameid);
        JSONObject formData = new JSONObject();
        formData.put("closedAt", closedAt);
        formData.put("username", username);
        formData.put("score", Integer.toString(Score));
        formData.put("isWinner", isWinner ? "true" : "false");
        try {
            String respose = callRestPut(endpoint, formData, null, null, String.class);
            return respose;
        } catch (Exception e) {
            throw new IllegalArgumentException("[CreateGame]: " + e.getMessage());
        }
    }
     */

    private int CreateRound(int game_id, String ClasseUT, String Time) {
        final String endpoint = "/rounds";
        JSONObject obj = new JSONObject();
        obj.put("gameId", game_id);
        obj.put("testClassId", ClasseUT);
        obj.put("startedAt", Time);
        String respose = callRestPost(endpoint, obj, null, null, String.class);
        // Parsing della stringa JSON
        JSONObject jsonObject = new JSONObject(respose);
        // Estrazione del valore di id
        return jsonObject.getInt("id");
    }

    private String EndRound(String Time, int roundId) {
        //Anche qui non è stato previsto un parametro per la chiamata rest e quindi va costruito a mano
        final String endpoint = "rounds/" + String.valueOf(roundId);
        try {
            JSONObject formData = new JSONObject();
            formData.put("closedAt", Time);
            String response = callRestPut(endpoint, formData, null, null, String.class);
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("[EndRound]: " + e.getMessage());
        }
    }

    private String CreateTurn(String Player_id, int Round_id, String Time) {
        final String endpoint = "/turns";
        JSONObject obj = new JSONObject();
        JSONArray playersArray = new JSONArray();
        playersArray.put(Player_id);
        obj.put("players", playersArray);
        obj.put("roundId", Round_id);
        obj.put("startedAt", Time);
        String respose = callRestPost(endpoint, obj, null, null, String.class);
        return respose;
    }

    private String EndTurn(String user_score, String Time, String turnId) {
        // Anche qui non è stato previsto un parametro per la chiamata rest e quindi va
        // costruito a mano
        final String endpoint = "turns/" + turnId;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("TurnID", turnId);
        jsonObject.put("scores", user_score);
        jsonObject.put("closedAt", Time);
        String response = callRestPut(endpoint, jsonObject,null, null, String.class);
        return response;
    }

    // Questa chiamata non è documentata nel materiale di caterina
    private String CreateScalata(String player_id, String scalata_name, String creation_Time, String creation_date) {
        final String endpoint = "/turns";
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("playerID", player_id);
        formData.add("scalataName", scalata_name);
        formData.add("creationTime", creation_Time);
        formData.add("creationDate", creation_date);
        String respose = callRestPost(endpoint, formData, null, String.class);
        return respose;
    }

}
