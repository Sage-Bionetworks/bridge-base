package org.sagebionetworks.bridge.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PropertiesConfigTest {

    private Path configFile;
    private static final String TEST_CONF_FILE = "conf/bridge.conf";

    @BeforeMethod
    public void before() throws URISyntaxException {
        configFile = Paths.get(getClass().getClassLoader().getResource("conf/bridge.conf").toURI());
    }

    @AfterMethod
    public void after() {
        System.clearProperty(PropertiesConfig.USER_KEY);
        System.clearProperty(PropertiesConfig.ENV_KEY);
    }

    @Test
    public void testGetUser() throws IOException {
        System.setProperty(PropertiesConfig.USER_KEY, "testGetUser");
        Config config = new PropertiesConfig(configFile);
        assertEquals(config.getUser(), "testGetUser");
    }

    @Test
    public void testGetDefaultUser() throws IOException {
        Config config = new PropertiesConfig(configFile);
        assertEquals(config.getUser(), PropertiesConfig.DEFAULT_USER);
    }

    @Test
    public void testGetEnvironment() throws IOException {
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        Config config = new PropertiesConfig(configFile);
        assertEquals(config.getEnvironment(), Environment.DEV);
    }

    @Test
    public void testGetDefaultEnvironment() throws IOException {
        Config config = new PropertiesConfig(configFile);
        assertEquals(config.getEnvironment(), PropertiesConfig.DEFAULT_ENV);
    }

    @Test(expectedExceptions=InvalidEnvironmentException.class)
    public void testGetInvalidEnvironment() throws IOException {
        System.setProperty(PropertiesConfig.ENV_KEY, "InvalidEnvironment");
        new PropertiesConfig(configFile);
    }

    @Test
    public void testGet() throws IOException {
        Config config = new PropertiesConfig(configFile);
        assertEquals(config.get("example.property"), "example.value");
    }

    @Test
    public void testGetEnvSpecific() throws IOException {
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        Config config = new PropertiesConfig(configFile);
        assertEquals(config.get("example.property"), "example.value.for.dev");
    }

    @Test
    public void testGetNull() throws IOException {
        Config config = new PropertiesConfig(configFile);
        assertNull(config.get("someFakePropertyThatDoesNotExist"));
    }

    @Test
    public void testGetInt() throws IOException {
        Config config = new PropertiesConfig(configFile, configFile);
        assertEquals(config.getInt("example.timeout"), 2000);
    }

    @Test
    public void testGetUserFromString() throws IOException {
        System.setProperty(PropertiesConfig.USER_KEY, "testGetUser");
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertEquals(config.getUser(), "testGetUser");
    }

    @Test
    public void testGetDefaultUserFromString() throws IOException {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertEquals(config.getUser(), PropertiesConfig.DEFAULT_USER);
    }

    @Test
    public void testGetEnvironmentFromString() throws IOException {
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertEquals(config.getEnvironment(), Environment.DEV);
    }

    @Test
    public void testGetDefaultEnvironmentFromString() throws IOException {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertEquals(config.getEnvironment(), PropertiesConfig.DEFAULT_ENV);
    }

    @Test(expectedExceptions=InvalidEnvironmentException.class)
    public void testGetInvalidEnvironmentFromString() throws IOException {
        System.setProperty(PropertiesConfig.ENV_KEY, "InvalidEnvironment");
        new PropertiesConfig(TEST_CONF_FILE);
    }

    @Test
    public void testGetFromString() throws IOException {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertEquals(config.get("example.property"), "example.value");
    }

    @Test
    public void testGetEnvSpecificFromString() throws IOException {
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertEquals(config.get("example.property"), "example.value.for.dev");
    }

    @Test
    public void testGetNullFromString() throws IOException {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        assertNull(config.get("someFakePropertyThatDoesNotExist"));
    }

    @Test
    public void testGetIntFromStringPath() throws IOException {
        Config config = new PropertiesConfig(TEST_CONF_FILE, configFile);
        assertEquals(config.getInt("example.timeout"), 2000);
    }

    @Test
    public void testGetList() throws IOException {
        Config config = new PropertiesConfig(configFile, configFile);
        List<String> list = config.getList("example.property");
        assertNotNull(list);
        assertEquals(list.size(), 1);
        assertEquals(list.get(0), "example.value");
        list = config.getList("example.list");
        assertNotNull(list);
        assertEquals(list.size(), 4);
        assertEquals(list.get(0), "a");
        assertEquals(list.get(1), "bc");
        assertEquals(list.get(2), "d");
        assertEquals(list.get(3), "e");
    }

    @Test
    public void testGetListFromStringPath() throws IOException {
        Config config = new PropertiesConfig(TEST_CONF_FILE, configFile);
        List<String> list = config.getList("example.property");
        assertNotNull(list);
        assertEquals(list.size(), 1);
        assertEquals(list.get(0), "example.value");
        list = config.getList("example.list");
        assertNotNull(list);
        assertEquals(list.size(), 4);
        assertEquals(list.get(0), "a");
        assertEquals(list.get(1), "bc");
        assertEquals(list.get(2), "d");
        assertEquals(list.get(3), "e");
    }

    @Test
    public void envSpecificPropertyOverwritesProperty() throws IOException, URISyntaxException {
        Path localPath = Paths.get(getClass().getClassLoader().getResource("conf/local.conf").toURI());
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        Config config = new PropertiesConfig(TEST_CONF_FILE, localPath);
        assertEquals(config.get("example.property"), "local.value.for.dev");
    }

    @Test
    public void envVariableOverwritesProperty() throws IOException, URISyntaxException {
        Path localPath = Paths.get(getClass().getClassLoader().getResource("conf/local.conf").toURI());
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        System.setProperty("example.property", "override.value.for.dev");
        Config config = new PropertiesConfig(TEST_CONF_FILE, localPath);
        assertEquals(config.get("example.property"), "override.value.for.dev");
        System.clearProperty("example.property");
    }

    @Test
    public void envVariableOverwritesEnvSpecificProperty() throws IOException, URISyntaxException {
        Path localPath = Paths.get(getClass().getClassLoader().getResource("conf/local.conf").toURI());
        System.setProperty(PropertiesConfig.ENV_KEY, "dev");
        System.setProperty("dev.example.property", "override.value.for.dev");
        Config config = new PropertiesConfig(TEST_CONF_FILE, localPath);
        assertEquals(config.get("example.property"), "override.value.for.dev");
        System.clearProperty("dev.example.property");
    }
    
    @Test
    public void testInterpolation() throws Exception {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        
        assertEquals(config.get("example.interpolated"), "This value should be 2000");
    }
    
    @Test
    public void testNoInterpolation() throws Exception {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        
        assertEquals(config.get("example.no.interpolation"), "This value should be ${no.value}");
    }

    @Test
    public void testNestedInterpolation() throws Exception {
        Config config = new PropertiesConfig(TEST_CONF_FILE);
        
        assertEquals(config.get("example.nested.interpolation"), "Testing This value should be 2000");
    }
}
