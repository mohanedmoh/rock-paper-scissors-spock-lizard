// ============================================================================
// Part 4: ask the history of the demo player.
//
// STATUS: NOT YET RUN on a real server. The EXPECTED lines come from a model of the game
// (a Python check of the 30 fixed rounds), not from Neo4j. Please compare and report any difference.
//
// First run the demo, with Java 21 and the password of your server:
//   NEO4J_PASSWORD=change-this-password ./gradlew run --args=demo
// Then run this script:
//   docker exec -i neo4j-airports cypher-shell -u neo4j -p change-this-password < cypher/part4_history.cypher
// The five history queries are the ones in the article, with the parameter $player replaced by 'demo'.
// To remove the demo data afterwards, run part4_cleanup.cypher.
// ============================================================================

// ---- 0. The rules: 5 gestures and 10 rules, however often the game has run ----
MATCH (g:Gesture)
WITH count(g) AS gestures
MATCH (:Gesture)-[r]->(:Gesture)
RETURN gestures, count(r) AS rules;
// EXPECTED: 5 | 10

// ---- 0b. What the demo stored: 1 game, 30 rounds, 29 NEXT links ----
MATCH (:Player {name: 'demo'})-[:PLAYED]->(g:Game)-[:HAS_ROUND]->(r:Round)
RETURN count(DISTINCT g) AS games, count(r) AS rounds;
// EXPECTED: 1 | 30

MATCH (:Round)-[n:NEXT]->(:Round)
RETURN count(n) AS nextLinks;
// EXPECTED: 29 (if only the demo player has played)

// ---- 1. How often did I win, lose or draw? ----
MATCH (:Player {name: 'demo'})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(r:Round)
RETURN r.outcome AS outcome, count(*) AS rounds
ORDER BY outcome;
// EXPECTED: DRAW 7, LOSS 8, WIN 15

// ---- 2. Which gesture do I play most? ----
MATCH (:Player {name: 'demo'})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(:Round)-[:PLAYER_MOVE]->(g:Gesture)
RETURN g.name AS gesture, count(*) AS times
ORDER BY times DESC, gesture;
// EXPECTED: rock 19, lizard 4, scissors 4, paper 3

// ---- 3. What do I play after a loss? ----
MATCH (:Player {name: 'demo'})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(:Round {outcome: 'LOSS'})
      -[:NEXT]->(:Round)-[:PLAYER_MOVE]->(g:Gesture)
RETURN g.name AS playedNext, count(*) AS times
ORDER BY times DESC, playedNext;
// EXPECTED: rock 5, paper 2, lizard 1

// ---- 4. Which two gestures do I play one after the other? (top 5) ----
MATCH (:Player {name: 'demo'})-[:PLAYED]->(:Game)-[:HAS_ROUND]->(a:Round)-[:NEXT]->(b:Round),
      (a)-[:PLAYER_MOVE]->(x:Gesture), (b)-[:PLAYER_MOVE]->(y:Gesture)
RETURN x.name AS first, y.name AS second, count(*) AS times
ORDER BY times DESC, first, second
LIMIT 5;
// EXPECTED: rock-rock 12, lizard-rock 4, rock-scissors 4, then two pairs with a count of 2

// ---- 5. Longest winning streak in one game ----
MATCH (:Player {name: 'demo'})-[:PLAYED]->(g:Game)-[:HAS_ROUND]->(r:Round)
WITH g, r ORDER BY r.number
WITH g, collect(r.outcome) AS outcomes
WITH g, reduce(acc = [0, 0], o IN outcomes |
       CASE WHEN o = 'WIN'
            THEN [acc[0] + 1, CASE WHEN acc[0] + 1 > acc[1] THEN acc[0] + 1 ELSE acc[1] END]
            ELSE [0, acc[1]]
       END) AS result
RETURN max(result[1]) AS longestWinStreak;
// EXPECTED: 5
