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
- Bad traits balance the strong scaling and have to be bred out (ideas: Slow, Frail, Timid, Clumsy).
- Current traits (Jump, Fast, Hardy, Vicious, Fertile) are a starting set, more to come.
- Magical traits add extra strength and are imparted by rituals (see Magic).

### Happiness

- Gives a mechanical reason to build nice looking pens.
- Unhappy animals don't breed. Happiness never affects tricks, so pets still work on rough adventures.
- Today: grass, crowding, monsters nearby, being hurt.
- Later: water, flowers, light, and preferences that differ by species (set in config).
  No block variety scoring, it's too easy to cheese.

### Tricks and music

- Animals listen for phrases: the notes they hear until a short pause. A single note is a one note
  phrase, so core tricks can be taught with one noteblock and no redstone.
- An animal acts on a phrase immediately, unless a longer phrase it knows starts with the same notes,
  in which case it waits for the pause. Simple vocabularies stay instant.
- Training: feed a glistering melon, play a phrase, the animal tries a trick, feed again to reward.
  Repeated rewards narrow the phrase down to one trick.
- Tricks chain: e.g. a location followed by Guard means guard that location.
- Teaching by demonstration: when an animal in training hears a phrase, it is far more likely to try
  the trick a nearby animal performs for that phrase, which makes it much quicker to reward and
  narrow down. Babies start with no tricks and learn this way from trained animals.
- A trained animal's tricks can be cleared to start teaching it again.
- Handheld instruments store phrases so tricks can be used while adventuring. They add to noteblocks,
  they don't replace them.
- Noteblock automation: redstone driven noteblocks command animals with nobody around.

### Ownership (bonding)

- Optional. Unbonded animals obey anyone and any noteblock.
- Bonded animals ignore other players' instruments and can't be leashed or bonded by anyone else.
  They still obey all noteblocks (noteblocks don't know who built them), so automation keeps working.
- Reuses vanilla ownership for wolves, ocelots and horses so every animal behaves the same way.
- Deliberate transfer action, for giving bred animals to friends.

### Protecting pets

- Golden apple: sets a respawn point, used up on the next death.
- Enchanted golden apple: permanent respawn point. Modded apples (e.g. diamond apples) can be
  configured into the same tiers.
- If the respawn point isn't loaded (far away, another dimension) the animal waits in spirit form until
  it is.
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

- [ ] Save animal data as versioned NBT instead of Java serialization, so updates don't break saves
- [ ] Remove code that only works in the dev environment (Jump trick reflects on `jump` by name)
- [ ] Sync animal state (traits, happiness, tricks) to clients, needed for the overlay
- [ ] Ownership data and rules (bonding, transfer, reuse vanilla owners)
- [ ] Way to test the release jar in a normal Forge install
- [ ] Fix known bugs: Sit doesn't sit, wander AI's crowding check uses x instead of y for its box,
      animals scared of water
- [ ] Remove leftovers: example lang entries, HarmonyHorse spawn egg, unused imports and debug prints

### Phase 1: MVP

- [ ] Reliable core tricks: follow, stay/sit, go to, guard, attack, come back
- [ ] Phrase based listening (single notes still instant)
- [ ] First handheld instrument
- [ ] Bonding and transfer
- [ ] Golden / enchanted golden apple respawn tiers, spirit form waiting
- [ ] Pets follow through dimensions and owner teleports, path to owner aggressively instead of
      teleporting to catch up
- [ ] Traits: exponential stacking, first bad traits, polish breeding, Jump trait takes less fall damage
- [ ] Happiness gates breeding, tune existing factors
- [ ] Overlay showing an animal's traits, happiness, tricks and owner
- [ ] Config cleanup (incl. option to quiet animal sounds), achievements or other in game hints so
      players can discover the systems

### Phase 2: Depth

- [ ] More tricks: detect traits, shepherd, go to player / animal spawn, where I died,
      where I'm looking, flee, dig, fetch, collect items, activate, place, bark,
      galvanize (short tankiness buff), lay down / backflip / roll over
- [ ] Trick chaining with phrases
- [ ] Teaching by demonstration
- [ ] Clear an animal's tricks to reset its training
- [ ] More traits and bad traits
- [ ] Happiness: water, flowers, light, eating grass, per species preferences
- [ ] Trick indicators, e.g. wolves look different while attacking, and don't attack sheep unless told to
- [ ] Animals holding items
- [ ] Pet armor (enchantable) and potions
- [ ] Rituals and magical traits
- [ ] Integrations: Thaumcraft, Botania, Witchery
- [ ] Respawn on owner upgrade

### Phase 3: Magical animals

- [ ] Magical animals to find in the world
- [ ] Breeding rituals that turn animals into magical ones (pegasus, ...)

## Open questions

- Are bonded animals' noteblock commands ever restricted, e.g. near the owner's base?
