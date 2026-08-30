package content;

import config.ConfigReader;
import model.Game;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsoupHelper {

    private static final Logger log = LoggerFactory.getLogger(JsoupHelper.class);
    private static final String HOME_TEAM_HEADER = "Heimmannschaft";
    private static final String BYE = "spielfrei";

    public static String getTitle(Document document) {
        return Objects.requireNonNull(document.body().getElementById("title")).text();
    }

    /**
     * Each row on the club's team overview page carries a team category label (e.g. "Herren 30 1")
     * in its first cell alongside the groupPage link - used as a fallback team label for cup pages,
     * whose own title doesn't include one.
     */
    public static List<DiscoveredPage> getDiscoveredPages(Document document) {
        List<DiscoveredPage> pages = new ArrayList<>();
        for (Element link : document.select("a[href*=groupPage]")) {
            Element row = link.closest("tr");
            String team = row == null ? "" : stripSquadNumber(firstCellText(row));
            pages.add(new DiscoveredPage(team, link.absUrl("href")));
        }
        return pages;
    }

    private static String firstCellText(Element row) {
        Elements cells = row.getElementsByTag("td");
        return cells.isEmpty() ? "" : cells.first().text();
    }

    private static String stripSquadNumber(String text) {
        return text.replaceAll("\\s+\\d+$", "");
    }

    /**
     * Some groups have extra leading columns (e.g. Spielort/Platz, or Nr. on cup pages) and some
     * pages have no team-roster table before the fixture table, so neither the number of tables
     * nor the column layout is fixed. Find the table whose header row contains "Heimmannschaft"
     * instead of assuming a fixed table/tbody index.
     *
     * @param document the page with gamedates of a specific team as document
     * @return the rows of the fixture table, or an empty list if the page has no such table
     */
    public static Elements getTableRows(Document document) {
        for (Element tbody : document.body().getElementsByTag("tbody")) {
            Elements rows = tbody.getElementsByTag("tr");
            if (!rows.isEmpty() && hasHeader(rows.first(), HOME_TEAM_HEADER)) {
                return rows;
            }
        }
        return new Elements();
    }

    private static boolean hasHeader(Element row, String headerText) {
        return row.getElementsByTag("th").stream().anyMatch(th -> th.text().equals(headerText));
    }

    /**
     * Data rows always carry two more leading columns than the header row (an unlabeled weekday
     * column and a blank spacer), so the "Heimmannschaft" header's position tells us exactly
     * where the home-team data column is, regardless of what extra columns precede it.
     */
    static int getColumnOffset(Elements tableRows) {
        Elements headerCells = tableRows.get(0).getElementsByTag("th");
        for (int i = 0; i < headerCells.size(); i++) {
            if (headerCells.get(i).text().equals(HOME_TEAM_HEADER)) {
                return i - 1;
            }
        }
        return 0;
    }

    public static List<Game> createGamesFromTableRows(String team, String title, Elements tableRows, ConfigReader configReader) throws ConfigurationException {
        if (tableRows.isEmpty()) {
            return new ArrayList<>();
        }
        int index = getColumnOffset(tableRows);
        String homeTeamName = configReader.getHomeTeam();

        List<Game> games = new ArrayList<>();
        String currentDateTime = null;
        // First row is the header of the table, in which we are not interested
        for (int i = 1; i < tableRows.size(); i++) {
            try {
                Elements td = tableRows.get(i).getElementsByTag("td");
                String dateTime = td.get(1).text().isEmpty() ? currentDateTime : td.get(1).text();
                String date = dateTime.split(" ")[0];
                String time = dateTime.split(" ")[1];
                currentDateTime = dateTime;
                String homeTeam = td.get(3 + index).text();
                String guestTeam = td.get(4 + index).text();
                boolean isBye = homeTeam.equalsIgnoreCase(BYE) || guestTeam.equalsIgnoreCase(BYE);
                if (!isBye && (homeTeam.contains(homeTeamName) || guestTeam.contains(homeTeamName))) {
                    String clubMatchId = generateClubMatchId(team, homeTeam, guestTeam);
                    games.add(createGame(clubMatchId, team, homeTeam, guestTeam, date, time, homeTeam.contains(homeTeamName)));
                }
            } catch (RuntimeException e) {
                log.warn("Skipping unparseable row {} on page '{}': {}", i, title, e.getMessage());
            }
        }
        return games;
    }

    static Game createGame(String clubMatchId, String team, String homeTeam, String guestTeam, String date, String time, boolean isHome) {
        return Game.builder()
                .clubMatchId(clubMatchId).team(team).homeTeam(homeTeam).guestTeam(guestTeam).date(date).time(time).isHome(isHome).build();
    }

    static String generateClubMatchId(String team, String homeTeam, String guestTeam) {
        String raw = team + homeTeam + guestTeam;
        String normalized = raw.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        return DigestUtils.sha256Hex(normalized);
    }
}
