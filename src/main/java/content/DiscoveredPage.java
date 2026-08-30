package content;

/**
 * A groupPage link discovered from a club's team overview page, along with the team category
 * label from that same row (e.g. "Herren 30"). Cup pages don't carry a team name in their own
 * title, so this label is used as a fallback for those.
 */
public record DiscoveredPage(String team, String url) {
}
