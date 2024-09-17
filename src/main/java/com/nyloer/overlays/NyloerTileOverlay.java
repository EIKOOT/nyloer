package com.nyloer.overlays;

import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
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
							if (config.displayMageTilesAsTrueTiles())
							{
								drawTrueTile(graphics, nyloer);
							}
							else
							{
								drawTile(graphics, nyloer);
							}
						}
						break;
					case "range":
						if (renderRange)
						{
							if (config.displayRangeTilesAsTrueTiles())
							{
								drawTrueTile(graphics, nyloer);
							}
							else
							{
								drawTile(graphics, nyloer);
							}
						}
						break;
					case "melee":
						if (renderMelee)
						{
							if (config.displayMeleeTilesAsTrueTiles())
							{
								drawTrueTile(graphics, nyloer);
							}
							else
							{
								drawTile(graphics, nyloer);
							}
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

	private void drawTrueTile(Graphics2D graphics, NyloerPlugin.NyloerNpc nyloer)
	{
		NPC npc = nyloer.getNpc();
		NPCComposition npcComposition = npc.getTransformedComposition();
		LocalPoint lp = LocalPoint.fromWorld(plugin.client, npc.getWorldLocation());
		if ((lp != null) && (npcComposition != null))
		{
			final int size = npcComposition.getSize();
			final LocalPoint centerLp = lp.plus(
				Perspective.LOCAL_TILE_SIZE * (size - 1) / 2,
				Perspective.LOCAL_TILE_SIZE * (size - 1) / 2);
			Polygon tilePoly = Perspective.getCanvasTileAreaPoly(plugin.client, centerLp, size);
			renderPoly(graphics, nyloer.getColor(), highlightWidth, tilePoly);
		}
	}

	private void renderPoly(Graphics2D graphics, Color borderColor, Stroke borderStroke, Shape polygon)
	{
		if (polygon != null)
		{
			graphics.setColor(borderColor);
			graphics.setStroke(borderStroke);
			graphics.draw(polygon);
		}
	}
}
