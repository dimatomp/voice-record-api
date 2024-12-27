# Voice Recording Storage API

My solution for an interview coding task to create a simple REST API for storing and retrieving audio voice records.

For each registered phrase & user, the app allows to store arbitrary number of voice records and provides the last recorded one on demand.

## Try it

Only Docker Compose is needed to run the app locally:
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
```