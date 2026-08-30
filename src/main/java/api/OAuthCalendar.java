package api;

import annotations.ForTestingOnly;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.extern.slf4j.Slf4j;
import model.Game;
import security.CalendarHelper;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class OAuthCalendar {

    private static final ZoneId GAME_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter GAME_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter GAME_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Calendar service;
    private final String calendarId;
    private final boolean postingEnabled;

    private static final String CLUB_MATCH_ID_KEY = "clubMatchId";

    @ForTestingOnly
    public OAuthCalendar(Calendar service, String calendarId) {
        this(service, calendarId, false);
    }

    @ForTestingOnly
    public OAuthCalendar(Calendar service, String calendarId, boolean postingEnabled) {
        this.service = service;
        this.calendarId = calendarId;
        this.postingEnabled = postingEnabled;
    }

    public OAuthCalendar(String calendarId, boolean postingEnabled) throws IOException {
        this.service = new CalendarHelper().buildService();
        this.calendarId = calendarId;
        this.postingEnabled = postingEnabled;
    }

    protected int generateAndPostEvents(List<Game> games) throws IOException {
        // Fetch all existing events including their clubMatchId
        Map<String, Event> existingEvents = getExistingEventsMap();
        log.info("Found {} existing events in calendar", existingEvents.size());

        int failures = 0;
        for (Game game : games) {
            String clubMatchId = game.clubMatchId();
            Event newEvent = createEvent(game);
            String eventLabel = clubMatchId + " - " + game.createSummary();

            try {
                if (existingEvents.containsKey(clubMatchId)) {
                    Event existingEvent = existingEvents.get(clubMatchId);

                    if (timeHasChanged(existingEvent, newEvent)) {
                        String oldTime = formatEventStartTime(existingEvent);
                        String newTime = formatEventStartTime(newEvent);
                        if (postingEnabled) {
                            updateEventTime(existingEvent.getId(), newEvent);
                            log.info("Updated time for event: {} ({} -> {})", eventLabel, oldTime, newTime);
                        } else {
                            log.info("Would update time for event: {} ({} -> {})", eventLabel, oldTime, newTime);
                        }
                    } else {
                        log.info("No changes for event: {}", eventLabel);
                    }
                } else if (postingEnabled) {
                    Event createdEvent = service.events().insert(calendarId, newEvent).execute();
                    log.info("Created new event: {} ({})", eventLabel, createdEvent.getHtmlLink());
                } else {
                    log.info("Would create new event: {}", eventLabel);
                }
            } catch (IOException e) {
                failures++;
                log.error("Failed to sync event: {} ({})", eventLabel, e.getMessage());
            }
        }

        if (!postingEnabled) {
            log.info("Posting disabled — no changes were written to the calendar.");
        }
        return failures;
    }

    boolean timeHasChanged(Event existingEvent, Event newEvent) {
        String existingStart = existingEvent.getStart().getDateTime().toString();
        String newStart = newEvent.getStart().getDateTime().toString();
        return !existingStart.equals(newStart);
    }

    String formatEventStartTime(Event event) {
        return event.getStart().getDateTime().toString();
    }

    private void updateEventTime(String googleEventId, Event newEvent) throws IOException {
        // We only need to send the fields that changed
        Event patchData = new Event();
        patchData.setStart(newEvent.getStart());
        patchData.setEnd(newEvent.getEnd());

        service.events().patch(calendarId, googleEventId, patchData).execute();
    }

    Map<String, Event> getExistingEventsMap() throws IOException {
        return service.events()
                .list(calendarId)
                .setSingleEvents(true)
                .execute()
                .getItems()
                .stream()
                .filter(e -> e.getExtendedProperties() != null &&
                        e.getExtendedProperties().getShared() != null &&
                        e.getExtendedProperties().getShared().containsKey(CLUB_MATCH_ID_KEY))
                .collect(Collectors.toMap(
                        e -> e.getExtendedProperties().getShared().get(CLUB_MATCH_ID_KEY),
                        e -> e,
                        (existing, _) -> existing
                ));
    }

    Event createEvent(Game game) {
        Event event = new Event();

        Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
        extendedProperties.setShared(Map.of(CLUB_MATCH_ID_KEY, game.clubMatchId()));
        event.setExtendedProperties(extendedProperties);

        DateTime startDateTime = getDateTime(game, false);
        DateTime endDateTime = getDateTime(game, true);
        EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("Europe/Berlin");
        EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("Europe/Berlin");
        event.setStart(start);
        event.setEnd(end);
        event.setSummary(game.createSummary());
        event.setColorId("10");
        event.setTransparency("transparent");
        return event;
    }

    DateTime getDateTime(Game game, boolean isEnd) {
        LocalDate localDate = LocalDate.parse(game.date(), GAME_DATE_FORMAT);
        LocalTime localTime = LocalTime.parse(game.time(), GAME_TIME_FORMAT);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        if (isEnd) {
            // We calculate each game day with 6 hours
            localDateTime = localDateTime.plusHours(6);
        }
        ZonedDateTime zonedDateTime = localDateTime.atZone(GAME_ZONE);
        return new DateTime(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
}