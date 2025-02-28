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
            underTestClassName: localStorage.getItem("underTestClassName"),
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

let isActionInProgress = false;

async function handleGameAction(isGameEnd) {
    isActionInProgress = true;
    run_button.disabled = true;
    coverage_button.disabled = true;

    const loadingKey = isGameEnd ? "loading_run" : "loading_cov";
    const buttonKey = isGameEnd ? "runButton" : "coverageButton";

    toggleLoading(true, loadingKey, buttonKey);
    setStatus("sending");

    // Otteniamo il FormData (con debug sul codice dell'editor)
    const formData = await getFormData();
    console.log("[handleGameAction] Dati inviati:", Object.fromEntries(formData.entries()));

    try {
        const response = await runGameAction("/run", formData, isGameEnd);
        setStatus("compiling");
        handleResponse(response, formData, isGameEnd, loadingKey, buttonKey);
    } catch (error) {
        console.error("[handleGameAction] Errore durante l'esecuzione:", error);
    }
    isActionInProgress = false;
}

function handleResponse(response, formData, isGameEnd, loadingKey, buttonKey) {
    const { robotScore, userScore, outCompile, coverage, gameId, roundId, coverageDetails, isWinner } = response;
    formData.append("gameId", gameId);
    formData.append("roundId", roundId);
    console_utente.setValue(outCompile);
    const parsedOutput = parseMavenOutput(outCompile);
    console.log("Parsed Maven Output:", parsedOutput);
    if (!coverage) {
        setStatus("error");
        handleCompileError(loadingKey, buttonKey);
        return;
    }
    processCoverage(coverage, formData, robotScore, userScore, isGameEnd, loadingKey, buttonKey, coverageDetails, isWinner);
}

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

function displayUserPoints(isGameEnd, valori_csv, robotScore, userScore, coverageDetails, isWinner) {
    const outputText = isGameEnd
        ? getConsoleTextRun(valori_csv, coverageDetails, robotScore, userScore, isWinner)
        : getConsoleTextCoverage(valori_csv, userScore, coverageDetails);
    console_robot.setValue(outputText);
}

function handleCompileError(loadingKey, buttonKey) {
    console_robot.setValue(getConsoleTextError());
    toggleLoading(false, loadingKey, buttonKey);
    resetButtons();
}

// === DEFINIZIONE DELLA FUNZIONE resetButtons ===
// Riabilita i pulsanti "run" e "coverage"
function resetButtons() {
    if (typeof run_button !== "undefined") {
        run_button.disabled = false;
    }
    if (typeof coverage_button !== "undefined") {
        coverage_button.disabled = false;
    }
    console.log("[resetButtons] I pulsanti sono stati riabilitati.");
}

// === EVENT LISTENERS PER I PULSANTI "RUN" E "COVERAGE" ===
document.getElementById("runButton").addEventListener("click", () => handleGameAction(true));
document.getElementById("coverageButton").addEventListener("click", () => handleGameAction(false));
