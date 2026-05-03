package model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.TestUtils.DEFAULT_GAME;

public class GameTest {

    @Test
    void toStringMatches() {
        Game game = DEFAULT_GAME;

        assertThat(game.toString()).isEqualTo("Game{clubMatchId='I am a very unique id', team='Herren', homeTeam='TC Karlsruhe-West 1', guestTeam='Karlsruher ETV 3', date='18.06.2023', time='09:30'}");
    }

    @Test
    void createSummary() {
        Game game = DEFAULT_GAME;
        String summary = game.createSummary();

        assertThat(summary).isEqualTo("Herren: TC Karlsruhe-West 1 - Karlsruher ETV 3 - [HEIM]");
    }
}
