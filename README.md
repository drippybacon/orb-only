# Orb Only

Removes the side number panels from the hitpoints, prayer, run energy and special attack orbs. Values can appear inside the circles or in movable labels.

## Labels

**Show labels** is enabled by default. Choose a **Label position**:

- **Center** (default): values stay inside each orb and shrink to fit.
- **Free (Alt-drag)**: hold Alt to move each label. Alt-right-click resets it to the orb. Saved positions are kept when switching back to Center.

Font size and colour are configurable. Active prayer and special attack highlights can override the label colour.

### Prayer display

**Show prayer points** is off by default: active prayers show estimated time remaining, based on current points, active prayers and equipment prayer bonus. Enable it to show points instead. Inactive prayers always show points.

Centered time labels use whole minutes for estimates of ten minutes or more; Free labels show the full time. This choice is independent of the core Prayer plugin's orb-text setting. Other orbs retain their native value text, including run-energy timers.

With the core **Prayer** plugin enabled, Orb Only also draws its potion-dose pulse and statistics tooltip using the core settings. **Show prayer tick helper** is enabled by default; turn it off to hide the tick sweep on the orb. The helper also requires **Pray flick location** set to **Prayer orb** or **Both** in the core Prayer settings.

## Indicators

- **Active prayer/special attack:** configurable border and label highlights, enabled by default. Borders also work with labels hidden.
- **Poison/venom countdown:** an estimated countdown ring around HP, enabled by default. Colour and thickness are configurable. Damage hitsplats resynchronise the timer; starting the plugin while already poisoned may leave the first interval inaccurate.

## Artwork and interfaces

Orb Only uses the current game sprites, including Resource Packs overrides; no pack edits are needed. It handles the normal and no-minimap orb interfaces and follows moved orbs.

Circular click regions expose the native orb actions and right-click options. Disabling the plugin restores the managed widget flags and backing artwork. These replacement click regions still need explicit RuneLite review before Plugin Hub distribution; see the [review follow-up](docs/REVIEW-FOLLOWUP.md).

## Build, run and test

Use JDK 17. From this directory on Windows:

```powershell
.\gradlew.bat jar
.\gradlew.bat run
.\gradlew.bat test
```

On macOS/Linux, use `./gradlew` instead. The first build downloads Gradle and dependencies.

- `jar`: writes the plugin to `build/libs/`.
- `run`: opens a development RuneLite client with Orb Only loaded.
- `test`: runs the headless regression suite. See [coverage and limits](docs/TESTING.md).

## License

BSD 2-Clause. See [LICENSE](LICENSE) and [third-party notices](THIRD-PARTY-NOTICES.md).
