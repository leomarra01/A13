/*
 *   Copyright (c) 2025 Stefano Marano https://github.com/StefanoMarano80017
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
package com.g2.Components;

import java.util.Collections;
import java.util.Map;

import com.g2.Interfaces.ServiceManager;
import com.g2.Model.User;

/**
 * Componente che recupera i dati del profilo utente dal servizio e li inserisce
 * nel modello della pagina.
 */
public class UserProfileComponent extends GenericObjectComponent {

    private final ServiceManager serviceManager;
    private final boolean IsFriendProfile;
    private final String userID;
    private final String FriendID;

    /**
     * Costruttore per il componente.
     *
     * @param serviceManager il ServiceManager per gestire la richiesta.
     * @param modelKey la chiave con cui inserire i dati nel modello.
     * @param userId l'ID dell'utente di cui recuperare il profilo.
     * @param serviceName il nome del servizio da cui recuperare i dati del
     * profilo.
     * @param action l'azione da eseguire per ottenere il profilo.
     */
    public UserProfileComponent(ServiceManager serviceManager,
            Boolean IsFriendProfile,
            String userID
    ) {
        super(null, null);  // Il costruttore della superclasse è invocato senza parametri
        this.serviceManager = serviceManager;
        this.IsFriendProfile = IsFriendProfile;
        this.userID = userID;
        this.FriendID = null;
    }

    public UserProfileComponent(ServiceManager serviceManager,
            Boolean IsFriendProfile,
            String userID,
            String FriendID
    ) {
        super(null, null);  // Il costruttore della superclasse è invocato senza parametri
        this.serviceManager = serviceManager;
        this.IsFriendProfile = IsFriendProfile;
        this.userID = userID;
        this.FriendID = FriendID;
    }

    /**
     * Esegue la logica per recuperare il profilo utente dal servizio e
     * inserirlo nel modello.
     *
     * @return una mappa con i dati del profilo utente.
     */
    @Override
    public Map<String, Object> getModel() {
        try {
            // Inserisce i dati del profilo utente nel modello con la chiave specificata
            User user = serviceManager.handleRequest("T23", "GetUser", User.class, this.userID);
            if (this.IsFriendProfile) {
                User FriendUser = (User) serviceManager.handleRequest("T23", "GetUser", this.FriendID);
                this.Model.put("user", FriendUser);
                this.Model.put("viewID", user.getUserProfile().getId());
            } else {
                // profilo privato dell'utente 
                this.Model.put("user", user);
                this.Model.put("viewID", null);
            }
            this.Model.put("isFriendProfile", IsFriendProfile);
            return this.Model;
        } catch (Exception e) {
            // Gestione delle eccezioni, ad esempio log dell'errore
            System.err.println("[UserProfileComponent]Errore durante il recupero del profilo utente: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
