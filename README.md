# Rock, paper, scissors, lizard, Spock on Neo4j

A command-line game in Java that keeps its rules, and every game you play, in a Neo4j graph.

This is the code for Part 4 of the "Neo4j for Developers" series: *Build a Small Game on Neo4j with the Java Driver*.

## What it does

You play rock, paper, scissors, lizard, Spock against the computer. The rules are stored in Neo4j as relationships between five gesture nodes. The game asks the graph who wins each round. It also stores every game as a graph: a player, a game and its rounds, linked in order. When you stop, it asks the stored games a few questions, such as which gesture you play after a loss.

## Requirements

- Java 21 or later. Tested with Java 21 and Java 25.
- Docker, to run Neo4j.
- Neo4j 2026.10. Tested with Community Edition 2026.10.1.
- Neo4j Java driver 6.3.0. Gradle downloads it for you.

You do not need to install Gradle. The project includes the Gradle wrapper, which downloads Gradle 9.8.2 on the first run.

The commands in this file are for a Unix shell. They were tested on macOS.

## Quick start

1. Get the code.

   ```bash
   git clone https://github.com/mohanedmoh/rock-paper-scissors-spock-lizard.git
   cd rock-paper-scissors-spock-lizard
   ```

2. Start Neo4j in Docker. Use a password of your own in place of `change-this-password`, and use the same one in the next steps.

   ```bash
   docker run -d --name neo4j-game -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/change-this-password neo4j:2026.10
   ```

   The first run downloads the image, so it can take a minute.

3. Wait until Neo4j answers. This loop ends when the server is ready, usually after a few seconds.

   ```bash
   until docker exec neo4j-game cypher-shell -u neo4j -p change-this-password "RETURN 1" > /dev/null 2>&1; do sleep 2; done; echo "Neo4j is ready"
   ```

4. Give the game the password.

   ```bash
   export NEO4J_PASSWORD=change-this-password
   ```

5. Run the demo. It plays 30 fixed rounds and prints the history. The first run also downloads Gradle and the driver.

   ```bash
   ./gradlew run --args=demo
   ```

   You see one line per round, then the history. The driver also prints two `INFO` lines, one when it opens the connection and one when it closes it. That is normal.

6. Ask the stored games the same questions in Cypher.

   ```bash
   docker exec -i neo4j-game cypher-shell -u neo4j -p change-this-password < cypher/part4_history.cypher
   ```

   The expected numbers are in the `EXPECTED` comments in the script.

7. Remove the demo data.

   ```bash
   docker exec -i neo4j-game cypher-shell -u neo4j -p change-this-password < cypher/part4_cleanup.cypher
   ```

When you have finished, stop and remove the container:

```bash
docker rm -f neo4j-game
```

## How to play

Start the game:

```bash
./gradlew run --console=plain
```

The plain console stops Gradle's progress display from getting in the way of the prompts.

The game asks for your name. Then, each round, type a gesture or a number from 1 to 5:

| Number | Gesture |
| :----- | :------ |
| 1 | rock |
| 2 | paper |
| 3 | scissors |
| 4 | lizard |
| 5 | spock |

Names are not case-sensitive. Anything else gets a reminder and the round is asked again. The computer picks at random. If you both pick the same gesture, the round is a draw.

**Type `q` to quit.** The game then prints your history.

The ten rules:

- scissors cuts paper
- paper covers rock
- rock crushes lizard
- lizard poisons spock
- spock smashes scissors
- scissors decapitates lizard
- lizard eats paper
- paper disproves spock
- spock vaporizes rock
- rock crushes scissors

Each gesture beats two others and loses to two others.

## The graph model

There are four kinds of node:

- `Gesture` has a `name`. There are five of them: rock, paper, scissors, lizard and spock.
- `Player` has a `name`.
- `Game` has an `id` and the time it `started`.
- `Round` has its `number` and its `outcome`. The outcome is `WIN`, `LOSS` or `DRAW` from the player's point of view.

The rules are relationships between gestures. They point from the winner to the loser, and the type is the verb. There are ten of them, with nine different types, because `CRUSHES` is used twice.

```text
(:Gesture {name: 'rock'})-[:CRUSHES]->(:Gesture {name: 'scissors'})
```

The games are stored like this:

```text
(:Player)-[:PLAYED]->(:Game)-[:HAS_ROUND]->(:Round)

(:Game)-[:HAS_ROUND]->(:Round {number: 1})
(:Game)-[:HAS_ROUND]->(:Round {number: 2})
(:Game)-[:HAS_ROUND]->(:Round {number: 3})

(:Round {number: 1})-[:NEXT]->(:Round {number: 2})-[:NEXT]->(:Round {number: 3})

(:Round)-[:PLAYER_MOVE]->(:Gesture)
(:Round)-[:COMPUTER_MOVE]->(:Gesture)
```

Every round of a game hangs off the game. `NEXT` joins each round to the one after it, so a game of 30 rounds has 29 `NEXT` links. `PLAYER_MOVE` and `COMPUTER_MOVE` point at the same five gesture nodes that hold the rules.

The game also creates a uniqueness constraint named `gesture_name` on `Gesture.name`.

## Project layout

```text
.
├── build.gradle                    Gradle build: the driver, Java 21, the main class
├── settings.gradle                 Project name
├── gradlew, gradlew.bat            Gradle wrapper scripts (Unix, Windows)
├── gradle/wrapper/                 Wrapper jar and the Gradle version to download
├── src/main/java/demo/game/
│   ├── Game.java                   Settings, the menu, the rounds, the demo and the computer's move
│   ├── Rules.java                  Loads the rules and asks the graph who wins
│   └── History.java                Saves every round and answers the five history questions
├── cypher/
│   ├── part4_history.cypher        The history questions, for the demo player
│   └── part4_cleanup.cypher        Removes the demo player's data
├── LICENSE
├── .gitignore
└── .gitattributes
```

## The Cypher scripts

Run both with `cypher-shell` inside the container, as in the quick start.

**`cypher/part4_history.cypher`** asks the history of the player `demo`. It holds the five questions that the game asks at the end of a game:

1. How often did I win, lose or draw?
2. Which gesture do I play most?
3. What do I play after a loss?
4. Which two gestures do I play one after the other?
5. What is my longest winning streak in one game?

It also has three checks first: the number of gestures and rules, the number of games and rounds the demo stored, and the number of `NEXT` links. Run the demo before this script. The `EXPECTED` comments give the numbers for the demo. The `NEXT` count is 29 only if the demo player is the only one who has played.

```bash
docker exec -i neo4j-game cypher-shell -u neo4j -p change-this-password < cypher/part4_history.cypher
```

**`cypher/part4_cleanup.cypher`** removes the player `demo` with their game and rounds. It leaves the five gestures and ten rules in place, then prints `demoPlayersLeft`, which should be `0`. It removes only `demo`. The demo also does this by itself at the start of every run.

```bash
docker exec -i neo4j-game cypher-shell -u neo4j -p change-this-password < cypher/part4_cleanup.cypher
```

## Configuration

The game reads four environment variables.

| Variable | Default | Meaning |
| :------- | :------ | :------ |
| `NEO4J_URI` | `bolt://localhost:7687` | Where the server is |
| `NEO4J_USER` | `neo4j` | The user name |
| `NEO4J_PASSWORD` | none | Required. The game stops with a message if it is not set |
| `NEO4J_DATABASE` | `neo4j` | The database to use |

An empty `NEO4J_URI`, `NEO4J_USER` or `NEO4J_DATABASE` counts as not set, so the default is used. There is no such fallback for the password.

Example, for a server on another port:

```bash
export NEO4J_URI=bolt://localhost:7688
```

## Troubleshooting

### Wrong password

The game stops with:

```text
AuthenticationException: The client is unauthorized due to authentication failure.
```

The password in `NEO4J_PASSWORD` is not the one you gave to `NEO4J_AUTH` in `docker run`. Check the `export` line. If you also set `NEO4J_USER`, check that it is `neo4j`.

If you have forgotten the password, remove the container and start again from step 2. This deletes its data.

```bash
docker rm -f neo4j-game
```

If `NEO4J_PASSWORD` is not set at all, the game prints `Set NEO4J_PASSWORD (and NEO4J_URI, NEO4J_USER, NEO4J_DATABASE if they differ from the defaults).` and stops. Run step 4 again in the same terminal.

### Port 7687 is already in use

Docker stops with:

```text
Bind for 0.0.0.0:7687 failed: port is already allocated
```

Another program or container is using the port. Often it is a Neo4j you started earlier. To see what it is:

```bash
lsof -nP -iTCP:7687 -sTCP:LISTEN
docker ps
```

Stop that program or container, or start this one on other ports and tell the game where it is:

```bash
docker run -d --name neo4j-game -p 7475:7474 -p 7688:7687 -e NEO4J_AUTH=neo4j/change-this-password neo4j:2026.10
export NEO4J_URI=bolt://localhost:7688
```

Port 7474 can clash in the same way. The error then names 7474.

### Java is too old

With Java 17, Gradle starts but the build stops with:

```text
> Java compilation initialization error
    error: invalid source release: 21
```

With Java 11 or older, Gradle itself refuses to start:

```text
Gradle requires JVM 17 or later to run. Your build is currently configured to use JVM 11.
```

Both mean the same thing. The game needs Java 21 or later. Check your version:

```bash
java -version
```

Install Java 21 or later. If you have several versions, pick one for this terminal. On macOS:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Connection refused, or Neo4j is still starting

The game stops with one of these:

```text
ServiceUnavailableException: Failed to establish connection with the server
ServiceUnavailableException: Unable to connect to localhost:7687, ensure the database is running and that there is a working network connection to it.
```

The first usually means the container is up but Neo4j has not finished starting. Run the wait loop from step 3, then try again. The log says `Started.` when the server is ready:

```bash
docker logs neo4j-game
```

The second means nothing is listening on that address. Check that the container is running with `docker ps`, and that `NEO4J_URI` matches the port you published.

### The unique constraint already exists

The game does not fail on this by itself. It creates the constraint with `IF NOT EXISTS`, so it is safe to start it again and again, even if a constraint with another name already covers `Gesture.name`.

You see the error when you create the constraint by hand, without `IF NOT EXISTS`:

```text
22N65: data exception - equivalent constraint already exists.
```

Nothing is wrong. The constraint is already there. To check, run `SHOW CONSTRAINTS`. To create it only when it is missing, add the words:

```cypher
CREATE CONSTRAINT gesture_name IF NOT EXISTS FOR (g:Gesture) REQUIRE g.name IS UNIQUE
```

A different message is a real problem. If the game stops with `Unable to create Constraint ... Both Node(...) and Node(...) have the label Gesture and property name`, the database already holds two nodes with the same gesture name, and the constraint cannot be created. This happens if gestures were loaded before without the constraint. If you have no games to keep, remove the gestures. The game builds them again at the next start.

```bash
docker exec neo4j-game cypher-shell -u neo4j -p change-this-password "MATCH (g:Gesture) DETACH DELETE g"
```

## Licence and credits

Licence: Apache License 2.0. See [LICENSE](LICENSE).

Written by Mohaned Hassan ([github.com/mohanedmoh](https://github.com/mohanedmoh)). Bug reports and questions: [github.com/mohanedmoh/rock-paper-scissors-spock-lizard/issues](https://github.com/mohanedmoh/rock-paper-scissors-spock-lizard/issues).

The rules are those of Sam Kass and Karen Bryla, who added lizard and Spock to the classic game: <https://www.samkass.com/theories/RPSSL.html>.

The game uses the [Neo4j Java driver](https://github.com/neo4j/neo4j-java-driver).
