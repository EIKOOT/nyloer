/*BSD 2-Clause License

Copyright (c) 2022, JaccodR
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

---
Most of the code was modified.
*/

package com.nyloer.nylostats;
import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.ArrayUtils;
import java.util.*;
import java.util.regex.Pattern;


public class NyloStats
{
	private Client client;
	private NyloerPlugin plugin;
	private NyloerConfig config;

	private final ArrayList<Stall> stallArray;
	private int w1T;
	private int lastNyloDeathT;
	private int bossSpawnT;
	private int bossDeathT;
	private Stats stats;

	private int currWave;
	private int stalls;
	private int ticksSinceLastWave;
	private int[] splits;
	private int[] preCapSplits;
	private int[] bossRotation;
	private int currCap;
	private ArrayList<String> stallMessages;
	private static final Pattern NYLO_COMPLETE = Pattern.compile("Wave 'The Nylocas' \\(.*\\) complete!");
//	private final int NYLOCAS_REGION_ID = 13122;

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
		stallMessages = new ArrayList<>();

		reset();
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
			if (currWave > 19)
			{
				currCap = 24;
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
		if (isNylocasVasilias(npc))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Boss spawned", "");
			bossSpawnT = client.getTickCount();
			printTicks("onNpcSpawned() BossSpawned");
		}
		if (isNylocas(npc))
		{
			if (isSplit(npc))
			{
				switch (npc.getId())
				{
					case NpcID.NYLOCAS_ISCHYROS_8342:
					case NpcID.NYLOCAS_ISCHYROS_10774:
					case NpcID.NYLOCAS_ISCHYROS_10791:
						splits[0]++;
					case NpcID.NYLOCAS_TOXOBOLOS_8343:
					case NpcID.NYLOCAS_TOXOBOLOS_10775:
					case NpcID.NYLOCAS_TOXOBOLOS_10792:
						splits[1]++;
					case NpcID.NYLOCAS_HAGIOS:
					case NpcID.NYLOCAS_HAGIOS_10776:
					case NpcID.NYLOCAS_HAGIOS_10793:
						splits[2]++;
				}
//				if (npc.getId() == NpcID.NYLOCAS_ISCHYROS_10774 || npc.getId() == NpcID.NYLOCAS_TOXOBOLOS_10775 || npc.getId() == NpcID.NYLOCAS_HAGIOS_10776)
//				{
//					isEntry = true;
//				}
			}
			else
			{
				waveSpawned();
			}
		}
	}

	public void waveSpawned()
	{
		if (ticksSinceLastWave <= 3)
		{
			return;
		}
		if (currWave > 1 && (ticksSinceLastWave - waveNaturalStalls.get(currWave)) > 0)
		{
			stalls += (ticksSinceLastWave - waveNaturalStalls.get(currWave)) / 4;
		}
		currWave++;
		if (currWave == 1)
		{
			w1T = client.getTickCount();
			printTicks("w1 spawned");
		}
		else if (currWave == 20)
		{
			preCapSplits = splits.clone();
		}
		else if (currWave == 22)
		{
			stats.bigsAlive22 = getBigsAliveCount();
		}
		else if (currWave == 28)
		{
			stats.bigsAlive22 = getBigsAliveCount();
		}
		else if (currWave == 29)
		{
			stats.bigsAlive29 = getBigsAliveCount();
		}
		else if (currWave == 30)
		{
			stats.bigsAlive30 = getBigsAliveCount();
		}
		else if (currWave == 31)
		{
			stats.bigsAlive31 = getBigsAliveCount();
		}
		ticksSinceLastWave = 0;
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
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Boss spawned", "");
			bossDeathT = client.getTickCount();
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
		if (NYLO_COMPLETE.matcher(msg).find())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Boss rotation: [<col=EF1020>" + bossRotation[0] +
				"</col>] [<col=00FF0A>" + bossRotation[2] + "</col>] [<col=2536CA>" + bossRotation[1] + "</col>]", "");
			saveStats();
			reset();
		}
		if (event.getMessage().equals("You have failed. The vampyres take pity on you and allow you to try again..."))
		{
			saveStats();
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
		if (!inTob && plugin.isNylocasRegionLast())
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
			case NpcID.NYLOCAS_VASILIAS_8355:
			case NpcID.NYLOCAS_VASILIAS_10787:
			case NpcID.NYLOCAS_VASILIAS_10808:
				bossRotation[0]++;
				break;
			case NpcID.NYLOCAS_VASILIAS_8356:
			case NpcID.NYLOCAS_VASILIAS_10788:
			case NpcID.NYLOCAS_VASILIAS_10809:
				bossRotation[1]++;
				break;
			case NpcID.NYLOCAS_VASILIAS_8357:
			case NpcID.NYLOCAS_VASILIAS_10789:
			case NpcID.NYLOCAS_VASILIAS_10810:
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

	private void printTicks(String src)
	{
		String msg = "src: " + src;
		msg += ", w1t: " + w1T;
		msg += ", lastNyloDeathT: " + lastNyloDeathT;
		msg += ", bossSpawnT" + bossSpawnT;
		msg += ", bossDeathT" + bossDeathT;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, "");
	}

	private void addStall(Stall stall)
	{
		stallArray.add(stall);
		plugin.sidePanel.addStall(stall);

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

		String stallMsg = "Stalled wave: <col=EF1020>" + stall.getWave() + "/31</col>";
		stallMsg += " - Nylos alive: <col=EF1020>" + stall.getAliveCount() + "/" + stall.getCapSize() + "</col>";
		stallMsg += " - Total Stalls: <col=EF1020>" + stall.getTotalStalls() + "</col>";
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", stallMsg, "");
	}

	private void saveStats()
	{
		if (lastNyloDeathT == -1)
		{
			return;
		}
		printTicks("saveStats()");
		int tTotal;
		int tBoss;
		int tBossSpawn;

		if (lastNyloDeathT != -1)
		{
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
				tBossSpawn = 0;
				tBoss = 0;
				tTotal = 0;
			}
		}
		else
		{
			tTotal = 0;
			tBoss = 0;
			tBossSpawn = 0;
		}
		stats.totalTime = ticks2Time(tTotal);
		stats.bossTime = ticks2Time(tBoss);
		stats.wavesTime = ticks2Time(tBossSpawn);
		plugin.sidePanel.addStats(stats);
	}

	private String ticks2Time(int ticks)
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

	private boolean isSplit(NPC npc)
	{
		WorldPoint location = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
		Point point = new Point(location.getRegionX(), location.getRegionY());
		NyloSpawns nyloSpawn = NyloSpawns.getLookup().get(point);
		return nyloSpawn == null;
	}

	private boolean isNylocasVasilias(NPC npc)
	{
		switch (npc.getId())
		{
			case NpcID.NYLOCAS_VASILIAS_10786:
			case NpcID.NYLOCAS_VASILIAS:
			case NpcID.NYLOCAS_VASILIAS_10807:
				return true;
		}
		return false;
	}

	private void reset()
	{
		stats = new Stats(
			"",
			"",
			"",
			0,
			0,
			0,
			0,
			0,
			0,
			-1,
			-1,
			-1,
			-1
		);

		stallArray.clear();
		w1T = -1;
		lastNyloDeathT = -1;
		bossSpawnT = -1;
		bossDeathT = -1;

//		isEntry = false;
		currWave = 0;
		ticksSinceLastWave = 0;
		stalls = 0;
		currCap = 12;
		stallMessages.clear();

		splits = new int[3];
		preCapSplits = new int[3];
		bossRotation = new int[3];
		bossRotation[0] = 1;
	}
}
