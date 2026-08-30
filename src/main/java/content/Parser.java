package content;

import annotations.ForTestingOnly;
import lombok.extern.slf4j.Slf4j;
import utils.StringHelper;
import config.ConfigReader;
import model.Game;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.List;

@Slf4j
public class Parser {

    private static final String CLUB_TEAMS_URL = "https://baden.liga.nu/cgi-bin/WebObjects/nuLigaTENDE.woa/wa/clubTeams?club=%s";
    private static final String CUP_CHAMPIONSHIP = "Pokalwettbewerb";

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

    public List<Game> getGames(DiscoveredPage page) throws IOException, ConfigurationException {
        log.debug("Fetching page {}", page.url());
        Document document = pageFetcher.fetch(page.url());

        String title = JsoupHelper.getTitle(document);
        // Cup pages don't carry a team name in their own title, so fall back to the label
        // discovered from the club's team overview page for those, prefixed to distinguish
        // cup matches from ordinary league games.
        String team = page.url().contains(CUP_CHAMPIONSHIP) ? "Pokal " + page.team() : StringHelper.extractTeam(title);
        Elements tableRows = JsoupHelper.getTableRows(document);
        List<Game> games = JsoupHelper.createGamesFromTableRows(team, title, tableRows, configReader);
        log.debug("Parsed {} game(s) for team '{}' from page '{}'", games.size(), team, title);
        return games;
    }

    public List<DiscoveredPage> discoverPages(String clubId) throws IOException {
        log.debug("Discovering pages for club {}", clubId);
        Document document = pageFetcher.fetch(CLUB_TEAMS_URL.formatted(clubId));
        List<DiscoveredPage> pages = JsoupHelper.getDiscoveredPages(document);
        log.info("Discovered {} page(s) for club {}", pages.size(), clubId);
        return pages;
    }
}
