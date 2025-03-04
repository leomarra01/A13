/*
 *   Copyright (c) 2024 Stefano Marano https://github.com/StefanoMarano80017
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/* REST_Editor.js
   Gestione delle chiamate REST per l’editor.
   (Dipende da Util_Editor.js, che deve essere caricato prima.)
*/

// === FUNZIONE PER LA RICHIESTA AJAX DEL GIOCO ===
async function runGameAction(url, formData, isGameEnd) {
    try {
        formData.append("isGameEnd", isGameEnd);
        const response = await ajaxRequest(url, "POST", formData, false, "json");
        return response;
    } catch (error) {
        console.error("Errore nella richiesta AJAX:", error);
        throw error;
    }
}

$(document).ready(function () {
    const savedContent = localStorage.getItem('codeMirrorContent');
    if (!savedContent) {
        const currentDate = new Date();
        const formattedDate = `${String(currentDate.getDate()).padStart(2, "0")}/${String(currentDate.getMonth() + 1).padStart(2, "0")}/${currentDate.getFullYear()}`;
        const replacements = {
            TestClasse: `Test${localStorage.getItem("underTestClassName")}`,
            username: jwtData.sub,
            userID: jwtData.userId,
            date: formattedDate,
        };
        SetInitialEditor(replacements);
    } else {
        editor_utente.setValue(savedContent);
        editor_utente.refresh();
    }
    if (localStorage.getItem('storico')) {
        viewStorico();
    }
});

let isActionInProgress = false; // Flag per indicare se un'azione è attualmente in corso

// Funzione principale per gestire l'azione del gioco
async function handleGameAction(isGameEnd) {
    isActionInProgress = true; // Imposta il flag per bloccare altre azioni
    run_button.disabled = true; // Disabilita il pulsante di esecuzione
    coverage_button.disabled = true; // Disabilita il pulsante di coverage

    // Determina le chiavi per il caricamento e il pulsante in base a isGameEnd
    const loadingKey = isGameEnd ? "loading_run" : "loading_cov";
    const buttonKey = isGameEnd ? "runButton" : "coverageButton";

    // Mostra l'indicatore di caricamento
    toggleLoading(true, loadingKey, buttonKey);
    // Aggiorna lo stato a "sending"
    setStatus("sending"); 

    // Otteniamo il FormData (con debug sul codice dell'editor)
    const formData = await getFormData();
    console.log("[handleGameAction] Dati inviati:", Object.fromEntries(formData.entries()));
    try {
        //Esegue l'azione di gioco 
        const response = await runGameAction("/run", formData, isGameEnd);
        setStatus("compiling");
        handleResponse(response, formData, isGameEnd, loadingKey, buttonKey);
    } catch (error) {
        console.error("[handleGameAction] Errore durante l'esecuzione:", error);
    }
    isActionInProgress = false;
}

function handleResponse(response, formData, isGameEnd, loadingKey, buttonKey) {
    const { robotScore, userScore, outCompile, 
            coverage, gameId, roundId,
            coverageDetails, isWinner} = response;    
    // Aggiorna i dati del modulo con gameId e roundId
    formData.append("gameId", gameId);
    formData.append("roundId", roundId);
    // Mostra l'output della compilazione nella console utente
    console_utente.setValue(outCompile);
    // Analizza l'output di Maven
    parseMavenOutput(outCompile);
    // Se non c'è copertura, gestisce l'errore di compilazione
    if (!coverage) {
        setStatus("error");
        //Gestione degli errori 
        handleCompileError(loadingKey, buttonKey);
        return;
    }
    // Se la copertura è disponibile, la processa
    processCoverage(coverage, formData, robotScore, userScore, isGameEnd, loadingKey, buttonKey, coverageDetails, isWinner);
}

// Processa la copertura del codice e aggiorna i dati di gioco
async function processCoverage(coverage, formData, robotScore, userScore, isGameEnd, loadingKey, buttonKey, coverageDetails, isWinner) {
    highlightCodeCoverage($.parseXML(coverage), editor_robot);
    orderTurno++; // Variabile globale per il turno
    // Per il report di coverage inviamo l'intero FormData
    const csvContent = await ajaxRequest(createApiUrl(formData, orderTurno), "POST", formData, false, "text");
    setStatus("loading");
    const valori_csv = extractThirdColumn(csvContent);
    updateStorico(orderTurno, userScore, valori_csv[0]);
    setStatus(isGameEnd ? "game_end" : "turn_end");
    toggleLoading(false, loadingKey, buttonKey);
    displayUserPoints(isGameEnd, valori_csv, robotScore, userScore, coverageDetails, isWinner);
    if (isGameEnd) {
        handleEndGame(userScore);
    } else {
        resetButtons();
    }
}

// Mostra i punti dell'utente nella console
function displayUserPoints(isGameEnd, valori_csv, robotScore, userScore, coverageDetails, isWinner) {
    const displayUserPoints = isGameEnd 
        ? getConsoleTextRun(valori_csv, coverageDetails, robotScore, userScore, isWinner) // Testo per la fine del gioco
        : getConsoleTextCoverage(valori_csv, userScore, coverageDetails); // Testo per la copertura

    console_robot.setValue(displayUserPoints); // Aggiorna la console del robot con i punti
}

// Gestisce gli errori di compilazione
function handleCompileError(loadingKey, buttonKey) {
    console_robot.setValue(getConsoleTextError()); // Mostra l'errore nella console del robot
    toggleLoading(false, loadingKey, buttonKey); // Nasconde l'indicatore di caricamento
    resetButtons(); // Reimposta i pulsanti
}


// Recupera il report di coverage da T8
async function fetchCoverageReport(formData) {
    const url = createApiUrl(formData, orderTurno); // Crea l'URL dell'API
    return await ajaxRequest(url, "POST", formData.get("testingClassCode"), false, "text"); // Esegue la richiesta AJAX
}

// Gestisce la fine del gioco, mostra un messaggio e pulisce i dati
function handleEndGame(userScore) {
    openModalWithText(
        status_game_end,
        `${score_partita_text} ${userScore} pt.`, // Mostra il punteggio dell'utente
        [{ text: vai_home, href: '/main', class: 'btn btn-primary' }] // Pulsante per tornare alla home
    );
}
// Reimposta i pulsanti per consentire nuove azioni
function resetButtons() {
    run_button.disabled = (localStorage.getItem("modalita") === "Allenamento"); // Abilita/disabilita in base alla modalità
    coverage_button.disabled = false; // Abilita il pulsante di coverage
}

/*
*   Se premo il tasto go back quando è in atto un caricamento 
*/
window.addEventListener('beforeunload', (event) => {
    if (isActionInProgress) {
        // Ottieni il link di destinazione. Puoi usare `event.target` per prendere il link dell'evento.
        // Se l'utente sta cercando di navigare tramite un link, usa `document.activeElement.href` se è un link.
        let targetUrl = '';
        // Verifica se l'evento proviene da un link cliccato
        if (document.activeElement && document.activeElement.tagName === 'A') {
            targetUrl = document.activeElement.href;
        }
        // Previeni il comportamento predefinito del browser
        event.preventDefault();
        // Il messaggio predefinito non può essere personalizzato, ma il modal può apparire
        return ''; // Restituisce una stringa vuota per attivare il messaggio predefinito
    }
});

// Pulsante "Run/Submit"
document.getElementById("runButton").addEventListener("click", () => handleGameAction(true));
// Pulsante "Coverage"
document.getElementById("coverageButton").addEventListener("click", () => handleGameAction(false));