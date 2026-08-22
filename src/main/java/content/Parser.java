package content;

import utils.StringHelper;
import config.ConfigReader;
import model.Game;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.List;

public class Parser {

    private final ConfigReader configReader;

    public Parser(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public List<Game> getGames(String page) throws IOException, ConfigurationException {
        Connection connection = Jsoup.connect(page);
        Document document = connection.get();

        String title = JsoupHelper.getTitle(document);
        String team = StringHelper.extractTeam(title);
        Elements tableRows = JsoupHelper.getTableRows(document);
        return JsoupHelper.createGamesFromTableRows(team, title, tableRows, configReader);
    }
}
