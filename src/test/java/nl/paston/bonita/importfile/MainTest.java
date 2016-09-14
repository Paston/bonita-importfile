/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.paston.bonita.importfile;

import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.Assert;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.csv.CSVRecord;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.session.APISession;
import org.junit.Before;

/**
 *
 * @author marti
 */
public class MainTest {

    private final String fullHeaderValue1 = "polKey (STRING)";
    private final String fullHeaderValue2 = "polKey(STRING)";
    private final String fullHeaderValue3 = "polKey ( STRING )";
    private final String fullHeaderValue4 = "polKey( String )";

    @Before
    public void setUp() throws Exception {
        Main.parseArguments(new String[0]);
    }
    
    @Test
    public void testGetHeader() {
        Assert.assertEquals("polKey", Main.getHeader(fullHeaderValue1));
        Assert.assertEquals("polKey", Main.getHeader(fullHeaderValue2));
        Assert.assertEquals("polKey", Main.getHeader(fullHeaderValue3));
        Assert.assertEquals("polKey", Main.getHeader(fullHeaderValue4));
    }

    @Test
    public void testGetHeaderType() {
        Assert.assertEquals("STRING", Main.getHeaderType(fullHeaderValue1));
        Assert.assertEquals("STRING", Main.getHeaderType(fullHeaderValue2));
        Assert.assertEquals("STRING", Main.getHeaderType(fullHeaderValue3));
        Assert.assertEquals("STRING", Main.getHeaderType(fullHeaderValue4));
    }

    @Test
    public void testParseRecord() {
        System.out.println("parseRecord");
        CSVRecord record = null;
        CSVRecord fullHeader = null;
        Map<Object, Object> expResult = null;
        Map<Object, Object> result = Main.parseRecord(record, fullHeader);
    }

    @Test
    public void testPushRecordToBonita() {
        System.out.println("pushRecordToBonita");
        ProcessAPI processAPI = null;
        ProcessDeploymentInfo info = null;
        String bonitaInputVar = "";
        Map<Object, Object> map = null;
        Main.pushRecordToBonita(processAPI, info, bonitaInputVar, map);
    }

    @Test
    public void testGetRecordObject() {
        System.out.println("getRecordObject");
        String headerType = "";
        String stringValue = "";
        Object expResult = null;
        Object result = Main.getRecordObject(headerType, stringValue);
    }

    @Test
    public void testGetFullHeader() {
        System.out.println("getFullHeader");
        Iterator<CSVRecord> iterator = null;
        CSVRecord expResult = null;
        CSVRecord result = Main.getFullHeader(iterator);
    }

    @Test
    public void testGetCSVRecords() {
        System.out.println("getCSVRecords");
        Reader in = null;
        Iterable<CSVRecord> expResult = null;
        Iterable<CSVRecord> result = Main.getCSVRecords(in);
    }

    @Test
    public void testGetReader() {
        System.out.println("getReader");
        CommandLine cmd = null;
        Reader expResult = null;
        Reader result = Main.getReader(cmd);
    }

    @Test
    public void testGetSelectedProcess() {
        System.out.println("getSelectedProcess");
        List<ProcessDeploymentInfo> processList = null;
        CommandLine cmd = null;
        ProcessDeploymentInfo expResult = null;
        ProcessDeploymentInfo result = Main.getSelectedProcess(processList, cmd);
    }

    @Test
    public void testGetProcessList() {
        System.out.println("getProcessList");
        ProcessAPI processAPI = null;
        List<ProcessDeploymentInfo> expResult = null;
        List<ProcessDeploymentInfo> result = Main.getProcessList(processAPI);
    }

    @Test
    public void testGetProcessAPI() {
        System.out.println("getProcessAPI");
        APISession apiSession = null;
        ProcessAPI expResult = null;
        ProcessAPI result = Main.getProcessAPI(apiSession);
    }

    @Test
    public void testGetAPISession() {
        System.out.println("getAPISession");
        LoginAPI loginAPI = null;
        String userName = "";
        char[] password = null;
        APISession expResult = null;
        APISession result = Main.getAPISession(loginAPI, userName, password);
    }

    @Test
    public void testGetLoginAPI() {
        System.out.println("getLoginAPI");
        String serverUrl = "";
        String applicationName = "";
        LoginAPI expResult = null;
        LoginAPI result = Main.getLoginAPI(serverUrl, applicationName);
    }

    @Test
    public void testGetConsoleInput_String_String() {
        System.out.println("getConsoleInput");
        String displayText = "";
        String defaultValue = "";
        String expResult = "";
        String result = Main.getConsoleInput(displayText, defaultValue);
    }

    @Test
    public void testGetConsoleInput_4args() {
        System.out.println("getConsoleInput");
        String displayText = "";
        String defaultValue = "";
        CommandLine cmd = null;
        String optionName = "";
        String expResult = "";
        String result = Main.getConsoleInput(displayText, defaultValue, cmd, optionName);
    }

    @Test
    public void testTryParse() {
        System.out.println("tryParse");
        Object obj = null;
        int expResult = 0;
        int result = Main.tryParse(obj);
    }

}

