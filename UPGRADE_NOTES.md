# Upgrade notes

Checked on 4 November 2026.

## Summary

One thing was upgraded: the Gradle wrapper, from 9.8.0 to 9.8.2. The Neo4j Java driver was already at the newest release, 6.3.0. No code changed. The game runs against Neo4j 2026.10.1 and prints what the article shows.

## Versions

| Item | Old | New | Notes |
| :--- | :-- | :-- | :---- |
| Neo4j Java driver (`org.neo4j.driver:neo4j-java-driver`) | 6.3.0 | 6.3.0 | Already the newest. See below. |
| Gradle wrapper | 9.8.0 | 9.8.2 | See below. |
| Gradle plugin `application` | built in, no version | no change | Part of Gradle itself. |
| Other plugins | none | none | |
| Test libraries | none | none | There is no `src/test`. |
| Java source and target level | 17 | 17 | Not raised. See below. |
| Neo4j server used for the test | not recorded | 2026.10.1 Community | Docker image `neo4j:2026.10`. |

### Neo4j Java driver

- Maven Central lists `org.neo4j.driver:neo4j-java-driver:6.3.0` as both `<latest>` and `<release>`. The `.pom` and `.jar` exist, and the SHA-1 of the jar matches the one Maven Central publishes.
- GitHub marks the 6.3.0 release as "Latest", not as a pre-release. It was published on 24 September 2026.
- The older notes in `README.txt` of the article package said the coordinate had not been read from Maven Central. It has now been checked.
- No alpha, beta or release candidate is newer.

### Gradle wrapper

- `services.gradle.org/versions/current` gives 9.8.2 as the newest stable release. 9.9.0-rc-1 exists and was skipped because it is a release candidate.
- 9.8.1 fixes three high-rated vulnerabilities. One of them is a build fetching malicious artefacts when a repository fails to set up its SSL connection. 9.8.2 fixes a false "graph wasn't stable enough" warning. This is the reason to upgrade.
- The wrapper task was run twice, as Gradle advises, so the jar and scripts come from 9.8.2.
- `gradle-wrapper.properties` changed on one line, the `distributionUrl`. `gradlew` and `gradlew.bat` did not change.
- The SHA-256 of the new `gradle-wrapper.jar` is `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`. It matches the `wrapperChecksum` that Gradle publishes for 9.8.2.

## Java level

The level was not raised.

- The driver 6.3.0 class files are major version 61. That is Java 17.
- Gradle 9.8.2 needs Java 17 or later to run. With Java 11 it stops with: `Gradle requires JVM 17 or later to run. Your build is currently configured to use JVM 11.`
- `./gradlew clean build` passed on Temurin 17.0.14 and on Temurin 21.0.13. The demo ran on Java 17.
- The README says "Java 17 or later". The article says to use Java 21, because Part 3 needs it. Part 4 does not.

## Driver release notes: breaking changes

Read from the GitHub releases for 6.0.0 to 6.3.0.

- 6.0.0 deleted APIs that had been deprecated. These include the transaction functions `readTransaction` and `writeTransaction` (#1647), `RxSession` (#1644), multi-value bookmarks (#1646), some session methods (#1649), `TrustStrategy.certFile()` (#1650) and `Notification#severity()` (#1651). It also deprecated the `Logging` abstraction in favour of `System.Logger` (#1648).
- 6.1.0 turned on one automatic retry for auto-commit queries that fail with an idempotent error. It can be switched off.
- 6.2.0 added support for the Neo4j UUID type and `QueryProfile`. 6.3.0 added Property Encryption.

None of this affects the project. The code already uses `executeRead`, `executeWrite`, `executeWriteWithoutResult`, try-with-resources for `Driver` and `Session`, and `verifyConnectivity()`. The only auto-commit call is the `CREATE CONSTRAINT ... IF NOT EXISTS` in `Rules.load`, and a retry of that is harmless.

## What changed in the code

Nothing under `src/`. No compile error, no deprecation warning and no API change came from the upgrade.

Files changed compared with the `game-demo` folder of the article package:

- `gradle/wrapper/gradle-wrapper.properties` and `gradle/wrapper/gradle-wrapper.jar`: Gradle 9.8.2, as above.
- `.gitattributes`: the repository's existing file gained a rule, `*.bat text eol=crlf`, so that `gradlew.bat` keeps its Windows line endings. The package had the same rule.
- `README.md`: rewritten.
- `UPGRADE_NOTES.md`: this file.

## Warnings left in the build output

`./gradlew clean build` succeeds on 9.8.0 and on 9.8.2. It prints no compiler warning, also with `-Xlint:all`, and no Gradle deprecation warning with `--warning-mode all`. The `test` task reports `NO-SOURCE` because there are no tests.

Two messages remain. Neither is an error.

- Gradle prints "Consider enabling configuration cache to speed up this build". This is a hint. It was left alone.
- When the game runs, the driver prints two `INFO` lines to the console, for example `INFO: Driver instance 270397815 created for server uri 'bolt://localhost:7687'` and `INFO: Closing driver instance 270397815`. The driver 6 logs through `System.Logger`. These lines are not in the sample output in the article. They were not removed, because that would change the code.

## Test against a real server

- Server: Neo4j Kernel 2026.10.1 Community, from `neo4j:2026.10`, in a new container with no data volume. Cypher 5 and 25 are both available. The default database is `neo4j`.
- `NEO4J_PASSWORD=change-this-password ./gradlew run --args=demo` ended with exit code 0. The 30 rounds and the history are identical to the output in the article. The only difference is the two `INFO` lines above.
- The demo ran several times. The rules stayed at 5 gestures and 10 rules, and the demo player always had 1 game and 30 rounds.
- `cypher/part4_history.cypher` and `cypher/part4_cleanup.cypher` ran with exit code 0.
- The container was removed afterwards.

### Comparison with the EXPECTED comments

The EXPECTED comments came from a Python model, not from a server. Every one of them matches.

| Query | Expected | Actual | Match |
| :---- | :------- | :----- | :---- |
| 0. Gestures and rules | 5 \| 10 | 5 \| 10 | yes |
| 0b. Games and rounds of `demo` | 1 \| 30 | 1 \| 30 | yes |
| 0b. `NEXT` links | 29 (if only the demo player has played) | 29 | yes |
| 1. Win, lose or draw | DRAW 7, LOSS 8, WIN 15 | DRAW 7, LOSS 8, WIN 15 | yes |
| 2. Most played gesture | rock 19, lizard 4, scissors 4, paper 3 | rock 19, lizard 4, scissors 4, paper 3 | yes |
| 3. Played after a loss | rock 5, paper 2, lizard 1 | rock 5, paper 2, lizard 1 | yes |
| 4. Pairs, top 5 | rock-rock 12, lizard-rock 4, rock-scissors 4, then two pairs with a count of 2 | rock-rock 12, lizard-rock 4, rock-scissors 4, paper-rock 2, rock-lizard 2 | yes |
| 5. Longest winning streak | 5 | 5 | yes |
| Cleanup: `demoPlayersLeft` | 0 | 0 | yes |

The EXPECTED comments were not edited.

## Left alone, for you to decide

- The header of `cypher/part4_history.cypher` still says "STATUS: NOT YET RUN" and names the container `neo4j-airports`. The README uses `neo4j-game`.
- The "Author's note" in the article says the output comes from a model. The numbers now match a real server.
- The article shows Java 21. This project needs only Java 17.
- The wrapper properties have no `distributionSha256Sum`. Adding one checks the Gradle download. Gradle publishes `21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607` for `gradle-9.8.2-bin.zip`.
- `game.java` in the repository root is the first version of the game from 2022. Gradle does not build it. It was kept.

## Sources

- Maven Central: `https://repo1.maven.org/maven2/org/neo4j/driver/neo4j-java-driver/maven-metadata.xml`, and the 6.3.0 `.pom`, `.jar` and `.jar.sha1`.
- Gradle: `https://services.gradle.org/versions/current` and `https://services.gradle.org/versions/all`, and the 9.8.1 and 9.8.2 release notes on `docs.gradle.org`.
- Driver release notes: `https://github.com/neo4j/neo4j-java-driver/releases`.
- Docker image tags: Docker Hub, `library/neo4j`.
