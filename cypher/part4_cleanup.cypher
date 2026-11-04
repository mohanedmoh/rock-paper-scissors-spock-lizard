// Part 4: remove the demo player with the game and the 30 rounds.
// The five gestures and the ten rules stay. The Java demo also does this on its own at the start of every run.
MATCH (p:Player {name: 'demo'})
OPTIONAL MATCH (p)-[:PLAYED]->(g:Game)
OPTIONAL MATCH (g)-[:HAS_ROUND]->(r:Round)
DETACH DELETE r, g, p;

MATCH (p:Player {name: 'demo'})
RETURN count(p) AS demoPlayersLeft;
// EXPECTED: 0
