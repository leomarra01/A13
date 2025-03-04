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

import com.g2.Game.GameDTO.GameResponseDTO;
import com.g2.Game.GameDTO.StartGameRequestDTO;
import com.g2.Game.GameDTO.StartGameResponseDTO;
import com.g2.Game.GameModes.GameLogic;
import com.g2.Game.Service.Exceptions.GameAlreadyExistsException;
import com.g2.Game.Service.Exceptions.GameDontExistsException;
import com.g2.Game.Service.GameServiceManager;

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

    /*
     *  Chiamata che controllo se la partita quindi esisteva già o meno
     *  se non esiste instanzia un nuovo gioco 
     */
    @PostMapping("/StartGame")
    public ResponseEntity<StartGameResponseDTO> startGame(
            @RequestBody(required = false) StartGameRequestDTO request,
            @CookieValue(name = "jwt", required = false) String jwt) {

        logger.info("[START_GAME] Richiesta ricevuta per avviare il gioco");

        if (jwt == null || jwt.isEmpty()) {
            logger.error("[START_GAME] Nessun JWT trovato nella richiesta.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new StartGameResponseDTO(-1, "JWT missing"));
        }

        if (request == null) {
            logger.error("[START_GAME] Il body della richiesta è NULL!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new StartGameResponseDTO(-1, "Request body is missing"));
        }

        String userId = request.getPlayerId();
        logger.info("[START_GAME] UserID estratto dal JWT: {}", userId);

        // Log dei parametri inviati
        logger.info("[START_GAME] Dati ricevuti: playerId={}, typeRobot={}, difficulty={}, mode={}, underTestClassName={}",
                request.getPlayerId(), request.getTypeRobot(), request.getDifficulty(),
                request.getMode(), request.getUnderTestClassName());

        try {
            GameLogic game = gameServiceManager.CreateGameLogic(
                    request.getPlayerId(),
                    request.getMode(),
                    request.getUnderTestClassName(),
                    request.getTypeRobot(),
                    request.getDifficulty());
            logger.info("[START_GAME] Partita creata con successo. GameID={}", game.getGameID());
            return ResponseEntity.ok(
                    new StartGameResponseDTO(game.getGameID(),
                            "created")
            );
        } catch (GameAlreadyExistsException e) {
            logger.error("[START_GAME] Errore: la partita esiste già per playerId={} e modalità={}", request.getPlayerId(), request.getMode());
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
            @RequestParam("isGameEnd") boolean isGameEnd) {
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
