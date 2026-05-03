package content;

import config.ConfigReader;
import model.Game;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsoupHelper {

    public JsoupHelper() {
    }

    public String getTitle(Document document) {
        return Objects.requireNonNull(document.body().getElementById("title")).text();
    }

    /**
     * @param document the page with gamedates of a specific team as document
     * @return a list of table rows of the date table
     */
    public Elements getTableRows(Document document) {
        // Get all elements by tag tbody which is a table
        Elements tables = document.body().getElementsByTag("tbody");
        // The second table is the date table
        Element dateTable = tables.get(1);
        // Return list (as Elements) of all table rows
        return dateTable.getElementsByTag("tr");
    }

    public List<Game> createGamesFromTableRows(String team, String title, Elements tableRows, ConfigReader configReader) throws IOException, ConfigurationException {
        int index = 0;
        if (configReader.getTeamsWithIndex().stream().anyMatch(teamWithIndex -> teamWithIndex.equals(title))) {
            index = 2;
        }
        List<Game> games = new ArrayList<>();
        String currentDateTime = null;
        for (int i = 0; i < tableRows.size(); i++) {
            if (i == 0) {
                // First row is header of table in which we are not interested
                continue;
            }
            Elements td = tableRows.get(i).getElementsByTag("td");
            String dateTime;
            if (td.get(1).text().isEmpty()) {
                dateTime = currentDateTime;
            } else {
                dateTime = td.get(1).text();
            }
            String date = dateTime.split(" ")[0];
            String time = dateTime.split(" ")[1];
            currentDateTime = dateTime;
            String homeTeam = td.get(3 + index).text();
            String guestTeam = td.get(4 + index).text();
            if (homeTeam.contains(configReader.getHomeTeam()) || guestTeam.contains(configReader.getHomeTeam())) {
                String clubMatchId = generateClubMatchId(team, homeTeam, guestTeam);
                Game game = createGame(clubMatchId, team, homeTeam, guestTeam, date, time, homeTeam.contains(configReader.getHomeTeam()));
                games.add(game);
            }
        }
        return games;
    }

    Game createGame(String clubMatchId, String team, String homeTeam, String guestTeam, String date, String time, boolean isHome) {
        return Game.builder()
                .clubMatchId(clubMatchId).team(team).homeTeam(homeTeam).guestTeam(guestTeam).date(date).time(time).isHome(isHome).build();
    }

    String generateClubMatchId(String team, String homeTeam, String guestTeam) {
        String raw = team + homeTeam + guestTeam;
        String normalized = raw.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        return DigestUtils.sha256Hex(normalized);
    }
}
