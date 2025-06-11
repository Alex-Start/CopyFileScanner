package settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {
    private static final Logger logger = LogManager.getLogger(PropertyReader.class);
    private static final String CONFIG_PROPERTIES = "/config.properties";

    public static int getPropertyAsInteger(String field, int defaultValue) {
        try {
            return Integer.parseInt(getPropertyAsString(field, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getPropertyAsBoolean(String field, boolean defaultValue) {
        return Boolean.parseBoolean(getPropertyAsString(field, String.valueOf(defaultValue)));
    }

    public static String getPropertyAsString(String field, String defaultValue) {
        Properties properties = new Properties();
        try (InputStream input = PropertyReader.class.getResourceAsStream(CONFIG_PROPERTIES)) {
            properties.load(input);
            return properties.getProperty(field, defaultValue);
        } catch (IOException e) {
            logger.error("Error reading property file: {}", e.getMessage());
            return defaultValue; // Default value in case of error
        }
    }

}
