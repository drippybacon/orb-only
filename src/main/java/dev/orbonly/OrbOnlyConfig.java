package dev.orbonly;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("orbonly")
public interface OrbOnlyConfig extends Config
{
	@ConfigItem(
		keyName = "showOrbValues",
		name = "Show labels",
		description = "Show the live orb values. Disable for icons only.",
		position = 0
	)
	default boolean showOrbValues()
	{
		return true;
	}

	@ConfigItem(
		keyName = "labelPosition",
		name = "Label position",
		description = "Center keeps values inside the orbs. Free lets you hold Alt and drag each label separately; Alt-right-click resets its position.",
		position = 1
	)
	default OrbLabelPosition labelPosition()
	{
		return OrbLabelPosition.CENTER;
	}

	@Range(min = 10, max = 24)
	@ConfigItem(
		keyName = "valueFontSize",
		name = "Label font size",
		description = "Choose the size of the labels. Centered labels shrink to fit inside their orb.",
		position = 2
	)
	default int valueFontSize()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "labelColor",
		name = "Label colour",
		description = "Colour of the labels. Active prayer and special attack use their highlight colours when those highlights are enabled.",
		position = 3
	)
	default Color labelColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "showPrayerTimeRemaining",
		name = "Show prayer time remaining",
		description = "Show estimated time until prayer runs out while prayers are active. Disable to show prayer points. Inactive prayers always show points. Applies to Center and Free labels.",
		position = 4
	)
	default boolean showPrayerTimeRemaining()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightActiveSpecialAttack",
		name = "Highlight active special attack",
		description = "Highlight the border and label while special attack is enabled.",
		position = 5
	)
	default boolean highlightActiveSpecialAttack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "activeSpecialAttackColor",
		name = "Active special attack colour",
		description = "Border and label colour while special attack is enabled.",
		position = 6
	)
	default Color activeSpecialAttackColor()
	{
		return new Color(80, 255, 128);
	}

	@ConfigItem(
		keyName = "highlightActivePrayer",
		name = "Highlight active prayer",
		description = "Highlight the border and label while quick prayers or an individual prayer are active.",
		position = 7
	)
	default boolean highlightActivePrayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "activePrayerColor",
		name = "Active prayer colour",
		description = "Border and label colour while quick prayers or an individual prayer are active.",
		position = 8
	)
	default Color activePrayerColor()
	{
		return new Color(80, 220, 255);
	}

	@ConfigItem(
		keyName = "showPoisonCountdown",
		name = "Poison/venom countdown",
		description = "Show a shrinking ring on the HP orb until the next poison or venom damage tick. It follows the orb automatically and resyncs with damage hitsplats.",
		position = 9
	)
	default boolean showPoisonCountdown()
	{
		return true;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "poisonRingThickness",
		name = "Countdown ring thickness",
		description = "Thickness of the poison/venom countdown ring in pixels.",
		position = 10
	)
	default int poisonRingThickness()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "poisonRingColor",
		name = "Poison ring colour",
		description = "Countdown ring colour while poisoned.",
		position = 11
	)
	default Color poisonRingColor()
	{
		return new Color(80, 255, 128);
	}

	@ConfigItem(
		keyName = "venomRingColor",
		name = "Venom ring colour",
		description = "Countdown ring colour while venomed.",
		position = 12
	)
	default Color venomRingColor()
	{
		return new Color(185, 80, 255);
	}
}
