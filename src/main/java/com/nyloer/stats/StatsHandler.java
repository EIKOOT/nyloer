package com.nyloer.stats;

import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import java.util.*;
import java.util.regex.Pattern;


@Getter
public class StatsHandler
{
	private final Client client;
	private final NyloerPlugin plugin;
	private final NyloerConfig config;

	private int currentWave;
	private int capSize;
	private int capSizePostcap;
	private int ticksSinceLastWave;

	private final ArrayList<Stall> stalls;
	private final Stats stats;
	private int w1T;
	private int lastNyloDeathT;
	private int bossSpawnT;
	private int bossDeathT;

	private static final Pattern NYLO_RETRY_MESSAGE = Pattern.compile("You have failed. The vampyres take pity on you and allow you to try again...");
	private static final Pattern NYLO_COMPLETE_MESSAGE = Pattern.compile("Wave 'The Nylocas' \\(.*\\) complete!");

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
	protected StatsHandler(NyloerPlugin plugin, NyloerConfig config)
	{
		this.client = plugin.client;
		this.plugin = plugin;
		this.config = config;

		stalls = new ArrayList<Stall>();
		stats = new Stats();
		reset();
	}

	public void reset()
	{
		currentWave = 0;
		capSize = 12;
		capSizePostcap = 24;
		ticksSinceLastWave = 0;
		stalls.clear();
		stats.reset();

		w1T = -1;
		lastNyloDeathT = -1;
		bossSpawnT = -1;
		bossDeathT = -1;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!plugin.isNylocasRegion())
		{
			return;
		}
		if (isCapCheck())
		{
			int nylocasAliveCount = getNylocasAliveCount();
			if (nylocasAliveCount >= capSize)
			{
				addStall(new Stall(currentWave, nylocasAliveCount, capSize, stalls.size() + 1));
			}
		}
		ticksSinceLastWave++;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		NyloerPlugin.NyloerNpc nyloer = plugin.getNyloersIndexMap().get(npc.getIndex());
		if (nyloer != null)
		{
			if ((!nyloer.isSplit()) && (ticksSinceLastWave > 3))
			{
				waveSpawned();
			}
		}
		else if (isNylocasVasiliasSpawn(npc))
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
			if (plugin.getNyloersIndexMap().size() == 0)
			{
				lastNyloDeathT = client.getTickCount();
			}
		}
		else if (isNylocasVasiliasDespawn(npc))
		{
			bossDeathT = client.getTickCount();
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged npcChanged)
	{
		int npcId = npcChanged.getNpc().getId();
		switch(npcId)
		{
			case NpcID.NYLOCAS_VASILIAS_8355:
			case NpcID.NYLOCAS_VASILIAS_10787:
			case NpcID.NYLOCAS_VASILIAS_10808:
				stats.bossRotation[0]++;
				break;
			case NpcID.NYLOCAS_VASILIAS_8356:
			case NpcID.NYLOCAS_VASILIAS_10788:
			case NpcID.NYLOCAS_VASILIAS_10809:
				stats.bossRotation[1]++;
				break;
			case NpcID.NYLOCAS_VASILIAS_8357:
			case NpcID.NYLOCAS_VASILIAS_10789:
			case NpcID.NYLOCAS_VASILIAS_10810:
				stats.bossRotation[2]++;
				break;
		}
	}

	public void waveSpawned()
	{
		currentWave++;
		ticksSinceLastWave = 0;
		switch (currentWave)
		{
			case 1:
				w1T = client.getTickCount();
				break;
			case 20:
				capSize = capSizePostcap;
			case 22:
				stats.bigsAlive22 = getBigsAliveCount();
				break;
			case 29:
				stats.bigsAlive29 = getBigsAliveCount();
				break;
			case 30:
				stats.bigsAlive30 = getBigsAliveCount();
				break;
			case 31:
				stats.bigsAlive31 = getBigsAliveCount();
				break;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!plugin.isNylocasRegion() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String msg = Text.removeTags(event.getMessage());
		if (NYLO_COMPLETE_MESSAGE.matcher(msg).find())
		{
			addStats();
			reset();
		}
		else if (NYLO_RETRY_MESSAGE.matcher(msg).find())
		{
			addStats();
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
		if (!inTob && currentWave != 0)
		{
			addStats();
			reset();
		}
	}

	private void addStall(Stall stall)
	{
		stalls.add(stall);
		plugin.sidePanel.addStall(stall);

		if (stall.getWave() < 20)
		{
			stats.stallCountPre++;
			if (stall.getWave() < 13)
			{
				stats.stallCount1to12++;
			}
			else
			{
				stats.stallCount13to19++;
			}
		}
		switch (stall.getWave())
		{
			case 21:
				stats.stallCount21++;
				break;
			case 22:
			case 23:
			case 24:
			case 26:
			case 27:
				stats.stallCount22to27++;
				break;
			case 28:
				stats.stallCount28++;
				break;
			case 29:
				stats.stallCount29++;
				break;
			case 30:
				stats.stallCount30++;
				break;
		}
	}

	public void addStats()
	{
		if (lastNyloDeathT == -1)
		{
			return;
		}
		int tTotal;
		int tBoss;
		int tBossSpawn;

		int tWaves = lastNyloDeathT - w1T + 4;
		int tBossSpawnWait = 16;
		if ((tWaves % 4) != 0)
		{
			tBossSpawnWait += 4 - (tWaves % 4);
		}
		tBossSpawn = tWaves + tBossSpawnWait;

		if (bossDeathT != -1)
		{
			tBoss = bossDeathT - bossSpawnT + 2;
			if ((tBoss % 4) != 0)
			{
				tBoss += 4 - (tBoss % 4);
			}
			tTotal = tBossSpawn + tBoss;
		}
		else
		{
			tBoss = 0;
			tTotal = 0;
		}
		stats.totalTime = _ticks2Time(tTotal);
		stats.bossTime = _ticks2Time(tBoss);
		stats.wavesTime = _ticks2Time(tBossSpawn);
		plugin.sidePanel.addStats(stats);
	}

	private boolean isCapCheck()
	{
		if ((currentWave > 1) && (currentWave < 31))
		{
			return (ticksSinceLastWave % 4 == 0) && (ticksSinceLastWave >= waveNaturalStalls.get(currentWave));
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
		}
		return nylocasAliveCount;
	}

	private int getBigsAliveCount()
	{
		int bigsAliveCount = 0;
		for (NyloerPlugin.NyloerNpc nyloer : plugin.getNyloersIndexMap().values())
		{
			if (nyloer.getSize().equals("BIG"))
			{
				bigsAliveCount++;
			}
		}
		return bigsAliveCount;
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

	private boolean isNylocasVasiliasSpawn(NPC npc)
	{
		switch (npc.getId())
		{
			case NpcID.NYLOCAS_VASILIAS:
			case NpcID.NYLOCAS_VASILIAS_10786:
			case NpcID.NYLOCAS_VASILIAS_10807:
				return true;
		}
		return false;
	}

	private boolean isNylocasVasiliasDespawn(NPC npc)
	{
		switch (npc.getId())
		{
			case NpcID.NYLOCAS_VASILIAS_8355:
			case NpcID.NYLOCAS_VASILIAS_8356:
			case NpcID.NYLOCAS_VASILIAS_8357:
			case NpcID.NYLOCAS_VASILIAS_10786:
			case NpcID.NYLOCAS_VASILIAS_10807:
				return true;
		}
		return false;
	}

	private String _ticks2Time(int ticks)
	{
		if (ticks == 0)
		{
			return "";
		}
		int millis = ticks * 600;
		String hundredths = String.valueOf(millis % 1000).substring(0, 1);
		return String.format(
			"%d:%02d.%s",
			TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
			TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
			hundredths
		);
	}
}
