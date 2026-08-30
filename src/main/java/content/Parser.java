package content;

import annotations.ForTestingOnly;
import utils.StringHelper;
import config.ConfigReader;
import model.Game;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.List;

public class Parser {

    private static final String CLUB_TEAMS_URL = "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/clubTeams?club=%s";

    private final ConfigReader configReader;
    private final PageFetcher pageFetcher;

    public Parser(ConfigReader configReader) {
        this(configReader, new JsoupPageFetcher());
    }

    @ForTestingOnly
    public Parser(ConfigReader configReader, PageFetcher pageFetcher) {
        this.configReader = configReader;
        this.pageFetcher = pageFetcher;
    }

    public List<Game> getGames(String page) throws IOException, ConfigurationException {
        Document document = pageFetcher.fetch(page);

        String title = JsoupHelper.getTitle(document);
        String team = StringHelper.extractTeam(title);
        Elements tableRows = JsoupHelper.getTableRows(document);
        return JsoupHelper.createGamesFromTableRows(team, title, tableRows, configReader);
    }

    public List<String> discoverPages(String clubId) throws IOException {
        Document document = pageFetcher.fetch(CLUB_TEAMS_URL.formatted(clubId));
        return JsoupHelper.getGroupPageLinks(document);
    }
}
