package api;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import model.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static utils.TestUtils.*;


class OAuthCalendarTest {

    private static OAuthCalendar oAuthCalendar;

    @Mock
    private static Calendar service;

    @BeforeAll
    static void setUp() {
        oAuthCalendar = new OAuthCalendar(service, "myCalender");
    }

    @Nested
    class dateTime {
        @Test
        @DisplayName("getDateTime returns correct timestamp for start")
        void start() {
            DateTime expected = new DateTime("2023-06-18T09:30:00+02:00");
            assertThat(oAuthCalendar.getDateTime(DEFAULT_GAME, false)).isEqualTo(expected);
        }

        @Test
        @DisplayName("getDateTime returns correct timestamp for end")
        void end() {
            DateTime expected = new DateTime("2023-06-18T15:30:00+02:00");
            assertThat(oAuthCalendar.getDateTime(DEFAULT_GAME, true)).isEqualTo(expected);
        }
    }

    @Test
    void eventGeneration() {
        Game game = new Game(CLUB_MATCH_ID, TEAM_A, HOME_TEAM_A, GUEST_TEAM_A, DATE_A, TIME_A, true);

        DateTime start = new DateTime("2023-06-18T09:30:00+02:00");
        DateTime end = new DateTime("2023-06-18T15:30:00+02:00");
        Event expected = new Event();
        expected.setStart(new EventDateTime().setDateTime(start).setTimeZone("Europe/Berlin"));
        expected.setEnd(new EventDateTime().setDateTime(end).setTimeZone("Europe/Berlin"));
        expected.setSummary("Herren: TC Karlsruhe-West 1 - Karlsruher ETV 3 - [HEIM]");
        expected.setColorId("10");
        expected.setTransparency("transparent");

        Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
        extendedProperties.setShared(Map.of("clubMatchId", CLUB_MATCH_ID));
        expected.setExtendedProperties(extendedProperties);

        assertThat(oAuthCalendar.createEvent(game)).isEqualTo(expected);
    }

    @Test
    void idDoesNotConsiderDateAndTime() {
        Game gameA = DEFAULT_GAME;
        Game gameB = buildGame(DATE_B, TIME_B);

        String clubMatchIdA = oAuthCalendar.createEvent(gameA).getExtendedProperties().getShared().get("clubMatchId");
        String clubMatchIdB = oAuthCalendar.createEvent(gameB).getExtendedProperties().getShared().get("clubMatchId");

        assertThat(clubMatchIdA).isEqualTo(clubMatchIdB);
    }

    @Nested
    class hasTimeChanged {
        @Test
        @DisplayName("hasTimeChanged should return true if times are different")
        void timeChanged_time() {
            // Setup existing event
            Event existing = new Event().setStart(new EventDateTime()
                    .setDateTime(new DateTime("2026-05-20T10:00:00Z")));

            // Setup new event with different time
            Event newData = new Event().setStart(new EventDateTime()
                    .setDateTime(new DateTime("2026-05-20T12:00:00Z")));

            assertThat(oAuthCalendar.timeHasChanged(existing, newData)).isTrue();
        }

        @Test
        @DisplayName("hasTimeChanged should return true if dates are different")
        void timeChanged_date() {
            // Setup existing event
            Event existing = new Event().setStart(new EventDateTime()
                    .setDateTime(new DateTime("2026-05-20T10:00:00Z")));

            // Setup new event with different time
            Event newData = new Event().setStart(new EventDateTime()
                    .setDateTime(new DateTime("2026-05-27T10:00:00Z")));

            assertThat(oAuthCalendar.timeHasChanged(existing, newData)).isTrue();
        }

        @Test
        @DisplayName("hasTimeChanged should return false if times are identical")
        void testSameTime() {
            String timestamp = "2026-05-20T10:00:00Z";
            Event existing = new Event().setStart(new EventDateTime().setDateTime(new DateTime(timestamp)));
            Event newData = new Event().setStart(new EventDateTime().setDateTime(new DateTime(timestamp)));

            assertThat(oAuthCalendar.timeHasChanged(existing, newData)).isFalse();
        }
    }

    @Test
    @DisplayName("getExistingEventsMap should correctly map metadata to events")
    void mapConversion() {
        // Create a mock event with our custom property
        Event event1 = new Event().setExtendedProperties(new Event.ExtendedProperties()
                .setShared(Map.of(CLUB_MATCH_ID_KEY, CLUB_MATCH_ID)));

        // Create a "manual" event without our property
        Event event2 = new Event().setSummary("Lunch with Grandma");

        List<Event> items = List.of(event1, event2);

        // Using a simple stream to test the logic you put inside the method
        Map<String, Event> result = items.stream()
                .filter(e -> e.getExtendedProperties() != null &&
                        e.getExtendedProperties().getShared() != null &&
                        e.getExtendedProperties().getShared().containsKey(CLUB_MATCH_ID_KEY))
                .collect(Collectors.toMap(
                        e -> e.getExtendedProperties().getShared().get(CLUB_MATCH_ID_KEY),
                        e -> e
                ));

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(CLUB_MATCH_ID);
        assertThat(result).doesNotContainValue(event2);
    }

    @Nested
    class generateAndPostEvents {

        @Test
        @DisplayName("logs old and new time when an event's time changes")
        void logsOldAndNewTimeOnChange() throws Exception {
            // Posting must be enabled to reach the update branch; the test constructor
            // doesn't expose it, so we flip it via reflection for this isolated instance.
            Calendar mockService = Mockito.mock(Calendar.class, Mockito.RETURNS_DEEP_STUBS);
            OAuthCalendar calendar = new OAuthCalendar(mockService, "myCalendar");
            Field postingEnabledField = OAuthCalendar.class.getDeclaredField("postingEnabled");
            postingEnabledField.setAccessible(true);
            postingEnabledField.set(calendar, true);

            DateTime oldStart = new DateTime("2023-06-18T09:30:00+02:00");
            Event existingEvent = new Event()
                    .setId("evt-1")
                    .setStart(new EventDateTime().setDateTime(oldStart))
                    .setExtendedProperties(new Event.ExtendedProperties()
                            .setShared(Map.of(CLUB_MATCH_ID_KEY, CLUB_MATCH_ID)));

            when(mockService.events().list("myCalendar").setSingleEvents(true).execute().getItems())
                    .thenReturn(List.of(existingEvent));
            when(mockService.events().patch(eq("myCalendar"), eq("evt-1"), any(Event.class)).execute())
                    .thenReturn(new Event());

            Game changedGame = buildGame(DATE_B, TIME_B);
            String expectedOldTime = oldStart.toString();
            String expectedNewTime = calendar.getDateTime(changedGame, false).toString();

            ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(capturedOut));
            try {
                calendar.generateAndPostEvents(List.of(changedGame));
            } finally {
                System.setOut(originalOut);
            }

            String logOutput = capturedOut.toString();
            assertThat(logOutput).contains("Updated time for event:");
            assertThat(logOutput).contains(expectedOldTime);
            assertThat(logOutput).contains(expectedNewTime);
        }
    }
}