/*
 * Copyright (C) 2015 Paston Solutions BV
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.paston.bonita.importfile;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessExecutionException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

/**
 *
 * @author <a href="mailto:martijnburger@paston.nl">Martijn Burger</a>
 */
public class Main {

    private static Logger log;

    private static final int NUMBER_OF_PROCESSES = 20;
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";

    protected static enum Cmd {
        SERVER_URL("serverUrl"),
        APPLICATION_NAME("applicationName"),
        USERNAME("username"),
        PASSWORD("password"),
        PROCESS_NAME("processName"),
        PROCESS_VERSION("processVersion"),
        INPUT_VARIABLE("inputVariable"),
        CSV_FILE("csvFile"),
        HELP("help"),
        TALKATIVE("talkative"),
        QUIET("quiet");

        private final String name;

        Cmd(String name) {
            this.name = name;
        }

        protected String getName() {
            return name;
        }

    }

    public static void main(String[] args) {
        // Parse the commandline arguments.
        CommandLine cmd = parseArguments(args);
        log.info("Starting bonita-importfile. For help information add -h.");

        // Get input paramters for Login API
        String serverUrl = getConsoleInput("Bonita server URL",
                "http://localhost:8080", cmd, Cmd.SERVER_URL.getName());

        String applicationName = getConsoleInput("Bonita application name",
                "bonita", cmd, Cmd.APPLICATION_NAME.getName());

        // Connect to Bonita server and create the Login API.
        LoginAPI loginAPI = getLoginAPI(serverUrl, applicationName);

        // Get login parameters for APISession
        String userName = getConsoleInput("Bonita user name",
                "walter.bates", cmd, Cmd.USERNAME.getName());

        char[] password = cmd.hasOption(Cmd.PASSWORD.getName())
                ? cmd.getOptionValue(Cmd.PASSWORD.getName()).toCharArray()
                : System.console().readPassword("Bonita password: ");

        // Create an APISession and a ProcessAPI.
        APISession apiSession = getAPISession(loginAPI, userName, password);
        ProcessAPI processAPI = getProcessAPI(apiSession);

        // Get a list of processes and let the user choose the process.
        List<ProcessDeploymentInfo> processList = getProcessList(processAPI);

        ProcessDeploymentInfo processDeploymentInfo
                = getSelectedProcess(processList, cmd);

        // Ge the bonita input variable for this process.
        String bonitaInputVar = cmd.hasOption(Cmd.INPUT_VARIABLE.getName())
                ? cmd.getOptionValue(Cmd.INPUT_VARIABLE.getName())
                : System.console().readLine("Bonita input variable name: ");

        // Read records from file;
        Reader in = getReader(cmd);
        Iterable<CSVRecord> records = getCSVRecords(in);

        // Parse and push records to Bonita.
        Iterator<CSVRecord> iterator = records.iterator();
        CSVRecord fullHeader = getFullHeader(iterator);
        for (CSVRecord record : records) {
            Map<Object, Object> map = parseRecord(record, fullHeader);
            pushRecordToBonita(processAPI,
                    processDeploymentInfo, bonitaInputVar, map);
        }
        log.info("Finished bonita-importfile succesfully.");
    }

    protected static Map<Object, Object> parseRecord(CSVRecord record,
            CSVRecord fullHeader) {
        if (record == null) {
            log.warn("Record is null.");
            return null;
        }
        log.info("Parsing record number: " + (record.getRecordNumber() - 1));
        log.debug(" with content: " + record.toString());
        final Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < record.size(); i++) {
            String header = getHeader(fullHeader.get(i));
            String headerType = getHeaderType(fullHeader.get(i));
            Object recordObject = getRecordObject(headerType, record.get(i));
            if (recordObject != null) {
                map.put(header, recordObject);
            } else {
                log.warn("Skipped value for record item: "
                        + record.get(i));
            }
        }
        return map;
    }

    protected static void pushRecordToBonita(ProcessAPI processAPI,
            ProcessDeploymentInfo info, String bonitaInputVar,
            Map<Object, Object> map) {
        if (map == null) {
            log.warn("map is null.");
            return;
        }
        log.info("Pushing record to Bonita server.");
        log.debug(map.toString());
        try {
            final Map<String, Serializable> inputs = new HashMap<>();
            inputs.put(bonitaInputVar, new HashMap(map));
            processAPI.startProcessWithInputs(info.getProcessId(), inputs);
            log.debug("Succesfully pushed record.");
        } catch (ProcessDefinitionNotFoundException |
                ProcessActivationException |
                ProcessExecutionException |
                ContractViolationException ex) {
            log.error("Cannot push data to bonita. Reason: "
                    + ex.getMessage());
            System.exit(1);
        }
    }

    protected static Object getRecordObject(String headerType,
            String stringValue) {
        if (headerType == null) {
            log.warn("Cannot parse an based on an empty headertype.");
            return null;
        }
        switch (headerType) {
            case "BOOLEAN":
                switch (stringValue) {
                    case TRUE:
                        return true;
                    case FALSE:
                        return false;
                    default:
                        log.warn("This is not a boolean ("
                                + TRUE + "," + FALSE + "): " + stringValue);
                        return null;
                }
            case "DATE":
                try {
                    DateFormat df = new SimpleDateFormat("dd/mm/yyyy");
                    return df.parse(stringValue);
                } catch (ParseException ex) {
                    log.warn("This is not a date: " + stringValue);
                    return null;
                }
            case "DOUBLE":
                try {
                    return Double.parseDouble(stringValue);
                } catch (NumberFormatException ex) {
                    log.warn("This is not a double: " + stringValue);
                    return null;
                }
            case "FLOAT":
                try {
                    return Float.parseFloat(stringValue);
                } catch (NumberFormatException ex) {
                    log.warn("This is not a float: " + stringValue);
                    return null;
                }
            case "INTEGER":
                try {
                    return Integer.parseInt(stringValue);
                } catch (NumberFormatException ex) {
                    log.warn("This is not an integer: " + stringValue);
                    return null;
                }
            case "LONG":
                try {
                    return Long.parseLong(stringValue);
                } catch (NumberFormatException ex) {
                    log.warn("This is not a long: " + stringValue);
                    return null;
                }
            case "STRING":
            case "TEXT":
                return stringValue;
            default:
                log.warn("Type not recognized from header: " + headerType);
                return null;
        }
    }

    protected static CSVRecord getFullHeader(Iterator<CSVRecord> iterator) {
        if (iterator == null) {
            log.warn("iterator is null.");
            return null;
        }
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            log.error("File is empty. Not parsing any records.");
            System.exit(1);
        }
        return null;
    }

    protected static Iterable<CSVRecord> getCSVRecords(Reader in) {
        if (in == null) {
            log.warn("in is null.");
            return null;
        }
        try {
            return CSVFormat.EXCEL.parse(in);
        } catch (IOException ex) {
            log.error("File cannot be parsed as csv.");
            System.exit(1);
        }
        return null;
    }

    protected static Reader getReader(CommandLine cmd) {
        log.debug("Reading CSV file.");
        if (cmd == null) {
            log.warn("cmd is null.");
            return null;
        }
        while (true) {
            String fileName = cmd.hasOption(Cmd.CSV_FILE.getName())
                    ? cmd.getOptionValue(Cmd.CSV_FILE.getName())
                    : System.console().readLine("Name of the CSV File to read: ");
            String userDir = System.getProperty("user.dir");
            File file = new File(userDir + File.separatorChar + fileName);
            try {
                InputStream is = new FileInputStream(file);
                log.info("Succesfully read CSV file.");
                return new InputStreamReader(is);
            } catch (FileNotFoundException ex) {
                log.error("File cannot be found: " + file.getAbsolutePath());
                System.exit(1);
            }
        }
    }

    protected static ProcessDeploymentInfo getSelectedProcess(
            List<ProcessDeploymentInfo> processList,
            CommandLine cmd) {
        if (cmd == null) {
            log.warn("cmd is null.");
            return null;
        }
        if (cmd.hasOption(Cmd.PROCESS_NAME.getName())
                && cmd.hasOption(Cmd.PROCESS_VERSION.getName())) {
            for (ProcessDeploymentInfo process : processList) {
                if (process.getName().equals(
                        cmd.getOptionValue(Cmd.PROCESS_NAME.getName()))
                        && process.getVersion().equals(
                                cmd.getOptionValue(Cmd.PROCESS_VERSION.getName()))) {
                    return process;
                }
            }
        }

        for (int i = 0; i < processList.size(); i++) {
            log.info(i + 1 + ". " + processList.get(i).getName()
                    + " (" + processList.get(i).getVersion() + ")");
        }
        int processId = 0;
        do {
            processId = tryParse(getConsoleInput("Select process",
                    Integer.toString(processId + 1))) - 1;
        } while (processId < 0 || processId >= processList.size());
        return processList.get(processId);
    }

    protected static List<ProcessDeploymentInfo> getProcessList(
            ProcessAPI processAPI) {
        log.debug("Retrieving process list.");
        SearchOptions searchOptions
                = new SearchOptionsBuilder(0, NUMBER_OF_PROCESSES)
                .sort(ProcessDeploymentInfoSearchDescriptor.DEPLOYMENT_DATE,
                        Order.DESC)
                .done();
        try {
            List<ProcessDeploymentInfo> deploymentInfoResults = null;
            if (processAPI != null) {
                deploymentInfoResults
                        = processAPI.searchProcessDeploymentInfos(
                                searchOptions).getResult();
            }
            log.debug("Successfully retrieved process list.");
            return deploymentInfoResults;
        } catch (SearchException ex) {
            log.error("Search process throws an exception.");
            log.error(ex.getMessage());
            System.exit(1);
        }
        return null;
    }

    protected static ProcessAPI getProcessAPI(APISession apiSession) {
        log.debug("Connecting to ProcessAPI.");
        try {
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            log.info("Succesfully connected to ProcessAPI.");
            return processAPI;
        } catch (BonitaHomeNotSetException | ServerAPIException |
                UnknownAPITypeException ex) {
            log.error("Error creating the Process API.");
            System.exit(1);
        }
        return null;
    }

    protected static APISession getAPISession(LoginAPI loginAPI,
            String userName, char[] password) {
        if (loginAPI == null) {
            log.warn("loginAPI is null.");
            return null;
        }
        log.debug("Logging into the bonita server.");
        try {
            APISession apiSession = loginAPI.login(userName,
                    new String(password));
            log.info("Succesfully logged into server.");
            return apiSession;
        } catch (LoginException ex) {
            log.error("Cannot loging with credentials");
            System.exit(1);
        }
        return null;
    }

    protected static LoginAPI getLoginAPI(String serverUrl,
            String applicationName) {
        log.debug("Connecting to server.");
        Map<String, String> settings = new HashMap<>();
        settings.put("server.url", serverUrl);
        settings.put("application.name", applicationName);
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, settings);
        try {
            LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
            log.info("Succesfully connected to server.");
            return loginAPI;
        } catch (BonitaHomeNotSetException | ServerAPIException |
                UnknownAPITypeException ex) {
            log.error("Error creating the Login API");
            System.exit(1);
        }
        return null;
    }

    protected static String getConsoleInput(String displayText,
            String defaultValue) {
        return getConsoleInput(displayText, defaultValue, null, null);
    }

    protected static String getConsoleInput(String displayText,
            String defaultValue, CommandLine cmd, String optionName) {
        if (cmd == null) {
            log.warn("cmd is null.");
            return null;
        }
        if (cmd != null && cmd.hasOption(optionName)) {
            return cmd.getOptionValue(optionName);
        } else {
            Console console = System.console();
            String consoleInput = console.readLine(displayText
                    + " (" + defaultValue + "): ");
            if (!consoleInput.trim().isEmpty()) {
                return consoleInput.trim();
            } else {
                return defaultValue;
            }
        }
    }

    protected static int tryParse(Object obj) {
        Integer retVal;
        try {
            retVal = Integer.parseInt((String) obj);
        } catch (NumberFormatException nfe) {
            retVal = 0;
        }
        return retVal;
    }

    protected static String getHeader(String fullHeaderValue) {
        if (fullHeaderValue != null && !fullHeaderValue.isEmpty()) {
            Matcher matcher = Pattern.compile("(.*?)[\\s\\(]")
                    .matcher(fullHeaderValue);
            matcher.find();
            String header = matcher.group(1);
            if (!header.isEmpty()) {
                return header;
            } else {
                log.warn("No header value found in: " + fullHeaderValue);
                return null;
            }
        } else {
            log.warn("No or empty header received.");
            return null;
        }
    }

    protected static String getHeaderType(String fullHeaderValue) {
        if (fullHeaderValue != null && !fullHeaderValue.isEmpty()) {
            Matcher matcher = Pattern.compile("\\((.*?)\\)")
                    .matcher(fullHeaderValue);
            matcher.find();
            String headerType = matcher.group(1).toUpperCase().trim();
            if (!headerType.isEmpty()) {
                return headerType;
            } else {
                log.warn("No header type found in: " + fullHeaderValue);
                return null;
            }
        } else {
            log.warn("No or empty header received.");
            return null;
        }
    }

    protected static CommandLine parseArguments(String[] args) {
        Options options = new Options();

        Option serverUrl = new Option("s", Cmd.SERVER_URL.getName(),
                true, "URL of the Bonita BPM Server.");
        options.addOption(serverUrl);

        Option applicationName = new Option("a", Cmd.APPLICATION_NAME.getName(),
                true, "Name of the Bonita BPM application.");
        options.addOption(applicationName);

        Option username = new Option("u", Cmd.USERNAME.getName(),
                true, "Username of the Bonita BPM user.");
        options.addOption(username);

        Option password = new Option("p", Cmd.PASSWORD.getName(),
                true, "Password of the Bonita BPM user.");
        options.addOption(password);

        Option processName = new Option("n", Cmd.PROCESS_NAME.getName(),
                true, "Name of the Bonita BPM Process");
        options.addOption(processName);

        Option procesVersion = new Option("v", Cmd.PROCESS_VERSION.getName(),
                true, "Version of the Bonita BPM Process");
        options.addOption(procesVersion);

        Option inputVariable = new Option("i", Cmd.INPUT_VARIABLE.getName(),
                true, "Input variable neede to start Bonita BPM Process.");
        options.addOption(inputVariable);

        Option csvFilename = new Option("c", Cmd.CSV_FILE.getName(),
                true, "Filename of the CSV file.");
        options.addOption(csvFilename);

        Option help = new Option("h", Cmd.HELP.getName(), false,
                "Display help information.");
        options.addOption(help);

        Option talkative = new Option("t", Cmd.TALKATIVE.getName(), false,
                "Show talkative logging.");
        options.addOption(talkative);

        Option quiet = new Option("q", Cmd.QUIET.getName(), false,
                "No logging infomation in shown.");
        options.addOption(quiet);

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException ex) {
            log.error(ex.getMessage());
        }
        if (commandLine != null && !commandLine.hasOption(Cmd.HELP.getName())) {
            if (commandLine.hasOption(Cmd.TALKATIVE.getName())) {
                System.setProperty(
                        SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
            }
            if (commandLine.hasOption(Cmd.QUIET.getName())) {
                System.setProperty(
                        SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
            }
            log = LoggerFactory.getLogger(Main.class);
            return commandLine;
        } else {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("bonita-importfile", options);
            System.exit(1);
        }

        return null;
    }

}
