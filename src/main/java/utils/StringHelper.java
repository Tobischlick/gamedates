package utils;

public class StringHelper {

    public static String extractTeam(String input) {
        String [] arr = input.split("\\.");
        return arr[0].substring(0, arr[0].length()-2);
    }
}
