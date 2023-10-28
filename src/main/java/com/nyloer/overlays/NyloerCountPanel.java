package com.nyloer.overlays;

import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;


public class NyloerCountPanel extends OverlayPanel
{
	private final NyloerPlugin plugin;
	private final NyloerConfig config;

	@Setter
	@Getter
	private Color color = Color.WHITE;

	@Inject
	private NyloerCountPanel(NyloerPlugin plugin, NyloerConfig config)
	{
		this.plugin = plugin;
		this.config = config;

		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);

		panelComponent.setPreferredSize(new Dimension(25, 40));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isNylocasRegion() || !config.showNylocasCountPanel())
		{
			return null;
		}
		Font font = new Font(config.fontsType().toString(), Font.BOLD, config.nylocasCountPanelFontsSize());
		String totalValue = String.valueOf(plugin.getNylocasAliveCount());

		if (config.nylocasCountPanelSimple())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(totalValue)
				.leftColor(color)
				.leftFont(font)
				.build());
		}
		return super.render(graphics);
	}
}
