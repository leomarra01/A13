package com.g2.Game.Service;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.g2.Game.GameDTO.GameResponseDTO;
import com.g2.Game.GameFactory.GameRegistry;
import com.g2.Game.GameModes.Compile.CompileResult;
import com.g2.Game.GameModes.GameLogic;
import com.g2.Game.Service.Exceptions.GameAlreadyExistsException;
import com.g2.Game.Service.Exceptions.GameDontExistsException;
import com.g2.Interfaces.ServiceManager;
import com.g2.Model.AchievementProgress;
import com.g2.Model.User;
import com.g2.Service.AchievementService;
import com.g2.Session.SessionService;
import com.g2.Session.Sessione;

@Service
public class GameService {

    private final ServiceManager serviceManager;
    private final GameRegistry gameRegistry;
    private final AchievementService achievementService;
    private final SessionService sessionService;
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    @Autowired
    public GameService(ServiceManager serviceManager,
            GameRegistry gameRegistry,
            AchievementService achievementService,
            SessionService sessionService) {
        this.serviceManager = serviceManager;
        this.gameRegistry = gameRegistry;
        this.achievementService = achievementService;
        this.sessionService = sessionService;
    }

    /*
    *  Sfrutto T4 per avere i risultati dei robot
     */
    private int GetRobotScore(String testClass, String robot_type, String difficulty) {
        logger.info("getRobotScore: Richiesta punteggio robot per testClass={}, robotType={}, difficulty={}.", testClass, robot_type, difficulty);
        try {
            String response_T4 = serviceManager.handleRequest("T4", "GetRisultati", String.class,
                    testClass, robot_type, difficulty);

            JSONObject jsonObject = new JSONObject(response_T4);
            return jsonObject.getInt("scores");
        } catch (JSONException e) {
            logger.error("[GAMECONTROLLER] GetRobotScore: Errore nel parsing della risposta JSON", e);
            throw new RuntimeException("Errore durante l'elaborazione della risposta del robot", e);
        } catch (Exception e) {
            logger.error("[GAMECONTROLLER] GetRobotScore: Errore generico nella richiesta a T4", e);
            throw new RuntimeException("Errore durante il recupero del punteggio del robot", e);
        }
    }

    // creare modello updateGame
    public GameLogic CreateGame(String playerId, String mode, String underTestClassName, String type_robot, String difficulty) throws GameAlreadyExistsException {
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            throw new RuntimeException("Sessione non esistente per il giocatore con ID: " + playerId);
        }

        if (sessionService.checkGame(sessionKey, mode)) {
            throw new GameAlreadyExistsException("Esiste già una partita per il giocatore con ID: " + playerId);
        }
        // Crea il game object usando i parametri ricevuti
        /*
        * gameRegistry istanzia dinamicamente uno degli oggetti gameLogic (sfida, allenamento, scalata e ecc)
        * basta passargli il campo mode e dinamicamente se ne occupa lui  
         */
        GameLogic gameLogic = gameRegistry.createGame(mode, serviceManager, playerId, underTestClassName, type_robot, difficulty);
        gameLogic.CreateGame();
        sessionService.addGameMode(sessionKey, gameLogic);
        logger.info("createGame: Inizio creazione partita per playerId={}, mode={}.", playerId, mode);
        return gameLogic;
    }

    public GameLogic GetGame(String mode, String playerId) throws GameDontExistsException {
        logger.info("getGame: Recupero partita per playerId={}, mode={}.", playerId, mode);
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            logger.error("getGame: Nessuna sessione trovata per playerId={}.", playerId);
            throw new GameDontExistsException("Non esiste una sessione per il giocatore con ID: " + playerId);
        }
        Sessione sessione = sessionService.getSession(sessionKey);
        if (sessione.getModalita() == null || !sessione.getModalita().containsKey(mode)) {
            logger.error("getGame: Nessun game trovato per playerId={} e modalità={}.", playerId, mode);
            throw new GameDontExistsException("Non esiste un game per il giocatore con ID: " + playerId + " con modalità: " + mode);
        }
        GameLogic gameLogic = sessione.getModalita().get(mode).getGameobject();
        gameLogic.setServiceManager(serviceManager);
        logger.info("getGame: Partita recuperata con successo per playerId={} e modalità={}.", playerId, mode);
        return gameLogic;
    }

    public Boolean destroyGame(String playerId, String mode) {
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            logger.error("destroyGame: Nessuna sessione trovata per playerId={}.", playerId);
            return false;
        }
        boolean removed = sessionService.removeGameMode(sessionKey, mode);
        if (removed) {
            logger.info("destroyGame: Distruzione partita per playerId={} e modalità={}.", playerId, mode);
        } else {
            logger.error("destroyGame: Impossibile distruggere il game per playerId={} e modalità={}.", playerId, mode);
        }
        return removed;
    }

    public CompileResult handleCompile(String Classname, String testingClassCode) {
        logger.info("handleCompile: Inizio compilazione per className={}.", Classname);
        return new CompileResult(Classname, testingClassCode, this.serviceManager);
    }

    public GameResponseDTO handleGameLogic(CompileResult compileResult, GameLogic currentGame, Boolean isGameEnd) {
        logger.info("handleGameLogic: Avvio logica di gioco per playerId={}.", currentGame.getPlayerID());
        /*
         *  Lo score è definito dalle performance del file XML del test 
         */
        int userscore = currentGame.GetScore(compileResult);
        int robotScore = GetRobotScore(currentGame.getClasseUT(), currentGame.getTypeRobot(), currentGame.getDifficulty());
        /*
         *  Avanzo nel gioco 
         */
        currentGame.NextTurn(userscore, robotScore);
        Boolean gameFinished = isGameEnd || currentGame.isGameEnd();
        logger.info("handleGameLogic: Stato partita (gameFinished={}) per playerId={}.", gameFinished, currentGame.getPlayerID());
        if (gameFinished) {
            logger.info("handleGameLogic: Partita terminata per playerId={}. Avvio aggiornamento progressi e notifiche.", currentGame.getPlayerID());
            updateProgressAndNotifications(currentGame.getPlayerID());
            EndGame(currentGame, userscore);
        }
        logger.info("handleGameLogic: Risposta creata per playerId={}.", currentGame.getPlayerID());
        // metodo "UpdateGame"
        return createResponseRun(compileResult, gameFinished, robotScore, userscore, currentGame.isWinner());
    }

    public void EndGame(GameLogic currentGame, int userscore) {
        logger.info("endGame: Terminazione partita per playerId={}.", currentGame.getPlayerID());
        /*
        *   L'utente ha deciso di terminare la partita o 
        *    la modalità di gioco ha determianto il termine
        *   Salvo la partita 
        *   Distruggo la partita salvata in sessione  
        */
        currentGame.EndRound();
        currentGame.EndGame(userscore);
        destroyGame(currentGame.getPlayerID(), currentGame.getMode());
    }

    /*
    * Wrapper che crea il DTO 
    */
    public GameResponseDTO createResponseRun(CompileResult compileResult,
            Boolean gameFinished,
            int robotScore,
            int UserScore,
            Boolean isWinner) {
        logger.info("createResponseRun: Creazione risposta per la partita (gameFinished={}, userScore={}, robotScore={}).", gameFinished, UserScore, robotScore);
        return new GameResponseDTO(compileResult, gameFinished, robotScore, UserScore, isWinner);
    }

    //Gestione Trofei e notifiche
    private void updateProgressAndNotifications(String playerId) {
        User user = serviceManager.handleRequest("T23", "GetUser", User.class, playerId);
        String email = user.getEmail();
        List<AchievementProgress> newAchievements = achievementService.updateProgressByPlayer(user.getId().intValue());
        achievementService.updateNotificationsForAchievements(email, newAchievements);
    }
}
