package com.nyloer;

import com.nyloer.nylostats.Stall;
import com.nyloer.nylostats.Stats;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;


public class NyloerSidePanel extends PluginPanel
{
	private final Client client;
	private final NyloerPlugin plugin;
	private final NyloerConfig config;

	JButton buttonMageSwaps;
	JButton buttonRangeSwaps;
	JButton buttonMeleeSwaps;
	JButton buttonCustomSwaps;
	Font defaultFont;
	Font selectedButtonFont;
	Font frameTitleFont;

	JTable stallsTable;
	DefaultTableModel stallsTableModel;
	JScrollBar stallsTableScrollBar;

	JTable statsTable;
	DefaultTableModel statsTableModel;
	JScrollBar statsTableScrollBar;

	@Inject
	NyloerSidePanel(Client client, NyloerPlugin plugin, NyloerConfig config)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.frameTitleFont = new Font(NyloerFonts.RUNESCAPE.toString(), Font.PLAIN, 16);
		this.defaultFont = new Font(NyloerFonts.DIALOG.toString(), Font.PLAIN, 12);
		this.selectedButtonFont = new Font(NyloerFonts.DIALOG.toString(), Font.PLAIN, 12);
	}

	public void startPanel()
	{
		getParent().setLayout(new BorderLayout());
		getParent().add(this, BorderLayout.CENTER);
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel layout = new JPanel();
		BoxLayout boxLayout = new BoxLayout(layout, BoxLayout.Y_AXIS);
		layout.setLayout(boxLayout);
		add(layout, BorderLayout.NORTH);

		layout.add(Box.createRigidArea(new Dimension(0, 15)));
		JPanel swapsFrame = createRoleSwapsFrame();
		layout.add(swapsFrame);

		layout.add(Box.createRigidArea(new Dimension(0, 15)));
		JScrollPane stallsPane = createStallsPane();
		layout.add(stallsPane);

		layout.add(Box.createRigidArea(new Dimension(0, 15)));
		JPanel statsPane = createRecentStatsFrame();
		layout.add(statsPane);
	}

	public void addStall(Stall stall)
	{
		String aliveDisplay = stall.getAliveCount() + "/" + stall.getCapSize();
		stallsTableModel.addRow(new Object[]{stall.getWave(), aliveDisplay, stall.getTotalStalls()});
		stallsTableScrollBar.setValue(stallsTableScrollBar.getMaximum() + 100);
	}

	public void addStats(Stats stats)
	{
		statsTableModel.insertRow(
			0,
				new Object[]{
				stats.totalTime,
				stats.bossTime,
				stats.wavesTime,
				stats.stallCountPre,
				stats.stallCount21,
				stats.stallCount22to27,
				stats.stallCount28,
				stats.stallCount29,
				stats.stallCount30
			}
			);
	}

	public void resetStallsTable()
	{
		if (stallsTableModel.getRowCount() > 0)
		{
			for (int i = stallsTableModel.getRowCount() - 1; i > -1; i--)
			{
				stallsTableModel.removeRow(i);
			}
		}
	}

	private JPanel createRoleSwapsFrame()
	{
		JPanel swapsFrame = new JPanel();
		swapsFrame.setLayout(new GridLayout(2, 2));
		TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Role Swaps");
		border.setTitleFont(frameTitleFont);
		swapsFrame.setBorder(border);

		buttonMageSwaps = new JButton("Mage");
		buttonRangeSwaps = new JButton("Range");
		buttonMeleeSwaps = new JButton("Melee");
		buttonCustomSwaps = new JButton("Custom");
		buttonMageSwaps.setPreferredSize(new Dimension(40, 40));
		buttonRangeSwaps.setPreferredSize(new Dimension(40, 40));
		buttonMeleeSwaps.setPreferredSize(new Dimension(40, 40));
		buttonCustomSwaps.setPreferredSize(new Dimension(40, 40));
		buttonMageSwaps.setFocusable(false);
		buttonRangeSwaps.setFocusable(false);
		buttonMeleeSwaps.setFocusable(false);
		buttonCustomSwaps.setFocusable(false);
		buttonMageSwaps.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonRangeSwaps.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonMeleeSwaps.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonCustomSwaps.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonMageSwaps.addActionListener(e -> _configureMageSwaps());
		buttonRangeSwaps.addActionListener(e -> _configureRangeSwaps());
		buttonMeleeSwaps.addActionListener(e -> _configureMeleeSwaps());
		buttonCustomSwaps.addActionListener(e -> _configureCustomSwaps());
		swapsFrame.add(buttonMageSwaps);
		swapsFrame.add(buttonMeleeSwaps);
		swapsFrame.add(buttonRangeSwaps);
		swapsFrame.add(buttonCustomSwaps);
		_resetRolesSelection();

		return swapsFrame;
	}

	private JScrollPane createStallsPane()
	{
		stallsTable = new JTable();

		String[] columnNames = {"Wave", "Alive", "Total"};
		stallsTableModel = new DefaultTableModel(columnNames, 0);
		stallsTable.setModel(stallsTableModel);
		stallsTable.getTableHeader().setReorderingAllowed(false);
		stallsTable.setDefaultEditor(Object.class, null);
		stallsTable.setPreferredScrollableViewportSize(new Dimension(0, 325));
		stallsTable.setRowHeight(25);
		stallsTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stallsTable.setFont(new Font(NyloerFonts.DIALOG.toString(), Font.PLAIN, 14));
		stallsTable.getTableHeader().setFont(new Font(NyloerFonts.DIALOG.toString(), Font.PLAIN, 12));
		stallsTable.setRowSelectionAllowed(false);
		stallsTable.setCellSelectionEnabled(false);
		stallsTable.setShowGrid(false);
		stallsTable.setFillsViewportHeight(true);

		DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
		cellRenderer.setVerticalAlignment(JLabel.CENTER);
		cellRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int columnIndex = 0; columnIndex < stallsTableModel.getColumnCount(); columnIndex++)
		{
			stallsTable.getColumnModel().getColumn(columnIndex).setCellRenderer(cellRenderer);
		}

		JScrollPane scrollPane = new JScrollPane(
			stallsTable,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
		TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Stalls");
		border.setTitleFont(frameTitleFont);
		scrollPane.setBorder(border);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		stallsTableScrollBar = scrollPane.getVerticalScrollBar();

		return scrollPane;
	}

	private JPanel createRecentStatsFrame()
	{
		JPanel statsFrame = new JPanel();
		BoxLayout boxLayout = new BoxLayout(statsFrame, BoxLayout.Y_AXIS);
		statsFrame.setLayout(boxLayout);
		TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Recent Times");
		border.setTitleFont(frameTitleFont);
		statsFrame.setBorder(border);

		statsTable = new JTable();
		String[] columnNames = {"Room", "Boss", "Waves", "Pre", "21", "22-27", "28", "29", "30"};
		statsTableModel = new DefaultTableModel(columnNames, 0);
		statsTable.setModel(statsTableModel);
		statsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
		statsTable.getColumnModel().getColumn(1).setPreferredWidth(60);
		statsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
		statsTable.getColumnModel().getColumn(3).setPreferredWidth(30);
		statsTable.getColumnModel().getColumn(4).setPreferredWidth(30);
		statsTable.getColumnModel().getColumn(5).setPreferredWidth(40);
		statsTable.getColumnModel().getColumn(6).setPreferredWidth(30);
		statsTable.getColumnModel().getColumn(7).setPreferredWidth(30);
		statsTable.getColumnModel().getColumn(8).setPreferredWidth(30);

		statsTable.getTableHeader().setReorderingAllowed(false);
		statsTable.setDefaultEditor(Object.class, null);
		statsTable.setPreferredScrollableViewportSize(new Dimension(0, 325));
		statsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		statsTable.setRowHeight(25);
		statsTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statsTable.setFont(new Font(NyloerFonts.ARIAL.toString(), Font.PLAIN, 14));
		statsTable.getTableHeader().setFont(new Font(NyloerFonts.DIALOG.toString(), Font.PLAIN, 12));
		statsTable.setRowSelectionAllowed(false);
		statsTable.setCellSelectionEnabled(false);
		statsTable.setShowGrid(false);
		statsTable.setFillsViewportHeight(true);

		DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
		cellRenderer.setVerticalAlignment(JLabel.CENTER);
		cellRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int columnIndex = 0; columnIndex < statsTableModel.getColumnCount(); columnIndex++)
		{
			statsTable.getColumnModel().getColumn(columnIndex).setCellRenderer(cellRenderer);
		}

		JScrollPane scrollPane = new JScrollPane(
			statsTable,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
		);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statsTableScrollBar = scrollPane.getVerticalScrollBar();
		statsFrame.add(scrollPane);

		JPanel statsButtonFrame = new JPanel();
		statsButtonFrame.setLayout(new GridLayout(1, 2));
		JButton buttonCopyTable = new JButton("Copy");
		JButton buttonClearTable = new JButton("Clear");
		buttonCopyTable.setFocusable(false);
		buttonClearTable.setFocusable(false);
		buttonCopyTable.setPreferredSize(new Dimension(40, 25));
		buttonClearTable.setPreferredSize(new Dimension(40, 25));
		buttonCopyTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonClearTable.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buttonCopyTable.addActionListener(e -> _copyToClipboard(statsTable));
		buttonClearTable.addActionListener(e -> _resetStatsTable());
		statsButtonFrame.add(buttonCopyTable);
		statsButtonFrame.add(buttonClearTable);
		statsFrame.add(Box.createRigidArea(new Dimension(0, 5)));
		statsFrame.add(statsButtonFrame);

		return statsFrame;
	}

	private void _copyToClipboard(JTable table)
	{
		StringBuffer sbf = new StringBuffer();
		table.selectAll();
		int numCols = table.getSelectedColumnCount();
		int numRows = table.getSelectedRowCount();
		int[] selectedRows = table.getSelectedRows();
		int[] selectsColumns = table.getSelectedColumns();
		table.clearSelection();
		for (int i = 0; i < numCols; i++)
		{
			sbf.append(statsTable.getModel().getColumnName(i));
			sbf.append("\t");
		}
		sbf.append("\n");
		for (int i = 0; i < numRows; i++)
		{
			for (int j = 0; j < numCols; j++)
			{
				sbf.append(table.getValueAt(selectedRows[i], selectsColumns[j]));
				if ( j < numCols - 1)
				{
					sbf.append("\t");
				}
			}
			sbf.append("\n");
		}
		StringSelection data  = new StringSelection(sbf.toString());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);
	}

	private void _resetStatsTable()
	{
		if (statsTableModel.getRowCount() > 0)
		{
			for (int i = statsTableModel.getRowCount() - 1; i > -1; i--)
			{
				statsTableModel.removeRow(i);
			}
		}
	}

	private void _configureMageSwaps()
	{
		NyloerPlugin.log.info(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Role Swaps").getTitleFont().toString());
		NyloerPlugin.log.debug("Configuring mage swaps.");
		String currentRole = plugin.roleSwapper.getCurrentRole();
		_resetRolesSelection();
		if ((currentRole == null) || (!currentRole.equals("mage")))
		{
			plugin.roleSwapper.setCurrentRole("mage");
			buttonMageSwaps.setForeground(Color.CYAN);
			buttonMageSwaps.setFont(selectedButtonFont);
		}
	}

	private void _configureRangeSwaps()
	{
		NyloerPlugin.log.debug("Configuring range swaps.");
		String currentRole = plugin.roleSwapper.getCurrentRole();
		_resetRolesSelection();
		if ((currentRole == null) || (!currentRole.equals("range")))
		{
			plugin.roleSwapper.setCurrentRole("range");
			buttonRangeSwaps.setForeground(Color.GREEN);
			buttonRangeSwaps.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			buttonRangeSwaps.setFont(selectedButtonFont);
		}
	}

	private void _configureMeleeSwaps()
	{
		NyloerPlugin.log.debug("Configuring melee swaps.");
		String currentRole = plugin.roleSwapper.getCurrentRole();
		_resetRolesSelection();
		if ((currentRole == null) || (!currentRole.equals("melee")))
		{
			plugin.roleSwapper.setCurrentRole("melee");
			buttonMeleeSwaps.setForeground(Color.WHITE);
			buttonMeleeSwaps.setFont(selectedButtonFont);
		}
	}

	private void _configureCustomSwaps()
	{
		NyloerPlugin.log.debug("Configured custom role swaps.");
		String currentRole = plugin.roleSwapper.getCurrentRole();
		_resetRolesSelection();
		if ((currentRole == null) || (!currentRole.equals("custom")))
		{
			plugin.roleSwapper.setCurrentRole("custom");
			buttonCustomSwaps.setForeground(Color.MAGENTA);
			buttonCustomSwaps.setFont(selectedButtonFont);
		}
	}

	private void _resetRolesSelection()
	{
		plugin.roleSwapper.setCurrentRole(null);

		buttonMageSwaps.setForeground(Color.GRAY);
		buttonRangeSwaps.setForeground(Color.GRAY);
		buttonMeleeSwaps.setForeground(Color.GRAY);
		buttonCustomSwaps.setForeground(Color.GRAY);

		buttonMageSwaps.setFont(defaultFont);
		buttonRangeSwaps.setFont(defaultFont);
		buttonMeleeSwaps.setFont(defaultFont);
		buttonCustomSwaps.setFont(defaultFont);
	}
}