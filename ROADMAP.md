# Harmony Roadmap

## Vision

Pets you invest in, take adventuring and command with music.

Vanilla pets are cute but fragile, and they fall far behind modded Minecraft's power levels, so nobody
takes them anywhere dangerous. Harmony adds gameplay around pets rather than a flat buff: you breed them
for traits, keep them happy in good homes, train them with noteblocks, and over time they become strong
enough to be a real option in modded play.

Noteblocks are the second pillar. Music exists in Minecraft but doesn't do anything; in Harmony it is
how you talk to animals, from a single noteblock, from a handheld instrument while adventuring, or from
redstone automation while you're away.

Built for playing with friends first, and to be published. Targets 1.7.10, so it can hook into
Thaumcraft 4, Witchery and Botania when they're installed.

## Design

### Animals

- All passive and neutral animals are Harmony animals: cows, sheep, pigs, chickens, mooshrooms,
  wolves, ocelots, horses, etc.
- Hostile mobs are considered case by case later (pet spiders yes, zombies no).
- Animals from other mods are added through config and integrations.

### Genetics (traits)

- Every animal has three trait slots. Wild animals roll them from a per-species list in the config,
  babies inherit each slot from a random parent.
- Stacked traits scale exponentially, so three Hardy is far stronger than one. This is what makes a
  well bred pet strong enough for modded play.
- Three copies of a trait is the max, and strong: speed x3.25 (Fast), health x8 (Hardy), attack x7
  (Vicious), jump x2.74 and fall damage x0.22 (Jump), 4 extra babies per parent (Fertile). Each copy
  multiplies, so one copy is the cube root of that.
- Bad traits undo one copy of their counterpart and have to be bred out: Slow, Frail, Weak, Clumsy.
  Behavioral ones (e.g. Timid) later.
- Wild animals usually have zero or one trait. Babies have a 5% chance per slot of a new trait from
  the species' wild list instead of inheriting one.
- `/harmony spawn <animal> [traits]` spawns an animal with chosen traits for testing.
- Magical traits add extra strength and are imparted by rituals (see Magic).

### Happiness

- Gives a mechanical reason to build nice looking pens.
- Happiness runs 0-20, new animals start at 5. Animals breed at 10+ (configurable) and are unhappy
  below 5. Happiness never affects tricks, so pets still work on rough adventures.
- Factors today: wandering to grass (+1), being hurt, confined, overcrowded (more than 8 animals in a
  5x5 area) or near monsters (-1 each).
- Feeding an animal its breeding food: if it's happy it looks for a mate right away, otherwise it gets
  happier (+3). Either way it shows its mood.
- Only kept animals (fed, trained, bonded, tamed, or born to kept parents) breed on their own, so wild
  herds stay vanilla. Nothing breeds with 16+ of its species within 16 blocks (configurable).
- Mood particles (green sparkles happy, white puffs content, grey clouds unhappy) show when a kept
  animal's mood changes and when any animal is fed its breeding food, so calm pens stay quiet.
- Happiness alone isn't synced to clients, only mood changes, to keep network traffic down.
- Animals breeding on their own wait between litters, per species (config, e.g. cows 20 minutes).
  Feeding still breeds after vanilla's 5 minutes.
- Planned rebalance: wild conditions only keep an animal content, they don't make it happy. Grass
  never raises happiness, it lowers it when missing; overcrowding is a huge penalty. Domestication
  comforts (hay, water troughs, shelter with sky access, bedding) are what make animals happy enough
  to breed.
- Later: water, flowers, light, and preferences that differ by species (set in config).
  No block variety scoring, it's too easy to cheese.

### Tricks and music

- Animals listen for phrases: the notes they hear until a 1 second pause, up to 8 steps. A single note
  is a one note phrase, so core tricks can be taught with one noteblock and no redstone. Notes played
  in the same tick form a chord, which counts as one step.
- An animal acts on a phrase immediately, unless a longer phrase it knows starts with the same notes,
  in which case it waits for the pause. Simple vocabularies stay instant.
- Training: feed a glistering melon, play a phrase, the animal tries a trick, feed again to reward.
  Repeated rewards narrow the phrase down to one trick.
- Tricks chain: e.g. a location followed by Guard means guard that location.
- Follow is a heel: the animal stays within a couple of blocks of its owner (or the nearest player),
  speeding up when behind, until told otherwise. Come Back walks to them and stops.
- A compass on an animal sets its next place (1, 2, 3, then 1 again) to where it stands; the Go to
  Place tricks walk there.
- Teaching by demonstration: when an animal in training hears a phrase, it is far more likely to try
  the trick a nearby animal performs for that phrase, which makes it much quicker to reward and
  narrow down. Babies start with no tricks and learn this way from trained animals.
- A trained animal's tricks can be cleared to start teaching it again.
- Training treats (dungeon, mineshaft, temple, stronghold and village loot) instantly teach one trick
  for the phrase written on the treat. `/harmony treat <trick> [phrase]` gives one for testing.
- Handheld instruments store phrases so tricks can be used while adventuring. They add to noteblocks,
  they don't replace them. One instrument per noteblock sound (harp, bass drum, snare, clicks, bass);
  the harp is first. Phrases are written on a small sheet of music (click lines and spaces, notes in
  the same column are a chord, right click for sharps), 4 slots per instrument. Bonded animals only
  obey their owner's instruments; Follow and Come Back go to whoever played.
- Noteblock automation: redstone driven noteblocks command animals with nobody around.

### Ownership (bonding)

- Optional. Unbonded animals obey anyone and any noteblock.
- Bond by naming the animal with a name tag. Name tags get a crafting recipe (string, lead, paper,
  black dye). Wolves, ocelots and horses can only be bonded by their tamer.
- Only the owner can leash, rename, train (melon), set the respawn point of, or use instruments on a
  bonded animal. Bonded animals still obey all noteblocks (noteblocks don't know who built them), so
  automation keeps working.
- Transfer with adoption papers (paper, black dye, name tag). Right clicking signs them: the first
  signature is the giver (the owner), the second the receiver. Right clicking an animal attaches it,
  in any order, so a breeder can sign and let a friend pick from the litter. When complete the animal
  changes owner (and tamer, for tameable animals) and the papers are used up.
- No way to release a bond back to nobody for now.

### Protecting pets

- Golden apple: sets a respawn point, used up on the next death.
- Enchanted golden apple: permanent respawn point. Modded apples (e.g. diamond apples) can be
  configured into the same tiers.
- Fainting is instant: the animal is healed (fire and potion effects cleared) and wakes up at its respawn
  point, or the nearest spot with room. A plain golden apple after an enchanted one only moves the point.
- If the respawn point isn't loaded (far away, another dimension) the animal waits in spirit form (saved
  with the world) until it is. Bonded animals tell their owner when they faint, wait and wake up.
- Respawning on the owner instead keeps a pet in the fight, so it's an expensive upgrade added to the
  animal later, not the default.
- Pets follow their owner through portals into other dimensions, and when the owner teleports.
- Otherwise pets path toward their owner much more aggressively instead of teleporting to catch up,
  so slow animals (e.g. Slow trait) fall behind.

### Gear and buffs

- Pet armor, which can be enchanted.
- Feeding potions to animals.
- Galvanize trick: a short term tankiness buff.

### Magic

- Rituals are songs: long phrases played at the right place with the right items.
- Rituals impart magical traits, and later breed animals into magical animals (e.g. a pegasus).
- Integrations: Thaumcraft (detect node trick, aspects), Botania, Witchery. Optional, only active when
  the mod is installed.

## Phases

### Phase 0: Foundation

- [x] Save animal data as versioned NBT instead of Java serialization, so updates don't break saves
- [x] Remove code that only works in the dev environment (Jump trick reflects on `jump` by name)
- [x] Sync animal state (traits, happiness, tricks) to clients, needed for the overlay
- [x] Ownership data and rules (bonding, transfer, reuse vanilla owners)
- [x] Way to test the release jar in a normal Forge install
- [x] Fix known bugs: Sit doesn't sit, wander AI's crowding check uses x instead of y for its box,
      animals scared of water, GO without a target teleports the animal south every half second
- [x] Remove leftovers: example lang entries, HarmonyHorse spawn egg, unused imports and debug prints

### Phase 1: MVP

- [x] Reliable core tricks: follow, stay/sit, go to, guard, attack, come back
      - Bug: animals sometimes clip through the floor after doing the Jump trick (jump now happens in
        the animal's own update; confirm in game)
- [x] Learning mode indicator: question marks while an animal is learning, hearts when it learns
- [x] Training treats: loot that instantly teaches a trick for a fixed phrase
- [x] Phrase based listening (single notes still instant)
- [x] First handheld instrument (harp)
- [x] Bonding and transfer (done with Phase 0 ownership)
- [x] Golden / enchanted golden apple respawn tiers, spirit form waiting
- [x] Pets follow through dimensions and owner teleports, path to owner aggressively instead of
      teleporting to catch up
- [x] Traits: exponential stacking, first bad traits, polish breeding, Jump trait takes less fall damage
- [x] Happiness gates breeding, tune existing factors
- [x] Bug: animals breed too fast, tune breeding rate (per species timer between litters for animals
      breeding on their own)
- [ ] Happiness rebalance and domestication comforts: grass never raises happiness, only lowers it
      when missing; overcrowding and having nowhere to wander are much bigger penalties; happiness
      comes from comforts players provide (hay bales, water troughs, shelter with access to the sky,
      bedding). Then shorten the breeding timers.
- [ ] Pets don't follow (path or teleport) into lava or fire unless they're fire resistant
- [x] Overlay showing an animal's traits, happiness, tricks and owner: the Pet Journal (replaces the
      Harmony Diagnostics debug item), plus a WAILA tooltip when WAILA is installed
- [ ] Config cleanup (incl. option to quiet animal sounds), achievements or other in game hints so
      players can discover the systems

### Phase 2: Depth

- [ ] More tricks: detect traits, shepherd, go to player / animal spawn, where I died,
      where I'm looking, flee, dig, fetch, collect items, activate, place, bark,
      galvanize (short tankiness buff), lay down / backflip / roll over
- [ ] Trick chaining with phrases
- [ ] Instruments for the other noteblock sounds: bass drum, snare, clicks, bass
- [ ] Teaching by demonstration
- [ ] Clear an animal's tricks to reset its training
- [ ] More traits and bad traits
- [ ] Happiness: water, flowers, light, eating grass, per species preferences
- [ ] Trick indicators, e.g. wolves look different while attacking, and don't attack sheep unless told to
- [ ] Animals holding items
- [ ] Sitting poses for animals without a vanilla one (cows, pigs, sheep, chickens, horses)
- [ ] Pet armor (enchantable) and potions
- [ ] Rituals and magical traits
- [ ] Integrations: Thaumcraft, Botania, Witchery
- [ ] Respawn on owner upgrade

### Phase 3: Magical animals

- [ ] Magical animals to find in the world
- [ ] Breeding rituals that turn animals into magical ones (pegasus, ...)

## Open questions

- Are bonded animals' noteblock commands ever restricted, e.g. near the owner's base?
