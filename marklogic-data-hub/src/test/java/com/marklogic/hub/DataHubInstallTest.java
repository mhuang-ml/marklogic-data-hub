package com.marklogic.hub;

import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.ext.modulesloader.impl.PropertiesModuleManager;
import com.marklogic.hub.util.Versions;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataHubInstallTest extends HubTestBase {

    @BeforeClass
    public static void setup() {
        XMLUnit.setIgnoreWhitespace(true);
        uninstallHub();
        installHub();
    }

    @Test
    public void testInstallHubModules() throws IOException {
        assertTrue(getDataHub().isInstalled().isInstalled());

        assertTrue(getModulesFile("/com.marklogic.hub/lib/config.xqy").startsWith(getResource("data-hub-test/core-modules/config.xqy")));
        int totalCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, null);
        int hubModulesCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, "hub-core-module");
        //As a note, whenever you see the 83 || 63, it's due to the additional building of the javascript files bundling down that will then get
        //deployed with the rest of the modules code. This means it'll be 20 higher than if the trace UI was never built
        assertTrue(totalCount + " is not correct", 83 == totalCount || 63 == totalCount);
        assertTrue(hubModulesCount + "  is not correct", 41 == hubModulesCount || 21 == hubModulesCount);

        assertTrue("trace options not installed", getModulesFile("/Default/data-hub-TRACING/rest-api/options/traces.xml").length() > 0);
        assertTrue("trace options not installed", getModulesFile("/Default/data-hub-JOBS/rest-api/options/jobs.xml").length() > 0);
        assertTrue("trace options not installed", getModulesFile("/Default/data-hub-STAGING/rest-api/options/default.xml").length() > 0);
        assertTrue("trace options not installed", getModulesFile("/Default/data-hub-FINAL/rest-api/options/default.xml").length() > 0);
    }

    @Test
    public void getHubModulesVersion() throws IOException {
        String version = getHubConfig().getJarVersion();
        assertEquals(version, new Versions(getHubConfig()).getHubVersion());
    }

    @Test
    public void testInstallUserModules() throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        URL url = DataHubInstallTest.class.getClassLoader().getResource("data-hub-test");
        String path = Paths.get(url.toURI()).toFile().getAbsolutePath();

        HubConfig hubConfig = getHubConfig(path);

        int totalCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, null);
        assertTrue(totalCount + " is not correct", 83 == totalCount || 63 == totalCount);

        installUserModules(hubConfig, true);

        totalCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, null);
        assertTrue(totalCount + " is not correct", 83 == totalCount || 103 == totalCount);

        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/collector.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/collector.xqy"));

        EvalResultIterator resultItr = runInModules(
            "xquery version \"1.0-ml\";\n" +
                "import module namespace sec=\"http://marklogic.com/xdmp/security\" at \n" +
                "    \"/MarkLogic/security.xqy\";\n" +
                "let $perms := xdmp:document-get-permissions('/entities/test-entity/harmonize/final/collector.xqy')\n" +
                "return\n" +
                "  fn:string-join(" +
                "    for $x in xdmp:invoke-function(function() {\n" +
                "      sec:get-role-names($perms/sec:role-id) ! fn:string()\n" +
                "    }," +
                "    map:entry(\"database\", xdmp:security-database())" +
                "    )" +
                "    order by $x ascending" +
                "    return $x, \",\")");
        EvalResult res = resultItr.next();
        assertEquals("data-hub-role,rest-admin,rest-reader,rest-writer", res.getString());

        assertEquals(
            getResource("data-hub-test/plugins/my-lib.xqy"),
            getModulesFile("/my-lib.xqy"));

        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/main.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/main.xqy"));


        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/content.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/content.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/headers.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/headers.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/triples.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/triples.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/writer.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/writer.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/final/main.xqy"),
            getModulesFile("/entities/test-entity/harmonize/final/main.xqy"));

        assertXMLEqual(
            getXmlFromResource("data-hub-test/final.xml"),
            getModulesDocument("/entities/test-entity/harmonize/final/final.xml"));


        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/input/hl7/content.xqy"),
            getModulesFile("/entities/test-entity/input/hl7/content.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/input/hl7/headers.xqy"),
            getModulesFile("/entities/test-entity/input/hl7/headers.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/input/hl7/triples.xqy"),
            getModulesFile("/entities/test-entity/input/hl7/triples.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/input/hl7/writer.xqy"),
            getModulesFile("/entities/test-entity/input/hl7/writer.xqy"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/input/hl7/main.xqy"),
            getModulesFile("/entities/test-entity/input/hl7/main.xqy"));
        assertXMLEqual(
            getXmlFromResource("data-hub-test/hl7.xml"),
            getModulesDocument("/entities/test-entity/input/hl7/hl7.xml"));

        assertXMLEqual(
            getXmlFromResource("data-hub-test/plugins/entities/test-entity/input/REST/options/doctors.xml"),
            getModulesDocument("/Default/" + HubConfig.DEFAULT_STAGING_NAME + "/rest-api/options/doctors.xml"));

        assertXMLEqual(
            getXmlFromResource("data-hub-test/plugins/entities/test-entity/harmonize/REST/options/patients.xml"),
            getModulesDocument("/Default/" + HubConfig.DEFAULT_FINAL_NAME + "/rest-api/options/patients.xml"));

        assertXMLEqual(
            getXmlFromResource("data-hub-helpers/test-conf-metadata.xml"),
            getModulesDocument("/marklogic.rest.transform/test-conf-transform/assets/metadata.xml"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/harmonize/REST/transforms/test-conf-transform.xqy"),
            getModulesFile("/marklogic.rest.transform/test-conf-transform/assets/transform.xqy"));

        assertXMLEqual(
            getXmlFromResource("data-hub-helpers/test-input-metadata.xml"),
            getModulesDocument("/marklogic.rest.transform/test-input-transform/assets/metadata.xml"));
        assertEquals(
            getResource("data-hub-test/plugins/entities/test-entity/input/REST/transforms/test-input-transform.xqy"),
            getModulesFile("/marklogic.rest.transform/test-input-transform/assets/transform.xqy"));

        String timestampFile = hubConfig.getUserModulesDeployTimestampFile();
        PropertiesModuleManager propsManager = new PropertiesModuleManager(timestampFile);
        propsManager.initialize();
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/my-lib.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/harmonize/final/content.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/harmonize/final/headers.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/harmonize/final/triples.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/harmonize/final/writer.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/harmonize/final/main.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/hl7/content.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/hl7/headers.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/hl7/triples.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/hl7/writer.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/hl7/main.xqy")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/REST/options/doctors.xml")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/harmonize/REST/options/patients.xml")));
        assertFalse(propsManager.hasFileBeenModifiedSinceLastLoaded(getResourceFile("data-hub-test/plugins/entities/test-entity/input/REST/transforms/test-input-transform.xqy")));
    }

    @Test
    public void testClearUserModules() throws URISyntaxException {
        URL url = DataHubInstallTest.class.getClassLoader().getResource("data-hub-test");
        String path = Paths.get(url.toURI()).toFile().getAbsolutePath();
        HubConfig hubConfig = getHubConfig(path);
        DataHub dataHub = new DataHub(hubConfig);
        dataHub.clearUserModules();

        int totalCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, null);
        assertTrue(totalCount + " is not correct", 83 == totalCount || 63 == totalCount);

        installUserModules(hubConfig, true);

        totalCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, null);
        assertTrue(totalCount + " is not correct", 83 == totalCount || 103 == totalCount);

        dataHub.clearUserModules();

        totalCount = getDocCount(HubConfig.DEFAULT_MODULES_DB_NAME, null);
        assertTrue(totalCount + " is not correct", 83 == totalCount || 63 == totalCount);

    }
}
