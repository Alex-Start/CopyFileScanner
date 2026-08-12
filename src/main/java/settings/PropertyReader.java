package settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {
    private static final Logger logger = LogManager.getLogger(PropertyReader.class);
    private static final String CONFIG_PROPERTIES = "/config.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = PropertyReader.class.getResourceAsStream(CONFIG_PROPERTIES)) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            logger.error("Error reading property file: {}", e.getMessage());
        }
    }

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
        return PROPERTIES.getProperty(field, defaultValue);
    }
}
