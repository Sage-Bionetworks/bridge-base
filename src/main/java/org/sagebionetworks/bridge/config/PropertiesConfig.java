package org.sagebionetworks.bridge.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Config backed by Java properties.
 * 
 * CHANGE: 2020-12-08 to test publication to S3 repository.
 */
public class PropertiesConfig implements Config {
    
    private static final PropertyPlaceholderHelper RESOLVER = new PropertyPlaceholderHelper("${", "}");
    
    /**
     * Default user when user is not specified in the config.
     */
    public static final String DEFAULT_USER = StandardSystemProperty.USER_NAME.value();

    /**
     * Default environment when environment is not specified in the config.
     */
    public static final Environment DEFAULT_ENV = Environment.LOCAL;

    /**
     * Comma surrounded by optional whitespace. Default delimiter to separate a list of values.
     */
    public static final String DEFAULT_LIST_DELIMITER = "\\s*,\\s*";

    static final String USER_KEY = "bridge.user";
    static final String ENV_KEY = "bridge.env";

    private final String user;
    private final Environment environment;
    private final Properties properties;
    private final Pattern delimiter;


    /**
     * Loads config from a template file.
     * <p>
     * The config template should contain all the config entries. The values can be default values,
     * or, if sensitive, dummy values. The values on the template file are to be overwritten by
     * environment variables and/or system properties in that order.
     * <p>
     * The config template file can be kept in the source code. Then its path can be obtained by
     * <code>Paths.get(getClass().getClassLoader().getResource("relative/path/to/config").getPath());</code>.
     *
     * @param configTemplate
     *            Path to the config template file. 
     */
    public PropertiesConfig(final Path configTemplate) throws IOException {
        this(configTemplate, null, DEFAULT_LIST_DELIMITER);
    }

    public PropertiesConfig(final String configTemplate) throws IOException {
        this(configTemplate, null, DEFAULT_LIST_DELIMITER);
    }

    /**
     * Loads config from a template file and a local config file.
     * <p>
     * The config template should contain all the config entries. The values can be default values,
     * or, if sensitive, dummy values. The values on the template file are to be overwritten by
     * those of the local config file, environment variables and/or system properties in that order.
     * <p>
     * The config template file can be kept in the source code. Then its path can be obtained by
     * <code>Paths.get(getClass().getClassLoader().getResource("relative/path/to/config").getPath());</code>.
     * <p>
     * The local config file can be kept in the user's home directory. Then its path can be obtained
     * by <code>Paths.get(System.getProperty("user.home") + "/path/to/config");</code>.
     *
     * @param configTemplate
     *            Path to the config template file in the source code.
     *
     * @param userConfig
     *            Path to the local config file.
     */
    public PropertiesConfig(final Path configTemplate, final Path userConfig) throws IOException {
        this(configTemplate, userConfig, DEFAULT_LIST_DELIMITER);
    }

    public PropertiesConfig(final String configTemplate, final Path userConfig) throws IOException {
        this(configTemplate, userConfig, DEFAULT_LIST_DELIMITER);
    }

    /**
     * Loads config from a template file and a local config file.
     * <p>
     * The config template should contain all the config entries. The values can be default values,
     * or, if sensitive, dummy values. The values on the template file are to be overwritten by
     * those of the local config file, environment variables and/or system properties in that order.
     * <p>
     * The config template file can be kept in the source code. Then its path can be obtained by
     * <code>Paths.get(getClass().getClassLoader().getResource("relative/path/to/config").getPath());</code>.
     * <p>
     * The local config file can be kept in the user's home directory. Then its path can be obtained
     * by <code>Paths.get(System.getProperty("user.home") + "/path/to/config");</code>.
     *
     * @param configTemplate
     *            Path to the config template file in the source code.
     *
     * @param localConfig
     *            Path to the local config file.
     *
     * @param delimiterRegex
     *            The regular expression for the delimiter that is used to
     *            separate a list of values.
     */
    public PropertiesConfig(final Path configTemplate, final Path localConfig,
            final String delimiterRegex) throws IOException {
        this(setupPropertiesFromPath(configTemplate), localConfig, delimiterRegex);
    }

    public PropertiesConfig(final String configTemplate, final Path localConfig,
                            final String delimiterRegex) throws IOException {
        this(setupPropertiesFromString(configTemplate), localConfig, delimiterRegex);
    }

    public PropertiesConfig(final Properties properties, final Path localConfig, final String delimiterRegex) throws IOException {
        checkNotNull(properties);
        checkNotNull(delimiterRegex);

        if (localConfig != null) {
            if (Files.exists(localConfig)) {
                properties.load(Files.newBufferedReader(localConfig, StandardCharsets.UTF_8));
            }
        }

        user = readUser(properties);
        environment = readEnvironment(properties);
        this.properties = new Properties(collapse(properties, environment.name().toLowerCase()));
        delimiter = Pattern.compile(delimiterRegex);
    }

    private static Properties setupPropertiesFromPath (final Path configTemplate) throws IOException {
        final Properties properties = new Properties();
        String path = configTemplate.toString();
        // For executable jar files, Spring Boot creates a jar with embedded jar files that is not
        // a standard format, and its URIs seem to reference a propriety FileSystem implementation.
        // However, we can apparently sidestep this with some parsing of the URI.
        if (path.contains("BOOT-INF")) {
            path = path.substring(path.indexOf("/BOOT-INF")).replaceAll("!", "");
            try (final InputStream in = PropertiesConfig.class.getResourceAsStream(path)) {
                properties.load(in);
            }
        } else {
            try (Reader templateReader = Files.newBufferedReader(configTemplate, StandardCharsets.UTF_8)) {
                properties.load(templateReader);
            }            
        }
        return properties;
    }

    private static Properties setupPropertiesFromString(final String configTemplate) throws IOException {
        Resource resource = new ClassPathResource(configTemplate);
        Properties properties = PropertiesLoaderUtils.loadProperties(resource);
        return properties;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String get(final String key) {
        checkNotNull(key);
        
        String value = properties.getProperty(key);
        return (value == null ) ? value : RESOLVER.replacePlaceholders(value, properties);
    }

    @Override
    public void set(String key, String value) {
        checkNotNull(key);
        checkNotNull(value);
        properties.setProperty(key, value);
    }

    @Override
    public int getInt(final String key) {
        checkNotNull(key);
        return Integer.parseInt(get(key));
    }

    @Override
    public List<String> getList(final String key) {
        checkNotNull(key);
        final String property = get(key);
        return Lists.newArrayList(delimiter.split(property));
    }

    private final ConfigReader envReader = new ConfigReader() {
        @Override
        public String read(String name) {
            try {
                // Change to a valid environment variable name
                name = name.toUpperCase().replace('.', '_');
                return System.getenv(name);
            } catch (SecurityException e) {
                throw new GetEnvException(name, e);
            }
        }
    };

    private final ConfigReader sysReader = new ConfigReader() {
        @Override
        public String read(String name) {
            try {
                return System.getProperty(name);
            } catch (SecurityException e) {
                throw new GetSystemPropertyException(name, e);
            }
        }
    };

    private String readUser(final Properties properties) {
        String user = read(USER_KEY, properties);
        if (Strings.isNullOrEmpty(user)) {
            return DEFAULT_USER;
        }
        return user;
    }

    private Environment readEnvironment(final Properties properties) {
        final String envName = read(ENV_KEY, properties);
        if (Strings.isNullOrEmpty(envName)) {
            return DEFAULT_ENV;
        }
        for (Environment env : Environment.values()) {
            if (env.name().toLowerCase().equals(envName)) {
                return env;
            }
        }
        throw new InvalidEnvironmentException(envName);
    }

    private String read(final String key, final Properties properties) {
        final String envVal = envReader.read(key);
        if (envVal != null) {
            return envVal;
        }
        final String sysVal = sysReader.read(key);
        if (sysVal != null) {
            return sysVal;
        }
        return properties.getProperty(key);
    }

    /**
     * Collapses the properties into new properties relevant to the current
     * environment. 1) Default properties usually bundled in the code base as
     * resources. 2) Overwrite with properties read from the user's home
     * directory 3) Merge the properties to the current environment 4) Overwrite
     * with properties read from the environment variables. 5) Overwrite with
     * properties read from the command-line arguments.
     */
    private Properties collapse(final Properties properties, final String envName) {
        Properties collapsed = new Properties();
        // Read the default properties
        for (final String key : properties.stringPropertyNames()) {
            if (isDefaultProperty(key)) {
                collapsed.setProperty(key, properties.getProperty(key));
            }
        }
        // Overwrite with properties for the current environment
        for (final String key : properties.stringPropertyNames()) {
            if (key.startsWith(envName + ".")) {
                String strippedName = key.substring(envName.length() + 1);
                collapsed.setProperty(strippedName, properties.getProperty(key));
            }
        }
        // Overwrite with environment variables and system properties
        for (final String key : properties.stringPropertyNames()) {
            String value = envReader.read(key);
            if (value == null) {
                value = sysReader.read(key);
            }
            if (value != null) {
                if (key.startsWith(envName + ".")) {
                    String strippedName = key.substring(envName.length() + 1);
                    collapsed.setProperty(strippedName, value);
                } else {
                    collapsed.setProperty(key, value);
                }
            }
        }
        return collapsed;
    }

    /**
     * When the property is not bound to a particular environment.
     */
    private boolean isDefaultProperty(final String propName) {
        for (Environment env : Environment.values()) {
            String envPrefix = env.name().toLowerCase() + ".";
            if (propName.startsWith(envPrefix)) {
                return false;
            }
        }
        return true;
    }
}
