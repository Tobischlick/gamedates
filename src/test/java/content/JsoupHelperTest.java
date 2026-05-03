package content;

import model.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.TestUtils.*;

public class JsoupHelperTest {

    private static JsoupHelper jsoupHelper;

    @BeforeAll
    static void setUp() {
        jsoupHelper = new JsoupHelper();
    }

    @Test
    void generateClubMatchId() {
        String clubMatchId = jsoupHelper.generateClubMatchId("Herren", "TC Karlsruhe-West", "KETV");
        assertThat(clubMatchId).isEqualTo("18db718b667b1cdafeffd386c07e4a7c290c4f3e700da8208d1a2f75ef58be0e");
    }

    @Test
    void createGame() {
        Game game = jsoupHelper.createGame(CLUB_MATCH_ID, TEAM_A, HOME_TEAM_A, GUEST_TEAM_A, DATE_A, TIME_A, true);
        Game expected = DEFAULT_GAME;

        assertThat(game).isEqualTo(expected);
    }
}
