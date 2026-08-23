package content;

import config.ConfigReader;
import model.Game;
import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static utils.TestUtils.*;

class ParserTest {

    private static final String PAGE_URL = "https://eine-valide-seite.de/pfad/datei1";

    private static final String HTML = """
            <html>
            <body>
                <div id="title">%s 1.Bezirksklasse Gr. 006</div>
                <table><tbody><tr><td>irrelevant</td></tr></tbody></table>
                <table>
                    <tbody>
                        <tr><td>header</td><td>header</td><td>header</td><td>header</td><td>header</td></tr>
                        <tr>
                            <td></td>
                            <td>%s %s</td>
                            <td></td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """.formatted(TEAM_A, DATE_A, TIME_A, HOME_TEAM_A, GUEST_TEAM_A);

    @Test
    void getGames_returnsGamesParsedFromFetchedPage() throws Exception {
        Document document = Jsoup.parse(HTML);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(PAGE_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
        when(configReader.getTeamsWithIndex()).thenReturn(List.of());
        when(configReader.getHomeTeam()).thenReturn(HOME_TEAM_A);

        Parser parser = new Parser(configReader, pageFetcher);

        List<Game> games = parser.getGames(PAGE_URL);

        Game expected = Game.builder()
                .clubMatchId(JsoupHelper.generateClubMatchId(TEAM_A, HOME_TEAM_A, GUEST_TEAM_A))
                .team(TEAM_A)
                .homeTeam(HOME_TEAM_A)
                .guestTeam(GUEST_TEAM_A)
                .date(DATE_A)
                .time(TIME_A)
                .isHome(true)
                .build();
        assertThat(games).containsExactly(expected);
        verify(pageFetcher).fetch(eq(PAGE_URL));
    }
}
