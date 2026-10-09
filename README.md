# Endeavours Ships

Player-built, water-only ships for the Endeavours survival modpack. Ships are assembled from connected vanilla blocks, move as server-authoritative entities, carry passengers and cargo, collide with the world, and can be restored to blocks.

## Requirements

- Minecraft 26.1.2
- NeoForge 26.1.2.114
- Java 25

Install the same mod JAR on clients and the dedicated server. No Valkyrien Skies or Eureka dependency is required.

## Playing

1. Build a connected hull directly over water. A vessel must contain 8–1024 allowed blocks by default and exactly one Ship Helm.
2. Craft the helm with planks and sticks, place it on the ship, and right-click it to assemble.
3. Use the normal boat controls to sail. Larger ships accelerate and turn more slowly. Up to eight players can ride, depending on ship size.
4. Sneak-right-click the moving ship to open its 54-slot cargo inventory.
5. Stop, hold a Ship Helm, and sneak-right-click the ship to place its blocks back into the world. Disassembly is refused when the destination is obstructed.

Supported structure blocks are controlled by the `endeavours_ships:ship_blocks` block tag. The included tag accepts the helm, planks, logs, wooden stairs/slabs/fences/trapdoors, wool, banners, chests, trapped chests, barrels, and ladders. Chests and barrels contribute their contents to the ship cargo during assembly and receive the cargo again on disassembly.

Server owners can change minimum/maximum block counts, maximum dimensions, maximum speed, and the required water contact ratio in `config/endeavours_ships-common.toml`.

## Implementation

This release uses an independent, lightweight water-ship implementation. A direct Eureka/Valkyrien Skies port was rejected after reviewing their source: the available releases target older Minecraft internals and VS2 relies on an extensive mixin and native physics stack that has no Minecraft 26.1 line. Endeavours Ships instead extends Minecraft's proven boat movement, passenger synchronization, interpolation, and collision path, then adds arbitrary captured block geometry, mass-scaled handling, persistent cargo, assembly safety checks, and client block rendering.

While assembled, container contents are available through the unified ship inventory. Other block-entity behavior is restored when the ship is disassembled. Chests and banners use their item-model representation while moving because their vanilla world renderers require stationary block entities.

## Development

```bash
./gradlew build --no-configuration-cache
./gradlew runGameTestServer --no-configuration-cache
./gradlew runClient --no-configuration-cache
./gradlew runServer --no-configuration-cache
```

Built mod JARs are written to `build/libs/`.

The automated GameTest builds a vessel on water with a loaded chest, assembles it, verifies the moving entity and cargo, disassembles it, and verifies that the helm, chest, and cargo were restored.

## Licensing and references

This repository does not copy or bundle Eureka or Valkyrien Skies code. Their source was reviewed to choose a viable architecture. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the verified licenses and reference links.
