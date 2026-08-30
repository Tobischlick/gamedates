package config;

import org.junit.jupiter.api.Test;

import javax.naming.ConfigurationException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigReaderTest {

    private static final String PATH_TO_FILE = "src/test/resources/";
    private static final String EMPTY_FILE = "application-empty.yml";
    private static final String TEST_FILE = "application-test.yml";

    @Test
    void getClubId_notSet() {
        ConfigReader configReader = new ConfigReader(getTestFile());
        assertThatThrownBy(configReader::getClubId)
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("No club id set");
    }

    @Test
    void getPostingEnabled_default() throws IOException {
        ConfigReader configReader = new ConfigReader(getEmptyFile());
        assertThat(configReader.getPostingEnabled()).isFalse();
    }

    @Test
    void getPostingEnabled_true() throws IOException {
        ConfigReader configReader = new ConfigReader(getTestFile());
        assertThat(configReader.getPostingEnabled()).isTrue();
    }

    @Test
    void getCalendarId_notSet() {
        ConfigReader configReader = new ConfigReader(getTestFile());
        assertThatThrownBy(configReader::getCalendarId)
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("No calendar id set");
    }

    @Test
    void getHomeTeam_notSet() {
        ConfigReader configReader = new ConfigReader(getTestFile());
        assertThatThrownBy(configReader::getHomeTeam)
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("No home team set");
    }

    private String getEmptyFile() {
        return PATH_TO_FILE + EMPTY_FILE;
    }

    private String getTestFile() {
        return PATH_TO_FILE + TEST_FILE;
    }

}