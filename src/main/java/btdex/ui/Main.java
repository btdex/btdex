package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.SecureRandom;

import javax.imageio.ImageIO;
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
import btdex.markets.MarketETH;
import btdex.markets.MarketLTC;
import btdex.markets.MarketTRT;
import burst.kit.entity.response.Account;
import burst.kit.entity.response.AssetAccount;
import burst.kit.entity.response.http.BRSError;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	OrderBook orderBook;
	TransactionsPanel transactionsPanel;

	JTabbedPane tabbedPane;

	JLabel nodeStatus;

	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;

	private JButton createOfferButton;

	private JComboBox<Market> marketComboBox;

	private JButton sendButton;

	private CopyToClipboardButton copyAddButton;

	private JLabel balanceLabelToken;

	private JLabel balanceLabelTokenPending;

	private JButton sendButtonToken;

	private MarketTRT token;

	public Main() {
		super("BTDEX");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		try {
			Image image = ImageIO.read(Main.class.getResourceAsStream("icon.png"));
			setIconImage(image);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
		
		marketComboBox.addItem(token = new MarketTRT());
		marketComboBox.addItem(new MarketBTC());
		marketComboBox.addItem(new MarketETH());
		marketComboBox.addItem(new MarketLTC());

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook((Market) marketComboBox.getSelectedItem());
		
		transactionsPanel = new TransactionsPanel();

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 18, COLOR);
		copyAddButton = new CopyToClipboardButton("", copyIcon);
		copyAddButton.setToolTipText("Copy your Burst address to clipboard");
		copyAddButton.setFont(largeFont);

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, 18, COLOR);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);

		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.PAPER_PLANE, 18, COLOR);
		Icon createOfferIcon = IconFontSwing.buildIcon(FontAwesome.USD, 18, COLOR);

		sendButton = new JButton(sendIcon);
		sendButton.setToolTipText("Send BURST...");
		sendButton.addActionListener(this);

		createOfferButton = new JButton(createOfferIcon);
		createOfferButton.setToolTipText("Create a new offer...");
		createOfferButton.addActionListener(this);

		sendButtonToken = new JButton(sendIcon);
		sendButtonToken.setToolTipText(String.format("Send %s...", token.toString()));
		sendButtonToken.addActionListener(this);

//		createOfferButtonBTDEX = new JButton(createOfferIcon);
//		createOfferButtonBTDEX.setToolTipText("Create a new BTDEX sell offer...");
//		createOfferButtonBTDEX.addActionListener(this);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, 18, COLOR);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);

		Icon yourTradesIcon = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 18, COLOR);
		tabbedPane.addTab("YOUR TRADES", yourTradesIcon, new JLabel());

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, 18, COLOR);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, new JLabel());

		Icon transactionsIcon = IconFontSwing.buildIcon(FontAwesome.EXCHANGE, 18, COLOR);
		tabbedPane.addTab("TRANSACTIONS", transactionsIcon, transactionsPanel);

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
		balanceLabelTokenPending = new JLabel("0");
		balanceLabelTokenPending.setToolTipText("Pending from trade rewards");
		top.add(new Desc(String.format("Balance (%s)", token), balanceLabelToken, balanceLabelTokenPending));
		top.add(new Desc("  ", sendButtonToken));
//		top.add(new Desc("  ", createOfferButtonBTDEX));

		
		top.add(new Desc("  ", settingsButton));

		nodeStatus = new JLabel();

		bottom.add(nodeStatus);

		pack();
		setMinimumSize(new Dimension(1024, 600));
		setLocationRelativeTo(null);
		getContentPane().setVisible(false);
		setVisible(true);

		Globals g = Globals.getInstance();
		if(g.getAddress()==null) {			
			// no public key or invalid, show the welcome screen
			Welcome welcome = new Welcome(this);
			
			welcome.setLocationRelativeTo(this);
			welcome.setVisible(true);
			if(welcome.getReturn() == 0) {
				System.exit(0);
				return;
			}
		}
		copyAddButton.setText(g.getAddress().getRawAddress());
		copyAddButton.setClipboard(g.getAddress().getFullAddress());
		getContentPane().setVisible(true);
		
		Thread updateThread = new UpdateThread();
		updateThread.start();
	}

	public class UpdateThread extends Thread {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				long balance = 0, locked = 0;
				try {
					Globals g = Globals.getInstance();
					try {
						Account ac = g.getNS().getAccount(g.getAddress()).blockingGet();
						balance = ac.getBalance().longValue();
					}
					catch (Exception e) {
						if(e.getCause() instanceof BRSError) {
							BRSError error = (BRSError) e.getCause();
							if(error.getCode() != 5) // unknown account
								throw e;
						}
						else
							throw e;
					}
					balanceLabel.setText(ContractState.format(balance));
					lockedBalanceLabel.setText("+" + ContractState.format(locked) + " locked");
					
					AssetAccount[] accounts = g.getNS().getAssetAccounts(token.getTokenID()).blockingGet();
					long tokenBalance = 0;
					for (AssetAccount aac : accounts) {
						if(aac.getAccount().getSignedLongId() == g.getAddress().getSignedLongId()) {
							tokenBalance += aac.getQuantity().longValue();
						}
					}
					balanceLabelToken.setText(token.numberFormat(tokenBalance));
					balanceLabelTokenPending.setText("+" + token.numberFormat(0) + " pending");

					nodeStatus.setText("Node: " + Globals.getConf().getProperty(Globals.PROP_NODE));

					orderBook.update();
					transactionsPanel.update();
				}
				catch (RuntimeException rex) {
					rex.printStackTrace();
					
					nodeStatus.setText(rex.getMessage());
				}

				try {
					sleep(10000);
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
			
			PlaceOrderDialog dlg = new PlaceOrderDialog(this, m, null);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);
		}
		else if (e.getSource() == marketComboBox) {
			orderBook.setMarket(m);
		}
		else if (e.getSource() == sendButton) {
			SendDialog dlg = new SendDialog(this, null);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);			
		}
		else if (e.getSource() == sendButtonToken) {
			SendDialog dlg = new SendDialog(this, token);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);			
		}
	}
}
