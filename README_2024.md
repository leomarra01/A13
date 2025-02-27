# Testing_Challenge_Game (by UNINA)- Ver. A13
Web App a supporto dell'insegnamento del Software Testing attraverso la Gamification. 

Questa Web App è descritta anche nel seguente articolo scientifico: 
Anna Rita Fasolino, Caterina Maria Accetto, Porfirio Tramontana:
"Testing Robot Challenge: A Serious Game for Testing Learning". Gamify@ISSTA 2024: 26-29, https://doi.org/10.1145/3678869.368568

# Descrizione 
The Testing_Challenge_Game Web Application was developed with the contributions of the students of the University of Naples Federico II in Italy, enrolled in the Software Architecture Design (SAD) courses held by the Prof. Anna Rita Fasolino in the academic years 2022-23, 2023-24, 2024-25.
It has  an extremely ambitious goal: to enhance the importance of testing, a discipline often mistreated and not explored in depth in university courses, through the innovative strategy of gamification which, as the term suggests, consists in using elements borrowed from games but in non-playful contexts. The result of the application of this mechanism was the design and subsequent development of the interactive game: "Man vs Automated Testing Tools challenges" which sees the students, from here on called players, compete, with tests designed using the JUnit framework, against robots (Randoop or EvoSuite) capable of automatically generating such tests; the challenge can be considered won by the participant capable of completing a certain coverage objective.

# Stato del progetto
Il Progetto è in fase di sviluppo ed evoluzione. Questo progetto è stato forkato dal Repository Master sito in: https://github.com/Testing-Game-SAD-2023/A13

# Contributori del Branch/ Fork corrente
I contributori principali del branch corrente sono: Carmine Bellotti e Giuseppe Castaldo.  
Questo repository contiene il codice sorgente e la documentazione relativi al Task R5 del progetto di Software Architecture Design. Il focus principale di questo task è l'implementazione di funzionalità avanzate per gli amministratori del sistema, con particolare attenzione alla gestione dei team e al monitoraggio delle performance degli studenti.


# Funzionalità dell'Applicazione 
L'applicazione prevede due tipi di utente (giocatore ed amministratore) che dovranno preliminarmente registrarsi al gioco con ruoli diversi. 
Il giocatore può:
- Giocare una Sfida di Testing contro i Robot Evosuite o Randoop
- Giocare una Sfida Multi-livello (Scalata) contro i Robot Evosuite o Randoop
- Allenarsi nella scrittura di Test JUnit per testare classi di codice in Java
- Visualizzare il proprio Profilo Utente, accedendo ai propri achievement (traguardi raggiunti)

L'amministratore del Gioco può:
- Caricare nuove classi in Java su cui i giocatori potranno allenarsi e sfidare i Robot
- Visualizzare l'elenco dei Giocatori iscritti e relativi traguardi
- Creare Team di Giocatori.
- Visualizzare tutte le relative informazioni di un Team, tra cui anche quelle dei Giocatori associati
- Assegnare un "Assignment" ad un determinato Team di Giocatori
- Visualizzare tutte le informazioni degli Assignment, tra cui data di Creazione e Scadenza. 


# Screenshot dell'applicazione e Video dimostrativi delle funzionalità principali (TBD).
Siccome un'immagine vale più di mille parole, è buona norma inserire degli screenshot dell'applicativo in modo tale da comunicare visivamente il layout e/o alcune funzionalità del sito. E' anche possibile creare GIF animate che possono essere molto esplicative. Per farlo si può utilizzare ad esempio Recordit.

#Video Dimostrativi  
Parte 1:
[Video 1]


https://github.com/user-attachments/assets/678ea8a4-13f6-461c-9fc3-6c24b22830ba


Parte 2:
[Video 2]


https://github.com/user-attachments/assets/4a1e9b37-95f2-4e72-91fe-86c186bd8ecf


Parte 3:
[Video 3]


https://github.com/user-attachments/assets/6a9d7260-f5a2-45e7-a59a-9e7102b01af1


# Come iniziare
1. Assicurarsi di aver correttamente clonato sul proprio workspace, il repository A13 di riferimento; si consiglia, innanzitutto, di 
scaricare, al seguente indirizzo https://git-scm.com/downloads, ed installare, sul proprio computer, Git (per una guida completa all'installazione fare riferimento al seguente indirizzo: https://github.com/git-guides/install-git).
Una volta aver completato l'installazione, sarà possibile clonare i repository GitHub sulla propria macchina:
<pre>
1) Aprire Git Bash (shell di Git)
2) Posizionarsi nella cartella all'interno della quale si desidera aggiungere il repository clonato
3) Andare alla pagina del repository che si intende clonare -> https://github.com/Testing-Game-SAD-2023/A13
4) Cliccare sul bottone: "< > Code" e copiare l' URL
5) Utilizzare il comando:
     git clone URL COPIATO
6) Premere invio
</pre>
![How to clone repository by URL](https://github.com/Testing-Game-SAD-2023/A13/blob/main/Immagini_installazione/git_clone.png)

2. Assicurarsi di avere Java e Maven installati correttamente sulla propria macchina e di aver settato le variabili d'ambiente.

# Specifiche e Versioni Utilizzate
```
Specifiche dispositivo
----------------------
Processore      Intel(R) Core(TM) i5-8265U CPU @ 1.60GHz   1.80 GHz
RAM installata  8,00 GB (7,88 GB utilizzabile)
----------------------
Specifiche Windows
----------------------
Edizione    Windows 10 Home
Versione    10.0.19045.4291
----------------------
Docker Desktop
----------------------
Versione    4.29.0
----------------------
Windows Subsystem for Linux
----------------------
Versione WSL: 2.0.14.0
Versione kernel: 5.15.133.1-1
Versione WSLg: 1.0.59
Versione MSRDC: 1.2.4677
Versione Direct3D: 1.611.1-81528511
Versione DXCore: 10.0.25131.1002-220531-1700.rs-onecore-base2-hyp
-----------------------
Maven
-----------------------
Versione    3.9.6
-----------------------   
```

# Come installare

### Step 1
Scaricare e installare Docker Desktop per il proprio sistema operativo: se già installato correttamente sulla macchina passare al Passo 2.

NOTA: sebbene il Windows Subsystem for Linux wsl2, una funzionalità che consente di eseguire un ambiente Linux all'interno del sistema operativo Windows garantendo la compatibilità tra Docker Desktop e Windows, normalmente venga installato e aggiornato durante l'installazione di Docker Desktop, vi sono casi in cui questo step non venga effettuato correttamente in maniera automatica (all'apertura di Docker è presente un messaggio di errore "WSL Error"), bisogna quindi installare manualmente wsl tramite i seguenti step:
<pre>
a) avviare il prompt dei comandi
b) digitare wsl --install e premere invio
c) digitare wsl --update e premere invio
d) riavviare la macchina
</pre>
1) Procedere all'installazione di Docker Desktop: https://www.docker.com/products/docker-desktop/
2) All'avvio di Docker Desktop nella sezione Settings -> General controllare che sia spuntata l'opzione "Use the WSL 2 based engine"

### Step 1B
Nel caso non sia la prima installazione, per la disinstallazione utilizzare "uninstaller.bat" mentre si ha in esecuzione Docker, in questo modo si elimina qualunque file presente su Docker.

### Step 2
Avviare lo script "installer.bat" se si sta usando una distribuzione Windows oppure "installermac.sh" nel caso si utilizzi macOS o una distro di Linux.
Per MacOS - eseguire nella cartella dove è presente il file ”installermac.sh” il comando "chmod +x installermac.sh" per renderlo eseguibile, e poi "./installermac.sh" per eseguirlo.
Tali script dovranno essere avviati unicamnete con Docker in esecuzione, altrimenti l'installazione non partirà. Saranno effettuate le seguenti operazioni:
1) Creazione della rete "global-network" comune a tutti i container.
2) Creazione del volume "VolumeT9" comune ai Task 1 e 9 e del volume "VolumeT8" comune ai Task 1 e 8.
3) Creazione dei singoli container in Docker desktop.
4) Esecuzione dei file di installazione nei container del task T8 e T7
5) Avvio dei container
6) Comandi di inizializzazione del database del task T1
   
NOTA: il container relativo al Task 9 ("Progetto-SAD-G19-master") si sospenderà autonomamente dopo l'avvio. Esso viene utilizzato solo per "popolare" il volume "VolumeT9" condiviso con il Task 1.

# Risoluzione di Problemi ricorrenti
Nel corso dell'installazione dello script: "installer.bat", è possibile incappare nell'errore:
<pre>
E: invalid operation update
</pre>
dovuto al modo in cui Windows e Linux memorizzano i caratteri di fine riga (CRLF Windows mentre LF Linux); per risolvere il problema è indispensabile switchare dalla codifica CRLF -> a quella LF.
Un modo rapido prevede di effettuare la modifica manualmente utilizzando come editor di testo Notepad++:
<pre>
Edit => EOL Conversion => Unix(LF) dopodichè salvare
</pre>
![Errore in fase di installazione](https://github.com/Testing-Game-SAD-2023/A13/blob/main/Immagini_installazione/errore.png)
(https://stackoverflow.com/questions/55258430/e-invalid-operation-update-error-while-running-shell-scripts-in-wsl)
# Passi opzionali per esporre l'applicazione su un indirizzo pubblico
__NB: Ogni lettera rappresenta una soluzione diversa__

# A: Installazione NGROK
Nel caso sia stato già installato basterà avviare il container e collegarsi all'indirizzo fornito in precedenza

 __PASSO A.1__:
Registrazione presso il sito: https://ngrok.com/

 __PASSO A.2__:
Accesso alla Dashboard: https://dashboard.ngrok.com/get-started

 __PASSO A.3__:
Scelta dell'agente (si consiglia Docker).

 __PASSO A.4__:
Inserire il comando nel prompt sostituendo il __*token*__ e il __*dominio statico*__ fornito:

    docker run --net=host -it -e NGROK_AUTHTOKEN=TOKEN ngrok/ngrok:latest http --domain= DOMINIO 80
![How to set ngrok](https://github.com/Testing-Game-SAD-2023/A13/blob/main/Immagini_installazione/ngrok.png)
A questo punto si avrà l'indirizzo pubblico come risposta nel prompt dei comandi.

*__NB__*: il comando può essere copiato direttamente dalla dashboard di Ngrok, si consiglia di utilizzare il dominio di tipo statico

# Breve Descrizione delle Modifiche e delle Nuove Feature aggiunte nel repository corrente
Le modifiche e le nuove funzionalità aggiunte al repository includono:

*   **Refactoring del componente T1** per allinearlo al pattern architetturale **Model-View-Controller (MVC)**.
*   **Implementazione di nuove funzionalità per la gestione dei team**, come la creazione, modifica, eliminazione, aggiunta e rimozione di studenti.
*   **Sviluppo di una dashboard interattiva per gli amministratori**, con metriche dettagliate sulle performance degli studenti.
*   **Introduzione di un nuovo package Service**, che funge da intermediario tra Controller e Repository e centralizza la logica applicativa.
*   **Aggiunta di nuovi controller per migliorare la modularità**, come TeamController, ClassUTController e AssignmentController.
*   **Implementazione di nuove API REST** per supportare le funzionalità avanzate del sistema, come la gestione dei team, degli assignment e degli studenti.
*   **Utilizzo di Postman per automatizzare il testing delle API**.

Inoltre, è stata **aggiornata la documentazione del progetto**, inclusi i diagrammi UML, per riflettere le modifiche apportate.


# Documentazione di Progetto della Versione corrente
Riportare il collegamento alla documentazione completa della versione modificata (file pdf) e ai file sorgente dei diagrammi prodotti che dovranno essere contenuti in una cartella denominata "Documentazione_2024" inclusa nel repository stesso.
[Documentazione Ufficiale Task R5](/Documentazione/Doc_T1/Documentazione_Task_R5_GrB16/Tesina_Ufficiale_Task_R5.pdf)




