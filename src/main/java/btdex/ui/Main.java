package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.bulenkov.darcula.DarculaLaf;

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

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	OrderBook orderBook;

	Config config = new Config();

	BurstAddress address = Globals.BC.getBurstAddressFromPassphrase(config.encryptedPassPhrase);

	JTabbedPane tabbedPane;

	JLabel nodeStatus;

	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;

	private JButton createOfferButton;

	private JComboBox<Market> marketComboBox;

	private JButton sendButton;

	public Main() {
		super("BTDEX" + (Globals.IS_TESTNET ? " - TESTNET" : ""));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		try {
			DarculaLaf laf = new DarculaLaf();
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		IconFontSwing.register(FontAwesome.getIconFont());
		setBackground(Color.BLACK);

		tabbedPane = new JTabbedPane();
		tabbedPane.setOpaque(true);

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

		getContentPane().add(top, BorderLayout.PAGE_START);
		getContentPane().add(bottom, BorderLayout.PAGE_END);

		marketComboBox = new JComboBox<Market>();
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, 18);
		Color COLOR = marketComboBox.getForeground();
		marketComboBox.setToolTipText("Select market");
		marketComboBox.setFont(largeFont);

		marketComboBox.addItem(new MarketBTC());
		marketComboBox.addItem(new MarketETH());
		marketComboBox.addItem(new MarketLTC());

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook((Market) marketComboBox.getSelectedItem());

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 18, COLOR);
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

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, 18, COLOR);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);

		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_UP, 18, COLOR);
		sendButton = new JButton(sendIcon);
		sendButton.addActionListener(this);
		sendButton.setToolTipText("Send BURST...");
		sendButton.setFont(largeFont);

		Icon createOfferIcon = IconFontSwing.buildIcon(FontAwesome.MONEY, 18, COLOR);
		createOfferButton = new JButton(createOfferIcon);
		createOfferButton.setToolTipText("Create a new sell offer...");
		createOfferButton.setFont(largeFont);

		createOfferButton.addActionListener(this);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, 18, COLOR);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);

		Icon ongoinIcon = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 18, COLOR);
		tabbedPane.addTab("ONGOING TRADES", ongoinIcon, new JLabel());

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, 18, COLOR);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		Market m = (Market) marketComboBox.getSelectedItem();
		if(e.getSource() == createOfferButton) {
			
			PlaceSell dlg = new PlaceSell(this, m);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);
		}
		else if (e.getSource() == marketComboBox) {
			orderBook.setMarket(m);
		}
		else if (e.getSource() == sendButton) {
			SendBurst dlg = new SendBurst(this);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);			
		}
	}
}
