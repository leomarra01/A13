package com.g2.Session;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.g2.Game.GameModes.GameLogic;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class Sessione implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id_sess")
    private String idSessione;

    @JsonProperty("id_user")
    private String userId;

    // Timestamp di creazione della sessione (formato ISO-8601)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;

    // Mappa che contiene, per ciascuna modalità, un wrapper che racchiude il game object
    @JsonProperty("modalita")
    private Map<String, ModalitaWrapper> modalita;


    // Costruttore no-arg necessario per la deserializzazione JSON
    public Sessione() {
        this.modalita = new HashMap<>();
    }

    /**
     * Costruttore con parametri.
     *
     * @param idSessione la chiave della sessione
     * @param userId     l'identificativo dell'utente
     * @param timestamp  il momento di creazione della sessione
     */
    public Sessione(String idSessione, String userId, Instant timestamp) {
        this.idSessione = idSessione;
        this.userId = userId;
        this.timestamp = timestamp;
        this.modalita = new HashMap<>();
    }

    /**
     * Aggiunge (o aggiorna) un game object per una modalità specifica.
     * Se la modalità è già presente, il nuovo game object sostituirà quello esistente.
     *
     * @param key  il nome della modalità (ad esempio "Sfida", "Allenamento")
     * @param game l'oggetto GameLogic da associare
     */
    public void addModalita(String key, GameLogic game) {
        String modeTimestamp = Instant.now().toString();
        this.modalita.put(key, new ModalitaWrapper(game, modeTimestamp));
    }

    // Getters e Setters

    public String getIdSessione() {
        return idSessione;
    }

    public void setIdSessione(String idSessione) {
        this.idSessione = idSessione;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, ModalitaWrapper> getModalita() {
        return modalita;
    }

    public void setModalita(Map<String, ModalitaWrapper> modalita) {
        this.modalita = modalita;
    }

    @Override
    public String toString() {
        return "Sessione{" +
                "idSessione='" + idSessione + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", modalita=" + modalita +
                '}';
    }

    /**
     * Wrapper interno per mappare il game object all'interno della mappa "modalita" in JSON.
     * Include anche un timestamp specifico per quando la modalità è stata aggiunta.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static class ModalitaWrapper implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("gameobject")
        private GameLogic gameobject;

        @JsonProperty("timestamp")
        private String timestamp;

        public ModalitaWrapper() {
        }

        public ModalitaWrapper(GameLogic game, String timestamp) {
            this.gameobject = game;
            this.timestamp = timestamp;
        }

        public GameLogic getGameobject() {
            return gameobject;
        }

        public void setGameobject(GameLogic gameobject) {
            this.gameobject = gameobject;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "ModalitaWrapper{" +
                    "gameobject=" + gameobject +
                    ", timestamp='" + timestamp + '\'' +
                    '}';
        }
    }
}
