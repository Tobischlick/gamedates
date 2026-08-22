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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsoupHelper {

    private static final Logger log = LoggerFactory.getLogger(JsoupHelper.class);

    public static String getTitle(Document document) {
        return Objects.requireNonNull(document.body().getElementById("title")).text();
    }

    /**
     * @param document the page with gamedates of a specific team as document
     * @return a list of table rows of the date table
     */
    public static Elements getTableRows(Document document) {
        // Get all elements by tag tbody which is a table
        Elements tables = document.body().getElementsByTag("tbody");
        // The second table is the date table
        Element dateTable = tables.get(1);
        // Return list (as Elements) of all table rows
        return dateTable.getElementsByTag("tr");
    }

    public static List<Game> createGamesFromTableRows(String team, String title, Elements tableRows, ConfigReader configReader) throws IOException, ConfigurationException {
        int index = 0;
        if (configReader.getTeamsWithIndex().stream().anyMatch(teamWithIndex -> teamWithIndex.equals(title))) {
            index = 2;
        }
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
                if (homeTeam.contains(homeTeamName) || guestTeam.contains(homeTeamName)) {
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
