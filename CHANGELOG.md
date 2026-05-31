# 2.1.0 - 2026-05-31

## Behavior changes

- `generateGodotBindings` no longer depends on `.gdj` files; it resolves `@RegisterClass` metadata from `.kt` sources referenced by `.tscn` scenes.
- The implicit circular dependency between `compileKotlin` and `generateGodotBindings` has been removed.
- `generateGodotBindings` is now a true prerequisite of `compileKotlin` and `kspKotlin`; a single `./gradlew build` is sufficient.

## Added

- `KtSourceLoader` — resolves Kotlin script classes on demand from `.tscn` script paths.
- `MultiFileWriter` atomic writes (staging directory, then replace).
- `CommandPaths` — shared project-root and output-directory resolution.
- `bindings-kotlin-no-gdj` test fixture and unit tests for `KtSourceLoader` / `MultiFileWriter`.

## Removed

- `GdjScanner` / `GdjInfo` — binding generation no longer reads `.gdj` files.
- "Run godot-kotlin-jvm compilation first" as a binding-generation prerequisite.

## Limitations

- Only the first `@RegisterClass` in a file is recognised; additional classes emit a warning.
- `typealias` parent types are not resolved.
- Third-party library scripts with only `.gdj` descriptors (no `.kt` source) are not supported yet.

# 2.0.0 - 2026-05-30

## Breaking changes

- Remove global `GDTree` and absolute-path `NodeRef`; replace with per-script `XxxBindings` and relative-path `ChildRef`.
- Rename Gradle task `generateNodeTree` → `generateGodotBindings`.
- Change output directory from `build/generated/godotNodeTree/kotlin` to `build/generated/godotBindings/kotlin`.
- Plugin now registers the generated source directory automatically; remove manual `sourceSets { kotlin.srcDir("build/generated/godotNodeTree/kotlin") }` from consumer projects.
- Only Kotlin scripts (`.kt` + `.gdj`) produce mount-point bindings; GDScript mount points are no longer supported.

## Added

- `GdjScanner` — recursive `.gdj` index keyed by `relativeSourcePath`.
- Extended `TscnParser` — multi-line node properties, `scriptExtId` on `RawNode`.
- `MountTreeBuilder` — bindings rooted at each Kotlin script mount point; flat relative `ChildRef` entries.
- `XxxScene` factories (`PATH` + `instantiate()`) for scenes whose root node has a Kotlin script.
- Instance child scenes resolve to Kotlin script types when the instanced scene root has a `.gdj` entry.
- Multi-mount validation — same Kotlin class on multiple nodes allowed only when subtrees are structurally identical.
- New error types: `IncompatibleMountTrees`, `UnsupportedUniqueNodeName`, `ScriptTypeMismatch`, `UnresolvedScriptClass`.
- Kotlin-oriented test fixture (`bindings-kotlin`) with multi-file golden output.
- `compileKotlin` depends on `generateGodotBindings`.

## Changed

- Generated API uses `getNode(NodePath(relativePath))` on the scripted node instead of absolute scene paths.
- `{ClassName}Bindings` naming comes from `.gdj` `fqName`, not from `.tscn` file names.
- Missing `.gdj` for a script emits a warning and skips that mount point (does not fail the task).

## Documentation

- README updated for 2.0 setup, `packageName` requirement, workflow, `ChildRef` scope, instance rules, and migration from 1.x.

# 1.1.0 - 2025-07-26

- Made project compatible with Godot 4.4
- Improved overall code quality
- Extracted parsing node tree to shared core library

# 1.0.0 - 2024-09-13

- Implement core functionalities
- Add generating node tree representation in Swift
