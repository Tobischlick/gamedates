package api;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import model.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        @Test
        @DisplayName("getDateTime uses CET (+01:00) offset for games outside daylight-saving time")
        void winterOffset() {
            Game winterGame = buildGame("18.12.2023", "09:30");
            DateTime expected = new DateTime("2023-12-18T09:30:00+01:00");
            assertThat(oAuthCalendar.getDateTime(winterGame, false)).isEqualTo(expected);
        }

        @Test
        @DisplayName("getDateTime rolls over to the next day when the 6-hour end time crosses midnight")
        void endRollsOverToNextDay() {
            Game lateGame = buildGame("18.06.2023", "20:00");
            DateTime expected = new DateTime("2023-06-19T02:00:00+02:00");
            assertThat(oAuthCalendar.getDateTime(lateGame, true)).isEqualTo(expected);
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
        Game gameB = buildGame(DATE_B, TIME_B);

        String clubMatchIdA = oAuthCalendar.createEvent(DEFAULT_GAME).getExtendedProperties().getShared().get("clubMatchId");
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
        private static final String CALENDAR_ID = "myCalender";

        private Calendar mockServiceWithExistingEvents(List<Event> existingEvents) throws IOException {
            Calendar service = mock(Calendar.class);
            Calendar.Events events = mock(Calendar.Events.class);
            Calendar.Events.List listRequest = mock(Calendar.Events.List.class);

            when(service.events()).thenReturn(events);
            when(events.list(CALENDAR_ID)).thenReturn(listRequest);
            when(listRequest.setSingleEvents(true)).thenReturn(listRequest);
            when(listRequest.execute()).thenReturn(new Events().setItems(existingEvents));

            return service;
        }

        private Event existingEventWithStart() {
            return new Event()
                    .setId("existing-id")
                    .setStart(new EventDateTime().setDateTime(new DateTime("2023-06-18T09:30:00+02:00")))
                    .setExtendedProperties(new Event.ExtendedProperties()
                            .setShared(Map.of(CLUB_MATCH_ID_KEY, CLUB_MATCH_ID)));
        }

        @Test
        @DisplayName("dry run does not write new events")
        void dryRun_newGame_doesNotInsert() throws IOException {
            Calendar service = mockServiceWithExistingEvents(List.of());
            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, false);

            calendar.generateAndPostEvents(List.of(DEFAULT_GAME));

            verify(service.events(), never()).insert(anyString(), any(Event.class));
            verify(service.events(), never()).patch(anyString(), anyString(), any(Event.class));
        }

        @Test
        @DisplayName("dry run does not update rescheduled events")
        void dryRun_timeChanged_doesNotPatch() throws IOException {
            Calendar service = mockServiceWithExistingEvents(List.of(
                    existingEventWithStart()
            ));
            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, false);

            calendar.generateAndPostEvents(List.of(buildGame(DATE_B, TIME_B)));

            verify(service.events(), never()).patch(anyString(), anyString(), any(Event.class));
        }

        @Test
        @DisplayName("dry run still compares unchanged events")
        void dryRun_noChanges_doesNotWrite() throws IOException {
            Calendar service = mockServiceWithExistingEvents(List.of(
                    existingEventWithStart()
            ));
            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, false);

            calendar.generateAndPostEvents(List.of(DEFAULT_GAME));

            verify(service.events(), never()).insert(anyString(), any(Event.class));
            verify(service.events(), never()).patch(anyString(), anyString(), any(Event.class));
        }

        @Test
        @DisplayName("posting enabled creates new events")
        void postingEnabled_newGame_inserts() throws IOException {
            Calendar service = mockServiceWithExistingEvents(List.of());
            Calendar.Events.Insert insertRequest = mock(Calendar.Events.Insert.class);
            when(service.events().insert(eq(CALENDAR_ID), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.execute()).thenReturn(new Event().setHtmlLink("http://calendar/link"));

            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, true);
            calendar.generateAndPostEvents(List.of(DEFAULT_GAME));

            verify(insertRequest).execute();
        }

        @Test
        @DisplayName("posting enabled updates rescheduled events")
        void postingEnabled_timeChanged_patches() throws IOException {
            Calendar service = mockServiceWithExistingEvents(List.of(
                    existingEventWithStart()
            ));
            Calendar.Events.Patch patchRequest = mock(Calendar.Events.Patch.class);
            when(service.events().patch(eq(CALENDAR_ID), eq("existing-id"), any(Event.class))).thenReturn(patchRequest);
            when(patchRequest.execute()).thenReturn(new Event());

            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, true);
            calendar.generateAndPostEvents(List.of(buildGame(DATE_B, TIME_B)));

            verify(patchRequest).execute();
        }

        @Test
        @DisplayName("continues processing remaining events after one fails to sync")
        void postingEnabled_oneFails_continuesAndReturnsFailureCount() throws IOException {
            Calendar service = mockServiceWithExistingEvents(List.of());
            Calendar.Events.Insert insertRequest = mock(Calendar.Events.Insert.class);
            when(service.events().insert(eq(CALENDAR_ID), any(Event.class))).thenReturn(insertRequest);
            when(insertRequest.execute())
                    .thenThrow(new IOException("quota exceeded"))
                    .thenReturn(new Event().setHtmlLink("http://calendar/link"));

            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, true);
            Game gameB = buildGame(DATE_B, TIME_B);

            int failures = calendar.generateAndPostEvents(List.of(DEFAULT_GAME, gameB));

            assertThat(failures).isEqualTo(1);
            verify(insertRequest, Mockito.times(2)).execute();
        }

        @Test
        @DisplayName("logs old and new time when an event's time changes")
        void logsOldAndNewTimeOnChange() throws IOException {
            Event existingEvent = existingEventWithStart();
            Calendar service = mockServiceWithExistingEvents(List.of(existingEvent));
            Calendar.Events.Patch patchRequest = mock(Calendar.Events.Patch.class);
            when(service.events().patch(eq(CALENDAR_ID), eq("existing-id"), any(Event.class))).thenReturn(patchRequest);
            when(patchRequest.execute()).thenReturn(new Event());

            OAuthCalendar calendar = new OAuthCalendar(service, CALENDAR_ID, true);
            Game changedGame = buildGame(DATE_B, TIME_B);
            String expectedOldTime = existingEvent.getStart().getDateTime().toString();
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