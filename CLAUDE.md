# Harmony

Minecraft 1.7.10 Forge mod (Forge 10.13.4.1614) about pets: genetics (traits), happiness, and tricks
taught with noteblocks. Read `ROADMAP.md` first: it has the vision, the design decisions already made
with the owner, and the phased task list. Don't re-decide things it already settles.

The owner plays this with friends and plans to publish it. It's unreleased, so there are no player
worlds to preserve: when a save format changes, bump the version and assume fresh worlds. Don't write
migration or compatibility code for old saves.

## Build and run

- Windows machine. Gradle 8.14 with RetroFuturaGradle (RFG) 1.4.9. Gradle runs on Java 17+ (Zulu 21 is
  installed); RFG downloads Java 8 to compile the mod.
- If Java fails with `Unrecognized option: -XmX1g`, the shell has a typo'd `JAVA_OPTS`; clear it for
  your commands (`JAVA_OPTS=`) and tell the user.
- `./gradlew build` builds `build/libs/harmony-1.0.jar` (the release jar, reobfuscated).
- `test.cmd` starts a dev server plus clients that join it, each in its own window. See the README.
  `-Players A,B` for several clients, `-Lag/-Jitter/-LagSpike` for a lag proxy, `-Release` to run the
  release jar in the real obfuscated game.
- Gradle tasks: `runClient`, `runServer` (dev game, folders `run/` and `run/server`), `runObfClient`,
  `runObfServer` (release jar, `run/obfuscated/...`). `-Pplayer=Name`, `-Pconnect=host:port`.
- `run/` is gitignored and holds the owner's test worlds. Never commit it, and don't modify their
  worlds; test on a throwaway world instead (the server accepts `--world <name>`, which needs an init
  script adding it to the run task's `extraArgs`; delete the world afterwards).
- Decompiled Minecraft/Forge sources (MCP names) are in `build/rfg/mcp_patched_ated_minecraft-sources.jar`
  after a build. Read them instead of guessing at vanilla behavior.

## Code layout

`src/main/java/com/harmony/harmonymod/`
- `HarmonyMod` - mod entry point, config (per-species trait lists, breeding values), registration.
- `HarmonyProps` - per-animal data (`IExtendedEntityProperties`): traits, happiness, tricks. Saved as
  versioned NBT (`DATA_VERSION`).
- `Traits` - trait slots, inheritance, attribute modifiers.
- `tricks/` - `TrickHandler` (AI task + learned phrases), `ActionSet` (candidate tricks per note),
  `TrickEnum` (trick bits and saved names), `Trick` subclasses.
- `sounds/SoundDB` - noteblock events -> animals within 16 blocks.
- `aitasks/` - happiness-aware wander and breeding AI replacing vanilla's.
- `HarmonyNetwork` - the mod's network channel ("harmony"); each message type has its own id.
- `HarmonyCommand` - `/harmony spawn <animal> [traits]`, spawns animals with chosen traits for testing
  (works from the server console too, spawning at world spawn).
- `sync/` - sends animals' data to clients. `ownership/` - bonding and adoption papers.
- `horsefix/` - client-driven horse riding (`HorseControl` server side, `HorseControlClient`).
- `client/` - client-only rendering (learning particles). `items/` - the diagnostics debug item.

## Rules that bite in 1.7.10

- No reflection by MCP name (`getDeclaredMethod("jump")` etc.): names differ in the real game, so it
  only works in dev. Call public methods, copy vanilla logic, or use an access transformer. Referencing
  `func_xxx`/`field_xxx` names directly in code is fine, the build remaps them.
- Client-only classes (`net.minecraft.client.*`) must stay out of code that loads on a dedicated
  server. Put client code in `@SideOnly(Side.CLIENT)` classes only reached from client paths.
- Saved names are permanent: trait enum names, `TrickEnum` names and `Trick.getSaveType()` values are
  written to NBT. Add new ones; don't rename existing ones.
- Mod packet handlers (SimpleNetworkWrapper) run on the main thread in 1.7.10.
- Logging is `System.out.println("HarmonyMod: ...")`. Match the surrounding code's style: tabs in
  most files, `/* */` comments, and keep each file's existing line endings (many are CRLF).

## Testing

- Verify changes yourself where you can: build, then run the dev server on a throwaway world,
  drive it with console commands on stdin (`summon`, `setblock` a noteblock + `redstone_block` to
  play a note, `save-all`, `stop`), and inspect or edit entity NBT in the region files. Harmony data is
  under the `harmony_HarmonyProps` key on each entity.
- Never run a test server in the owner's folders (`run/server`, `run/obfuscated/server`) or on port
  25565: they may have a server running there. Point the run task's `workingDir` at your own folder
  under `run/` and set a different `server-port` in its `server.properties`.
- Run the release jar (`runObfServer` / `test.cmd -Release`) for anything that could behave
  differently outside the dev environment.
- You can't see the game. For anything visual (UI, rendering, animation, feel), give the owner short
  numbered test steps using `test.cmd`.

## Workflow

- One branch and one pull request per roadmap item, branched from an up to date `master`. Don't
  bundle items. When done, tick the item in `ROADMAP.md` in the same PR.
- `gh` isn't installed: push the branch, then give the owner the compare link
  (`https://github.com/LokiW/Harmony/compare/master...<branch>`) and a ready-to-paste PR title and
  description (summary, testing, any manual test steps).
- Commit messages: a short summary line, a body explaining why, ending with the co-author line the
  harness gives you.
- Other agents may be working in parallel. Work in your own clone or git worktree, keep to your item's
  files where possible, and run servers on a free port (`test.cmd -Port <n>`) or a uniquely named
  world. If your item needs something shared (a new network message id, a `DATA_VERSION` bump, a new
  saved field), say so in your PR description so the owner can spot conflicts.
- A new clone or worktree has an empty `run/`, so the first server run needs `eula.txt`. Accepting the
  Minecraft EULA is the owner's decision: ask them to run `test.cmd` once there (it prompts), or to
  confirm you may copy `eula.txt` from their main checkout. Never write `eula=true` on your own.
