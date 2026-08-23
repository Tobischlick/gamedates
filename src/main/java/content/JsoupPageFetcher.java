package content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class JsoupPageFetcher implements PageFetcher {
    @Override
    public Document fetch(String url) throws IOException {
        return Jsoup.connect(url).get();
    }
}
