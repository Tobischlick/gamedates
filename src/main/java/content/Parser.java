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
import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final ConfigReader configReader;

    public Parser(ConfigReader configReader) {
        this.configReader = configReader;
    }

    public List<Game> getGames(String page) throws IOException, ConfigurationException {
        JsoupHelper jsoupHelper = new JsoupHelper();
        StringHelper stringHelper = new StringHelper();

        Connection connection = Jsoup.connect(page);
        Document document = null;

        try {
            document = connection.get();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }

        List<Game> games = new ArrayList<>();

        if (document != null) {
            String title = jsoupHelper.getTitle(document);
            String team = stringHelper.extractTeam(title);
            Elements tableRows = jsoupHelper.getTableRows(document);
            games = jsoupHelper.createGamesFromTableRows(team, title, tableRows, configReader);
        } else {
            System.out.println("Document is null");
        }
        return games;
    }
}
