# Godot Kotlin Tree

> [!Important]
> The current version of the project has been tested on macOS and **may not work properly** on Linux and Windows.

> [!Warning]
> **Breaking change in 2.0.0** — The global `GDTree` object and absolute-path `NodeRef` have been removed.
> See the [Breaking Changes](#breaking-changes) section for migration guidance.

Godot Kotlin Tree enhances development of Godot games using Kotlin bindings by generating typed
`XxxBindings` objects (relative-path `ChildRef`) for every node that has a `@RegisterClass` Kotlin
script attached, and `XxxScene` factory objects for every scene whose root node carries a script.

Instead of:

```kotlin
getNode(NodePath("/root/Path/To/Some/Nested/Area/Node")) as Area2D
```

use the generated typed delegate:

```kotlin
@RegisterClass
class Main : Node2D() {
    val timer: Timer by MainBindings.Timer
    val spawnPoint: Marker2D by MainBindings.SpawnPoint
}
```

The bindings are generated automatically from `.tscn` and `.gdj` files. Rebuilding after node-tree
changes produces compile-time errors instead of silent runtime crashes.

For more information about developing Godot games using Kotlin, head to the [Godot Kotlin JVM](https://godot-kotl.in/en/stable/) website.

## Setup

### Prerequisites

This plugin is designed to work alongside **[godot-kotlin-jvm](https://godot-kotl.in/en/stable/)**.
Before bindings can be generated:

1. Apply the `godot-kotlin-jvm` plugin in your project.
2. Compile your Kotlin scripts so that `.gdj` descriptor files are generated.
3. Run `generateGodotBindings` (or `./gradlew build` — see below).

Without `.gdj` files the generator emits a **warning** and skips the corresponding script nodes.

### Kotlin

Configure the plugin in `build.gradle.kts`:

```kotlin
plugins {
    id("com.utopia-rise.godot-kotlin-jvm") version "0.13.1-4.4.1"
    id("io.github.tomwyr.godot-kotlin-tree") version "2.0.0"
}

godotNodeTree {
    // Strongly recommended: must match the package of your @RegisterClass scripts.
    // Generated Bindings reference Kotlin types (e.g. Bird, Hud) from this package.
    packageName = "com.example.game"

    // Only needed when the Godot project root is NOT the Kotlin project root.
    // projectPath = "path/to/godot/project"

    // Default: true — requires project.godot under the resolved project root.
    // validateProjectPath = true
}
```

The plugin automatically:

- adds `build/generated/godotBindings/kotlin` to the `main` Kotlin source set (**no manual `sourceSets` block needed**);
- makes `compileKotlin` depend on `generateGodotBindings`.

> **Do not** keep the old 1.x source set entry:
> `kotlin.srcDir("build/generated/godotNodeTree/kotlin")` — remove it when upgrading.

Add a plugin repository in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```

#### Configuration reference

| Option | Default | Description |
| ------ | ------- | ----------- |
| `packageName` | `null` | Package for all generated files (`ChildRef.kt`, `*Bindings.kt`, `*Scene.kt`). **Set this to the same package as your game scripts** so instance types like `Bird` resolve at compile time. |
| `projectPath` | `null` (Kotlin project root) | Relative path from the Kotlin project root to the Godot project directory containing `project.godot`. |
| `validateProjectPath` | `true` | When `true`, fails if `project.godot` is not found under the resolved project root. |

### Godot

No extra Godot Editor configuration is required beyond attaching Kotlin scripts (`res://.../*.kt`) to nodes in `.tscn` files.

## Usage

### Recommended workflow

1. Attach a Kotlin script (`@RegisterClass`) to a node in the Godot Editor.
2. Build Kotlin / run godot-kotlin-jvm tasks to generate `.gdj` files.
3. Run `./gradlew generateGodotBindings` or `./gradlew build`.
4. Reference child nodes from your script via `XxxBindings` (see below).

### Generated output

The task scans `.tscn` and `.gdj` files and writes multiple files under
`build/generated/godotBindings/kotlin/<package>/`:

```kotlin
// MainBindings.kt
package com.example.game

import godot.api.*

object MainBindings {
    val Timer = ChildRef<Timer>("Timer")
    val SpawnPoint = ChildRef<Marker2D>("SpawnPoint")
    val Bird = ChildRef<Bird>("Bird")       // instanced scene whose root has Bird.kt
    val Hud = ChildRef<Hud>("Hud")
    // ...
}

// MainScene.kt — only when the scene root node has a script
package com.example.game

import godot.api.PackedScene
import godot.api.ResourceLoader

object MainScene {
    const val PATH = "res://scene/Main.tscn"

    fun instantiate(): Main =
        (ResourceLoader.load(PATH) as PackedScene).instantiate() as Main
}
```

### Using bindings in your scripts

```kotlin
@RegisterClass
class Main : Node2D() {
    val timer: Timer by MainBindings.Timer
    val bird: Bird by MainBindings.Bird

    @RegisterFunction
    override fun _ready() {
        timer.start()
        bird.globalPosition = Vector2(160, 300)
    }
}
```

#### How `ChildRef` resolves nodes

- Paths in `XxxBindings` are **relative to the node where the script is attached**, not absolute `/root/...` paths.
- `val bird: Bird by MainBindings.Bird` on `Main` is equivalent to calling `getNode("Bird") as Bird` on the `Main` node instance.
- For an **instanced** child scene whose root has a Kotlin script, the binding type is the script class (e.g. `Bird`), not the underlying Godot node type (e.g. `CharacterBody2D`).

#### Property delegation behaviour

`by MainBindings.Xxx` uses Kotlin property delegation. **Each read** of the property calls `getNode` again — it does not cache the reference at declaration time. This is usually fine; for hot paths you can cache manually in `_ready()`:

```kotlin
lateinit var bird: Bird

@RegisterFunction
override fun _ready() {
    bird = getNode("Bird") as Bird
}
```

Use bindings from `_ready()` onward. Accessing them earlier (e.g. in field initializers) may fail if the scene tree is not ready yet.

### Requirements for binding generation

- The node must have a Kotlin script (`res://.../*.kt`) attached in the `.tscn` file.
- The corresponding `.gdj` file must exist (from godot-kotlin-jvm). Missing `.gdj` → warning, node skipped.
- **`packageName` must match your script package** when bindings reference Kotlin instance types (`Bird`, `Hud`, …). Without it, generated code has no package and Kotlin types will not resolve.

### Generation rules

| Rule | Behaviour |
| ---- | --------- |
| Bindings root | The node with the Kotlin script (not necessarily the scene root) |
| `{ClassName}Bindings` naming | From `.gdj` `fqName` simple name (e.g. `com.example.game.Main` → `MainBindings`) |
| `{SceneName}Scene` naming | From the `.tscn` file name |
| Instanced scenes (`instance=…`) | One `ChildRef` entry only; **internal nodes of the instanced scene are not expanded** in the parent bindings |
| Same script on multiple nodes | Allowed only if all mount-point subtrees are **structurally identical**; otherwise generation fails |
| `%` unique node names | **Not supported** — generation fails if a node name contains `%` |
| GDScript-only scripts | Do not produce their own `*Bindings`; they may still appear as `ChildRef<GodotType>` children in a parent Kotlin binding |

### Generated files

| File | Description |
| ---- | ----------- |
| `ChildRef.kt` | Delegate helper; **one file** under the configured `packageName` |
| `{ClassName}Bindings.kt` | Flat `ChildRef` fields for every direct/indirect child of the scripted node (subject to instance rules above) |
| `{SceneName}Scene.kt` | `PATH` + `instantiate()`; only when the **scene root** has a Kotlin script |

## Compatibility

| godot-kotlin-tree | godot-kotlin-jvm |
| ----------------- | ---------------- |
| 1.0.x             | 0.8.1-4.2.0      |
| 1.1.x             | 0.13.1-4.4.1     |
| 2.0.x             | 0.13.1-4.4.1     |

_Note: the suffix of the godot-kotlin-jvm version is also the compatible Godot engine version._

Other version pairs may work but have not been tested.

## Breaking Changes

### 2.0.0

- **`GDTree` removed** — use per-class `XxxBindings` instead of `GDTree.Scene.Node`.
- **`generateNodeTree` renamed** to `generateGodotBindings`.
- **Output directory** changed from `build/generated/godotNodeTree/kotlin` to `build/generated/godotBindings/kotlin`.
- **`NodeRef` replaced by `ChildRef`** — relative paths from the scripted node; no more `/root/...` absolute paths.
- **Manual `sourceSets` no longer required** — the plugin registers the generated directory automatically. Remove old `kotlin.srcDir("build/generated/godotNodeTree/kotlin")` entries.
- **Set `packageName`** — strongly recommended; required for correct resolution of Kotlin instance types in generated bindings.
- **No `GDScript` binding generation** — only `.kt` script mount points produce `*Bindings` / `*Scene`. GDScript children still appear as `ChildRef<GodotType>` in parent bindings.
- **`.gdj` files** — run godot-kotlin-jvm compilation first; missing `.gdj` → warning, node skipped.

### Migration example

**Before (1.x):**

```kotlin
val colorRect by GDTree.Main.ColorRect
```

**After (2.x):**

```kotlin
val colorRect: ColorRect by MainBindings.ColorRect
```

## Contributing

Every kind of help aiming to improve quality and add new functionalities is welcome. Feel free to:

- Open an issue to request new features, report bugs, ask for help.
- Open a pull request to propose changes, fix bugs, improve documentation.
- Tell others about this project.
