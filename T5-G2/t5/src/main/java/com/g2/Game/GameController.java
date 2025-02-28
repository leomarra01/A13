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
package com.g2.Game;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g2.Game.GameDTO.GameResponseDTO;
import com.g2.Game.GameDTO.StartGameRequestDTO;
import com.g2.Game.GameDTO.StartGameResponseDTO;
import com.g2.Game.GameModes.GameLogic;
import com.g2.Game.Service.Exceptions.GameAlreadyExistsException;
import com.g2.Game.Service.Exceptions.GameDontExistsException;
import com.g2.Game.Service.GameServiceManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


//Qui introduco tutte le chiamate REST per la logica di gioco/editor
@CrossOrigin
@RestController
public class GameController {

    /*
     * Interfaccia per gestire gli endpoint
     */
    private final GameServiceManager gameServiceManager;
    /*
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    @Autowired
    public GameController(GameServiceManager gameServiceManager) {
        this.gameServiceManager = gameServiceManager;
    }



    // Metodo helper per estrarre userId dal JWT
    private String getPlayerIdFromJwt(String jwt) {
        try {
            logger.info("[JWT] Decodifica del token JWT...");
            String[] chunks = jwt.split("\\.");
            if (chunks.length < 2) {
                logger.error("[JWT] Il token JWT non è valido!");
                return null;
            }
            String payload = new String(java.util.Base64.getDecoder().decode(chunks[1]));
            org.json.JSONObject jsonObject = new org.json.JSONObject(payload);
            return jsonObject.optString("userId", null);
        } catch (Exception e) {
            logger.error("[JWT] Errore nella decodifica del JWT", e);
            return null;
        }
    }

    /*    LATO CLIENT
     *    On load document - check game -> da usare in fetchPrevisusGame
     *    
     *     check game (nuovo /CheckGame)-> [sfida, allenamento]
     * 
     *     continua -> /editor
     *     nuova    -> eliminare il vecchio game (nuovo /RemoveGame) 
     *                 e poi chiamare /StartGame con nuovi parametri 
     */
    /*
     *  Chiamata che controllo se la partita quindi esisteva già o meno
     *  se non esiste instanzia un nuovo gioco 
     */
     /**
     * Endpoint per avviare una partita.
     * Il client invia nel body tutti i parametri necessari:
     * - playerId  
     * - mode  
     * - underTestClassName  
     * - type_robot  
     * - difficulty  
     *
     * Il backend crea un nuovo game object per quella modalità, lo inserisce nella sessione e restituisce un DTO con l'ID del game.
     */
    @PostMapping("/StartGame")
    public ResponseEntity<StartGameResponseDTO> startGame(
            @RequestBody(required = false) StartGameRequestDTO request,
            @CookieValue(name = "jwt", required = false) String jwt) {

        logger.info("[START_GAME] Richiesta ricevuta per avviare il gioco");

        if (jwt == null || jwt.isEmpty()) {
            logger.error("[START_GAME] Nessun JWT trovato nella richiesta.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new StartGameResponseDTO(-1, "JWT missing"));
        }

        String userId = getPlayerIdFromJwt(jwt);
        logger.info("[START_GAME] UserID estratto dal JWT: {}", userId);

        if (request == null) {
            logger.error("[START_GAME] Il body della richiesta è NULL!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StartGameResponseDTO(-1, "Request body is missing"));
        }

        // Log dei parametri inviati
        logger.info("[START_GAME] Dati ricevuti: playerId={}, typeRobot={}, difficulty={}, mode={}, underTestClassName={}",
                request.getPlayerId(), request.getTypeRobot(), request.getDifficulty(),
                request.getMode(), request.getUnderTestClassName());

        String mode = request.getMode();
        if (mode == null || mode.isEmpty()) {
            logger.error("[START_GAME] Modalità di gioco non specificata!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StartGameResponseDTO(-1, "Invalid request: mode is missing"));
        }

        // Validazioni specifiche per modalità
        if (mode.equalsIgnoreCase("Sfida")) {
            if (request.getPlayerId() == null || request.getPlayerId().isEmpty() ||
                request.getTypeRobot() == null || request.getTypeRobot().isEmpty() ||
                request.getDifficulty() == null || request.getDifficulty().isEmpty() ||
                request.getUnderTestClassName() == null || request.getUnderTestClassName().isEmpty()) {

                logger.error("[START_GAME] Modalità 'Sfida': Uno o più campi obbligatori sono mancanti!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new StartGameResponseDTO(-1, "Invalid request: missing fields in Sfida mode"));
            }
        } else if (mode.equalsIgnoreCase("Allenamento")) {
            if (request.getPlayerId() == null || request.getPlayerId().isEmpty() ||
                request.getUnderTestClassName() == null || request.getUnderTestClassName().isEmpty()) {

                logger.error("[START_GAME] Modalità 'Allenamento': Uno o più campi obbligatori sono mancanti!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new StartGameResponseDTO(-1, "Invalid request: missing fields in Allenamento mode"));
            }
            request.setTypeRobot("NONE");
            request.setDifficulty("EASY");
        }

        try {
            logger.info("[START_GAME] Creazione della partita per playerId={} in modalità={}", request.getPlayerId(), mode);
            GameLogic game = gameServiceManager.CreateGameLogic(
                    request.getPlayerId(),
                    mode,
                    request.getUnderTestClassName(),
                    request.getTypeRobot(),
                    request.getDifficulty());
            logger.info("[START_GAME] Partita creata con successo. GameID={}", game.getGameID());
            return ResponseEntity.ok(new StartGameResponseDTO(game.getGameID(), "created"));
        } catch (GameAlreadyExistsException e) {
            logger.error("[START_GAME] Errore: la partita esiste già per playerId={} e modalità={}", request.getPlayerId(), mode);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StartGameResponseDTO(-1, "GameAlreadyExistsException"));
        } catch (Exception e) {
            logger.error("[START_GAME] Errore generico durante la creazione della partita!", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StartGameResponseDTO(-1, "Internal server error"));
        }
    }


    /*
     *  Chiamata principale del game engine, l'utente ogni volta può comunicare la sua richiesta di
     *  calcolare la coverage/compilazione, il campo isGameEnd è da utilizzato per indicare se è anche un submit e
     *  quindi vuole terminare la partita ed ottenere i risultati del robot
     */
    @PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GameResponseDTO> Runner(
            @RequestParam(value = "testingClassCode", required = false, defaultValue = "") String testingClassCode,
            @RequestParam(value = "playerId") String playerId,
            @RequestParam("mode") String mode,
            @RequestParam("isGameEnd") Boolean isGameEnd) {
        try {
            GameResponseDTO response = gameServiceManager.PlayGame(playerId, mode, testingClassCode, isGameEnd);
            return ResponseEntity.ok().body(response);
        } catch (GameDontExistsException e) {
            /*
             * Il player non ha impostato una partita prima di arrivare all'editor
             */
            logger.error("[GAMECONTROLLER][StartGame] " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // Metodo per creare una risposta di errore
    /*
     * ERROR CODE che mando al client
     *  0 - modalità non esiste
     *  1 -  l'utente ha cambiato le impostazioni della partita
     *  2 -  esiste già la partita
     *  3 -  è avvenuta un eccezione
     *  4 -  non esiste la partita
     *  5 -  partita eliminata
     */
    @SuppressWarnings("unused")
    private ResponseEntity<String> createErrorResponse(String errorMessage, String errorCode) {
        JSONObject error = new JSONObject();
        error.put("error", errorMessage);
        error.put("errorCode", errorCode);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error.toString());
    }

}
