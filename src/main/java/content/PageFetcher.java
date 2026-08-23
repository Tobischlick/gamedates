package content;

import org.jsoup.nodes.Document;

import java.io.IOException;

public interface PageFetcher {
    Document fetch(String url) throws IOException;
}
