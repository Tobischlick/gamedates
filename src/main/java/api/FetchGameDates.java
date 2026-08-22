package api;


import config.ConfigReader;
import content.Parser;
import model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.naming.ConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class FetchGameDates {

    private static final Logger log = LoggerFactory.getLogger(FetchGameDates.class);

    public static void main(String[] args) throws GeneralSecurityException, IOException, ConfigurationException {
        ConfigReader configReader = new ConfigReader("src/main/resources/application.yml");
        Parser parser = new Parser(configReader);

        List<String> pages = configReader.getPages();
        List<Game> games = new ArrayList<>();
        int failedPages = 0;
        for (String page : pages) {
            try {
                games.addAll(parser.getGames(page));
            } catch (IOException | ConfigurationException e) {
                failedPages++;
                log.error("Failed to fetch/parse page {}: {}", page, e.getMessage());
            }
        }

        log.info("Found {} games at nuliga", games.size());

        String calendarId = configReader.getCalendarId();
        boolean postingEnabled = configReader.getPostingEnabled();

        OAuthCalendar oAuthCalendar = new OAuthCalendar(calendarId, postingEnabled);
        int failedEvents = oAuthCalendar.generateAndPostEvents(games);

        int totalFailures = failedPages + failedEvents;
        if (totalFailures > 0) {
            log.error("Completed with {} failure(s): {} page(s), {} event(s)", totalFailures, failedPages, failedEvents);
            System.exit(1);
        }
    }
}
