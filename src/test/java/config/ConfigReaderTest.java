package config;

import org.junit.jupiter.api.Test;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigReaderTest {

    private static final String PATH_TO_FILE = "src/test/resources/";
    private static final String EMPTY_FILE = "application-empty.yml";
    private static final String TEST_FILE = "application-test.yml";

    @Test
    void getPages_empty() throws IOException {
        ConfigReader configReader = new ConfigReader(PATH_TO_FILE + EMPTY_FILE);
        assertThat(configReader.getPages()).isEmpty();
    }

    @Test
    void getPages_notEmpty() throws IOException {
        ConfigReader configReader = new ConfigReader(PATH_TO_FILE + TEST_FILE);
        List<String> actual = configReader.getPages();
        List<String> expected = List.of(
                "https://eine-valide-seite.de/pfad/datei1",
                "https://eine-valide-seite.de/pfad/datei2"
        );
        assertThat(actual).containsExactlyElementsOf(expected);
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

    @Test
    void getTeamsWithIndex_empty() throws IOException {
        ConfigReader configReader = new ConfigReader(PATH_TO_FILE + EMPTY_FILE);
        assertThat(configReader.getTeamsWithIndex()).isEmpty();
    }

    @Test
    void getTeamsWithIndex_notEmpty() throws IOException {
        ConfigReader configReader = new ConfigReader(PATH_TO_FILE + TEST_FILE);
        List<String> actual = configReader.getTeamsWithIndex();
        List<String> expected = List.of(
                "U18w",
                "Herren (4er)"
        );
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private String getEmptyFile() {
        return PATH_TO_FILE + EMPTY_FILE;
    }

    private String getTestFile() {
        return PATH_TO_FILE + TEST_FILE;
    }

}