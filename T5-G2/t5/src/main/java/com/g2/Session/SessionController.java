package com.g2.Session;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin
@RestController
@RequestMapping("/session")
public class SessionController {

    private final SessionService sessionService;
    private final ObjectMapper mapper;

    @Autowired
    public SessionController(SessionService sessionService, ObjectMapper mapper) {
        this.sessionService = sessionService;
        this.mapper = mapper;
    }

    /**
     * Endpoint per visualizzare tutte le sessioni attive.
     */
    @GetMapping("/all")
    public ResponseEntity<List<Sessione>> getAllSessions() {
        List<Sessione> sessions = sessionService.getAllSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Endpoint per ottenere la sessione relativa a un determinato player.
     * Il parametro "playerId" viene passato come query parameter.
     */
    @GetMapping("/get") // cambiare in player
    public ResponseEntity<Sessione> getSession(@RequestParam String playerId) {
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Sessione sessione = sessionService.getSession(sessionKey);
        return ResponseEntity.ok(sessione);
    }

    /**
     * Endpoint per aggiornare (o creare) la modalità per un game.
     * Il client invia nel body un JSON con la nuova mappa delle modalità da aggiornare;
     * l'ID della sessione non viene inviato, ma il backend lo recupera tramite l'ID utente.
     *
     * Esempio di JSON inviato:
     * {
     *   "Sfida": {
     *     "@class": "com.g2.Session.Sessione$ModalitaWrapper",
     *     "gameobject": { ... },  // qui va l'oggetto GameLogic (o i dati utili per inizializzarlo)
     *     "timestamp": "2025-02-26T11:50:06Z"
     *   }
     * }
     *
     * Il parametro query "playerId" serve a individuare la sessione.
     */
    @PutMapping("/updateModalita")
    public ResponseEntity<String> updateModalita(@RequestBody Map<String, Object> modalitaUpdate,
                                                 @RequestParam String playerId) {
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sessione non trovata per il player");
        }
        Sessione sessione = sessionService.getSession(sessionKey);
        // Convertiamo il payload in Map<String, Sessione.ModalitaWrapper>
        Map<String, Sessione.ModalitaWrapper> modalita = mapper.convertValue(
                modalitaUpdate,
                new TypeReference<Map<String, Sessione.ModalitaWrapper>>() {}
        );
        sessione.setModalita(modalita);
        boolean updated = sessionService.updateSession(sessionKey, sessione, SessionService.DEFAULT_SESSION_TTL);
        if (updated) {
            return ResponseEntity.ok("Modalita aggiornata con successo");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore nell'aggiornamento della modalita");
        }
    }

    /**
     * Endpoint per eliminare una modalità specifica dalla sessione.
     * Invece di cancellare l'intera sessione, viene rimosso soltanto il gameobject relativo alla modalità indicata.
     */
    @DeleteMapping("/deleteModalita")
    public ResponseEntity<String> deleteModalita(@RequestParam String playerId, @RequestParam String mode) {
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sessione non trovata per il player");
        }
        boolean removed = sessionService.removeGameMode(sessionKey, mode);
        if (removed) {
            return ResponseEntity.ok("Modalita '" + mode + "' eliminata con successo");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore nell'eliminazione della modalita '" + mode + "'");
        }
    }

    /**
     * (Eventuale) Endpoint per eliminare l'intera sessione.
     * Normalmente non si elimina la sessione, ma questo endpoint è a disposizione se necessario.
     */
    @PostMapping("/delete")
    public ResponseEntity<String> deleteSessionPost(@RequestParam String playerId) {
        String sessionKey = sessionService.getExistingSessionKeyForPlayer(playerId);
        if (sessionKey == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sessione non trovata per il player");
        }
        sessionService.deleteSession(sessionKey);
        return ResponseEntity.ok("Sessione eliminata con successo");
    }
}
