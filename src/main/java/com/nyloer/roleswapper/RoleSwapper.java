package com.nyloer.roleswapper;

import com.nyloer.NyloerConfig;
import com.nyloer.NyloerPlugin;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import static net.runelite.api.MenuAction.WIDGET_TARGET_ON_NPC;
import static net.runelite.api.MenuAction.WIDGET_TARGET_ON_PLAYER;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.widgets.Widget;
import static net.runelite.api.widgets.WidgetID.SPELLBOOK_GROUP_ID;
import static net.runelite.api.MenuAction.GAME_OBJECT_FIFTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_EIGHTH_OPTION;
import static net.runelite.api.MenuAction.PLAYER_FIRST_OPTION;
import net.runelite.api.MenuEntry;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;


public class RoleSwapper
{
	Client client;
	NyloerPlugin plugin;
	NyloerConfig config;

	@Getter
	@Setter
	String currentRole;

	final List<CustomSwap> mageSwaps = new ArrayList<>();
	final List<CustomSwap> rangeSwaps = new ArrayList<>();
	final List<CustomSwap> meleeSwaps = new ArrayList<>();
	final List<CustomSwap> customSwaps = new ArrayList<>();

	@Inject
	protected RoleSwapper(NyloerPlugin plugin, NyloerConfig config)
	{
		this.client = plugin.client;
		this.plugin = plugin;
		this.config = config;
		this.currentRole = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(NyloerConfig.GROUP))
		{
			return;
		}
		reloadSwaps();
	}

	public void reloadSwaps()
	{
		NyloerPlugin.log.info("Reloading role swaps...");
		clearSwaps();
		mageSwaps.addAll(loadConfigSwaps(config.mageRoleSwaps()));
		rangeSwaps.addAll(loadConfigSwaps(config.rangeRoleSwaps()));
		meleeSwaps.addAll(loadConfigSwaps(config.meleeRoleSwaps()));
		customSwaps.addAll(loadConfigSwaps(config.customRoleSwaps()));
	}

	private void clearSwaps()
	{
		mageSwaps.clear();
		rangeSwaps.clear();
		meleeSwaps.clear();
		customSwaps.clear();
	}

	private Collection<? extends CustomSwap> loadConfigSwaps(String customSwaps)
	{
		List<CustomSwap> swaps = new ArrayList<>();
		for (String customSwap : customSwaps.split("\n"))
		{
			if (customSwap.trim().equals(""))
			{
				continue;
			}
			NyloerPlugin.log.info(customSwap);
			swaps.add(CustomSwap.fromString(customSwap));
		}
		swaps.add(CustomSwap.fromString("attack,verzik vitur*"));
		swaps.add(CustomSwap.fromString("use,special attack"));
		swaps.add(CustomSwap.fromString("Activate, Quick-Prayers"));
		swaps.add(CustomSwap.fromString("Deactivate, Quick-Prayers"));

		return swaps;
	}

	@Subscribe(priority = -2)
	public void onPostMenuSort(PostMenuSort e)
	{
		swapEntries();
	}

	public void swapEntries()
	{
		if (isShiftPressed())
		{
			return;
		}
		if (currentRole == null)
		{
			return;
		}
		MenuEntry[] menuEntries = client.getMenuEntries();
		if (menuEntries.length == 0)
		{
			return;
		}
		int topEntryIndex = menuEntries.length - 1;
		MenuEntry topEntry = menuEntries[topEntryIndex];
		if (_mayNotBeLeftClick(topEntry))
		{
			return;
		}
		int entryIndex = getEntryIndexToSwap(menuEntries);
		if (entryIndex >= 0)
		{
			MenuEntry entryToSwap = menuEntries[entryIndex];
			if (_isProtected(topEntry) || _isProtected(entryToSwap))
			{
				return;
			}

			// minimap orbs
			if (topEntryIndex > entryIndex)
			{
				menuEntries[topEntryIndex] = entryToSwap;
				menuEntries[entryIndex] = topEntry;
				// the client will open the right-click menu on left-click if the entry at the top is a CC_OP_LOW_PRIORITY.
				if (entryToSwap.getType() == MenuAction.CC_OP_LOW_PRIORITY)
				{
					entryToSwap.setType(MenuAction.CC_OP);
				}
			}
		}
		client.setMenuEntries(menuEntries);
	}

	private List<CustomSwap> getCurrentSwaps()
	{
		if (currentRole.equals("mage"))
		{
			return mageSwaps;
		}
		if (currentRole.equals("range"))
		{
			return rangeSwaps;
		}
		if (currentRole.equals("melee"))
		{
			return meleeSwaps;
		}
		if (currentRole.equals("custom"))
		{
			return customSwaps;
		}
		return null;
	}

	private int getEntryIndexToSwap(MenuEntry[] menuEntries)
	{
		List<CustomSwap> currentSwaps = getCurrentSwaps();
		if (currentSwaps == null)
		{
			return -1;
		}
		int entryIndex = -1;
		int latestMatchingSwapIndex = -1;
		MenuEntry topEntry = menuEntries[menuEntries.length - 1];
		String topEntryOption = Text.standardize(topEntry.getOption());
		String topEntryTarget = Text.standardize(topEntry.getTarget());

		// prefer to swap menu entries that are already at or near the top of the list.
		for (int i = menuEntries.length - 1; i >= 0; i--)
		{
			MenuEntry entry = menuEntries[i];
			String option = Text.standardize(entry.getOption());
			String target = Text.standardize(entry.getTarget());
			int swapIndex = matches(option, target, topEntryOption, topEntryTarget, currentSwaps);
			if (swapIndex > latestMatchingSwapIndex)
			{
				entryIndex = i;
				latestMatchingSwapIndex = swapIndex;
			}
		}
		return entryIndex;
	}

	private boolean _mayNotBeLeftClick(MenuEntry entry)
	{
		MenuAction type = entry.getType();
		if (type.getId() >= PLAYER_FIRST_OPTION.getId() && type.getId() <= PLAYER_EIGHTH_OPTION.getId())
		{
			return true;
		}
		if (type == MenuAction.NPC_FOURTH_OPTION || type == MenuAction.NPC_FIFTH_OPTION)
		{
			if (entry.getNpc() != null)
			{
				String[] actions = entry.getNpc().getTransformedComposition().getActions();
				if ("Lure".equals(actions[3]) || "Knock-out".equals(actions[4]))
				{
					return true;
				}
			}
		}
		if (type == GAME_OBJECT_FIFTH_OPTION)
		{
			return client.getVarbitValue(2176) == 1; // in building mode.
		}
		return false;
	}

	private boolean isShiftPressed()
	{
		return client.isKeyPressed(KeyCode.KC_SHIFT);
	}

	private boolean _isProtected(MenuEntry entry)
	{
		MenuAction type = entry.getType();
		if (type == WIDGET_TARGET_ON_PLAYER || type == WIDGET_TARGET_ON_NPC)
		{
			Widget selectedWidget = client.getSelectedWidget();
			if ((selectedWidget != null) && (selectedWidget.getId() == SPELLBOOK_GROUP_ID))
			{
				return true;
			}
		}
		return _mayNotBeLeftClick(entry);
	}

	private static int matches(String entryOption, String entryTarget, String topEntryOption, String topEntryTarget, List<CustomSwap> swaps)
	{
		for (int i = 0; i < swaps.size(); i++)
		{
			CustomSwap swap = swaps.get(i);
			if (swap.matches(entryOption, entryTarget, topEntryOption, topEntryTarget))
			{
				return i;
			}
		}
		return -1;
	}

	@Getter
	@ToString
	@RequiredArgsConstructor
	@EqualsAndHashCode
	static class CustomSwap
	{
		private final String option;
		private final String target;
		private final String topOption;
		private final String topTarget;

		private final MatchType optionType;
		private final MatchType targetType;
		private final MatchType topOptionType;
		private final MatchType topTargetType;

		public static CustomSwap fromString(String s)
		{
			String[] split = s.split(",");
			return new CustomSwap(
				split[0].toLowerCase().trim(),
				split.length > 1 ? split[1].toLowerCase().trim() : "",
				split.length > 2 ? split[2].toLowerCase().trim() : null,
				split.length > 3 ? split[3].toLowerCase().trim() : null
			);
		}

		CustomSwap(String option, String target)
		{
			this(option, target, null, null);
		}

		CustomSwap(String option, String target, String topOption, String topTarget)
		{
			this.optionType = MatchType.getType(option);
			this.option = MatchType.prepareMatch(option, optionType);
			this.targetType = MatchType.getType(target);
			this.target = MatchType.prepareMatch(target, targetType);
			this.topOptionType = MatchType.getType(topOption);
			this.topOption = MatchType.prepareMatch(topOption, topOptionType);
			this.topTargetType = MatchType.getType(topTarget);
			this.topTarget = MatchType.prepareMatch(topTarget, topTargetType);
		}

		boolean matches(String option, String target)
		{
			return matches(option, target, "", "");
		}

		public boolean matches(String option, String target, String topOption, String topTarget)
		{
			return optionType.matches(option, this.option) &&
				targetType.matches(target, this.target) &&
				topOptionType.matches(topOption, this.topOption) &&
				topTargetType.matches(topTarget, this.topTarget);
		}
	}
}
