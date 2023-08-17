package com.nyloer;

import java.util.HashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;

import java.awt.Color;
import java.util.List;


@Slf4j
public class CustomFontConfig
{

	private static final CustomFontConfig INSTANCE = new CustomFontConfig();
	@Getter
	private final HashMap<String, Color> colorSettings = new HashMap<>();

	public Color getColor(String fontConfigKey)
	{
		return colorSettings.get(fontConfigKey);
	}

	public List<Color> parse(NyloerConfig config)
	{
		String input = config.customFontConfig();
		if (input == null || input.isEmpty())
		{
			return null;
		}
		for (String entry : Text.fromCSV(input))
		{
			try
			{
				String[] parts = entry.split(":");
				log.info(Integer.parseInt(parts[0].trim()) + "-" + parts[1].trim() + " -> " + Color.decode(parts[2].trim()).toString());
				colorSettings.put(
					Integer.parseInt(parts[0].trim()) + "-" + parts[1].trim(),
					Color.decode(parts[2].trim())
				);
			}
			catch (Exception e)
			{
				log.warn("Parse exception: \"" + entry + "\"\n" + e.getMessage());
			}
		}
		return null;
	}

	public static CustomFontConfig getInstance()
	{
		return INSTANCE;
	}
}