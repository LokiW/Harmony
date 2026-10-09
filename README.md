# Harmony
Minecraft mod focused around better pets

## Building

Needs a JDK 17+ to run Gradle (21 recommended); Gradle downloads the Java 8 used to compile the mod.

    ./gradlew build

The mod jar ends up in `build/libs/`.

## Testing

`test.cmd` starts a local dedicated server with the mod plus clients that join it, each in its own window:

    .\test.cmd                       # server + one client (Developer)
    .\test.cmd -Players Alex,Steve   # server + two clients, e.g. to test two riders
    .\test.cmd -ServerOnly           # just the server
    .\test.cmd -NoServer             # clients joining a server that's already running
    .\test.cmd -Lag 300 -Jitter 50   # clients join through a proxy adding 300 ms ping (+/-50 ms)
    .\test.cmd -Lag 200 -LagSpike 1500 -Players Alex,Steve -LagPlayers Alex
                                     # only Alex lags, and freezes for 1.5 s every 10 s

The first run asks you to accept the Minecraft EULA for the server. Server files live in `run/server`,
extra players in `run/players/<name>`, and the listed players are made server operators.
The lag options need Python, see `scripts/lagproxy.py`.
Single player still works with `./gradlew runClient`.
