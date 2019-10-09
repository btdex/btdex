package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.bulenkov.darcula.DarculaLaf;

import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.markets.MarketBTC;
import btdex.markets.MarketBTDEX;
import btdex.markets.MarketETH;
import btdex.markets.MarketLTC;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.response.Account;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	OrderBook orderBook;

	BurstAddress address;

	JTabbedPane tabbedPane;

	JLabel nodeStatus;

	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;

	private JButton createOfferButton;

	private JComboBox<Market> marketComboBox;

	private JButton sendButton;

	private JButton copyAddButton;

	private JLabel balanceLabelToken;

	private JLabel lockedBalanceLabelToken;

	private JButton sendButtonToken;

	public Main() {
		super("BlockTalk DEX");
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
		marketComboBox.addItem(new MarketBTDEX());

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook((Market) marketComboBox.getSelectedItem());

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 18, COLOR);
		copyAddButton = new JButton(copyIcon);
		copyAddButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection stringSelection = new StringSelection(address.getFullAddress());
				clipboard.setContents(stringSelection, null);
				
				Toast.makeText(Main.this, "Address copied to clipboard.").display();
			}
		});
		copyAddButton.setToolTipText("Copy your Burst address to clipboard");
		copyAddButton.setFont(largeFont);

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, 18, COLOR);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);

		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.ARROW_UP, 18, COLOR);
		Icon createOfferIcon = IconFontSwing.buildIcon(FontAwesome.MONEY, 18, COLOR);

		sendButton = new JButton(sendIcon);
		sendButton.setToolTipText("Send BURST...");
		sendButton.addActionListener(this);

		createOfferButton = new JButton(createOfferIcon);
		createOfferButton.setToolTipText("Create a new BURST sell offer...");
		createOfferButton.addActionListener(this);

		sendButtonToken = new JButton(sendIcon);
		sendButtonToken.setToolTipText("Send BTDEX...");
		sendButtonToken.addActionListener(this);

//		createOfferButtonBTDEX = new JButton(createOfferIcon);
//		createOfferButtonBTDEX.setToolTipText("Create a new BTDEX sell offer...");
//		createOfferButtonBTDEX.addActionListener(this);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, 18, COLOR);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);

		Icon ongoinIcon = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 18, COLOR);
		tabbedPane.addTab("ONGOING TRADES", ongoinIcon, new JLabel());

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, 18, COLOR);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, new JLabel());

		top.add(new Desc("Market", marketComboBox));
		top.add(new Desc("Your Burst address", copyAddButton));

		balanceLabel = new JLabel("0");
		balanceLabel.setToolTipText("Available balance");
		balanceLabel.setFont(largeFont);
		lockedBalanceLabel = new JLabel("0");
		lockedBalanceLabel.setToolTipText("Amount locked in trades");
		top.add(new Desc("Balance (BURST)", balanceLabel, lockedBalanceLabel));
		top.add(new Desc("  ", sendButton));
		top.add(new Desc("  ", createOfferButton));

		balanceLabelToken = new JLabel("0");
		balanceLabelToken.setToolTipText("Available balance");
		balanceLabelToken.setFont(largeFont);
		lockedBalanceLabelToken = new JLabel("0");
		lockedBalanceLabelToken.setToolTipText("Amount locked in trades");
		top.add(new Desc("Balance (BTDEX)", balanceLabelToken, lockedBalanceLabelToken));
		top.add(new Desc("  ", sendButtonToken));
//		top.add(new Desc("  ", createOfferButtonBTDEX));

		
		top.add(new Desc("  ", settingsButton));

		nodeStatus = new JLabel();

		bottom.add(nodeStatus);

		pack();
		setMinimumSize(new Dimension(1024, 600));
		setLocationRelativeTo(null);
		setVisible(true);

		Properties conf = Globals.getConf();
		
		String publicKeyStr = conf.getProperty(Globals.PROP_PUBKEY);
		if(publicKeyStr == null || publicKeyStr.length()!=64) {
			// no public key or invalid, show the welcome screen
			
		}

		// get the updated public key and continue
		publicKeyStr = conf.getProperty(Globals.PROP_PUBKEY);
		byte []publicKey = Globals.BC.parseHexString(publicKeyStr);
		address = Globals.BC.getBurstAddressFromPublic(publicKey);
		copyAddButton.setText(address.getRawAddress());
		
		Thread updateThread = new UpdateThread();
		updateThread.start();
	}

	public class UpdateThread extends Thread {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Account ac = Globals.getInstance().getNS().getAccount(address).blockingGet();
					
					balanceLabel.setText(ContractState.format(ac.getBalance().longValue()));
					lockedBalanceLabel.setText("+" + ContractState.format(0) + " locked");

					balanceLabelToken.setText(ContractState.format(0));
					lockedBalanceLabelToken.setText("+" + ContractState.format(0) + " locked");

					nodeStatus.setText("Node: " + Globals.getConf().getProperty(Globals.PROP_NODE));

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
		StringBuilder sb = new StringBuilder();
		byte[] entropy = new byte[Words.TWELVE.byteLength()];
		new SecureRandom().nextBytes(entropy);
		new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);
		System.out.println(sb.toString());
		
		new Main();
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
