# SkriptRegistration

**SkriptRegistration** (SKR) is a simplified wrapper around Skript's syntax registration API, designed to make it easier for Skript addon developers to register syntax elements without dealing with the complexity of Skript's internal registry.

SKR also aims to provide as much backwards compatibility as possible between Skript versions, though some breaking changes may be unavoidable.

---

## Features

SKR supports registration of all standard Skript syntax element types:

| Element | Description |
|---|---|
| **Types** | Custom `ClassInfo` registrations, including enum and Paper `Registry`-backed types |
| **Effects** | Standard effects and section-style effects with entry validators |
| **Conditions** | Standard conditions and `PropertyCondition`-style conditions |
| **Expressions** | Simple, combined, event-value, and property expressions |
| **Events** | Custom Skript events backed by one or more Bukkit event classes |
| **Sections** | Section syntax with optional entry validators |
| **Structures** | Top-level structure syntax with optional entry validators |
| **Functions** | Custom Skript functions via `DefaultFunction` |
| **Event Values** | Event values with optional changers, time offsets, and exclusions |

In addition to registration, SKR includes:

- **`JsonDocGenerator`** — generates a `json-docs.json` file in your plugin's data folder, covering all registered syntax with names, descriptions, examples, patterns, return types, changers, and event values. Intended for use with external documentation sites.
- **`TaskUtils` / `Scheduler`** — a unified scheduling API that transparently supports both standard Bukkit/Spigot schedulers and Folia's region-based schedulers (global, regional, and entity schedulers).

---

## Setup

See the [**Wiki**](https://github.com/ShaneBeee/SkriptRegistration/wiki) for full setup and usage instructions.

To see a real-world example of SKR in use, refer to [**SkBee**](https://github.com/ShaneBeee/SkBee).

---

## Dependency

Add the JitPack repository and SKR dependency to your `build.gradle.kts`:

```kts
repositories {
    maven("https://jitpack.io")
}
```

```kts
dependencies {
    implementation("com.github.ShaneBeee:SkriptRegistration:<version>")
}
```

Replace `<version>` with the latest release tag from [JitPack](https://jitpack.io/#ShaneBeee/SkriptRegistration).

---

## Shading

SKR must be shaded into your addon's jar. It is **strongly recommended** to relocate the SKR package when shading to avoid class conflicts with other addons that also shade SKR.

```kts
plugins {
    id("com.github.johnrengelman.shadow") version "<version>"
}
```

```kts
tasks {
    shadowJar {
        relocate("com.github.shanebeee.skr", "com.yourname.youraddon.skr")
    }
}
```

Without relocation, if two addons ship different versions of SKR under the same package, one will shadow the other at runtime, which can cause subtle and hard-to-diagnose bugs. Always relocate.

---

## Javadocs

Full API documentation is available at [**shanebeee.github.io/docs/SkriptRegistration/latest/**](https://shanebeee.github.io/docs/SkriptRegistration/latest/).

---

## License

SkriptRegistration is licensed under the [**GPL-3.0 License**](https://github.com/ShaneBeee/SkriptRegistration/blob/master/LICENSE).
