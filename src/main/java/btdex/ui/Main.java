package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import btdex.core.Config;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.markets.MarketBTC;
import btdex.markets.MarketETH;
import btdex.markets.MarketLTC;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Account;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class Main extends JFrame {

	private static final long serialVersionUID = 1L;

	OrderBook orderBook;

	Config config = new Config();

	BurstAddress address = Globals.BC.getBurstAddressFromPassphrase(config.encryptedPassPhrase);

	JTabbedPane tabbedPane = new JTabbedPane();

	JLabel nodeStatus;

	class Desc extends JPanel {
		private static final long serialVersionUID = 1L;

		public Desc(String desc, Component child) {
			super(new BorderLayout());

			add(child, BorderLayout.CENTER);
			add(new JLabel(desc), BorderLayout.PAGE_START);
		}
	}

	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;

	public Main() {
		super("BTDEX");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//		try {
//			Class<?> lafc = null;
//			try {
//				lafc = Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
//			} catch (Exception e) {
//				lafc = Class.forName("javax.swing.plaf.nimbus.NimbusLookAndFeel");
//			}
//			LookAndFeel laf = (LookAndFeel) lafc.getConstructor().newInstance();
//			UIManager.setLookAndFeel(laf);
//		} catch (Exception e) {
//		}
		System.setProperty("awt.useSystemAAFontSettings","on");
		System.setProperty("swing.aatext", "true");

		IconFontSwing.register(FontAwesome.getIconFont());

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

		getContentPane().add(top, BorderLayout.PAGE_START);
		getContentPane().add(bottom, BorderLayout.PAGE_END);

		JComboBox<Market> marketComboBox = new JComboBox<>();
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, 18);
		marketComboBox.setFont(largeFont);

		marketComboBox.addItem(new MarketBTC());
		marketComboBox.addItem(new MarketETH());
		marketComboBox.addItem(new MarketLTC());
		
		marketComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Market m = (Market) marketComboBox.getSelectedItem();
				orderBook.setMarket(m);
			}
		});
		orderBook = new OrderBook((Market) marketComboBox.getSelectedItem());

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 18);
		JButton copyAddButton = new JButton(address.getRawAddress(), copyIcon);
		copyAddButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection stringSelection = new StringSelection(address.getFullAddress());
				clipboard.setContents(stringSelection, null);
			}
		});
		copyAddButton.setToolTipText("Copy your Burst address to clipboard");
		copyAddButton.setFont(largeFont);

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, 18);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);

		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.SHARE, 18);
		JButton sendButton = new JButton(sendIcon);
		sendButton.setToolTipText("Send BURST...");
		sendButton.setFont(largeFont);

		Icon createOfferIcon = IconFontSwing.buildIcon(FontAwesome.MONEY, 18);
		JButton createOfferButton = new JButton(createOfferIcon);
		createOfferButton.setToolTipText("Create a new sell offer...");
		createOfferButton.setFont(largeFont);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, 18);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);

		Icon ongoinIcon = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 18);
		tabbedPane.addTab("ONGOING TRADES", ongoinIcon, new JLabel());

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, 18);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, new JLabel());

		balanceLabel = new JLabel();
		balanceLabel.setFont(largeFont);

		lockedBalanceLabel = new JLabel();
		lockedBalanceLabel.setFont(largeFont);

		//		JLabel marketLabel = new JLabel("Market: ");
		//		marketLabel.setFont(largeFont);
		//		top.add(marketLabel);
		top.add(new Desc("Market", marketComboBox));

		top.add(new Desc("Your Burst address", copyAddButton));
		top.add(new Desc("Available balance (BURST)", balanceLabel));
		top.add(new Desc("  ", sendButton));
		top.add(new Desc("  ", createOfferButton));
		top.add(new Desc("Locked in trades (BURST)", lockedBalanceLabel));
		top.add(new Desc("  ", settingsButton));

		nodeStatus = new JLabel();

		bottom.add(nodeStatus);

		pack();
		setMinimumSize(new Dimension(900, 600));
		setLocationRelativeTo(null);
		setVisible(true);
		
		Thread updateThread = new UpdateThread();
		updateThread.start();
	}

	public class UpdateThread extends Thread {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Account ac = Globals.NS.getAccount(address).blockingGet();
					balanceLabel.setText(ContractState.format(ac.getBalance().longValue()));
					lockedBalanceLabel.setText(ContractState.format(0));

					nodeStatus.setText("Node: " + config.node);

					orderBook.update();

					sleep(10000);
				}
				catch (RuntimeException rex) {
					nodeStatus.setText(rex.getMessage());
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}
	};

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Main();
			}
		});
	}
}
