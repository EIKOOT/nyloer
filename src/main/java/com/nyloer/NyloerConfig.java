package com.nyloer;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;


@ConfigGroup("NyloerConfig")
public interface NyloerConfig extends Config
{
	@ConfigSection(
		name = "General Settings",
		position = 0,
		description = "General settings",
		closedByDefault = false
	)
	String generalSettings = "generalSettings";

	@ConfigItem(
		position = 0,
		keyName = "showNylocasSymbol",
		name = "Show Nylocas Symbol",
		description = "Displays selected symbol instead of wave.",
		section = generalSettings
	)
	default boolean showNylocasSymbol()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "meleeNylocasSymbol",
		name = "Melee Symbol",
		description = "Symbol of melee nylocas.",
		section = generalSettings
	)
	default String meleeNylocasSymbol()
	{
		return "I";
	}

	@ConfigItem(
		position = 2,
		keyName = "rangeNylocasSymbol",
		name = "Range Symbol",
		description = "Symbol of range nylocas.",
		section = generalSettings
	)
	default String rangeNylocasSymbol()
	{
		return "T";
	}

	@ConfigItem(
		position = 3,
		keyName = "mageNylocasSymbol",
		name = "Mage Symbol",
		description = "Symbol of mage nylocas.",
		section = generalSettings
	)
	default String mageNylocasSymbol()
	{
		return "H";
	}

	@ConfigItem(
		position = 4,
		keyName = "showNylocasWave",
		name = "Show Nylocas Wave",
		description = "Display wave on nylocas. \"Show nylocas symbol\" must be disabled.",
		section = generalSettings
	)
	default boolean showNylocasWave()
	{
		return false;
	}

	@ConfigItem(
		position = 5,
		keyName = "wavePrefix",
		name = "Wave Spawn Prefix",
		description = "Prefix for spawned nylocas from waves.",
		section = generalSettings
	)
	default String wavePrefix()
	{
		return "";
	}

	@ConfigItem(
		position = 6,
		keyName = "splitPrefix",
		name = "Split Spawn Prefix",
		description = "Prefix for spawned nylocas from splits.",
		section = generalSettings
	)
	default String splitPrefix()
	{
		return "s";
	}

	// ------------------------------------------------------------

	@ConfigSection(
		name = "Font Settings",
		position = 1,
		description = "Fonts settings",
		closedByDefault = false
	)
	String fontsSettings = "fontsSettings";

	@ConfigItem(
		position = 0,
		keyName = "fontsType",
		name = "Font Type",
		description = "Nylocas wave number font type.",
		section = fontsSettings
	)
	default NyloerFonts fontsType()
	{
		return NyloerFonts.ARIAL;
	}

	@ConfigItem(
		position = 1,
		keyName = "fontsBold",
		name = "Bold",
		description = "Nylocas wave number fonts will be bold.",
		section = fontsSettings
	)
	default boolean fontsBold()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "fontsSize",
		name = "Font Size",
		description = "Nylocas wave number size.",
		section = fontsSettings
	)
	default int fontsSize()
	{
		return 12;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "meleeNylocasColor",
		name = "Melee Color",
		description = "Color of melee nylocas.",
		section = fontsSettings
	)
	default Color meleeNylocasColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		position = 4,
		keyName = "rangeNylocasColor",
		name = "Range color",
		description = "Color of range nylocas.",
		section = fontsSettings
	)
	default Color rangeNylocasColor()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
		position = 5,
		keyName = "mageNylocasColor",
		name = "Mage Color",
		description = "Color of mage nylocas.",
		section = fontsSettings
	)
	default Color mageNylocasColor()
	{
		return Color.CYAN;
	}
}
