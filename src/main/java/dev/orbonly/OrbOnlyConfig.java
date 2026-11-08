/*
 * Copyright (c) 2026, drippybacon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dev.orbonly;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("orbonly")
public interface OrbOnlyConfig extends Config
{
	@ConfigSection(
			name = "Labels",
			description = "Show and position the orb value labels.",
			position = 0
	)
	String LABELS_SECTION = "labels";

	@ConfigSection(
			name = "Prayer",
			description = "Prayer points, time remaining and tick helper.",
			position = 1
	)
	String PRAYER_SECTION = "prayer";

	@ConfigSection(
			name = "Highlights",
			description = "Colours for active prayer and special attack.",
			position = 2
	)
	String HIGHLIGHTS_SECTION = "highlights";

	@ConfigSection(
			name = "Poison and venom",
			description = "Estimated countdown around the hitpoints orb.",
			position = 3
	)
	String POISON_SECTION = "poisonAndVenom";

	@ConfigItem(
			keyName = "showOrbValues",
			name = "Show labels",
			description = "Show orb values. Turn off for icons only.",
			position = 0,
			section = LABELS_SECTION
	)
	default boolean showOrbValues()
	{
		return true;
	}

	@ConfigItem(
			keyName = "labelPosition",
			name = "Label position",
			description = "Center values inside each orb, or Alt-drag Free labels. Alt-right-click resets a Free label.",
			position = 1,
			section = LABELS_SECTION
	)
	default OrbLabelPosition labelPosition()
	{
		return OrbLabelPosition.CENTER;
	}

	@Range(min = 10, max = 24)
	@ConfigItem(
			keyName = "valueFontSize",
			name = "Label font size",
			description = "Font size; centered labels shrink to fit.",
			position = 2,
			section = LABELS_SECTION
	)
	default int valueFontSize()
	{
		return 16;
	}

	@ConfigItem(
			keyName = "labelColor",
			name = "Label colour",
			description = "Label colour when no active-state highlight applies.",
			position = 3,
			section = LABELS_SECTION
	)
	default Color labelColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
			keyName = "showPrayerPoints",
			name = "Show prayer points",
			description = "Show points instead of estimated time while prayers are active. Inactive prayers always show points.",
			position = 4,
			section = PRAYER_SECTION
	)
	default boolean showPrayerPoints()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showPrayerTickHelper",
			name = "Show prayer tick helper",
			description = "Show the Prayer plugin's tick sweep on the orb. Uses its flick-helper settings.",
			position = 5,
			section = PRAYER_SECTION
	)
	default boolean showPrayerTickHelper()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightActiveSpecialAttack",
			name = "Highlight active special attack",
			description = "Highlight the orb and label when special attack is armed.",
			position = 6,
			section = HIGHLIGHTS_SECTION
	)
	default boolean highlightActiveSpecialAttack()
	{
		return true;
	}

	@ConfigItem(
			keyName = "activeSpecialAttackColor",
			name = "Active special attack colour",
			description = "Border and label colour for an armed special attack.",
			position = 7,
			section = HIGHLIGHTS_SECTION
	)
	default Color activeSpecialAttackColor()
	{
		return new Color(80, 255, 128);
	}

	@ConfigItem(
			keyName = "highlightActivePrayer",
			name = "Highlight active prayer",
			description = "Highlight the orb and label while quick prayers or any individual prayer are active.",
			position = 8,
			section = HIGHLIGHTS_SECTION
	)
	default boolean highlightActivePrayer()
	{
		return true;
	}

	@ConfigItem(
			keyName = "activePrayerColor",
			name = "Active prayer colour",
			description = "Border and label colour while prayer is active.",
			position = 9,
			section = HIGHLIGHTS_SECTION
	)
	default Color activePrayerColor()
	{
		return new Color(80, 220, 255);
	}

	@ConfigItem(
			keyName = "showPoisonCountdown",
			name = "Poison/venom countdown",
			description = "Show an estimated countdown to the next poison/venom hit around the HP orb.",
			position = 10,
			section = POISON_SECTION
	)
	default boolean showPoisonCountdown()
	{
		return true;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
			keyName = "poisonRingThickness",
			name = "Countdown ring thickness",
			description = "Countdown ring width in pixels.",
			position = 11,
			section = POISON_SECTION
	)
	default int poisonRingThickness()
	{
		return 2;
	}

	@ConfigItem(
			keyName = "poisonRingColor",
			name = "Poison ring colour",
			description = "Ring colour while poisoned.",
			position = 12,
			section = POISON_SECTION
	)
	default Color poisonRingColor()
	{
		return new Color(80, 255, 128);
	}

	@ConfigItem(
			keyName = "venomRingColor",
			name = "Venom ring colour",
			description = "Ring colour while venomed.",
			position = 13,
			section = POISON_SECTION
	)
	default Color venomRingColor()
	{
		return new Color(185, 80, 255);
	}
}
