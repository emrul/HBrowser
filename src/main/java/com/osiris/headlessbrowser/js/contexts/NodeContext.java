package com.osiris.headlessbrowser.js.contexts;

import com.osiris.betterthread.BetterThreadManager;
import com.osiris.headlessbrowser.exceptions.NodeJsCodeException;
import com.osiris.headlessbrowser.utils.AsyncReader;
import com.osiris.headlessbrowser.utils.DownloadTask;
import com.osiris.headlessbrowser.utils.OS;
import com.osiris.headlessbrowser.utils.TrashOutput;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.osiris.headlessbrowser.utils.OS.ARCH;
import static com.osiris.headlessbrowser.utils.OS.TYPE;

public class NodeContext implements AutoCloseable {
    public final Process process;
    public final AsyncReader processInput;
    public final AsyncReader processErrorInput;
    public final OutputStream processOutput;
    public final PrintStream debugOutput;
    public final File lastJsCodeExecutionResultFile;
    public final int timeout;
    public final File parentNodeDir;
    public final File workingDir;
    public final String nodeExePath;
    public final String npmExePath;
    public final String npxExePath;
    public File installationDir;

    public NodeContext() {
        this(null, null, 30);
    }

    /**
     * @param parentNodeDir path of an empty directory (can also not exist yet) to install the latest Node.js into if needed.
     *                      If null we check for an existing system-wide installation of Node.js and use that,
     *                      otherwise we install the latest version into ./NodeJS-Installation ("." is the current working directory).
     * @param debugOutput   if null, debug output won't be written/printed, otherwise it gets printed/written to the provided {@link OutputStream}.
     * @param timeout       the max time in seconds to wait for JavaScript code to finish. Set to 0 to disable.
     */
    public NodeContext(File parentNodeDir, OutputStream debugOutput, int timeout) {
        this.timeout = timeout;
        if (debugOutput == null)
            this.debugOutput = new PrintStream(new TrashOutput());
        else
            this.debugOutput = new PrintStream(debugOutput);
        PrintStream out = this.debugOutput;
        if (parentNodeDir == null) parentNodeDir = new File(System.getProperty("user.dir") + "/node-js");
        this.parentNodeDir = parentNodeDir;
        Objects.requireNonNull(this.parentNodeDir);
        if (!parentNodeDir.exists()) parentNodeDir.mkdirs();
        this.workingDir = new File(this.parentNodeDir + "/node-js-working-dir");
        Objects.requireNonNull(workingDir);
        if (!workingDir.exists()) workingDir.mkdirs();
        this.installationDir = new File(this.parentNodeDir + "/node-js-installation");
        Objects.requireNonNull(installationDir);
        if (!this.installationDir.exists()) this.installationDir.mkdirs();
        // Download and install NodeJS into current working directory if no installation found
        try {
            if (installationDir.listFiles() == null || this.installationDir.listFiles().length == 0) { // Do install if needed
                String url = "https://nodejs.org/dist/latest/";
                out.println("Installing latest NodeJS release from '" + url + "'...");
                Document docLatest = Jsoup.connect(url).get();

                String downloadUrl = null;
                for (Element e :
                        docLatest.getElementsByTag("a")) {
                    String attr = e.attr("href");
                    if (isCorrectFileForOs(attr.replace(url, ""))) {
                        downloadUrl = url + attr;
                        break;
                    }
                }

                if (downloadUrl == null)
                    throw new FileNotFoundException("Failed to find latest NodeJS download url at '" + url + "' for OS '" + OS.TYPE.name + "' with ARCH '" + ARCH.name() + "'.");

                // Download the zip file and extract its contents
                if (TYPE.equals(OS.Type.WINDOWS)) {
                    File downloadZip = new File(installationDir + "/download.zip");
                    if (downloadZip.exists()) downloadZip.delete();
                    downloadZip.createNewFile();
                    DownloadTask downloadTask = new DownloadTask("Download", new BetterThreadManager(), downloadUrl, downloadZip, "zip");
                    downloadTask.start();
                    while (!downloadTask.isFinished()) {
                        out.println("Download-Task > " + downloadTask.getStatus());
                        Thread.sleep(1000);
                    }
                    out.println("Download-Task > " + downloadTask.getStatus());

                    out.print("Extracting Node.js files...");
                    out.flush();
                    ZipFile zipFile = new ZipFile(downloadZip);
                    zipFile.extractFile(zipFile.getFileHeaders().get(0).getFileName(), installationDir.getPath());
                    downloadZip.delete();
                    out.println(" SUCCESS!");
                } else {
                    File downloadFile = new File(installationDir + "/download.tar.gz");
                    if (downloadFile.exists()) downloadFile.delete();
                    downloadFile.createNewFile();
                    DownloadTask downloadTask = new DownloadTask("Download", new BetterThreadManager(), downloadUrl, downloadFile, "gzip", "tar.gz", "tar", "tar+gzip", "x-gtar", "x-gzip", "x-tgz");
                    downloadTask.start();
                    while (!downloadTask.isFinished()) {
                        out.println("Download-Task > " + downloadTask.getStatus());
                        Thread.sleep(1000);
                    }
                    out.println("Download-Task > " + downloadTask.getStatus());
                    out.print("Extracting Node.js files...");
                    out.flush();
                    ArchiverFactory.createArchiver(downloadFile).extract(downloadFile, installationDir);
                    // Result should be .../headless-browser/node-js/node<version>/...
                    downloadFile.delete();
                    out.println(" SUCCESS!");
                }
            }
            this.installationDir = new File(installationDir + "/" + installationDir.listFiles()[0].getName());

            File nodeExeFile, npmExeFile, npxExeFile;
            if (TYPE.equals(OS.Type.WINDOWS)) {
                nodeExeFile = new File(installationDir + "/node.exe");
                npmExeFile = new File(installationDir + "/npm.cmd");
                npxExeFile = new File(installationDir + "/npx.cmd");
            } else { // Linux, mac and co.
                nodeExeFile = new File(installationDir + "/bin/node");
                npmExeFile = new File(installationDir + "/bin/npm");
                npxExeFile = new File(installationDir + "/bin/npx");
            }

            if (!nodeExeFile.exists())
                throw new FileNotFoundException(nodeExeFile.getAbsolutePath());
            if (!npmExeFile.exists())
                throw new FileNotFoundException(npmExeFile.getAbsolutePath());
            if (!npxExeFile.exists())
                throw new FileNotFoundException(npxExeFile.getAbsolutePath());

            nodeExePath = nodeExeFile.getAbsolutePath();
            npmExePath = npmExeFile.getAbsolutePath();
            npxExePath = npxExeFile.getAbsolutePath();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Start NodeJS and get the processes I/O
        try {
            out.print("Initialising NodeJS...");
            out.flush();
            ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(
                    nodeExePath, "--interactive"));
            processBuilder.directory(workingDir);
            // Must be inherited so that NodeJS closes when this application closes.
            // Wrong! It seems like NodeJS closes if the parent process dies, even if its Piped I/O.
            process = processBuilder.start();
            Objects.requireNonNull(process.getInputStream());
            Objects.requireNonNull(process.getOutputStream());
            // Somehow the I/O from the node.exe cannot be read?
            // Tried multiple things without success.
            // Update: Node.exe must be started with this flag to get correct I/O: --interactive
            processInput = new AsyncReader(process.getInputStream(),
                    line -> out.println("[" + Instant.now().toString() + " " + getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "|LOG] " + line));
            processErrorInput = new AsyncReader(process.getErrorStream());
            processOutput = process.getOutputStream();
            out.println(" SUCCESS!");
            out.println("Node-JS was started from: " + nodeExePath);

            Thread t = new Thread(() -> {
                // Keeps the java application running until NodeJS closes
                try {
                    while (process.isAlive()) {
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            t.start();

            executeJavaScript("function sleep(ms) {\n" +
                    "  return new Promise(resolve => setTimeout(resolve, ms));\n" +
                    "}");

            lastJsCodeExecutionResultFile = new File(workingDir + "/JavaScriptCodeResult.txt");
            if (!lastJsCodeExecutionResultFile.exists()) lastJsCodeExecutionResultFile.createNewFile();
            String resultFilePath = lastJsCodeExecutionResultFile.getAbsolutePath().replace("\\", "/"); // To avoid issues with indows file path formats
            executeJavaScript("function writeToJava(result) {\n" +
                    "var fs = require('fs');\n" +
                    "var data = fs.writeFileSync('" + resultFilePath + "', result);\n" +
                    "};\n" +
                    "console.log('Context initialised!');\n");

        } catch (Exception e) {
            System.err.println("Error during start of NodeJS! Details:");
            throw new RuntimeException(e);
        }
    }

    /**
     * Note that {@link #determineArchAndOs()} must have been called before executing this method. <br>
     *
     * @param fileName example: node-v16.10.0-darwin-arm64.tar.gz
     * @return true if the provided file name contains details matching the current system.
     */
    private boolean isCorrectFileForOs(String fileName) {
        if (!fileName.contains("."))
            return false; // cannot be a directory
        if (TYPE.equals(OS.Type.MAC)) {
            if (!fileName.contains(".tar.gz"))
                return false;
            // Mac has another name: darwin instead of mac
            if (StringUtils.containsIgnoreCase(fileName, "darwin")
                    && (StringUtils.containsIgnoreCase(fileName, ARCH.name()) || containsIgnoreCase(fileName, ARCH.altNames)))
                return true;
        } else if (TYPE.equals(OS.Type.WINDOWS)) {
            // Must be a zip file
            if (!fileName.contains(".zip"))
                return false;
            // Mac has another name: win instead of windows
            if (StringUtils.containsIgnoreCase(fileName, "win")
                    && (StringUtils.containsIgnoreCase(fileName, ARCH.name()) || containsIgnoreCase(fileName, ARCH.altNames)))
                return true;
        } else {
            if (!fileName.contains(".tar.gz"))
                return false;
        }
        return StringUtils.containsIgnoreCase(fileName, TYPE.name)
                && (StringUtils.containsIgnoreCase(fileName, ARCH.name()) || containsIgnoreCase(fileName, ARCH.altNames));
    }

    private boolean containsIgnoreCase(String fileName, String[] altNames) {
        for (String altName :
                altNames) {
            if (StringUtils.containsIgnoreCase(fileName, altName))
                return true;
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        debugOutput.println("CLOSING... " + this);
        process.destroy();
        process.waitFor();
        process.destroyForcibly();
        debugOutput.println("CLOSED " + this);
    }

    public synchronized NodeContext writeLine(String line) throws IOException {
        if (line.contains("\n")) {
            debugOutput.println("Writing multiple lines to NodeJS context:");
            debugOutput.println("START >>>>>>>>>");
            int lineNumber = 1;
            String singleLine = null;
            try (BufferedReader br = new BufferedReader(new StringReader(line))) {
                while ((singleLine = br.readLine()) != null) {
                    debugOutput.println(lineNumber + "| " + singleLine);
                    lineNumber++;
                    processOutput.write("\n".getBytes(StandardCharsets.UTF_8)); // To ensure that multi-lined code from before doesn't affect the next lines
                    processOutput.flush();
                    processOutput.write(".break\n".getBytes(StandardCharsets.UTF_8)); // To ensure that multi-lined code from before doesn't affect the next lines
                    processOutput.flush();
                    processOutput.write((singleLine + "\n").getBytes(StandardCharsets.UTF_8));
                    processOutput.flush();
                    processOutput.write(".break\n".getBytes(StandardCharsets.UTF_8)); // To ensure that multi-lined code from before doesn't affect the next lines
                    processOutput.flush();

                }
            }
            debugOutput.println("END <<<<<<<<<<<");
        } else {
            debugOutput.println("Writing line to NodeJS context: " + line);
            processOutput.write("\n".getBytes(StandardCharsets.UTF_8)); // To ensure that multi-lined code from before doesn't affect the next lines
            processOutput.flush();
            processOutput.write(".break\n".getBytes(StandardCharsets.UTF_8)); // To ensure that multi-lined code from before doesn't affect the next lines
            processOutput.flush();
            processOutput.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            processOutput.flush();
            processOutput.write(".break\n".getBytes(StandardCharsets.UTF_8)); // To ensure that multi-lined code from before doesn't affect the next lines
            processOutput.flush();
        }
        return this;
    }

    /**
     * See {@link #executeJavaScript(String, int, boolean)} for details.
     */
    public NodeContext executeJavaScript(String jsCode) throws NodeJsCodeException {
        return executeJavaScript(jsCode, timeout, true);
    }

    /**
     * Executes JavaScript code from the provided {@link String} in the <br>
     * current {@link NodeContext}. <br>
     * Note that everything related to JavaScript gets printed/written to the {@link #debugOutput}, which <br>
     * means that you must have provided this {@link NodeContext} a {@link #debugOutput} at initialisation to see your codes output. <br>
     */
    public synchronized NodeContext executeJavaScript(String jsCode, int timeout, boolean wrapInTryCatch) throws NodeJsCodeException {
        File tmpJs = null;
        try {
            // Writing stuff directly to the process output/NodeJs REPL console somehow is very error-prone.
            // That's why instead we create a temp file with the js code in it and load it using the .load command.
            long msStart = System.currentTimeMillis();
            tmpJs = new File(workingDir + "/temp" + msStart + ".js");
            if (!tmpJs.exists()) tmpJs.createNewFile();

            if (wrapInTryCatch) {
                jsCode = "try{\n" + // Just to make sure that errors get definitively caught
                        jsCode + "\n" +
                        "console.log('Execution of JS-Code(" + msStart + ") finished!');\n" +
                        "} catch (e){\n" +
                        "  console.error('CAUGHT JS-EXCEPTION: ' + e.name + '\\n'" +
                        "                 + 'MESSAGE: ' + e.message + '\\n'" +
                        "                 + 'LINE: ' + e.lineNumber + '\\n'" +
                        "                 + 'STACK: ' + e.stack+'\\n');\n" +
                        "}\n"
                ;
            } else {
                jsCode = jsCode + "\n" +
                        "console.log('Execution of JS-Code(" + msStart + ") finished!');\n";
            }

            if (jsCode.contains("\n")) {
                debugOutput.println("Executing following JS-Code(" + msStart + "): ");
                debugOutput.println("JS-CODE START >");
                String singleLine = null;
                try (BufferedReader br = new BufferedReader(new StringReader(jsCode))) {
                    while ((singleLine = br.readLine()) != null) {
                        debugOutput.println(singleLine);
                    }
                }
                debugOutput.println("JS-CODE END <");
            } else {
                debugOutput.println("Executing following JS-Code(" + msStart + "): " + jsCode);
            }

            AtomicBoolean wasExecuted = new AtomicBoolean();
            List<String> errors = new ArrayList<>(2);
            Consumer<String> consoleLogListener = line -> {
                if (line.contains("" + msStart)) wasExecuted.set(true);
            };
            processInput.listeners.add(consoleLogListener);

            Consumer<String> consoleErrorLogListener = line -> errors.add(line);
            processErrorInput.listeners.add(consoleErrorLogListener);

            Files.write(tmpJs.toPath(), jsCode.getBytes(StandardCharsets.UTF_8));
            executeJavaScriptFromFile(tmpJs);
            debugOutput.println("Waiting for JavaScript result...");
            // Wait until we receive a response, like undefined

            for (int i = 0; i < timeout * 10; i++) { // Example timeout = 30s * 10 = 300loops * 100ms = 30000ms = 30s
                if (wasExecuted.get() || !errors.isEmpty())
                    break;
                else
                    Thread.sleep(100); // Total of 30 seconds
            }

            if (timeout == 0) { // Since the timeout is 0 we wait indefinitely for the script to finish
                while (!wasExecuted.get() && errors.isEmpty()) {
                    Thread.sleep(100);
                }
            }

            debugOutput.println("Took " + (System.currentTimeMillis() - msStart) + "ms.");

            if (!errors.isEmpty()) {
                throw new NodeJsCodeException("Error during JavaScript code execution! Details: ", errors);
            }

            if (!wasExecuted.get())
                throw new NodeJsCodeException("Script execution timeout of 30 seconds reached! This means that the script didn't finish within the last 30 seconds.", null);


            processErrorInput.listeners.remove(consoleErrorLogListener);
            processInput.listeners.remove(consoleLogListener);
            tmpJs.delete();
        } catch (NodeJsCodeException e) {
            tmpJs.delete();
            throw e;
        } catch (Exception e) {
            if (tmpJs != null) tmpJs.delete();
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * See {@link #executeJavaScript(String, int, boolean)} for details.
     */
    public synchronized String executeJSAndGetResult(String jsCode) {
        return executeJSAndGetResult(jsCode, timeout, true);
    }

    /**
     * Expects: [val1, val2, ...] <br>
     * or expects: val1, val2, ... <br>
     */
    public String[] parseJSStringArrayToJavaStringArray(String jsCodeResult) {
        String[] array = jsCodeResult.replace("[", "")
                .replace("]", "")
                .split(",");
        for (String val :
                array) {
            val = val.trim();
        }
        return array;
    }

    public String parseJavaListToJSArray(List<String> list) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            String val = list.get(i);
            if (i == list.size() - 1) result.append("'" + val + "'");
            else result.append("'" + val + "',");
        }
        result.append("]");
        return result.toString();
    }

    /**
     * The provided jsCode must have this at the end: <br>
     * <pre>
     *     var result = InsertYourFunctionsResultHere;
     * </pre>
     * That result will get returned to this Java method. <br>
     * See {@link #executeJavaScript(String)} for details.
     *
     * @param timeout 30 seconds is the default, set to 0 to disable.
     */
    public synchronized String executeJSAndGetResult(String jsCode, int timeout, boolean wrapInTryCatch) {
        try {
            executeJavaScript(jsCode + "\n"
                    + "writeToJava(result);\n", timeout, wrapInTryCatch);

            StringBuilder result = new StringBuilder();
            String line = null;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(lastJsCodeExecutionResultFile))) {
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line + "\n");
                }
            }

            // Clear the files content because we already got what we need
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(lastJsCodeExecutionResultFile))) {
                bufferedWriter.write("");
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes JavaScript code from the provided {@link File} in the <br>
     * current {@link NodeContext}.
     */
    public NodeContext executeJavaScriptFromFile(File jsFile) throws IOException {
        writeLine(".load " + jsFile.getAbsolutePath());
        return this;
    }

    /**
     * See <br>
     * https://docs.npmjs.com/cli/v6/commands/npm-install <br>
     * for details. <br>
     */
    public NodeContext npmInstall() throws IOException, InterruptedException {
        return npmInstall(null);
    }

    public NodeContext npmInstall(String packageName) throws IOException, InterruptedException {
        debugOutput.println("[NPM-INSTALL] Installing '" + packageName + "'...");
        Process result;
        if (packageName != null)
            result = executeNpmWithArgs("install", packageName);
        else
            result = executeNpmWithArgs("install");
        if (result.exitValue() != 0)
            throw new IOException("Failed to install/download " + packageName + "!" +
                    " Npm finished with exit code '" + process.exitValue() + "'.");
        debugOutput.println("[NPM-INSTALL] Installed '" + packageName + "' successfully!");
        return this;
    }

    /**
     * Executes the "npm" command with the provided arguments. <br>
     * Waits until it finishes and then returns the {@link Process}. <br>
     * You can check if it was successful by checking if {@link Process#exitValue()} returns 0. <br>
     *
     * @param args
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Process executeNpmWithArgs(String... args) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add(npmExePath);
        if (args != null && args.length != 0) commands.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(commands).directory(workingDir).start();
        new AsyncReader(process.getInputStream()).listeners.add(line -> debugOutput.println("[NPM] " + line));
        new AsyncReader(process.getErrorStream()).listeners.add(line -> System.err.println("[NPM-ERROR] " + line));
        while (process.isAlive())
            Thread.sleep(500);
        return process;
    }

    public Process executeNpxWithArgs(String... args) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add(npxExePath);
        if (args != null && args.length != 0) commands.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(commands).directory(workingDir).start();
        new AsyncReader(process.getInputStream()).listeners.add(line -> debugOutput.println("[NPX] " + line));
        new AsyncReader(process.getErrorStream()).listeners.add(line -> System.err.println("[NPX-ERROR] " + line));
        while (process.isAlive())
            Thread.sleep(500);
        return process;
    }

    public Process executeInNewTerminal(File workingDir, Consumer<String> onLineReceived,
                                        Consumer<String> onErrorLineReceived, String... commands) throws IOException {
        Process p;
        if (OS.isWindows()) {
            try {  // Try powershell first, use cmd as fallback
                p = new ProcessBuilder("powershell").directory(workingDir).start();
                if (!p.isAlive()) throw new Exception();
            } catch (Exception e) {
                p = new ProcessBuilder("cmd").directory(workingDir).start();
            }
        } else { // Unix based system, like Linux, Mac etc...
            try {  // Try bash first, use sh as fallback
                p = new ProcessBuilder("/bin/bash").directory(workingDir).start();
                if (!p.isAlive()) throw new Exception();
            } catch (Exception e) {
                p = new ProcessBuilder("/bin/sh").directory(workingDir).start();
            }
        }
        Process process = p;
        InputStream in = process.getInputStream();
        InputStream inErr = process.getErrorStream();
        OutputStream out = process.getOutputStream();
        new AsyncReader(in, onLineReceived);
        new AsyncReader(inErr, onErrorLineReceived);
        if (commands != null && commands.length != 0)
            for (String command :
                    commands) {
                out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        return process;
    }

    /**
     * Events get fired when a line gets printed to the <br>
     * Node.js consoles {@link OutputStream}.<br>
     */
    public NodeContext onPrintLine(Consumer<String> listener) {
        processInput.listeners.add(listener);
        return this;
    }

    public File getParentNodeDir() {
        return parentNodeDir;
    }

    public Process getProcess() {
        return process;
    }

    public AsyncReader getProcessInput() {
        return processInput;
    }

    public OutputStream getProcessOutput() {
        return processOutput;
    }

    public PrintStream getDebugOutput() {
        return debugOutput;
    }

    public AsyncReader getProcessErrorInput() {
        return processErrorInput;
    }

    public File getLastJsCodeExecutionResultFile() {
        return lastJsCodeExecutionResultFile;
    }

    public int getTimeout() {
        return timeout;
    }


}
