# Testing

Use JDK 17 and the included Gradle wrapper. From the repository root, run:

```powershell
.\gradlew.bat test
```

On macOS/Linux, use `./gradlew test`.

To run only the frame-recovery regression test:

```powershell
.\gradlew.bat test --tests dev.orbonly.OrbOnlyPluginTest
```

The tests build the local JAR and use JUnit 4, Mockito and simulated client/widget state. They run without opening RuneLite. To launch the development client, see [Development](DEVELOPMENT.md).

## Coverage

| Suite | Main checks |
| --- | --- |
| `OrbOnlyPluginTest` | A missed `ScriptPostFired` event cannot prevent the next frame from hiding the native value and button again. |
| `OrbUiStateTest` | Widget restoration, game-hidden controls, layout changes, missing artwork, logout and circular bounds. |
| `OrbFrameImagesTest` | Transparent side panels, opaque backings, sprite overrides and image-cache invalidation. |
| `OrbInteractionsTest` | Native actions, operation slots, action masks and priority for other interfaces and overlays. |
| `OrbPrayerHelperTest` | Points/time labels, drain calculations, equipment bonus, prayer aliases, potion thresholds, core settings, tooltips and the tick sweep. |
| `OrbPoisonTimerTest` | Countdown interpolation, poison/venom transitions, damage synchronisation, cures and session changes. |
| `OrbValueOverlayTest` | Active prayer and mainhand/offhand special-attack colours and toggles. |
| `OrbPackagingTest` | Matching license notices and exclusion of test/development classes and dependencies from the local JAR. |

The poison tests use a controllable clock, and artwork tests use generated sprites. Test dependencies are not shipped with the plugin.

Results and test counts are in:

- HTML: `build/reports/tests/test/index.html`
- XML: `build/test-results/test/`

## What still needs a live client

These tests cannot reproduce native rendering, packet timing or the order in which other plugins change menus. The frame-recovery test verifies the fallback for a missed notification; it does not establish the original cause of a live rendering issue. Packaging tests inspect the local JAR rather than a Hub-built artifact.

Use the [manual checks](DEVELOPMENT.md#manual-checks) for layouts, Resource Packs, profile switching and preference migration.
