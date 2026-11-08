# Development

Use JDK 17 and the Gradle wrapper included in the repository. The source targets Java 11. The first build downloads Gradle and the required dependencies.

## Build and run

Run these commands from the repository root on Windows:

| Command | Result |
| --- | --- |
| `.\gradlew.bat jar` | Builds the plugin JAR in `build/libs/`. |
| `.\gradlew.bat run` | Opens a development RuneLite client with Orb Only loaded. |
| `.\gradlew.bat test` | Runs the regression suite. See [Testing](TESTING.md). |

On macOS/Linux, replace `.\gradlew.bat` with `./gradlew`.

For Jagex-account login in the development client, follow RuneLite's [Using Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Plugin Hub

Orb Only was accepted into Plugin Hub in [PR #19843](https://github.com/runelite/plugin-hub/pull/19843), merged on 7 November 2026.

The Hub builds the commit recorded in [`plugins/orb-only`](https://github.com/runelite/plugin-hub/blob/master/plugins/orb-only). To release an update, push the source changes, then submit a Hub pull request updating that commit hash. See the official [update instructions](https://github.com/runelite/plugin-hub#updating-a-plugin).

## Maintenance notes

- `OrbUiState` manages native widget flags and restores them when widgets are retired or the plugin stops. Frame preparation also recovers from a missed script-finished notification; `OrbOnlyPluginTest` covers that recovery.
- `OrbFrameImages` replaces backing artwork while keeping native gauges and icons. Cleanup restores only sprite overrides still owned by Orb Only.
- `OrbInteractions` projects native orb actions into circular click regions. Keep the native operation identifiers, action masks and menu targets intact when changing it.
- Prayer calculations and potion tables are adapted from RuneLite. Source references and attribution are in [third-party notices](../THIRD-PARTY-NOTICES.md); compare with upstream when prayers or items change.
- Keep the root license and notice files identical to their `src/main/resources/META-INF/` copies. `OrbPackagingTest` checks both the resource files and the local JAR.
- The old `showPrayerTimeRemaining` preference migrates to the inverse `showPrayerPoints` value. An existing `showPrayerPoints` preference takes priority.

The README screenshots live in `docs/images/`. They are documentation assets and are not bundled with the plugin.

## Manual checks

The automated suite uses simulated widgets. When changing rendering or interaction behaviour, also check the affected cases in a logged-in client:

- Modern resizable, Classic resizable, Classic fixed, no-minimap layouts and moved orbs.
- HP cure, quick prayers, run, both special-attack operations, hidden controls and right-click menus alongside other plugins.
- Center and Free labels, Alt-drag/reset, active highlights, core Prayer helpers and poison/venom countdowns.
- Resource Pack changes, disable/re-enable, login/logout and world hopping; check that the original appearance returns when Orb Only is disabled.
- RuneLite profile switching and migration from the old prayer-display preference.
