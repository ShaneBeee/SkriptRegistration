SkriptRegistration (or SKR for short) is a simplified version of Skript's syntax registry.  
It enables Skript addons to easily register syntaxes without having to deal with the complexities of Skript's syntax registry.  
SKR also plans to provide as much backwards compatability as possible, but you never know, things might just have to change.

## Usage:
Until I have time to write a proper usage guide, please refer to [**SkBee**](https://github.com/ShaneBeee/SkBee) to see how this library is used.

## Repo:
```kts
repositories {
    // JitPack
    maven("https://jitpack.io")
}
```
```kts
dependencies {
    // SkriptRegistration
    implementation("com.github.ShaneBeee:SkriptRegistration:<version>")
}
```

## Shading:
It is highly recommended that you relocate the library when shading to avoid conflicts with other addons that may use SKR.
```kts
tasks {
    shadowJar {
        relocate("com.github.shanebeee.skr", "my.poject.skr")
    }
}
```

## Javadocs:
Check out SKR's [**Javadocs**](https://shanebeee.github.io/docs/SkriptRegistration/latest/).

## License:
SkriptRegistration is licensed under the [**GPL-3 License**](https://github.com/ShaneBeee/SkriptRegistration/blob/master/LICENSE).
