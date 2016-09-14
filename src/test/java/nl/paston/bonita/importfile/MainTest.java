/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.paston.bonita.importfile;

import org.junit.Test;
import org.junit.Assert;
import static nl.paston.bonita.importfile.Main.*;

/**
 *
 * @author marti
 */
public class MainTest {

    private final String fullHeaderValue1 = "polKey (STRING)";
    private final String fullHeaderValue2 = "polKey(STRING)";
    private final String fullHeaderValue3 = "polKey ( STRING )";
    private final String fullHeaderValue4 = "polKey( String )";

    @Test
    public void testGetHeader() {
        Assert.assertEquals("polKey", getHeader(fullHeaderValue1));
        Assert.assertEquals("polKey", getHeader(fullHeaderValue2));
        Assert.assertEquals("polKey", getHeader(fullHeaderValue3));
        Assert.assertEquals("polKey", getHeader(fullHeaderValue4));
    }

    @Test
    public void testGetHeaderType() {
        Assert.assertEquals("STRING", getHeaderType(fullHeaderValue1));
        Assert.assertEquals("STRING", getHeaderType(fullHeaderValue2));
        Assert.assertEquals("STRING", getHeaderType(fullHeaderValue3));
        Assert.assertEquals("STRING", getHeaderType(fullHeaderValue4));
    }

}

