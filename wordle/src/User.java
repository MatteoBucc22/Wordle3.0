
import java.util.HashMap;

public class User {
    
    String username;
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isHas_played() {
        return has_played;
    }

    public void setHas_played(boolean has_played) {
        this.has_played = has_played;
    }

    public int getWin_streak() {
        return win_streak;
    }

    public void setWin_streak(int win_streak) {
        this.win_streak = win_streak;
    }

    public int getLongest_win_streak() {
        return longest_win_streak;
    }

    public void setLongest_win_streak(int longest_win_streak) {
        this.longest_win_streak = longest_win_streak;
    }

    public int getGames_won() {
        return games_won;
    }

    public void setGames_won(int games_won) {
        this.games_won = games_won;
    }

    public int getGames_played() {
        return games_played;
    }

    public void setGames_played(int games_played) {
        this.games_played = games_played;
    }

    public void setIs_logged(boolean is_logged) {
        this.is_logged = is_logged;
    }

    public HashMap<Integer, Integer> getGuess_distribution() {
        return guess_distribution;
    }

    public void setGuess_distribution(HashMap<Integer, Integer> guess_distribution) {
        this.guess_distribution = guess_distribution;
    }

    String password;
    boolean has_played;
    boolean is_logged;
    int win_streak;
    int longest_win_streak;
    int games_won;
    int games_played;
    HashMap<Integer,Integer> guess_distribution;

    public User(String username, String password){
        this.username = username;
        this.password = password;
        this.win_streak = 0;
        this.longest_win_streak = 0;
        this.games_played = 0;
        this.games_won = 0;
        this.has_played = false;
        this.is_logged = false;
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        map.put(1,0);
        map.put(2,0);
        map.put(3,0);
        map.put(4,0);
        map.put(5,0);
        map.put(6,0);
        map.put(7,0);
        map.put(8,0);
        map.put(9,0);
        map.put(10,0);
        map.put(11,0);
        map.put(12,0);
        this.guess_distribution = map;

    }

    
    
}

