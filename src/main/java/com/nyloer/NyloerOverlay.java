package com.nyloer;

import com.google.common.collect.ArrayListMultimap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.stream.IntStream;
import javax.inject.Inject;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class NyloerOverlay extends Overlay
{
	public ArrayListMultimap<WorldPoint, NyloerPlugin.NyloerNpc> nyloers;
	private final NyloerConfig config;
	private final NyloerPlugin plugin;
	private final NpcUtil npcUtil;

	@Inject
	private NyloerOverlay(NyloerPlugin plugin, NyloerConfig config, NpcUtil npcUtil)
	{
		this.setPosition(OverlayPosition.DYNAMIC);
		this.setPriority(OverlayPriority.HIGH);
		this.setLayer(OverlayLayer.ABOVE_SCENE);
		this.npcUtil = npcUtil;
		this.plugin = plugin;
		this.config = config;
		this.nyloers = ArrayListMultimap.create();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		this.nyloers.clear();
		if (this.plugin.nyloers.isEmpty())
		{
			return null;
		}
		nyloers = ArrayListMultimap.create();
		for (NyloerPlugin.NyloerNpc nyloer : this.plugin.nyloers)
		{
			if (nyloer.isAlive() && !npcUtil.isDying(nyloer.getNpc()))
			{
				nyloers.put(nyloer.getNpc().getWorldLocation(), nyloer);
			}
		}
		if (!nyloers.isEmpty())
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int style;
			if (config.fontsBold())
			{
				style = Font.BOLD;
			}
			else
			{
				style = Font.PLAIN;
			}
			graphics.setFont(new Font(config.fontsType().toString(), style, config.fontsSize()));

			nyloers.asMap().forEach(
				(worldPoint, npcs) -> {
					int offset = 0;
					for (NyloerPlugin.NyloerNpc nyloer : npcs)
					{
						this.draw(graphics, nyloer, offset);
						offset += graphics.getFontMetrics().getHeight();
					}
				});
		}
		return null;
	}

	private void draw(Graphics2D graphics, NyloerPlugin.NyloerNpc nyloer, int offset)
	{
		graphics.setColor(Color.BLACK);
		String prefix;
		String text;
		if (nyloer.isSplit())
		{
			prefix = config.splitPrefix();
		}
		else
		{
			prefix = config.wavePrefix();
		}
		if (config.showNylocasWave())
		{
			text = prefix + nyloer.getWaveSpawned();
		}
		else if (config.showNylocasSymbol())
		{
			text = prefix + nyloer.getNyloerSymbol();
		}
		else
		{
			text = prefix;
		}
		Point canvasTextLocation = nyloer.getNpc().getCanvasTextLocation(graphics, text, 0);
		if (canvasTextLocation == null)
		{
			return;
		}
		int x = canvasTextLocation.getX();
		int y = canvasTextLocation.getY() + offset;
		IntStream.range(-1, 2).forEachOrdered(ex -> {
			IntStream.range(-1, 2).forEachOrdered(ey -> {
				if (ex != 0 && ey != 0)
				{
					graphics.drawString(text, x + ex, y + ey);
				}
			});
		});
		graphics.setColor(nyloer.getColor());
		graphics.drawString(text, x, y);
	}
}
