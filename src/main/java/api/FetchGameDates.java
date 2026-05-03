package api;


import config.ConfigReader;
import content.Parser;
import model.Game;


import javax.naming.ConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class FetchGameDates {

    static void main() throws GeneralSecurityException, IOException, ConfigurationException {
        ConfigReader configReader = new ConfigReader("src/main/resources/application.yml");
        Parser parser = new Parser(configReader);
        List<String> pages;
        try {
            pages = configReader.getPages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Game> games = pages
                .stream()
                .flatMap(page -> {
                    try {
                        return parser.getGames(page).stream();
                    } catch (IOException | ConfigurationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        System.out.printf("Found %s games at nuliga%n", games.size());

        String calendarId = configReader.getCalendarId();
        boolean postingEnabled = configReader.getPostingEnabled();

        OAuthCalendar oAuthCalendar = new OAuthCalendar(calendarId, postingEnabled);
        oAuthCalendar.generateAndPostEvents(games);
    }
}
