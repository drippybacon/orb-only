# Review follow-up

Implementation changes following the static review of commit `aa8412ab141cf9ece8bbdde314f804a9fe16621c` on 31 October 2026.

## Findings addressed in code

| Finding | Change |
| --- | --- |
| Notices missing from Hub standard packaging | Full license and notice copies are main resources under `META-INF`, independent of the custom `jar` task. |
| Hidden prayer button suppresses core features | The replacement orb draws the dose pulse, statistics tooltip and tick helper using core Prayer settings. |
| Offhand special state is not highlighted | Highlighting accepts positive native states, including `2`; `0` is inactive. |
| Fixed-mode clipping hides the replacement backing | A sprite override on the visible native backing places the opaque circle beneath the gauge, replacing the scene-clipped `UNDER_WIDGETS` overlay. |

The implementation also handles no-minimap widgets, reuses unchanged surfaces, limits reconciliation after scripts to orb/layout updates, bounds the image cache and preserves native menu targets. Sprite cleanup restores only overrides still owned by Orb Only.

## Maintenance notes

- Keep root license/notice files and their `src/main/resources/META-INF/` copies identical.
- Prayer calculations and restoration-item data are adapted from RuneLite commit `7715a1d26e6cbbfdce7ad266a39c8c3c611211a0`; source links and licenses are in [third-party notices](../THIRD-PARTY-NOTICES.md). Recheck the copied tables when upstream prayers or items change.
- The old `showPrayerTimeRemaining` preference migrates to the inverse `showPrayerPoints` value. An existing new preference takes priority.

## Review status

**The replacement orb click regions have not received RuneLite approval.** Keeping native widget dimensions unchanged still changes the effective click areas. [PR #19569](https://github.com/runelite/plugin-hub/pull/19569) concerns overlay menu-tab buttons and does not approve these orb replacements. Seek review of the exact implementation before Hub distribution.

## Validation limits and remaining checks

On 31 October 2026, the user reported that the plugin looked correct in Modern resizable, Classic resizable and Classic fixed layouts.

[Automated tests](TESTING.md) cover mocked client behaviour, image generation and local JAR contents. They do not establish logged-in rendering, menu ordering, Hub artifact contents or policy approval.

Before release, check:

- No-minimap layouts and moved/reordered orbs.
- HP cure, quick-prayer, run and both special operations; game-hidden/disabled controls; states `0`, `1`, `2`; selected items/spells and right-click ordering with other plugins.
- Core Prayer tick, dose and tooltip settings; supported potions and wrench effects; upgraded prayers/LMS.
- Poison/venom application, curing, hopping and connection loss.
- Resource-pack changes, plugin disable/re-enable, and restoration of original flags/artwork.
- Prayer display preference migration and RuneLite profile switching.
- License notices in the actual Hub artifact, plus explicit RuneLite review of the click regions.
