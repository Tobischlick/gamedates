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
                        <tr><th>Datum</th><th>Heimmannschaft</th><th>Gastmannschaft</th></tr>
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

    private static final String BYE_ROW_HTML = """
            <html>
            <body>
                <div id="title">%s 1.Bezirksklasse Gr. 006</div>
                <table><tbody><tr><td>irrelevant</td></tr></tbody></table>
                <table>
                    <tbody>
                        <tr><th>Datum</th><th>Heimmannschaft</th><th>Gastmannschaft</th></tr>
                        <tr>
                            <td></td>
                            <td>%s %s</td>
                            <td></td>
                            <td>%s</td>
                            <td>spielfrei</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """.formatted(TEAM_A, DATE_A, TIME_A, HOME_TEAM_A);

    // Cup ("Pokalwettbewerb") pages have no team-roster table and only one <table>, an extra
    // "Nr." column instead of Spielort/Platz, round-separator rows, and (unlike league pages) a
    // #title that carries no team name at all - format taken from the real page at
    // https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=Pokalwettbewerb+2026&group=2235038
    private static final String CUP_PAGE_URL =
            "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=Pokalwettbewerb+2026&group=2235038";
    private static final String CUP_PAGE_TITLE = "B2 - Gr. 012";
    private static final String CUP_PAGE_TEAM = "Herren 30";
    private static final String CUP_PAGE_EXPECTED_TEAM = "Pokal Herren 30";

    private static final String CUP_PAGE_HTML = """
            <html>
            <body>
                <div id="title">%s</div>
                <table>
                    <tbody>
                        <tr><th>Datum</th><th>Nr.</th><th>Heimmannschaft</th><th>Gastmannschaft</th></tr>
                        <tr><td>Viertelfinale</td><td></td><td></td><td></td></tr>
                        <tr>
                            <td></td>
                            <td>%s %s</td>
                            <td></td>
                            <td>2</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """.formatted(CUP_PAGE_TITLE, DATE_A, TIME_A, HOME_TEAM_A, GUEST_TEAM_A);

    private static final String NO_FIXTURE_TABLE_HTML = """
            <html>
            <body>
                <div id="title">%s Unrelated Page</div>
                <table><tbody><tr><td>nothing relevant here</td></tr></tbody></table>
            </body>
            </html>
            """.formatted(TEAM_A);

    private static final String CLUB_ID = "33362";
    private static final String CLUB_TEAMS_URL =
            "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/clubTeams?club=" + CLUB_ID;

    private static final String LEAGUE_URL =
            "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+S+2026&group=6";
    private static final String MIXED_URL =
            "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+M+2026&group=4";

    // Row structure (category cell + link cell) taken from the real clubTeams page.
    private static final String CLUB_TEAMS_HTML = """
            <html>
            <body>
                <table>
                    <tbody>
                        <tr><td>Herren 30 1</td><td>Contact</td><td><a href="/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+S+2026&group=6">Herren 30 1.Bezirksklasse Gr. 006</a></td></tr>
                        <tr><td>Mixed 1</td><td>Contact</td><td><a href="/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=B2+M+2026&group=4">Mixed 1.Bezirksklasse Gr. 004</a></td></tr>
                        <tr><td>Herren 30 2</td><td>Contact</td><td><a href="/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/groupPage?championship=Pokalwettbewerb+2026&group=2235038">B2 - Gr. 012</a></td></tr>
                    </tbody>
                </table>
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

        List<Game> games = parser.getGames(new DiscoveredPage(TEAM_A, PAGE_URL));

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

        List<Game> games = parser.getGames(new DiscoveredPage(TEAM_A, PAGE_URL));

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
    void getGames_skipsByeRowsMarkedSpielfrei() throws Exception {
        Document document = Jsoup.parse(BYE_ROW_HTML);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(PAGE_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
        when(configReader.getHomeTeam()).thenReturn(HOME_TEAM_A);

        Parser parser = new Parser(configReader, pageFetcher);

        List<Game> games = parser.getGames(new DiscoveredPage(TEAM_A, PAGE_URL));

        assertThat(games).isEmpty();
    }

    @Test
    void getGames_usesDiscoveryTimeTeamLabelForCupPagesWithoutOwnTitle() throws Exception {
        Document document = Jsoup.parse(CUP_PAGE_HTML);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(CUP_PAGE_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
        when(configReader.getHomeTeam()).thenReturn(HOME_TEAM_A);

        Parser parser = new Parser(configReader, pageFetcher);

        List<Game> games = parser.getGames(new DiscoveredPage(CUP_PAGE_TEAM, CUP_PAGE_URL));

        Game expected = Game.builder()
                .clubMatchId(JsoupHelper.generateClubMatchId(CUP_PAGE_EXPECTED_TEAM, HOME_TEAM_A, GUEST_TEAM_A))
                .team(CUP_PAGE_EXPECTED_TEAM)
                .homeTeam(HOME_TEAM_A)
                .guestTeam(GUEST_TEAM_A)
                .date(DATE_A)
                .time(TIME_A)
                .isHome(true)
                .build();
        assertThat(games).containsExactly(expected);
    }

    @Test
    void getGames_returnsEmptyListWhenPageHasNoFixtureTable() throws Exception {
        Document document = Jsoup.parse(NO_FIXTURE_TABLE_HTML);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(PAGE_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
        when(configReader.getHomeTeam()).thenReturn(HOME_TEAM_A);

        Parser parser = new Parser(configReader, pageFetcher);

        List<Game> games = parser.getGames(new DiscoveredPage(TEAM_A, PAGE_URL));

        assertThat(games).isEmpty();
    }

    @Test
    void discoverPages_returnsTeamAndAbsoluteUrlForEachGroupPageLink() throws Exception {
        Document document = Jsoup.parse(CLUB_TEAMS_HTML, CLUB_TEAMS_URL);
        PageFetcher pageFetcher = mock(PageFetcher.class);
        when(pageFetcher.fetch(CLUB_TEAMS_URL)).thenReturn(document);

        ConfigReader configReader = mock(ConfigReader.class);
        Parser parser = new Parser(configReader, pageFetcher);

        List<DiscoveredPage> pages = parser.discoverPages(CLUB_ID);

        assertThat(pages).containsExactly(
                new DiscoveredPage("Herren 30", LEAGUE_URL),
                new DiscoveredPage("Mixed", MIXED_URL),
                new DiscoveredPage("Herren 30", CUP_PAGE_URL)
        );
        verify(pageFetcher).fetch(eq(CLUB_TEAMS_URL));
    }
}
