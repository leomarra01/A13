package RemoteCCC.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class CompilationService {
    
    // Config è una classe di supporto che gestisce i path e altre impostazioni
    protected final Config config;
    private final String testingClassName;
    private final String testingClassCode;
    private final String underTestClassName;
    private final String underTestClassCode;
    
    // Inizializzo outputMaven come stringa vuota per evitare "null"
    public String outputMaven = "";
    public Boolean Errors;
    public String Coverage;
    
    // Se mvn_path è impostato in application.properties o in ambiente, 
    // assicurati che contenga il comando corretto (ad es. "mvn" o il percorso assoluto)
    @Value("${variabile.mvn}")
    private String mvn_path;
    
    protected static final Logger logger = LoggerFactory.getLogger(CompilationService.class);
    
    public CompilationService(String testingClassName, String testingClassCode,
                              String underTestClassName, String underTestClassCode, 
                              String mvn_path) {
        this.config = new Config();
        this.testingClassName = testingClassName;
        this.testingClassCode = testingClassCode;
        this.underTestClassName = underTestClassName;
        this.underTestClassCode = underTestClassCode;
        this.mvn_path = mvn_path;
        logger.info("[CompilationService] Servizio creato con successo");
    }
    
    public void compileAndTest() throws IOException, InterruptedException {
        try {
            createDirectoriesAndCopyPom();
            saveCodeToFile(this.testingClassName, this.testingClassCode, config.getTestingClassPath());
            saveCodeToFile(this.underTestClassName, this.underTestClassCode, config.getUnderTestClassPath());
            logger.info("[CompilationService] Avvio Maven");
            if (compileExecuteCoverageWithMaven()) {
                // Legge il file di report di coverage; il path deve puntare al file (ad es. jacoco.xml)
                this.Coverage = readFileToString(config.getCoverageFolderPath());
                this.Errors = false;
                logger.info("[CompilationService] Compilazione terminata senza errori.");
            } else {
                this.Coverage = null;
                this.Errors = true;
                logger.info("[CompilationService] Compilazione terminata con errori");
            }
            deleteFile(config.getUnderTestClassPath() + underTestClassName);
            deleteFile(config.getTestingClassPath() + testingClassName);
            deleteTemporaryDirectories(config.getPathCompiler());
            logger.info("[CompilationService] File temporanei eliminati");
        } catch (FileConcurrencyException e) {
            logger.error("[CompilationService] [LOCK ERROR] ", e);
        } catch (IOException e) {
            logger.error("[CompilationService] [I/O ERROR] ", e);
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("[CompilationService] [ARGS ERROR] ", e);
        } catch (RuntimeException e) {
            logger.error("[CompilationService] [RUNTIME ERROR] ", e);
        } catch (Exception e) {
            logger.error("[CompilationService] [GENERIC ERROR] ", e);
        }
    }
    
    protected void createDirectoriesAndCopyPom() throws IOException {
        createDirectoryIfNotExists(config.getPathCompiler());
        logger.info("[CompilationService] Directory creata o già esistente: {}", config.getPathCompiler());
        
        copyPomFile();
        logger.info("[CompilationService] Pom file copiato in: {}pom.xml", config.getPathCompiler());
        
        createDirectoryIfNotExists(config.getTestingClassPath());
        logger.info("[CompilationService] Directory testing creata o già esistente: {}", config.getTestingClassPath());
        
        createDirectoryIfNotExists(config.getUnderTestClassPath());
        logger.info("[CompilationService] Directory under test creata o già esistente: {}", config.getUnderTestClassPath());
        
        createDirectoryIfNotExists(config.getCoverageFolderPath());
        logger.info("[CompilationService] Directory coverage creata o già esistente: {}", config.getCoverageFolderPath());
    }
    
    protected void createDirectoryIfNotExists(String path) throws IOException {
        File directory = new File(path);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("[createDirectoryIfNotExists] Impossibile creare la directory: " + path);
            }
        } else {
            logger.info("[createDirectoryIfNotExists] La directory esiste già: {}", path);
        }
    }
    
    private void copyPomFile() throws IOException {
        File pomFile = new File(config.getUsrPath() + config.getsep() + "ClientProject" + config.getsep() + "pom.xml");
        File destPomFile = new File(config.getPathCompiler() + "pom.xml");
        
        if (!pomFile.exists()) {
            throw new IOException("[copyPomFile] Il file pom.xml non esiste: " + pomFile.getAbsolutePath());
        }
        
        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
        readLock.lock();
        try (FileChannel sourceChannel = FileChannel.open(pomFile.toPath(), StandardOpenOption.READ);
             FileChannel destChannel = FileChannel.open(destPomFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            long size = sourceChannel.size();
            long position = 0;
            while (position < size) {
                position += sourceChannel.transferTo(position, size - position, destChannel);
            }
        } catch (OverlappingFileLockException | FileLockInterruptionException | NonWritableChannelException | ClosedChannelException e) {
            throw new FileConcurrencyException("[copyPomFile] Errore di lock: " + e.getMessage(), e);
        } finally {
            readLock.unlock();
        }
    }
    
    private void saveCodeToFile(String nameclass, String code, String path) throws IOException {
        if (nameclass == null || nameclass.isEmpty()) {
            throw new IllegalArgumentException("[saveCodeToFile] Il nome della classe non può essere nullo o vuoto.");
        }
        if (!nameclass.endsWith(".java")) {
            throw new IllegalArgumentException("[saveCodeToFile] Il nome della classe deve avere l'estensione .java");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("[saveCodeToFile] Il percorso non può essere nullo o vuoto.");
        }
        // Aggiunge la dichiarazione del package se prevista nel config
        String packageDeclaration = config.getPackageDeclaration();
        code = (packageDeclaration != null && !packageDeclaration.isEmpty() 
                    ? packageDeclaration + System.lineSeparator() 
                    : "") + code;
        File tempFile = new File(path + nameclass);
        tempFile.deleteOnExit();
        try (FileChannel channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            logger.info("[CompilationService] Creato il file: {}{}", path, nameclass);
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(code);
            channel.write(buffer);
        } catch (IOException e) {
            throw new IOException("[saveCodeToFile] Errore durante la scrittura nel file: " + tempFile.getAbsolutePath(), e);
        }
    }
    
    private boolean compileExecuteCoverageWithMaven() throws RuntimeException {
        logger.info("Percorso mvn: {}", mvn_path);
        ProcessBuilder processBuilder = new ProcessBuilder(mvn_path, "clean", "compile", "test");
        processBuilder.directory(new File(config.getPathCompiler()));
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        Process process = null;
        try {
            process = processBuilder.start();
            readProcessOutput(process, output, errorOutput);
            boolean finished = process.waitFor(15, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("[compileExecuteCoverageWithMaven] Timeout superato. Processo terminato forzatamente.");
            }
            // Concatena l'output raccolto (già inizializzato a "")
            outputMaven += output.toString();
            logger.info("[compileExecuteCoverageWithMaven] Output Maven: {}", output.toString());
            // Se exit value è 0, compilazione considerata andata a buon fine
            return process.exitValue() == 0;
        } catch (IOException e) {
            logger.error("[CompilationService] [MAVEN] {}", errorOutput.toString());
            throw new RuntimeException("[compileExecuteCoverageWithMaven] Errore I/O: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("[CompilationService] [MAVEN] {}", errorOutput.toString());
            Thread.currentThread().interrupt();
            throw new RuntimeException("[compileExecuteCoverageWithMaven] Processo interrotto: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
    
    private void readProcessOutput(Process process, StringBuilder output, StringBuilder errorOutput)
            throws IOException, InterruptedException {
        try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = outputReader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append(System.lineSeparator());
            }
        }
    }
    
    private void deleteFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("[deleteFile] Impossibile eliminare il file: " + file.getAbsolutePath());
            }
        } else {
            logger.warn("[deleteFile] Il file non esiste: {}", file.getAbsolutePath());
        }
    }
    
    private void deleteTemporaryDirectories(String path) throws IOException {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                throw new IOException("[deleteTemporaryDirectories] Errore nell'eliminazione della directory: " + dir.getAbsolutePath(), e);
            }
        } else {
            logger.warn("[deleteTemporaryDirectories] La directory non esiste o non è valida: {}", dir.getAbsolutePath());
        }
    }
    
    protected void deleteCartelleTest() throws IOException {
        deleteTemporaryDirectories(config.getPathCompiler());
    }
    
    private String readFileToString(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public class FileConcurrencyException extends IOException {
        public FileConcurrencyException(String message) {
            super(message);
        }
        public FileConcurrencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
