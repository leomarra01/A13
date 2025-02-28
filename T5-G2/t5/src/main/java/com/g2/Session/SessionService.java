package com.g2.Session;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.g2.Game.GameModes.GameLogic;


@Service
public class SessionService {
    private static final String KEY_PREFIX = "session:";

    // TTL di default per tutte le operazioni relative alla sessione (in secondi)
    public static final long DEFAULT_SESSION_TTL = 10800L; // 3 ore

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final RedisTemplate<String, Sessione> redisTemplate;

    @Autowired
    public SessionService(RedisTemplate<String, Sessione> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Interfaccia funzionale per incapsulare una chiamata sulle operazioni di sessione.
     */
    @FunctionalInterface
    public static interface SessionCall<R> {
        R execute() throws Exception;
    }
    
    /**
     * Eccezione per errori relativi alle operazioni sulla sessione.
     */
    public static class SessionException extends RuntimeException {
        public SessionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Metodo centralizzato per eseguire operazioni sulla sessione.
     * Rileva specificamente le eccezioni relative al RedisTemplate e le rilancia come SessionException.
     *
     * @param caller  Identificativo della chiamata (per il logging)
     * @param call    La logica da eseguire, incapsulata in un SessionCall
     * @return Il risultato dell'operazione
     * @throws SessionException in caso di errori legati al RedisTemplate o altri errori di sessione
     */
    private <R> R executeSessionCall(String caller, SessionCall<R> call) {
        try {
            return call.execute();
        } catch (DataAccessException e) {
            logger.error("RedisTemplate error in {}: {}", caller, e.getMessage(), e);
            throw new SessionException("RedisTemplate error in " + caller + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument in {}: {}", caller, e.getMessage(), e);
            throw new SessionException("Invalid argument in " + caller + ": " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error in {}: {}", caller, e.getMessage(), e);
            throw new SessionException("Error in " + caller + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Costruisce la chiave della sessione usando playerId e un timestamp.
     * Il formato della chiave sarà: "session:playerId:timestamp".
     */
    private String buildCompositeKey(String playerId, String timestamp) {
        return KEY_PREFIX + playerId + ":" + timestamp;
    }
    
    /**
     * Crea una sessione per il giocatore specificato.
     * Se ttlSeconds > 0 lo utilizza, altrimenti usa DEFAULT_SESSION_TTL.
     */
    public String createSession(String playerId, long ttlSeconds) {
        return executeSessionCall("createSession", () -> {
            Instant now = Instant.now();
            String sessionKey = buildCompositeKey(playerId, now.toString());
            Sessione sessione = new Sessione(sessionKey, playerId, now);
            long ttl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_SESSION_TTL;
            redisTemplate.opsForValue().set(sessionKey, sessione, ttl, TimeUnit.SECONDS);
            return redisTemplate.opsForValue().get(sessionKey) != null ? sessionKey : null;
        });
    }
    
    /**
     * Aggiunge un oggetto GameLogic per una modalità alla sessione identificata da sessionKey.
     */
    public boolean addGameMode(String sessionKey, GameLogic gameLogic) {
        return executeSessionCall("addGameMode", () -> {
            Sessione sessione = redisTemplate.opsForValue().get(sessionKey);
            if (sessione == null) {
                logger.error("addGameMode - La sessione non esiste per sessionKey: {}", sessionKey);
                return false;
            }
            if (gameLogic == null || gameLogic.getMode() == null) {
                logger.error("addGameMode - GameLogic o modalità non valida");
                return false;
            }
            // Forza la modalità "Sfida" se il valore è "sfida" (indipendentemente dal case)
            String modeKey = gameLogic.getMode().equalsIgnoreCase("sfida") ? "Sfida" : gameLogic.getMode();
            sessione.addModalita(modeKey, gameLogic);
            
            Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
            if (ttl == null || ttl <= 0) {
                ttl = DEFAULT_SESSION_TTL;
            }
            redisTemplate.opsForValue().set(sessionKey, sessione, ttl, TimeUnit.SECONDS);
            
            Sessione updatedSession = redisTemplate.opsForValue().get(sessionKey);
            if (updatedSession != null && updatedSession.getModalita() != null) {
                return updatedSession.getModalita().containsKey(modeKey);
            } else {
                logger.error("addGameMode - La sessione aggiornata è nulla o priva di modalità.");
                return false;
            }
        });
    }
    
    
    /**
     * Recupera la sessione identificata dalla sessionKey.
     */
    public Sessione getSession(String sessionKey) {
        return executeSessionCall("getSession", () -> redisTemplate.opsForValue().get(sessionKey));
    }
    
    /**
     * Elimina la sessione identificata dalla sessionKey.
     */
    public void deleteSession(String sessionKey) {
        executeSessionCall("deleteSession", () -> {
            redisTemplate.delete(sessionKey);
            return null;
        });
    } 
    /**
     * Aggiorna la sessione identificata da sessionKey con i dati di updatedSession e imposta il TTL.
     */
    public boolean updateSession(String sessionKey, Sessione updatedSession, long ttlSeconds) {
        return executeSessionCall("updateSession", () -> {
            if (updatedSession == null) {
                throw new IllegalArgumentException("La sessione aggiornata non può essere null");
            }
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey))) {
                logger.error("updateSession - La sessione con id {} non esiste.", sessionKey);
                return false;
            }
            long ttl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_SESSION_TTL;
            redisTemplate.opsForValue().set(sessionKey, updatedSession, ttl, TimeUnit.SECONDS);
            return redisTemplate.opsForValue().get(sessionKey) != null;
        });
    }
    
    /**
     * Recupera tutte le sessioni presenti su Redis.
     */
    public List<Sessione> getAllSessions() {
        return executeSessionCall("getAllSessions", () -> {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            return (keys == null || keys.isEmpty()) ? Collections.emptyList() : redisTemplate.opsForValue().multiGet(keys);
        });
    }
    
    /**
     * Recupera il TTL (in secondi) della sessione identificata da sessionKey.
     */
    public Long getSessionTTL(String sessionKey) {
        return executeSessionCall("getSessionTTL", () -> redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS));
    }
    
    /**
     * Cerca una sessione esistente per il player.
     */
    public String getExistingSessionKeyForPlayer(String playerId) {
        return executeSessionCall("getExistingSessionKeyForPlayer", () -> {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + playerId + ":*");
            return (keys != null && !keys.isEmpty()) ? keys.iterator().next() : null;
        });
    }
    
    /**
     * Verifica se nella sessione esistente è presente un game object per la modalità specificata.
     */
    public boolean checkGame(String sessionKey, String mode) {
        return executeSessionCall("checkGame", () -> {
            Sessione sessione = getSession(sessionKey);
            return sessione != null && sessione.getModalita().containsKey(mode);
        });
    }
    
    /**
     * Rimuove il game object per la modalità specificata dalla sessione, senza eliminare l'intera sessione.
     */
    public boolean removeGameMode(String sessionKey, String mode) {
        return executeSessionCall("removeGameMode", () -> {
            Sessione sessione = getSession(sessionKey);
            if (sessione == null) {
                logger.debug("removeGameMode - Sessione non trovata per sessionKey: {}", sessionKey);
                return false;
            }
            logger.debug("removeGameMode - Prima rimozione, sessione.modalita = {}", sessione.getModalita());
            if (sessione.getModalita().containsKey(mode)) {
                sessione.getModalita().remove(mode);
                logger.debug("removeGameMode - Modalità '{}' rimossa.", mode);
            } else {
                logger.debug("removeGameMode - Modalità '{}' non presente nella sessione.", mode);
            }
            logger.debug("removeGameMode - Dopo rimozione, sessione.modalita = {}", sessione.getModalita());
            Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
            if (ttl == null || ttl <= 0) {
                ttl = DEFAULT_SESSION_TTL;
            }
            redisTemplate.opsForValue().set(sessionKey, sessione, ttl, TimeUnit.SECONDS);
            Sessione updatedSession = getSession(sessionKey);
            logger.debug("removeGameMode - Sessione aggiornata: {}", updatedSession);
            return true;
        });
    }
    
    /**
     * Metodo centralizzato per aggiornare (o creare) il game object per una modalità.
     * Se esiste già e forceNew è true, rimuove il game object e ne crea uno nuovo.
     * Se non esiste, crea e aggiunge il nuovo game object.
     * Ritorna true se l'operazione va a buon fine, false se non si forza un nuovo gioco.
     */
    public boolean updateGameMode(String sessionKey, String mode, Supplier<GameLogic> gameLogicSupplier, boolean forceNew) throws Exception {
        return executeSessionCall("updateGameMode", () -> {
            Sessione sessione = getSession(sessionKey);
            if (sessione == null) {
                throw new SessionException("Sessione non trovata per sessionKey: " + sessionKey, null);
            }
            if (checkGame(sessionKey, mode)) {
                if (forceNew) {
                    logger.info("updateGameMode: forceNew=true, rimuovo il game object per la modalità {}", mode);
                    boolean removed = removeGameMode(sessionKey, mode);
                    if (!removed) {
                        throw new SessionException("Impossibile rimuovere il game object per la modalità " + mode, null);
                    }
                } else {
                    logger.info("updateGameMode: Game già esistente per la modalità {} e forceNew=false", mode);
                    return false;
                }
            }
            GameLogic gameLogic = gameLogicSupplier.get();
            // Inizializza il game object usando initGame (deve essere public)
            gameLogic.CreateGame();
            boolean added = addGameMode(sessionKey, gameLogic);
            if (added) {
                logger.info("updateGameMode: Nuovo game object aggiunto per la modalità {}", mode);
            }
            return added;
        });
    }
}
