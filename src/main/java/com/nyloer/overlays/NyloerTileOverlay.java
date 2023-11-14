package com.nyloer.overlays;

import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;


public class NyloerTileOverlay extends Overlay
{
	private final NyloerConfig config;
	private final NyloerPlugin plugin;
	private final NpcUtil npcUtil;

	Stroke highlightWidth;

	@Setter
	private boolean renderMage;
	@Setter
	private boolean renderRange;
	@Setter
	private boolean renderMelee;

	@Inject
	private NyloerTileOverlay(NyloerPlugin plugin, NyloerConfig config, NpcUtil npcUtil)
	{
		this.setPosition(OverlayPosition.DYNAMIC);
		this.setPriority(OverlayPriority.HIGH);
		this.setLayer(OverlayLayer.ABOVE_SCENE);
		this.npcUtil = npcUtil;
		this.plugin = plugin;
		this.config = config;

		this.renderMage = false;
		this.renderRange = false;
		this.renderMelee = false;

		this.highlightWidth = new BasicStroke((float) config.tileHighlightWidth());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(NyloerConfig.GROUP))
		{
			this.highlightWidth = new BasicStroke((float) config.tileHighlightWidth());
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.plugin.getNylocasAliveCount() == 0)
		{
			return null;
		}
		for (NyloerPlugin.NyloerNpc nyloer : this.plugin.getNyloersIndexMap().values())
		{
			if ((nyloer.isAlive()) && (!npcUtil.isDying(nyloer.getNpc())))
			{
				switch (nyloer.getStyle())
				{
					case "mage":
						if (renderMage)
						{
							drawTile(graphics, nyloer);
						}
						break;
					case "range":
						if (renderRange)
						{
							drawTile(graphics, nyloer);
						}
						break;
					case "melee":
						if (renderMelee)
						{
							drawTile(graphics, nyloer);
						}
						break;
				}
			}
		}
		return null;
	}

	private void drawTile(Graphics2D graphics, NyloerPlugin.NyloerNpc nyloer)
	{
		Polygon polygon = nyloer.getNpc().getCanvasTilePoly();
		if (polygon != null)
		{
			OverlayUtil.renderPolygon(graphics, polygon, nyloer.getColor(), highlightWidth);
		}
	}
}
