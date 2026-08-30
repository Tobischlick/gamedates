package utils;

import model.Game;

public class TestUtils {

    public static final String CLUB_MATCH_ID = "I am a very unique id";
    public static final String CLUB_MATCH_ID_KEY = "clubMatchId";

    public static final String DATE_A = "18.06.2023";
    public static final String DATE_B = "25.06.2023";

    public static final String TIME_A = "09:30";
    public static final String TIME_B = "14:00";

    public static final String TEAM_A = "Herren";

    public static final String HOME_TEAM_A = "TC Karlsruhe-West 1";
    public static final String GUEST_TEAM_A = "Karlsruher ETV 3";

    public static final Game DEFAULT_GAME = Game.builder()
            .clubMatchId(CLUB_MATCH_ID)
            .team(TEAM_A)
            .homeTeam(HOME_TEAM_A)
            .guestTeam(GUEST_TEAM_A)
            .date(DATE_A)
            .time(TIME_A)
            .isHome(true)
            .build();

    public static Game buildGame(String date, String time) {
        return Game.builder()
                .clubMatchId(CLUB_MATCH_ID)
                .team(TEAM_A)
                .homeTeam(HOME_TEAM_A)
                .guestTeam(GUEST_TEAM_A)
                .date(date)
                .time(time)
                .isHome(true)
                .build();
    }
}
