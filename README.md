<div align="center">

# 🧭 waypoint-api

**Per-player, packet-based waypoints for Paper, from 1.16.5 to the latest version.**

Floating holograms that always point towards the target, show live distance and are visible through walls.
Zero real entities, zero world changes, and every player sees only their own waypoints.

![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)
![Paper](https://img.shields.io/badge/Paper-1.16.5%20→%20latest-blue?style=flat-square)
![PacketEvents](https://img.shields.io/badge/PacketEvents-2.7.0-purple?style=flat-square)
![Build](https://img.shields.io/badge/build-Gradle-02303A?style=flat-square)

[![Showcase](https://img.youtube.com/vi/5C41Hfzvy4o/maxresdefault.jpg)](https://www.youtube.com/watch?v=5C41Hfzvy4o)

▶️ **[Watch the showcase on YouTube](https://www.youtube.com/watch?v=5C41Hfzvy4o)**

</div>

---

## ✨ Features

- **Packet-only**: fake entities sent through [PacketEvents](https://github.com/retrooper/packetevents). Nothing exists server-side, nothing gets saved to the world.
- **Per-player**: each player has their own set of waypoints, invisible to everyone else.
- **Always on screen**: far targets are projected onto a fixed distance in front of the player, so the hologram never disappears in render distance or behind terrain.
- **Live distance**: `42m`, `1.3km`, refreshed only when the displayed value actually changes.
- **Reach detection**: optional radius plus a cancellable `WaypointReachEvent`.
- **1.16.5 → latest**: `TextDisplay` where the server *and* the client support it, invisible marker `ArmorStand` everywhere else. Works behind ViaVersion as well.
- **Cheap**: players standing still cost nothing, there are no snapshot copies on the tick, packets are batched with one flush per player per tick, and there's no `String.format` on the hot path.
- **Fail-fast API**: immutable `Waypoint` with a validating builder, main-thread assertions and meaningful exceptions.

## 🧩 How it renders

| Server | Client | Renderer | Notes |
|---|---|---|---|
| ≥ 1.20.2 | ≥ 1.19.4 | `TextDisplay` | Smooth movement (teleport interpolation), multi-line, `scale` supported |
| 1.19.4 – 1.20.1 | ≥ 1.19.4 | `TextDisplay` | Same, without teleport interpolation |
| any | < 1.19.4 | `ArmorStand` (marker) | Single-line name tag, `scale` ignored |
| 1.16.5 – 1.19.3 | any | `ArmorStand` (marker) | Single-line name tag, `scale` ignored |

Metadata indices are picked by **server** version, because PacketEvents encodes in the server protocol and ViaVersion translates from there. The entity type is picked by **client** version.

## 📦 Installation

```kotlin
repositories {
    maven("https://repo.eternalcode.pl/releases")
}

dependencies {
    implementation("dev.piotrulla:waypoint-api:1.0.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
}
```

`plugin.yml`:

```yaml
depend: [packetevents]
```

> PacketEvents and Paper are `compileOnly` in the API, so they are **not** pulled in transitively.
> PacketEvents has to be installed on the server as a plugin.

## 🚀 Quick start

```java
public final class MyPlugin extends JavaPlugin {

    private PacketWaypointService waypointService;

    @Override
    public void onEnable() {
        this.waypointService = new PacketWaypointService(this);
        this.waypointService.start();
    }

    @Override
    public void onDisable() {
        this.waypointService.shutdown();
    }

    public void pointToQuest(Player player, Location target) {
        Waypoint waypoint = Waypoint.builder("quest-main", target)
                .label(Component.text("⚑ Quest", NamedTextColor.GOLD))
                .reachRadius(3.0)
                .build();

        this.waypointService.show(player, waypoint);
    }
}
```

React when the player gets there:

```java
@EventHandler
public void onReach(WaypointReachEvent event) {
    event.getPlayer().sendMessage(Component.text("You've arrived!", NamedTextColor.GREEN));
    // event.setCancelled(true) -> keep the waypoint instead of removing it
}
```

## 📖 API

### `Waypoint.builder(String id, Location location)`

| Option | Default | Allowed | Description |
|---|---|---|---|
| `label(Component)` | `Component.text(id)` | non-null | Text shown on the hologram |
| `displayDistance(double)` | `12.0` | `2.0 – 48.0` | How far from the player's eyes a far waypoint is projected |
| `scale(float)` | `1.0` | `> 0`, finite | Hologram size (`TextDisplay` only) |
| `reachRadius(double)` | `0.0` (off) | `≥ 0`, finite | Distance at which `WaypointReachEvent` fires |
| `showDistance(boolean)` | `true` | — | Adds the distance below the label (or after it, on armor stands) |

`Waypoint` is immutable. To change something, use `waypoint.toBuilder()...build()` and `show` it again under the same id.

### `WaypointService`

| Method | Description |
|---|---|
| `show(Player, Waypoint)` | Shows the waypoint, replacing an existing one with the same id |
| `hide(Player, String id)` | Hides one waypoint, returns `false` if it wasn't there |
| `hideAll(Player)` | Hides everything for that player |
| `findWaypoint(Player, String id)` | `Optional<Waypoint>` |
| `getWaypoints(Player)` | Immutable list of the player's active waypoints |

`PacketWaypointService` also has:

| Method | Description |
|---|---|
| `new PacketWaypointService(plugin)` | Update interval of 2 ticks |
| `new PacketWaypointService(plugin, ticks)` | Custom update interval (≥ 1) |
| `start()` / `shutdown()` | Registers the listener and tick task / removes everything |

### `WaypointReachEvent`

Fired after the update tick when a player is inside `reachRadius`.
- **Not cancelled**: the waypoint is removed.
- **Cancelled**: the waypoint stays, and the event fires again the next time the player moves while still in radius.

### Threading

All `WaypointService` methods must be called from the **primary server thread**. Calling them from any other thread throws `IllegalStateException`.

### Lifecycle handled for you

- Quit: player state is dropped.
- World change and respawn: holograms are re-spawned (the client wipes entities).
- Long teleport (> 32 blocks): holograms are destroyed and re-spawned at the new place.
- Waypoint in another world than the player: hidden until they come back.

## 🎮 Example plugin

The `example-plugin` module is a ready-to-run demo (the one in the video).

| Command | Description |
|---|---|
| `/wp add <id> [label...]` | Waypoint at your position (label supports `&` colors) |
| `/wp at <id> <x> <y> <z> [label...]` | Waypoint at coordinates |
| `/wp remove <id>` | Remove one (tab-completes your ids) |
| `/wp clear` | Remove all |
| `/wp list` | List your waypoints |
| `/wp demo` | North / East / South / West 50 blocks away (reach 3), plus one 2.5 km away |

Permission: `waypoint.example` (default: op).

### Run it

```bash
# Paper 1.21.4 on Java 21 - TextDisplay renderer
./gradlew :example-plugin:runServer

# Paper 1.16.5 on Java 17 - ArmorStand fallback renderer
./gradlew :example-plugin:runServerLegacy
```

PacketEvents is downloaded automatically, and the JDKs are provisioned by the foojay toolchain resolver.
Servers live in `example-plugin/run/<version>`. Remember to `op <nick>` in the console.

## 🛠️ Building & testing

```bash
./gradlew build                 # compile + tests
./gradlew test                  # JUnit 5 + Mockito
./gradlew :publishToMavenLocal  # check sources/javadoc jars locally
```

Tests cover the pure logic: builder validation, projection math, distance formatting and keys, movement detection, entity id allocation and display metadata layouts per version.

## ⚠️ Known limitations

- **Paper only**: Spigot 1.16 – 1.18 has no Adventure in its API.
- **Java 17+** at runtime. Paper 1.16.5 runs fine on 17 (recent builds).
- **No Folia support yet**: the tick runs on the global scheduler.
- **Armor stand fallback**: single line, no `scale`, name tag rendering follows vanilla rules.

---
