package demo.game;

import java.util.Map;

import org.neo4j.driver.Session;

/** Stores every game in Neo4j and asks questions about it. */
final class History {

    private History() {}

    /** A player plays games. A game has rounds. Returns the id of the new game. */
    static String startGame(Session session, String player) {
        return session.executeWrite(tx -> tx.run("""
            MERGE (p:Player {name: $player})
            CREATE (g:Game {id: randomUUID(), started: datetime()})
            CREATE (p)-[:PLAYED]->(g)
            RETURN g.id AS id
            """, Map.of("player", player)).single().get("id").asString());
    }

    /** One round is one node. It points at both gestures, and the previous round points at it. */
    static void saveRound(Session session, String gameId, int number, String player, String computer, String result) {
        session.executeWriteWithoutResult(tx -> {
            tx.run("""
                MATCH (g:Game {id: $game}), (pg:Gesture {name: $player}), (cg:Gesture {name: $computer})
                CREATE (r:Round {number: $number, outcome: $result})
                CREATE (g)-[:HAS_ROUND]->(r)
                CREATE (r)-[:PLAYER_MOVE]->(pg)
                CREATE (r)-[:COMPUTER_MOVE]->(cg)
                """, Map.of("game", gameId, "player", player, "computer", computer, "number", number, "result", result));
            tx.run("""
                MATCH (g:Game {id: $game})-[:HAS_ROUND]->(previous:Round {number: $number - 1}),
                      (g)-[:HAS_ROUND]->(r:Round {number: $number})
                CREATE (previous)-[:NEXT]->(r)
                """, Map.of("game", gameId, "number", number));
        });
    }

    /** Remove a player with all their games and rounds. The demo calls this so that each run starts clean. */
    static void forget(Session session, String player) {
        session.executeWriteWithoutResult(tx -> tx.run("""
            MATCH (p:Player {name: $player})
            OPTIONAL MATCH (p)-[:PLAYED]->(g:Game)
            OPTIONAL MATCH (g)-[:HAS_ROUND]->(r:Round)
            DETACH DELETE r, g, p
            """, Map.of("player", player)));
    }

    static final String OUTCOMES = """
        MATCH (:Player {name: $player})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(r:Round)
        RETURN r.outcome AS outcome, count(*) AS rounds
        ORDER BY outcome
        """;

    static final String FAVOURITE = """
        MATCH (:Player {name: $player})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(:Round)-[:PLAYER_MOVE]->(g:Gesture)
        RETURN g.name AS gesture, count(*) AS times
        ORDER BY times DESC, gesture
        """;

    static final String STREAK = """
        MATCH (:Player {name: $player})-[:PLAYED]->(g:Game)-[:HAS_ROUND]->(r:Round)
        WITH g, r ORDER BY r.number
        WITH g, collect(r.outcome) AS outcomes
        WITH g, reduce(acc = [0, 0], o IN outcomes |
               CASE WHEN o = 'WIN'
                    THEN [acc[0] + 1, CASE WHEN acc[0] + 1 > acc[1] THEN acc[0] + 1 ELSE acc[1] END]
                    ELSE [0, acc[1]]
               END) AS result
        RETURN max(result[1]) AS longestWinStreak
        """;

    static final String AFTER_A_LOSS = """
        MATCH (:Player {name: $player})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(:Round {outcome: 'LOSS'})
              -[:NEXT]->(:Round)-[:PLAYER_MOVE]->(g:Gesture)
        RETURN g.name AS playedNext, count(*) AS times
        ORDER BY times DESC, playedNext
        """;

    static final String HABITS = """
        MATCH (:Player {name: $player})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(a:Round)-[:NEXT]->(b:Round),
              (a)-[:PLAYER_MOVE]->(x:Gesture), (b)-[:PLAYER_MOVE]->(y:Gesture)
        RETURN x.name AS first, y.name AS second, count(*) AS times
        ORDER BY times DESC, first, second
        LIMIT 5
        """;

    /** Print the answers to the five questions for one player. */
    static void report(Session session, String player) {
        var parameters = Map.<String, Object>of("player", player);
        System.out.println();
        System.out.println("History for " + player);
        print(session, "Results", OUTCOMES, parameters);
        print(session, "Gestures played", FAVOURITE, parameters);
        print(session, "Longest winning streak in one game", STREAK, parameters);
        print(session, "What you play after a loss", AFTER_A_LOSS, parameters);
        print(session, "Gesture pairs you play one after the other (top 5)", HABITS, parameters);
    }

    private static void print(Session session, String title, String query, Map<String, Object> parameters) {
        System.out.println();
        System.out.println(title);
        var records = session.executeRead(tx -> tx.run(query, parameters).list());
        for (var record : records) {
            var line = new StringBuilder("  ");
            for (var key : record.keys()) {
                line.append(record.get(key).asObject()).append("  ");
            }
            System.out.println(line.toString().stripTrailing());
        }
    }
}
