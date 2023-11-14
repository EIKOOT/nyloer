package com.nyloer;

import com.google.inject.Provides;
import com.nyloer.stats.StatsHandler;
import com.nyloer.overlays.NyloerOverlay;
import com.nyloer.overlays.NyloerTileOverlay;
import com.nyloer.roleswapper.RoleSwapper;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import java.awt.event.KeyEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Nyloer"
)
public class NyloerPlugin extends Plugin implements KeyListener
{
	@Inject
	public Client client;

	@Inject
	public ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private EventBus eventBus;

	@Inject
	public NyloerConfig config;

	@Inject
	public RoleSwapper roleSwapper;

	@Inject
	public StatsHandler statsHandler;

	@Inject
	public CustomFontConfig customFontConfig;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NpcUtil npcUtil;

	@Inject
	private NyloerOverlay nyloerOverlay;

	@Inject
	public NyloerTileOverlay nyloerTileOverlay;

	@Inject
	private KeyManager keyManager;

	public NyloerSidePanel sidePanel;

	private static final int NYLOCAS_REGION_ID = 13122;
	private final int[] NYLOCAS_PILLAR_IDS = {8358, 10811, 10790};
	private final int[] NYLOCAS_NPC_IDS = {
		8342, 8348, 8345, 8351, 8343, 8349, 8346, 8352, 8344, 8350, 8347, 8353,
		10791, 10792, 10793, 10797, 10798, 10799, 10794, 10795, 10796,
		10800, 10801, 10802, 10774, 10777, 10780, 10783, 10775, 10778,
		10781, 10784, 10776, 10779, 10782, 10785
	};
	private final int[] MELEE_NYLOCAS_IDS = {8342, 8345, 8348, 8351, 10774, 10777, 10780, 10783, 10791, 10794, 10797, 10800};
	private final int[] RANGE_NYLOCAS_IDS = {8343, 8346, 8349, 8352, 10775, 10778, 10781, 10784, 10792, 10795, 10798, 10801};
	private final int[] MAGE_NYLOCAS_IDS = {8344, 8347, 8350, 8353, 10776, 10779, 10782, 10785, 10793, 10796, 10799, 10802};

	@Getter
	private boolean isNylocasRegion = false;

	@Getter
	private boolean isNylocasRegionLast = false;

	@Getter
	private boolean pillarsSpawned;

	@Getter
	private int w1T = 0;

	@Getter
	public int makeDarkerT;

	@Getter
	private int waveNumber;

	@Getter
	private int nylocasAliveCount;

	private int lastWaveTickSpawned;

	@Getter
	ArrayList<NyloerNpc> nyloers = new ArrayList<NyloerNpc>();
	@Getter
	HashMap<Integer, NyloerNpc> nyloersIndexMap = new HashMap<>();

	public static final Logger log = LoggerFactory.getLogger(NyloerPlugin.class);

	@Override
	protected void startUp() throws Exception
	{
		createSidePanel();
		keyManager.registerKeyListener(this);
		eventBus.register(roleSwapper);
		eventBus.register(statsHandler);
		roleSwapper.reloadSwaps();
		start();
	}

	@Override
	protected void shutDown() throws Exception
	{
		stop();
		keyManager.unregisterKeyListener(this);
		eventBus.unregister(roleSwapper);
		eventBus.unregister(statsHandler);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (config.makeDarkerHotkey().matches(e))
		{
			NyloerPlugin.log.debug("Making nyloers darker...");
			makeDarkerT = this.client.getTickCount() - 1;
			makeDarkerT -= (makeDarkerT - w1T) % 4;
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Provides
	NyloerConfig provideConfig(ConfigManager configManager)
	{
		return (NyloerConfig) configManager.getConfig(NyloerConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(NyloerConfig.GROUP))
		{
			return;
		}
		customFontConfig.getColorSettings().clear();
		customFontConfig.parse(config);
	}

	private void createSidePanel()
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/ico.png");
		sidePanel = new NyloerSidePanel(client, this, config);
		NavigationButton sidePanelButton = NavigationButton.builder().tooltip("Nyloer").icon(icon).priority(6).panel(sidePanel).build();
		clientToolbar.addNavigation(sidePanelButton);
		sidePanel.startPanel();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			stop();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getTickCount() % 5 == 0)
		{
			updateNylocasRegion();
		}
		if (isNylocasRegion && !isNylocasRegionLast)
		{
			start();
		}
		else if (!isNylocasRegion && isNylocasRegionLast)
		{
			stop();
		}
		isNylocasRegionLast = isNylocasRegion;
		if (isNylocasRegion)
		{
			for (NyloerNpc nyloer : nyloers)
			{
				if (nyloer.npc.isDead())
				{
					nyloer.setAlive(false);
				}
				else if (nyloer.isAlive)
				{
					nyloer.incrementTicks();
				}
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!isNylocasRegion)
		{
			return;
		}
		NPC npc = event.getNpc();
		String npcName = npc.getName();
		if (npcName == null)
		{
			return;
		}
		if (!pillarsSpawned && ArrayUtils.contains(NYLOCAS_PILLAR_IDS, npc.getId()))
		{
			reset();
			sidePanel.resetStallsTable();
			pillarsSpawned = true;
			return;
		}
		if (ArrayUtils.contains(NYLOCAS_NPC_IDS, npc.getId()))
		{
			NyloerNpc nyloer = new NyloerNpc(npc);
			if (!nyloer.spawn.equals("SPLIT") && lastWaveTickSpawned != client.getTickCount())
			{
				lastWaveTickSpawned = client.getTickCount();
				++waveNumber;
				++nyloer.waveSpawned;
				nyloer.updateStyle(nyloer.id);
				if (waveNumber == 1)
				{
					w1T = client.getTickCount();
				}
				else if (waveNumber == 22)
				{
					makeDarkerT = client.getTickCount();
				}
				else if (waveNumber == 29)
				{
					makeDarkerT = client.getTickCount();
				}
			}
			nyloers.add(nyloer);
			nyloersIndexMap.put(nyloer.index, nyloer);
			updateNylocasAliveCount();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (!isNylocasRegion)
		{
			return;
		}
		NyloerNpc nyloer = nyloersIndexMap.remove(event.getNpc().getIndex());
		if (nyloer != null)
		{
			updateNylocasAliveCount();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (isNylocasRegion)
		{
			if (event.getType().equals(ChatMessageType.GAMEMESSAGE))
			{
				if (event.getMessage().equals("You have failed. The vampyres take pity on you and allow you to try again..."))
				{
					reset();
				}
			}
		}
	}

	private void start()
	{
		NyloerPlugin.log.debug("Starting Nyloer.");
		customFontConfig.parse(config);
		overlayManager.add(nyloerOverlay);
		overlayManager.add(nyloerTileOverlay);
	}

	private void stop()
	{
		NyloerPlugin.log.debug("Stopping Nyloer.");
		customFontConfig.getColorSettings().clear();
		overlayManager.remove(nyloerOverlay);
		overlayManager.remove(nyloerTileOverlay);
		reset();
	}

	private void reset()
	{
		NyloerPlugin.log.debug("Resetting Nyloer.");
		waveNumber = 0;
		nylocasAliveCount = 0;
		pillarsSpawned = false;
		nyloers.clear();
		nyloerOverlay.nyloers.clear();
	}

	private void updateNylocasRegion()
	{
		isNylocasRegion = ArrayUtils.contains(client.getMapRegions(), NYLOCAS_REGION_ID);
	}

	private void updateNylocasAliveCount()
	{
		nylocasAliveCount = nyloersIndexMap.size();
	}

	public class NyloerNpc
	{
		@Getter
		private NPC npc;

		@Getter
		private int id;

		@Getter
		private int lastId;

		@Getter
		private String style;

		@Getter
		private int index;

		@Getter
		private String size;

		@Getter
		@Setter
		private boolean isAlive;

		@Getter
		private boolean isSplit;

		@Getter
		private String spawn;

		@Getter
		private int waveSpawned;

		@Getter
		public int tickSpawned;

		@Getter
		private int ticksAlive;

		@Getter
		private String nyloerSymbol;

		@Getter
		private Color color;

		@Getter
		private Color outlineColor;

		@Getter
		private boolean colorDarker;

		@Getter
		private Font font;

		@Getter
		private String fontConfigKey;

		public void incrementTicks()
		{
			++ticksAlive;
			if (ticksAlive == 52)
			{
				isAlive = false;
			}
			if (lastId != npc.getId())
			{
				lastId = npc.getId();
				updateStyle(npc.getId());
			}
		}

		private void updateStyle(int id)
		{
			if (ArrayUtils.contains(MELEE_NYLOCAS_IDS, id))
			{
				style = "melee";
				fontConfigKey = waveSpawned + "-" + "melee";
				color = config.meleeNylocasColor();
				outlineColor = config.meleeNylocasOutlineColor();
				nyloerSymbol = config.meleeNylocasSymbol();
			}
			else if (ArrayUtils.contains(RANGE_NYLOCAS_IDS, id))
			{
				style = "range";
				fontConfigKey = waveSpawned + "-" + "range";
				color = config.rangeNylocasColor();
				outlineColor = config.rangeNylocasOutlineColor();
				nyloerSymbol = config.rangeNylocasSymbol();
			}
			else if (ArrayUtils.contains(MAGE_NYLOCAS_IDS, id))
			{
				style = "mage";
				fontConfigKey = waveSpawned + "-" + "mage";
				color = config.mageNylocasColor();
				outlineColor = config.mageNylocasOutlineColor();
				nyloerSymbol = config.mageNylocasSymbol();
			}
			else
			{
				fontConfigKey = null;
				color = Color.WHITE;
				outlineColor = Color.BLACK;
				isAlive = false;
			}
			Color customColor = customFontConfig.getColor(fontConfigKey);
			if (customColor != null)
			{
				color = customColor;
			}
			if (colorDarker)
			{
				color = color.darker().darker();
			}
		}

		private void configureFonts()
		{
			int style;
			if (isSplit)
			{
				style = config.splitFontsBold() ? Font.BOLD : Font.PLAIN;
				this.font = new Font(config.splitFontsType().toString(), style, config.splitFontsSize());
			}
			else
			{
				style = config.fontsBold() ? Font.BOLD : Font.PLAIN;
				this.font = new Font(config.fontsType().toString(), style, config.fontsSize());
			}
		}

		private String findSpawn(NPC npc)
		{
			int x = WorldPoint.fromLocalInstance(NyloerPlugin.this.client, npc.getLocalLocation()).getRegionX();
			int y = WorldPoint.fromLocalInstance(NyloerPlugin.this.client, npc.getLocalLocation()).getRegionY();
			if (x == 17 && y == 25)
			{
				return "WEST NORTH";
			}
			else if (x == 17 && y == 24)
			{
				return "WEST SOUTH";
			}
			else if (x == 31 && y == 9)
			{
				return "SOUTH WEST";
			}
			else if (x == 32 && y == 9)
			{
				return "SOUTH EAST";
			}
			else if (x == 46 && y == 24)
			{
				return "EAST SOUTH";
			}
			else if (x == 46 && y == 25)
			{
				if (npc.getComposition().getSize() == 1)
				{
					return "EAST NORTH";
				}
				else
				{
					return "EAST BIG";
				}
			}
			else if (x == 18 && y == 25)
			{
				return "WEST BIG";
			}
			else if (x == 32 && y == 10)
			{
				return "SOUTH BIG";
			}
			else if (x == 47 && y == 25)
			{
				return "EAST BIG 30";
			}
			else
			{
				return "SPLIT";
			}
		}

		public NyloerNpc(NPC npc)
		{
			this.npc = npc;
			this.id = npc.getId();
			this.lastId = npc.getId();
			this.index = npc.getIndex();
			this.isAlive = true;
			this.spawn = findSpawn(npc);
			this.isSplit = this.spawn.equals("SPLIT");
			this.tickSpawned = NyloerPlugin.this.client.getTickCount();
			this.colorDarker = false;
			if (isSplit && config.splitsAsNextWave() && tickSpawned > NyloerPlugin.this.lastWaveTickSpawned)
			{
				this.waveSpawned = NyloerPlugin.this.waveNumber + 1;
			}
			else
			{
				this.waveSpawned = NyloerPlugin.this.waveNumber;
			}
			this.ticksAlive = 0;
			this.updateStyle(id);
			this.configureFonts();
			this.size = npc.getComposition().getSize() == 1 ? "SMALL" : "BIG";
		}
	}
}
