# AGENTS.md

## Cursor Cloud specific instructions

### Services

| Service | Purpose | How to start |
|---------|---------|--------------|
| **MongoDB** | Primary data store | `sudo docker run -dp 27017:27017 -v mongo:/data/db --name mongo --restart=always mongo` |
| **Docker daemon** | Required for Testcontainers (tests) and MongoDB | `sudo dockerd &>/tmp/dockerd.log &` (wait ~3s before using) |

### Build, test, and run

See `CLAUDE.md` for standard commands (`mvn clean package`, `mvn test`, application CLI flags).

### Non-obvious caveats

- **JDK 25 is required.** The VM snapshot has Temurin 25 installed at `/usr/lib/jvm/jdk-25.0.2+10`. `JAVA_HOME` and `PATH` are set in `~/.bashrc`. If a future JDK 25 patch changes the directory name, update the bashrc export accordingly.
- **Maven** is installed at `/opt/apache-maven-3.9.9` with a symlink at `/usr/local/bin/mvn`.
- **Docker must be running** before `mvn test` because Testcontainers spins up a MongoDB container for integration tests. Start dockerd first, then run tests.
- **MongoDB must be running** before the application can start. The app connects to `mongodb://127.0.0.1:27017/smhy` by default.
- The application is a **CLI runner**, not a web server. It runs a forecast/report cycle and exits. Use `--forecast --locations <name>` to test a single location quickly.
- The `--reports` flag generates HTML files to `/tmp/smhy/` (configurable via `smhy.output.dir`).
- Tests marked with `@IntegrationTest` require the `itest` Spring profile and are skipped by default (`mvn test` runs 63 tests, 7 skipped).
- There is no Maven wrapper (`mvnw`) in this repo; Maven must be installed separately.
