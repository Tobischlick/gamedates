package utils;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class StringHelperTest {

    @ParameterizedTest(name = "Input {0} should extract team {1}")
    @CsvSource({
            "Herren 1.Bezirksklasse Gr. 006, Herren",
            "Damen 40 1.Bezirksliga Gr. 010, Damen 40",
            "Junioren U18 2.Kreisliga Gr. 050, Junioren U18",
            "Herren 30 1.Oberliga Gr. 001, Herren 30"
    })
    void extractTeam(String input, String expected) {
        StringHelper stringHelper = new StringHelper();
        String team = stringHelper.extractTeam(input);

        assertThat(team).isEqualTo(expected);
    }
}
