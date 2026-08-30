package config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;

@Slf4j
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
            log.error("CLUB_ID environment variable is not set");
            throw new ConfigurationException("No club id set");
        }
        return clubIdFromEnv;
    }

    public boolean getPostingEnabled() throws IOException {
        String postingEnabledFromEnv = System.getenv("POSTING_ENABLED");
        if (postingEnabledFromEnv != null) {
            boolean postingEnabled = Boolean.parseBoolean(postingEnabledFromEnv);
            log.debug("posting-enabled resolved to {} (from POSTING_ENABLED env override)", postingEnabled);
            return postingEnabled;
        }
        JsonNode postingEnabledJsonNode = getJsonNode();
        boolean postingEnabled = postingEnabledJsonNode.asBoolean(false);
        log.debug("posting-enabled resolved to {} (from application.yml)", postingEnabled);
        return postingEnabled;
    }

    public String getCalendarId() throws ConfigurationException {
        String calendarIdFromEnv = System.getenv("CALENDAR_ID");

        if (calendarIdFromEnv == null) {
            log.error("CALENDAR_ID environment variable is not set");
            throw new ConfigurationException("No calendar id set");
        }
        return calendarIdFromEnv;
    }

    public String getHomeTeam() throws ConfigurationException {
        String homeTeamFromEnv = System.getenv("HOME_TEAM");

        if (homeTeamFromEnv == null) {
            log.error("HOME_TEAM environment variable is not set");
            throw new ConfigurationException("No home team set");
        }
        return homeTeamFromEnv;
    }

    private JsonNode getJsonNode() throws IOException {
        return objectMapper.readTree(this.file).get("posting-enabled");
    }
}
