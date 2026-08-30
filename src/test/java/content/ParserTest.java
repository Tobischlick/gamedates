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

    private static final String EXTRA_COLUMNS_HTML = """
            <html>
            <body>
                <div id="title">%s 2.Bezirksklasse Gr. 030</div>
                <table><tbody><tr><td>irrelevant</td></tr></tbody></table>
                <table>
                    <tbody>
                        <tr><th>Datum</th><th>Spielort</th><th>Platz</th><th>Heimmannschaft</th><th>Gastmannschaft</th></tr>
                        <tr>
                            <td></td>
                            <td>%s %s</td>
                            <td></td>
                            <td>irrelevant venue</td>
                            <td>irrelevant platz</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """.formatted(TEAM_A, DATE_A, TIME_A, HOME_TEAM_A, GUEST_TEAM_A);

    private static final String CLUB_ID = "33362";
    private static final String CLUB_TEAMS_URL =
            "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/clubTeams?club=" + CLUB_ID;

    private static final String CLUB_TEAMS_HTML = """
            <html>
            <body>
                <a href="/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+S+2026&group=6">Herren 30</a>
                <a href="/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+M+2026&group=4">Mixed</a>
                <a href="/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/someOtherPage?foo=bar">Irrelevant link</a>
            </body>
            </html>
            """;

    @Test
    void getGames_returnsGamesParsedFromFetchedPage() throws Exception {
        Document document = Jsoup.parse(HTML);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(PAGE_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
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

    @Test
    void getGames_detectsExtraColumnsFromHeaderAndOffsetsTeamColumns() throws Exception {
        Document document = Jsoup.parse(EXTRA_COLUMNS_HTML);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(PAGE_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
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
    }

    @Test
    void discoverPages_returnsAbsoluteGroupPageLinksFromClubTeamsPage() throws Exception {
        Document document = Jsoup.parse(CLUB_TEAMS_HTML, CLUB_TEAMS_URL);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(CLUB_TEAMS_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
        Parser parser = new Parser(configReader, pageFetcher);

        List<String> pages = parser.discoverPages(CLUB_ID);

        assertThat(pages).containsExactly(
                "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+S+2026&group=6",
                "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+M+2026&group=4"
        );
        verify(pageFetcher).fetch(eq(CLUB_TEAMS_URL));
    }
}
