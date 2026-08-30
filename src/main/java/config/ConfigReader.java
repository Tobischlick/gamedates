package config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;

public class ConfigReader {

    private final File file;
    private final ObjectMapper objectMapper;

    public ConfigReader(String path) {
        this.file = new File(path);
        this.objectMapper = new ObjectMapper(new YAMLFactory());
    }

    public String getClubId() throws ConfigurationException {
        String clubIdFromEnv = System.getenv("CLUB_ID");

        if (clubIdFromEnv == null) {
            throw new ConfigurationException("No club id set");
        }
        return clubIdFromEnv;
    }

    public boolean getPostingEnabled() throws IOException {
        String postingEnabledFromEnv = System.getenv("POSTING_ENABLED");
        if (postingEnabledFromEnv != null) {
            return Boolean.parseBoolean(postingEnabledFromEnv);
        }
        JsonNode postingEnabledJsonNode = getJsonNode("posting-enabled");
        return postingEnabledJsonNode.asBoolean(false);
    }

    public String getCalendarId() throws ConfigurationException {
        String calendarIdFromEnv = System.getenv("CALENDAR_ID");

        if (calendarIdFromEnv == null) {
            throw new ConfigurationException("No calendar id set");
        }
        return calendarIdFromEnv;
    }

    public String getHomeTeam() throws ConfigurationException {
        String homeTeamFromEnv = System.getenv("HOME_TEAM");

        if (homeTeamFromEnv == null) {
            throw new ConfigurationException("No home team set");
        }
        return homeTeamFromEnv;
    }

    private JsonNode getJsonNode(String key) throws IOException {
        return objectMapper.readTree(this.file).get(key);
    }
}
