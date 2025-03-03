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

/* UTIL_Editor.js
   Funzioni di utilità per l'editor.
*/

// Inserire qui eventuale flush del localStorage

// === FUNZIONI PER COSTRUIRE L'URL E IL REPORT ===
function createApiUrl(formData, orderTurno) {
    const className = formData.get("underTestClassName") || formData.get("className");
    const playerId = formData.get("playerId");
    const gameId = formData.get("gameId") || "defaultGameId";
    const roundId = formData.get("roundId") || "defaultRoundId";

    const classePath = `VolumeT8/FolderTreeEvo/${className}/${className}SourceCode/${className}`;
    const testPath = generaPercorsoTest(orderTurno, formData);
    const apiUrl = `/api/${classePath}+${testPath}+/app+${playerId}`;
    console.log("[createApiUrl] URL generato:", apiUrl);
    return apiUrl;
}

function generaPercorsoTest(orderTurno, formData) {
    let modalita = localStorage.getItem("modalita");
    const playerId = formData.get("playerId");
    const gameId = formData.get("gameId");
    const roundId = formData.get("roundId");
    const className = formData.get("underTestClassName") || formData.get("className");

    // Se modalità Allenamento, forziamo "Sfida", altrimenti usiamo GetMode()
    modalita = (modalita === "Allenamento") ? "Sfida" : GetMode();

    if (modalita === "Scalata" || modalita === "Sfida") {
        const scalataPart = (modalita === "Scalata")
            ? `/${localStorage.getItem("SelectedScalata")}${localStorage.getItem("scalataId")}`
            : "";
        return `/VolumeT8/FolderTreeEvo/StudentLogin/Player${playerId}/${modalita}${scalataPart}/${className}/Game${gameId}/Round${roundId}/Turn${orderTurno}/TestReport`;
    } else {
        console.error("Errore: modalità non trovata");
        window.location.href = "/main";
        return null;
    }
}

// === FUNZIONI PER ESTRARE IL REPORT CSV ===
function extractThirdColumn(csvContent) {
    const rows = csvContent.split("\n");
    const thirdColumnValues = [];
    rows.slice(1).forEach(row => {
        const cells = row.split(",");
        if (cells.length >= 3) {
            thirdColumnValues.push(cells[2]);
        }
    });
    return thirdColumnValues;
}

// === FUNZIONI PER GENERARE IL TESTO DI OUTPUT ===
const you_win = `
__     ______  _    _  __          _______ _   _ 
\\ \\   / / __ \\| |  | | \\ \\        / /_   _| \\ | |
 \\ \\_/ / |  | | |  | |  \\ \\  /\\  / /  | | |  \\| |
  \\   /| |  | | |  | |   \\ \\/  \\/ /   _| |_| |\\  |
   |_|  \\____/ \\____/      \\/  \\/   |_____|_| \\_|
`;

const you_lose = `
__     ______  _    _   _      ____   _____ ______ 
\\ \\   / / __ \\| |  | | | |    / __ \\ / ____|  ____|
 \\ \\_/ / |  | | |  | | | |   | |  | | (___ | |__   
  \\   /| |  | | |  | | | |   | |  | |\\___ \\|  __|  
   | | | |__| | |__| | | |___| |__| |____) | |____ 
   |_|  \\____/ \\____/  |______\\____/|_____/|______|
`;

const errorText = `
______ _____  _____   ____   _____  
|  ____|  __ \\|  __ \\ / __ \\ / ____| 
| |__  | |__) | |__) | |  | | (___   
|  __| |  _  /|  _  /| |  | |\\___ \\  
| |____| | \\ \\| | \\ \\| |__| |____) | 
|______|_|  \\_\\_|  \\_\\____/|_____/
`;

function getConsoleTextCoverage(valori_csv, gameScore, coverageDetails) {
    const lineCoveragePercentage = (coverageDetails.line.covered / (coverageDetails.line.covered + coverageDetails.line.missed)) * 100;
    const branchCoveragePercentage = (coverageDetails.branch.covered / (coverageDetails.branch.covered + coverageDetails.branch.missed)) * 100;
    const instructionCoveragePercentage = (coverageDetails.instruction.covered / (coverageDetails.instruction.covered + coverageDetails.instruction.missed)) * 100;

    const consoleText =
`============================== Results ===============================
Il tuo punteggio: ${gameScore}pt
============================== JaCoCo ===============================
Line Coverage COV%:  ${lineCoveragePercentage}% LOC
covered: ${coverageDetails.line.covered}  
missed: ${coverageDetails.line.missed}
----------------------------------------------------------------------
Branch Coverage COV%:  ${branchCoveragePercentage}% LOC
covered: ${coverageDetails.branch.covered} 
missed: ${coverageDetails.branch.missed}
----------------------------------------------------------------------
Instruction Coverage COV%:  ${instructionCoveragePercentage}% LOC
covered: ${coverageDetails.instruction.covered} 
missed: ${coverageDetails.instruction.missed}
============================== EvoSuite ===============================
la tua Coverage:  ${valori_csv[0] * 100}% LOC
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[1] * 100}% Branch
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[2] * 100}% Exception
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[3] * 100}% WeakMutation
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[4] * 100}% Output
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[5] * 100}% Method
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[6] * 100}% MethodNoException
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[7] * 100}% CBranch
======================================================================
`;
    return consoleText;
}

function getConsoleTextRun(valori_csv, coverageDetails, punteggioRobot, gameScore, isWinner) {
    const lineCoveragePercentage = (coverageDetails.line.covered / (coverageDetails.line.covered + coverageDetails.line.missed)) * 100;
    const branchCoveragePercentage = (coverageDetails.branch.covered / (coverageDetails.branch.covered + coverageDetails.branch.missed)) * 100;
    const instructionCoveragePercentage = (coverageDetails.instruction.covered / (coverageDetails.instruction.covered + coverageDetails.instruction.missed)) * 100;

    const header = isWinner ? you_win : you_lose;
    const consoleText =
`=====================================================================
${header}
============================== Results ===============================
Il tuo punteggio: ${gameScore}pt
----------------------------------------------------------------------
La coverage del robot: ${punteggioRobot}% LOC
============================== JaCoCo ===============================
Line Coverage COV%:  ${lineCoveragePercentage}% LOC
covered: ${coverageDetails.line.covered}  
missed: ${coverageDetails.line.missed}
----------------------------------------------------------------------
Branch Coverage COV%:  ${branchCoveragePercentage}% LOC
covered: ${coverageDetails.branch.covered} 
missed: ${coverageDetails.branch.missed}
----------------------------------------------------------------------
Instruction Coverage COV%:  ${instructionCoveragePercentage}% LOC
covered: ${coverageDetails.instruction.covered} 
missed: ${coverageDetails.instruction.missed}
============================== EvoSuite ===============================
la tua Coverage:  ${valori_csv[0] * 100}% LOC
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[1] * 100}% Branch
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[2] * 100}% Exception
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[3] * 100}% WeakMutation
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[4] * 100}% Output
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[5] * 100}% Method
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[6] * 100}% MethodNoException
----------------------------------------------------------------------
Il tuo punteggio EvoSuite: ${valori_csv[7] * 100}% CBranch
=====================================================================
`;
    return consoleText;
}

function getConsoleTextError() {
    return `=====================================================================
${errorText}
============================== Results ===============================
Ci sono stati errori di compilazione, controlla la console!
`;
}

// === FUNZIONI DI UTILITÀ PER L'EDITOR E LA SESSIONE ===

function replaceText(text, replacements) {
    return text.replace(/\b(TestClasse|username|userID|date)\b/g, match => replacements[match] || match);
}

function SetInitialEditor(replacements) {
    const text = editor_utente.getValue();
    console.log("[SetInitialEditor] Testo originale:", text);
    const newContent = replaceText(text, replacements);
    console.log("[SetInitialEditor] Testo aggiornato:", newContent);
    editor_utente.setValue(newContent);
}

function getParameterByName(name) {
    const url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    const regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)");
    const results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return "";
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function GetMode() {
    const mode = getParameterByName("mode");
    if (mode) {
        const trimmed = mode.replace(/[^a-zA-Z0-9\s]/g, " ").trim();
        return (trimmed.toLowerCase() === "sfida") ? "Sfida" : trimmed;
    }
    return "Sfida";
}

// === FUNZIONI PER LA SESSIONE ===

async function fetchPreviousGameData() {
    const playerId = String(parseJwt(getCookie("jwt")).userId);
    try {
        const response = await fetch(`/session/${playerId}`);
        const data = await response.json();
        if (data && data.modalita && data.modalita[GetMode()] && data.modalita[GetMode()].gameobject) {
            console.log("[fetchPreviousGameData] Trovato gameobject per la modalità " + GetMode() + ":", data.modalita[GetMode()].gameobject);
            return data.modalita[GetMode()].gameobject;
        }
        return null;
    } catch (error) {
        console.error("Errore durante il recupero della sessione:", error);
        return null;
    }
}

async function getFormData() {
    const formData = new FormData();
    const gameObject = await fetchPreviousGameData();
    if (gameObject) {
        console.log("[getFormData] Recuperato game object dalla sessione:", gameObject);
        formData.append("playerId", gameObject.player_id);
        formData.append("mode", (gameObject.mode.toLowerCase() === "sfida") ? "Sfida" : gameObject.mode);
        formData.append("underTestClassName", gameObject.class_ut);
        if (gameObject.game_id) formData.append("gameId", gameObject.game_id);
        if (gameObject.round_id) formData.append("roundId", gameObject.round_id);
    } else {
        console.warn("[getFormData] Nessun game object trovato in sessione; uso fallback da localStorage.");
        formData.append("playerId", String(parseJwt(getCookie("jwt")).userId));
        formData.append("mode", "Sfida");
        formData.append("underTestClassName", localStorage.getItem("underTestClassName"));
    }
    let codeValue = editor_utente.getValue();
    console.log("[getFormData] Valore originale dell'editor:", codeValue);
    console.log("[getFormData] Lunghezza codice editor:", codeValue.length);
    console.log("[getFormData] Prime 100 caratteri:", codeValue.substring(0, 100));
    if (!codeValue || codeValue.trim() === "" || codeValue.startsWith("null")) {
        console.error("[getFormData] Il codice dell'editor NON è valido. Valore ricevuto:", codeValue);
        codeValue = codeValue.replace(/^null/, "");
        console.error("[getFormData] Valore dopo aver rimosso 'null':", codeValue);
        if (!codeValue.trim()) {
            console.error("[getFormData] Il codice risultante è vuoto dopo aver rimosso 'null'.");
        }
    }
    formData.append("testingClassCode", codeValue);
    console.log("[getFormData] FormData contenuto:", Object.fromEntries(formData.entries()));
    return formData;
}

function parseMavenOutput(output) {
    console.log("[parseMavenOutput] Output Maven completo:", output);
    const lines = output.split("\n");
    let buildStatus = "UNKNOWN";
    lines.forEach((line, index) => {
        if (line.includes("TestClasse")) {
            console.warn(`[parseMavenOutput] Riga ${index} contenente "TestClasse":`, line);
        }
    });
    for (const line of lines) {
        if (line.includes("BUILD SUCCESS")) {
            buildStatus = "SUCCESS";
            break;
        } else if (line.includes("BUILD FAILURE")) {
            buildStatus = "FAILURE";
            break;
        }
    }
    console.log("[parseMavenOutput] Stato di build:", buildStatus);
    return {
        status: buildStatus,
        rawOutput: output,
    };
}

async function ajaxRequest(url, method = "POST", data = null, isJson = true, dataType = "json") {
    try {
        let processedData, contentType, processData;
        if (data instanceof FormData) {
            processedData = data;
            contentType = false;
            processData = false;
        } else {
            processedData = isJson && data ? JSON.stringify(data) : data;
            contentType = isJson ? "application/json; charset=UTF-8" : false;
            processData = true;
        }
        const options = {
            url: url,
            type: method,
            dataType: dataType,
            data: processedData,
            contentType: contentType,
            processData: processData,
            xhrFields: { withCredentials: true }
        };
        console.log("[ajaxRequest] Options:", options);
        const response = await $.ajax(options);
        console.log("[ajaxRequest] Risposta:", response);
        return response;
    } catch (error) {
        console.error("Si è verificato un errore in ajaxRequest:", error);
        throw error;
    }
}

function toggleLoading(showSpinner, divId, buttonId) {
    const divElement = document.getElementById(divId);
    if (!divElement) {
        console.error(`Elemento con ID "${divId}" non trovato.`);
        return;
    }
    const spinner = divElement.querySelector(".spinner-border");
    const statusText = divElement.querySelector('[role="status"]');
    const icon = divElement.querySelector("i");
    if (showSpinner) {
        spinner.style.display = "inline-block";
        statusText.innerText = window.loading || "Loading...";
        icon.style.display = "none";
    } else {
        spinner.style.display = "none";
        statusText.innerText = statusText.getAttribute('data-title') || "";
        icon.style.display = "inline-block";
    }
}

const statusMessages = {
    sending:    { showSpinner: true,  text: window.status_sending || "Invio..." },
    loading:    { showSpinner: true,  text: window.status_loading || "Caricamento..." },
    compiling:  { showSpinner: true,  text: window.status_compiling || "Compilazione in corso..." },
    ready:      { showSpinner: false, text: window.status_ready || "Pronto" },
    error:      { showSpinner: false, text: window.status_error || "Errore" },
    turn_end:   { showSpinner: false, text: window.status_turn_end || "Turno terminato" },
    game_end:   { showSpinner: false, text: window.status_game_end || "Partita terminata" }
};

function setStatus(statusName) {
    const divElement = document.getElementById("status_compiler");
    if (!divElement) {
        console.error(`Elemento con ID "status_compiler" non trovato.`);
        return;
    }
    const spinner = divElement.querySelector(".spinner-border");
    const statusText = divElement.querySelector("#status_text");
    const icon = divElement.querySelector("i");
    const status = statusMessages[statusName];
    if (!status) {
        console.error(`Stato "${statusName}" non definito.`);
        return;
    }
    if (status.showSpinner) {
        spinner.style.display = "inline-block";
        statusText.innerText = status.text;
        icon.style.display = "none";
    } else {
        spinner.style.display = "none";
        statusText.innerText = status.text;
        icon.style.display = "inline-block";
    }
}
