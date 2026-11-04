package demo.game;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Session;

/** The rules of the game, stored in Neo4j as relationships from the winner to the loser. */
final class Rules {

    /** One rule: the winner, the verb (it becomes the relationship type) and the loser. */
    record Beats(String winner, String verb, String loser) {}

    /** The result of one round from the point of view of the player. */
    record Outcome(String result, String verb) {}

    static final List<String> GESTURES = List.of("rock", "paper", "scissors", "lizard", "spock");

    static final List<Beats> RULES = List.of(
        new Beats("scissors", "CUTS", "paper"),
        new Beats("paper", "COVERS", "rock"),
        new Beats("rock", "CRUSHES", "lizard"),
        new Beats("lizard", "POISONS", "spock"),
        new Beats("spock", "SMASHES", "scissors"),
        new Beats("scissors", "DECAPITATES", "lizard"),
        new Beats("lizard", "EATS", "paper"),
        new Beats("paper", "DISPROVES", "spock"),
        new Beats("spock", "VAPORIZES", "rock"),
        new Beats("rock", "CRUSHES", "scissors")
    );

    private Rules() {}

    /** Safe to run again and again: the constraint and every MERGE leave an existing rule alone. */
    static void load(Session session) {
        session.run("CREATE CONSTRAINT gesture_name IF NOT EXISTS FOR (g:Gesture) REQUIRE g.name IS UNIQUE").consume();

        session.executeWriteWithoutResult(tx -> {
            tx.run("UNWIND $names AS name MERGE (:Gesture {name: name})", Map.of("names", GESTURES));
            for (Beats rule : RULES) {
                // A relationship type cannot be a parameter. The verb comes from our own list above, never from the user.
                tx.run(
                    "MATCH (w:Gesture {name: $winner}), (l:Gesture {name: $loser}) MERGE (w)-[:" + rule.verb() + "]->(l)",
                    Map.of("winner", rule.winner(), "loser", rule.loser())
                );
            }
        });
    }

    /** Ask the graph who wins. One query covers a win, a loss and a draw. */
    static Outcome judge(Session session, String player, String computer) {
        if (player.equals(computer)) {
            return new Outcome("DRAW", "");
        }
        return session.executeRead(tx -> {
            var row = tx.run("""
                MATCH (p:Gesture {name: $player}), (c:Gesture {name: $computer})
                OPTIONAL MATCH (p)-[w]->(c)
                OPTIONAL MATCH (c)-[l]->(p)
                RETURN type(w) AS winVerb, type(l) AS lossVerb
                """, Map.of("player", player, "computer", computer)).single();
            if (!row.get("winVerb").isNull()) {
                return new Outcome("WIN", row.get("winVerb").asString());
            }
            if (!row.get("lossVerb").isNull()) {
                return new Outcome("LOSS", row.get("lossVerb").asString());
            }
            throw new IllegalStateException("No rule between " + player + " and " + computer + ". Was the graph loaded?");
        });
    }
}
