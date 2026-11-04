package demo.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * Rock, paper, scissors, lizard, Spock. The rules and the history live in Neo4j.
 *
 * Run it with `./gradlew run`, or with `./gradlew run --args=demo` for a fixed set of 30 rounds.
 */
public final class Game {

    record Move(String player, String computer) {}

    public static void main(String[] args) {
        var uri = env("NEO4J_URI", "bolt://localhost:7687");
        var user = env("NEO4J_USER", "neo4j");
        var database = env("NEO4J_DATABASE", "neo4j");
        var password = System.getenv("NEO4J_PASSWORD");
        if (password == null) {
            System.err.println("Set NEO4J_PASSWORD (and NEO4J_URI, NEO4J_USER, NEO4J_DATABASE if they differ from the defaults).");
            System.exit(1);
        }
        var demo = args.length > 0 && args[0].equals("demo");

        // Closing is automatic: the session first, then the driver, even if something throws.
        try (var driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
             var session = driver.session(SessionConfig.forDatabase(database))) {
            driver.verifyConnectivity();
            Rules.load(session);

            if (demo) {
                History.forget(session, "demo");
                play(session, "demo", demoMoves(30));
                History.report(session, "demo");
            } else {
                playAtTheKeyboard(session);
            }
        }
    }

    /** The same 30 moves every time, so that your output can be compared with the article. */
    static List<Move> demoMoves(int rounds) {
        var computer = new Random(42);
        var player = new Random(7);
        var moves = new ArrayList<Move>();
        for (int i = 0; i < rounds; i++) {
            // This player likes rock: half of the time rock, otherwise any gesture.
            var mine = player.nextBoolean() ? "rock" : Rules.GESTURES.get(player.nextInt(Rules.GESTURES.size()));
            var theirs = Rules.GESTURES.get(computer.nextInt(Rules.GESTURES.size()));
            moves.add(new Move(mine, theirs));
        }
        return moves;
    }

    static void play(Session session, String player, List<Move> moves) {
        var gameId = History.startGame(session, player);
        int number = 0;
        for (var move : moves) {
            playRound(session, gameId, ++number, player, move);
        }
    }

    static void playAtTheKeyboard(Session session) {
        var in = new Scanner(System.in);
        System.out.print("Your name: ");
        var player = in.nextLine().trim();
        var gameId = History.startGame(session, player);
        var random = new Random();
        int number = 0;
        while (true) {
            System.out.print("Round " + (number + 1) + ". " + Rules.GESTURES + " or 1 to 5, q to stop: ");
            var line = in.nextLine().trim().toLowerCase(Locale.ROOT);
            if (line.equals("q")) {
                break;
            }
            var mine = parse(line);
            if (mine == null) {
                System.out.println("Please type a name from the list, a number from 1 to 5, or q.");
                continue;
            }
            var theirs = Rules.GESTURES.get(random.nextInt(Rules.GESTURES.size()));
            playRound(session, gameId, ++number, player, new Move(mine, theirs));
        }
        History.report(session, player);
    }

    /** Accepts a name or a number from 1 to 5. Anything else, including 0, gives null. */
    static String parse(String line) {
        if (Rules.GESTURES.contains(line)) {
            return line;
        }
        try {
            int index = Integer.parseInt(line);
            return index >= 1 && index <= Rules.GESTURES.size() ? Rules.GESTURES.get(index - 1) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static void playRound(Session session, String gameId, int number, String player, Move move) {
        var outcome = Rules.judge(session, move.player(), move.computer());
        History.saveRound(session, gameId, number, move.player(), move.computer(), outcome.result());

        var verb = outcome.verb().toLowerCase(Locale.ROOT);
        var text = switch (outcome.result()) {
            case "WIN" -> "You win: " + move.player() + " " + verb + " " + move.computer() + ".";
            case "LOSS" -> "You lose: " + move.computer() + " " + verb + " " + move.player() + ".";
            default -> "A draw: both chose " + move.player() + ".";
        };
        System.out.printf("Round %d: you %s, computer %s. %s%n", number, move.player(), move.computer(), text);
    }

    private static String env(String name, String fallback) {
        var value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
