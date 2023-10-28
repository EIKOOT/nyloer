package com.nyloer.nylostats;

import com.google.inject.Provides;

import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.regex.Pattern;
import org.lwjgl.system.linux.Stat;


public class NyloStats
{
	private Client client;
	private NyloerPlugin plugin;
	private NyloerConfig config;

	private ArrayList<Stall> stallArray;
	int w1T;
	int lastNyloDeathT;
	int bossSpawnT;
	int bossDeathT;

	private int currWave;
	private int stalls;
	private int ticksSinceLastWave;
	private int[] splits;
	private int[] preCapSplits;
	private int[] bossRotation;
	private boolean isHmt;
	private int currCap;
	private ArrayList<String> stallMessages;
	private static final Pattern NYLO_COMPLETE = Pattern.compile("Wave 'The Nylocas' \\(.*\\) complete!");
	private final int NYLOCAS_REGIONID = 13122;


	private static final HashMap<Integer, Integer> waveNaturalStalls;

	static
	{
		waveNaturalStalls = new HashMap<>();
		waveNaturalStalls.put(1, 4);
		waveNaturalStalls.put(2, 4);
		waveNaturalStalls.put(3, 4);
		waveNaturalStalls.put(4, 4);
		waveNaturalStalls.put(5, 16);

		waveNaturalStalls.put(6, 4);
		waveNaturalStalls.put(7, 12);
		waveNaturalStalls.put(8, 4);
		waveNaturalStalls.put(9, 12);
		waveNaturalStalls.put(10, 8);

		waveNaturalStalls.put(11, 8);
		waveNaturalStalls.put(12, 8);
		waveNaturalStalls.put(13, 8);
		waveNaturalStalls.put(14, 8);
		waveNaturalStalls.put(15, 8);

		waveNaturalStalls.put(16, 4);
		waveNaturalStalls.put(17, 12);
		waveNaturalStalls.put(18, 8);
		waveNaturalStalls.put(19, 12);
		waveNaturalStalls.put(20, 16);

		waveNaturalStalls.put(21, 8);
		waveNaturalStalls.put(22, 12);
		waveNaturalStalls.put(23, 8);
		waveNaturalStalls.put(24, 8);
		waveNaturalStalls.put(25, 8);

		waveNaturalStalls.put(26, 4);
		waveNaturalStalls.put(27, 8);
		waveNaturalStalls.put(28, 4);
		waveNaturalStalls.put(29, 4);
		waveNaturalStalls.put(30, 4);
	}

	@Inject
	protected NyloStats(NyloerPlugin plugin, NyloerConfig config)
	{
		this.client = plugin.client;
		this.plugin = plugin;
		this.config = config;

		stallArray = new ArrayList<Stall>();
		w1T = -1;
		lastNyloDeathT = -1;
		bossSpawnT = -1;
		bossDeathT = -1;

		currWave = 0;
		ticksSinceLastWave = 0;
		stalls = 0;
		currCap = 12;
		isHmt = false;
		stallMessages = new ArrayList<>();
		splits = new int[3];
		preCapSplits = new int[3];
		bossRotation = new int[3];
		bossRotation[0] = 1;
	}

	@Provides
	NyloerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NyloerConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!inNyloRegion())
		{
			return;
		}
		if (isCapCheck())
		{
			if (isHmt)
			{
				if (hmtWavesCheck() == 1)
				{
					return;
				}
			}
			if (currWave > 19)
			{
				currCap = 24; // regular and hmt postcap is 24
			}

			int nylocasAliveCount = getNylocasAliveCount();
			if (nylocasAliveCount >= currCap)
			{
				addStall(new Stall(currWave, nylocasAliveCount, currCap, stallArray.size() + 1));
			}
		}
		ticksSinceLastWave++;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		if (isNylocas(npc))
		{
			if (isSplit(npc))
			{
				if (npc.getId() == 8342 || npc.getId() == 10774 || npc.getId() == 10791)
				{
					splits[0]++;
				}
				else if (npc.getId() == 8343 || npc.getId() == 10775 || npc.getId() == 10792)
				{
					splits[1]++;
				}
				else if (npc.getId() == 8344 || npc.getId() == 10776 || npc.getId() == 10793)
				{
					splits[2]++;
				}

				if (npc.getId() == 10791 || npc.getId() == 10792 || npc.getId() == 17093)
				{
					isHmt = true;
				}
			}
			else
			{
				if (ticksSinceLastWave > 3)
				{
					if (currWave > 1 && (ticksSinceLastWave - waveNaturalStalls.get(currWave)) > 0)
					{
						int stallAmount = (ticksSinceLastWave - waveNaturalStalls.get(currWave)) / 4;

						if (isHmt && currWave == 10)
						{
							stallAmount -= 2;
						}
						else if (isHmt && currWave == 30)
						{
							stallAmount -= 3;
						}
						stalls += stallAmount;
					}
					currWave++;
					if (currWave == 1)
					{
						w1T = client.getTickCount();
					}
					if (currWave == 20)
					{
						preCapSplits = splits.clone();
					}
					ticksSinceLastWave = 0;
				}
			}
		}
		else if (isNylocasVasilias(npc))
		{
			bossSpawnT = client.getTickCount();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		if (isNylocas(npc))
		{
			if (getNylocasAliveCount() == 0)
			{
				lastNyloDeathT = client.getTickCount();
			}
		}
		else if (isNylocasVasilias(npc))
		{
			bossDeathT = client.getTickCount() - 2;
			saveStats();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!inNyloRegion() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String msg = Text.removeTags(event.getMessage());
		if (NYLO_COMPLETE.matcher(msg).find())
		{
			if (currWave != 31)
			{
				reset();
				return;
			}
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Boss rotation: [<col=EF1020>" + bossRotation[0] +
				"</col>] [<col=00FF0A>" + bossRotation[2] + "</col>] [<col=2536CA>" + bossRotation[1] + "</col>]", "");
			reset();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		int tobVar = client.getVarbitValue(Varbits.THEATRE_OF_BLOOD);
		boolean inTob = tobVar == 2 || tobVar == 3;
		if (!inTob)
		{
			saveStats();
			reset();
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged npcChanged)
	{
		int npcId = npcChanged.getNpc().getId();

		switch (npcId)
		{
			case 8355:
			case 10787:
			case 10808:
				bossRotation[0]++;
				break;
			case 8356:
			case 10788:
			case 10809:
				bossRotation[1]++;
				break;
			case 8357:
			case 10789:
			case 10810:
				bossRotation[2]++;
				break;
		}
	}

	private void printSplits()
	{

		String msgCap = "Pre cap splits: [<col=EF1020>" + preCapSplits[0] +
			"</col>] [<col=00FF0A>" + preCapSplits[1] + "</col>] [<col=2536CA>" + preCapSplits[2] + "</col>]";
		msgCap += " Post cap splits: [<col=EF1020>" + (splits[0] - preCapSplits[0]) +
			"</col>] [<col=00FF0A>" + (splits[1] - preCapSplits[1]) + "</col>] [<col=2536CA>" + (splits[2] - preCapSplits[2]) + "</col>]";

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msgCap, "");

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Total splits: [<col=EF1020>" + splits[0] +
			"</col>] [<col=00FF0A>" + splits[1] + "</col>] [<col=2536CA>" + splits[2] + "</col>]", "");
	}

	private void addStall(Stall stall)
	{
		stallArray.add(stall);
		plugin.sidePanel.addStall(stall);

		String stallMsg = "Stalled wave: <col=EF1020>" + stall.getWave() + "/31</col>";
		stallMsg += " - Nylos alive: <col=EF1020>" + stall.getAliveCount() + "/" + stall.getCapSize() + "</col>";
		stallMsg += " - Total Stalls: <col=EF1020>" + stall.getTotalStalls() + "</col>";
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", stallMsg, "");
	}

	private void saveStats()
	{
		int tWaves = lastNyloDeathT - w1T + 4;
		int tBossSpawnWait = 16;
		if ((tWaves % 4) != 0)
		{
			tBossSpawnWait += 4 - (tWaves % 4);
		}
		int tBossSpawn = tWaves + tBossSpawnWait;
		int tBoss = bossDeathT - bossSpawnT;
		if ((tBoss % 4) != 0)
		{
			tBoss += 4 - (tBoss % 4);
		}
		int tTotal = tBossSpawn + tBoss;

		Stats stats = new Stats(
			ticks2Time(tTotal),
			ticks2Time(tBoss),
			ticks2Time(tBossSpawn),
			0,
			0,
			0,
			0,
			0,
			0
		);
		for (Stall stall : stallArray)
		{
			if (stall.getWave() < 20)
			{
				stats.stallCountPre++;
			}
			else if (stall.getWave() == 21)
			{
				stats.stallCount21++;
			}
			else if ((stall.getWave() >= 22) && (stall.getWave() <= 27))
			{
				stats.stallCount22to27++;
			}
			else if (stall.getWave() == 28)
			{
				stats.stallCount28++;
			}
			else if (stall.getWave() == 29)
			{
				stats.stallCount29++;
			}
			else if (stall.getWave() == 30)
			{
				stats.stallCount30++;
			}
		}
		plugin.sidePanel.addStats(stats);
	}

	private String ticks2Time(int ticks)
	{
		int millis = ticks * 600;
		String hundredths = String.valueOf(millis % 1000).substring(0, 1);
		return String.format(
			"%d:%02d.%s",
			TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
			TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
			hundredths
		);
	}

	private boolean inNyloRegion()
	{
		return ArrayUtils.contains(client.getMapRegions(), NYLOCAS_REGIONID);
	}

	private boolean isCapCheck()
	{
		if ((currWave > 1) && (currWave < 31))
		{
			return (ticksSinceLastWave % 4 == 0) && (ticksSinceLastWave >= waveNaturalStalls.get(currWave));
		}
		return false;
	}

	private int getNylocasAliveCount()
	{
		int nylocasAliveCount = 0;
		for (NPC npc : client.getNpcs())
		{
			if (isNylocas(npc))
			{
				nylocasAliveCount++;
			}
			if (isNylocasPrinkipas(npc))
			{
				nylocasAliveCount += 3; // nylo prince adds 3 to the cap
			}
		}
		return nylocasAliveCount;
	}

	private boolean isNylocas(NPC npc)
	{
		String name = npc.getName();
		if (name == null)
		{
			return false;
		}
		return (name.equals("Nylocas Ischyros")) || (name.equals("Nylocas Toxobolos")) || (name.equals("Nylocas Hagios"));
	}

	private boolean isSplit(NPC npc)
	{
		WorldPoint location = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
		Point point = new Point(location.getRegionX(), location.getRegionY());
		NyloSpawns nyloSpawn = NyloSpawns.getLookup().get(point);
		return nyloSpawn == null;
	}

	private boolean isNylocasVasilias(NPC npc)
	{
		String name = npc.getName();
		if (name == null)
		{
			return false;
		}
		return name.equals("Nylocas Vasilias");
	}

	private boolean isNylocasPrinkipas(NPC npc)
	{
		String name = npc.getName();
		if (name == null)
		{
			return false;
		}
		return name.equals("Nylocas Prinkipas");
	}

	private int hmtWavesCheck()
	{
		if (currWave == 10)
		{
			int hmtWaveTen = waveNaturalStalls.get(currWave) + 8; // Hmt wave 10 has 2 additional natural stalls
			if (!(ticksSinceLastWave >= hmtWaveTen))
			{
				ticksSinceLastWave++;
				return 1;
			}
		}
		else if (currWave == 30)
		{
			int hmtWaveThirty = waveNaturalStalls.get(currWave) + 12;    // Hmt wave 30 has 3 additional natural stalls
			if (!(ticksSinceLastWave >= hmtWaveThirty))
			{
				ticksSinceLastWave++;
				return 1;
			}
		}
		currCap = 15; // hmt precap is 15
		return 0;
	}

	private void reset()
	{
		stallArray.clear();
		w1T = -1;
		lastNyloDeathT = -1;
		bossSpawnT = -1;
		bossDeathT = -1;

		currWave = 0;
		ticksSinceLastWave = 0;
		stalls = 0;
		isHmt = false;
		currCap = 12;
		stallMessages.clear();
		splits = new int[3];
		preCapSplits = new int[3];
		bossRotation = new int[3];
		bossRotation[0] = 1;
	}
}
