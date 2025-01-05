# Voice Recording Storage API

My solution for an interview coding task to create a simple REST API for storing and retrieving audio voice records.

For each registered phrase & user, the app allows to store arbitrary number of voice records and provides the last recorded one on demand.

## Try it

Only Docker Compose is needed to run the app locally. Tested on: Docker Desktop (Windows, WSL2), Podman (Mac)
```sh
docker compose up
```

A few commands to play around and get started:
```sh
curl http://localhost:8080/audio/user/1/phrase/1 --form audio_file=@src/test/resources/input.m4a

curl http://localhost:8080/audio/user/1/phrase/1/m4a > test.m4a
```

## Supported media containers/codecs

* **User input**: `.m4a` files containing audio in AAC or ALAC format
* **DB storage**: everything is stored in WAV format with PCM float 32bit encoding

## Tests

Some additional dependencies required to run available automated tests:
* At least one JDK installation (tested with OpenJDK 17 and 21)
* MySQL server (you can start one quickly with `docker compose up mysql`)
* FFMpeg installation available in PATH (because tests use `ffprobe` to verify consistency of audio output)

With both above parts ready, tests can be run with Gradle:
```sh
./gradlew test
# or, if you have Gradle 8 installed:
gradle test
```

## Some improvement thoughts/ideas

* **Choice of technical stack:** I used Kotlin & Spring Boot because the JD mentions them, but in real life I would
likely consider another language that allows for straightforward integration with `libav` or a similar mature &
feature-rich media processing backend. Maybe C/C++, Rust, ...?
* **Backend design:** Again, I built everything in a monolith because technical stack was predefined, but in theory
there could be e.g. a small C/C++ microservice for transcoding, integrated with main Java backend that would do DB
interaction & business logic.
* **Database:** Using MySQL for simplicity and because I'm most familiar with it, but in real life I would only keep up
with this choice after running PT and ensuring that MySQL works well with the application's heterogenous data (relational
tables, medium-sized BLOBs) and expected storage volume in production.
* **Streamed processing:** The current implementation always propagates audio as a byte stream during transcoding &
database r/w operations since it is a more memory-efficient approach, but the M4A input from user is fully stored in
memory before its processing starts. Thankfully, Spring Boot has some default setting to limit size of submitted files,
but theoretically such input should be accepted from client in a byte stream as well, as opposed to being read one-shot.
* **CI/CD, Observability and Security:** The code in this repo is clearly not ready for an automated non-local deployment.
In real life, major improvements would have to be made for config/secret management, artifact building, container
hardening, request authn/authz, DB authorization, separation of DB schema management from backend, input sanitization,
observability, etc.
