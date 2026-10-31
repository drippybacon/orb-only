# Orb Only

Standalone RuneLite plugin with compact, circular hitpoints, prayer, run energy, and special attack orbs. Configurable RuneScape text with a black outline keeps their live values readable, including while hovered.

**Show labels** is enabled by default. Turn it off for icons only. **Label position** offers:

- **Center** (default): labels stay inside their orb and follow it when moved. Long values shrink to fit.
- **Free (Alt-drag)**: hold Alt and drag each label independently. RuneLite saves each label's position. Alt-right-click a label to reset it to its orb; returning to Center keeps your saved Free positions for later.

**Label font size** and **Label colour** apply in either mode. Free labels use the chosen font size without shrinking to the orb. The original text is still used, including run-energy timer formatting from RuneLite.

**Show prayer time remaining** is off by default, so the prayer label shows current prayer points. Enable it for an estimate based on the active prayers, current points and equipped prayer bonus. Inactive prayers always show points. Center labels use compact time formatting; Free labels show the full time. This setting controls Orb Only's labels independently of the core Prayer plugin's orb-text replacement setting.

The core **Prayer** plugin's tick helper is also drawn on the replacement orb. Its **Pray flick location** must be **Prayer orb** or **Both**. Orb Only honours that plugin's helper colour and **Never hide prayer flick helper** setting, and uses the actual rendered circle so the sweep follows moved orbs. The line is drawn over centered labels to stay visible. The core Prayer bar helper continues to use the core plugin's normal settings.

**Highlight active special attack** and **Highlight active prayer** are enabled by default. An armed special attack has a green border and value; active quick prayers or individual prayers have a cyan border and value. Both colours are configurable and override the regular label colour while active. The borders remain visible with labels turned off.

**Poison/venom countdown** is enabled by default. While poisoned or venomed, a ring drains clockwise around the HP frame until the next damage tick. Poison is green and venom is purple by default; both colours and the ring thickness are configurable. The countdown shares Orb Only's HP frame bounds and follows moved/reordered orbs automatically, without separate alignment offsets. It also appears with labels disabled or moved away from the orb.

Timing follows the 30-game-tick cycle used by [Poison Ring](https://github.com/Jin-Jiyunsun/jins-plugins/tree/942f65dc361cc3f1b1abb9e54a2471080c4951a7), resynchronising on the local player's poison/venom damage hitsplats. A newly applied poison/venom starts a cycle; changes in severity alone do not repeatedly reset it. Cures and immunity hide the ring. If Orb Only is started while already poisoned, the first interval is an estimate until a damage hitsplat is observed. Login, hopping and connection loss clear stale timing; ordinary region loading preserves it.

The circular frames are generated inside the plugin from the active game sprites. It works with the default game artwork and with Resource Packs; no edited resource pack or custom pack path is required. Normal, hovered and flashing frames retain the current artwork. An opaque background beneath each gauge closes transparent edge gaps. Native orb fills, icons and regeneration/timer displays continue to render normally.

Circular overlay hover/click regions follow the orbs when moved or reordered. Native widget positions and dimensions stay unchanged. The overlay exposes the original widget actions, including right-click options, for real user clicks. Invisible number panels no longer select neighbouring orbs. Interface menus and RuneLite overlay controls take priority. Disabling Orb Only restores the original frame and number visibility and click-blocking flags.

HP is rendered in the same circular style whether or not its cure button has a client script. Server-handled orb actions use the native click flags, and actions hidden by the game stay hidden.

This follows the overlay menu approach used by [Interface Layout's merged update](https://github.com/runelite/plugin-hub/pull/19569); Orb Only itself has not been reviewed for the Plugin Hub.

## Build and run

Use JDK 17 or newer. From this folder on Windows:

```powershell
.\gradlew.bat jar
.\gradlew.bat run
```

On macOS or Linux, use `./gradlew jar` and `./gradlew run`.

The first build downloads Gradle and the RuneLite dependencies. The JAR is written to `build/libs/`. The `run` task opens a development RuneLite client with Orb Only loaded.

## License

BSD 2-Clause. See [LICENSE](LICENSE) and [third-party notices](THIRD-PARTY-NOTICES.md).
