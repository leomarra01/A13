package com.g2.Game.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.g2.Game.GameDTO.GameResponseDTO;
import com.g2.Game.GameModes.Compile.CompileResult;
import com.g2.Game.GameModes.GameLogic;
import com.g2.Game.Service.Exceptions.GameAlreadyExistsException;
import com.g2.Game.Service.Exceptions.GameDontExistsException;

@Service
public class GameServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceManager.class);
    private final GameService gameService;

    @Autowired
    public GameServiceManager(GameService gameService) {
        this.gameService = gameService;
    }

    public GameLogic CreateGameLogic(String playerId,
            String mode,
            String underTestClassName,
            String type_robot,
            String difficulty) throws GameAlreadyExistsException {
        return gameService.CreateGame(playerId, mode, underTestClassName, type_robot, difficulty);
    }

    protected GameLogic GetGameLogic(String playerId, String mode) throws GameDontExistsException {
        return gameService.GetGame(mode, playerId);
    }

    protected CompileResult compileGame(GameLogic game, String testingClassCode) {
        return gameService.handleCompile(game.getClasseUT(), testingClassCode);
    }

    public GameResponseDTO PlayGame(String playerId, String mode, String testingClassCode, Boolean isGameEnd) throws GameDontExistsException {
        logger.info("[PlayGame] Inizio esecuzione per playerId={} e mode={}", playerId, mode);
        /*
         * Recupero la sessioen di gioco 
         */
        GameLogic currentGame = GetGameLogic(playerId, mode);
        logger.info("[PlayGame] GameLogic recuperato: gameID={}", currentGame.getGameID());
        /*
         * Compilo il test dell'utente  
         */
        CompileResult compile = compileGame(currentGame, testingClassCode);
        if (compile == null) {
            logger.error("[PlayGame] Il risultato della compilazione è null.");
            throw new RuntimeException("compile is null");
        }
        logger.info("[PlayGame] Esito compilazione: success={}", compile.getSuccess());
        /*
        *   getSuccess() mi dà l'esito della compilazione => se l'utente ha scritto un test senza errori 
        */
        if (compile.getSuccess()) {
            /*
            *  vado avanti col gioco 
            *  restituisce l'oggetto json che rispecchia lo stato del game
            *  l'utente può imporre la fine del gioco con isGameEnd
             */
            logger.info("[PlayGame] Coverage disponibile; procedo con handleGameLogic.");
            return gameService.handleGameLogic(compile, currentGame, isGameEnd);
        } else {
            /*
             * Restituisco un Json solo con info parziali 
             */
            logger.warn("[PlayGame] Compilazione non andata a buon fine; restituisco risposta parziale.");
            return gameService.createResponseRun(compile, false, 0, 0, false);
        }
    }
}
