package model;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@Builder
public record Game(String clubMatchId, String team, String homeTeam, String guestTeam, String date, String time, boolean isHome
) {

    public String createSummary() {
        String location = this.isHome ? "[HEIM]" : "[AUSWÄRTS]";
        return this.team + ": " + this.homeTeam + " - " + this.guestTeam + " - " + location;
    }

    @Override
    @NotNull
    public String toString() {
        return "Game{" +
                "clubMatchId='" + clubMatchId + '\'' +
                ", team='" + team + '\'' +
                ", homeTeam='" + homeTeam + '\'' +
                ", guestTeam='" + guestTeam + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
