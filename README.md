
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
It is highly recommended you shade the library to avoid conflicts with other addons that may use this.
```kts
tasks {
    shadowJar {
        relocate("com.github.shanebeee.skr", "my.poject.skr")
    }
}
```

## License:
SkriptRegistration is licensed under the [**GPL-3 License**](https://github.com/ShaneBeee/SkriptRegistration/blob/master/LICENSE).
