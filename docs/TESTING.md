# Automated tests

Use JDK 17. From the project directory:

```powershell
.\gradlew.bat clean test
```

On macOS/Linux, use `./gradlew clean test`. To run one suite, add e.g. `--tests dev.orbonly.OrbInteractionsTest`.

The task builds the JAR, then runs headless JUnit 4/Mockito tests with client/API doubles. It does not start RuneLite. `OrbOnlyDev` is the separate launcher used by `run`.

## Coverage

| Suite | Main checks |
| --- | --- |
| `OrbUiStateTest` | Visibility/blocking restoration, game-hidden controls, native updates, surface reuse, no-minimap selection, missing artwork, logout and circle bounds. |
| `OrbFrameImagesTest` | Transparent number panels, opaque backings, native backing API use outside scene coordinates, override ownership and cache invalidation/eviction. |
| `OrbInteractionsTest` | Operation slots, component IDs, targets, masks, hidden controls and priority for foreign interfaces/overlay controls. |
| `OrbPrayerHelperTest` | Points/time labels, combined drain, equipment bonus, prayer aliases/LMS, representative potion thresholds, wrench effects, core settings, tooltips and tick-sweep geometry. |
| `OrbPoisonTimerTest` | Tick interpolation, severity/venom transitions, local damage resynchronisation, cures/immunity and session changes. |
| `OrbValueOverlayTest` | Special states `0`, `1`, `2`, highlight toggles and active-prayer colours. |
| `OrbPackagingTest` | Matching root/resource/JAR notices and exclusion of test/development classes and dependencies. |

Poison timing uses a controllable clock; artwork tests use synthetic sprites. No test sleeps are required. Test dependencies are not shipped with the plugin.

Results and test counts are in:

- HTML: `build/reports/tests/test/index.html`
- XML: `build/test-results/test/`

## Limits

Mocks do not reproduce native rendering, packet processing or menu subscriber ordering. The backing test exercises the sprite API at Fixed-mode coordinates; it does not render the native interface. Packaging tests inspect the local JAR and standard resource files, not a Hub-produced artifact. The prayer-display preference migration is not covered by the current suite.

Complete the [remaining checks](REVIEW-FOLLOWUP.md#validation-limits-and-remaining-checks) before release.
