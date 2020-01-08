package btdex.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.bulenkov.darcula.DarculaLaf;

import bt.BT;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.response.Account;
import burst.kit.entity.response.AssetBalance;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.http.BRSError;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import okhttp3.Response;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	public static final int ICON_SIZE = 22;
	
	Image icon;

	CardLayout cardLayout;
	OrderBook orderBook;
	TransactionsPanel transactionsPanel;
	HistoryPanel historyPanel;
	AccountsPanel accountsPanel;

	JTabbedPane tabbedPane;

	JLabel statusLabel;
	JButton nodeButton;
	
	Icon ICON_CONNECTED, ICON_DISCONNECTED;
	
	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;

	private JButton createOfferButton;

	private JComboBox<Market> marketComboBox;

	private JButton sendButton;

	private CopyToClipboardButton copyAddButton;

	private Desc tokenDesc;
	
	private JLabel balanceLabelToken;

	private JLabel balanceLabelTokenPending;

	private JButton sendButtonToken;

	private Market token;
	
	private long lastUpdated;
	
	public Main() {
		super("BTDEX" + (Globals.getInstance().isTestnet() ? "-TESTNET" : ""));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		String version = "dev";
		
		try {
			icon = ImageIO.read(Main.class.getResourceAsStream("/icon.png"));
			setIconImage(icon);
			
			Properties versionProp = new Properties();
			versionProp.load(Main.class.getResourceAsStream("/version.properties"));
			version = versionProp.getProperty("version");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		try {
			DarculaLaf laf = new DarculaLaf();
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		IconFontSwing.register(FontAwesome.getIconFont());
		setBackground(Color.BLACK);
		
		Globals g = Globals.getInstance();
		
		tabbedPane = new JTabbedPane();
		tabbedPane.setOpaque(true);

		JPanel topAll = new JPanel(new BorderLayout());
		JPanel bottomAll = new JPanel(new BorderLayout());

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		cardLayout = new CardLayout();
		getContentPane().setLayout(cardLayout);
		
		JPanel content = new JPanel(new BorderLayout());
		JPanel splash = new JPanel(new BorderLayout());
		splash.add(new JLabel(new ImageIcon(icon)), BorderLayout.CENTER);

		getContentPane().add(content, "content");
		getContentPane().add(splash, "splash");

		topAll.add(top, BorderLayout.CENTER);
		content.add(topAll, BorderLayout.PAGE_START);
		content.add(bottomAll, BorderLayout.PAGE_END);
				
		JPanel bottomRight = new JPanel();
		bottomAll.add(bottomRight, BorderLayout.LINE_END);
		bottomAll.add(bottom, BorderLayout.CENTER);
		
		marketComboBox = new JComboBox<Market>();
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, ICON_SIZE);
		Color COLOR = marketComboBox.getForeground();
		marketComboBox.setToolTipText("Select market");
		marketComboBox.setFont(largeFont);
		
		Icon versionIcon = IconFontSwing.buildIcon(FontAwesome.CODE_FORK, ICON_SIZE, COLOR);
		JButton versionButton = new JButton(version, versionIcon);
		versionButton.setToolTipText("Check for a new release...");
		versionButton.setVerticalAlignment(SwingConstants.CENTER);
		versionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse("https://github.com/btdex/btdex/releases");
			}
		});
		bottomRight.add(versionButton);
		
		for(Market m : g.getMarkets())
			marketComboBox.addItem(m);
		token = g.getToken();

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook(this, (Market) marketComboBox.getSelectedItem());
		
		transactionsPanel = new TransactionsPanel();
		historyPanel = new HistoryPanel(this, (Market) marketComboBox.getSelectedItem());
		accountsPanel = new AccountsPanel(this);
		
		ICON_CONNECTED = IconFontSwing.buildIcon(FontAwesome.WIFI, ICON_SIZE, COLOR);
		ICON_DISCONNECTED = IconFontSwing.buildIcon(FontAwesome.EXCLAMATION, ICON_SIZE, COLOR);

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, ICON_SIZE, COLOR);
		copyAddButton = new CopyToClipboardButton("", copyIcon);
		copyAddButton.setToolTipText("Copy your Burst address to clipboard");
		copyAddButton.setFont(largeFont);

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, ICON_SIZE, COLOR);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);
		settingsButton.setVisible(false);

		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.PAPER_PLANE, ICON_SIZE, COLOR);
		Icon createOfferIcon = IconFontSwing.buildIcon(FontAwesome.CART_PLUS, ICON_SIZE, COLOR);

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

		content.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, ICON_SIZE, COLOR);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, ICON_SIZE, COLOR);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, historyPanel);

//		Icon accountIcon = IconFontSwing.buildIcon(FontAwesome.USER_CIRCLE, ICON_SIZE, COLOR);
//		tabbedPane.addTab("ACCOUNTS", accountIcon, accountsPanel);

		Icon transactionsIcon = IconFontSwing.buildIcon(FontAwesome.LINK, ICON_SIZE, COLOR);
		tabbedPane.addTab("TRANSACTIONS", transactionsIcon, transactionsPanel);
		
		if(icon!=null) {
			JButton iconButton = new JButton(new ImageIcon(icon.getScaledInstance(64, 64, Image.SCALE_SMOOTH)));
			topAll.add(iconButton, BorderLayout.LINE_END);
			
			iconButton.setToolTipText("Opens the BTDEX website");
			iconButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					browse("https://btdex.trade");
				}
			});
		}

		top.add(new Desc("Market", marketComboBox));
		top.add(new Desc("Your Burst address", copyAddButton));
		
		balanceLabel = new JLabel("0");
		balanceLabel.setToolTipText("Available balance");
		balanceLabel.setFont(largeFont);
		lockedBalanceLabel = new JLabel("0");
		lockedBalanceLabel.setToolTipText("Amount locked in orders");
		top.add(new Desc("Balance (BURST)", balanceLabel, lockedBalanceLabel));
		top.add(new Desc("  ", sendButton));
		top.add(new Desc("  ", createOfferButton));

		balanceLabelToken = new JLabel("0");
		balanceLabelToken.setToolTipText("Available balance");
		balanceLabelToken.setFont(largeFont);
		balanceLabelTokenPending = new JLabel("0");
		balanceLabelTokenPending.setToolTipText("Amount locked in orders or rewards pending");
		top.add(tokenDesc = new Desc(String.format("Balance (%s)", token), balanceLabelToken, balanceLabelTokenPending));
		top.add(new Desc("  ", sendButtonToken));
//		top.add(new Desc("  ", createOfferButtonBTDEX));

		
		top.add(new Desc("  ", settingsButton));

		nodeButton = new JButton(g.getNode());
		nodeButton.setToolTipText("Select node...");
		nodeButton.addActionListener(this);
		statusLabel = new JLabel();

		bottom.add(nodeButton);
		bottom.add(statusLabel);

		pack();
		setMinimumSize(new Dimension(1200, 600));
		setLocationRelativeTo(null);
		cardLayout.last(getContentPane());
		setVisible(true);
		
		// The testnet pre-release warning note
		if(g.isTestnet()) {
			JOptionPane.showMessageDialog(Main.this,
					"You selected to run on TESTNET.\n"
					+ "Make sure you are connected to a testnet node!\n", "TESTNET version",
					JOptionPane.INFORMATION_MESSAGE);
		}

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

		// check if this is a known account
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		boolean newAccount = false;
		try {
			g.getNS().getAccount(g.getAddress()).blockingGet();
		}
		catch (Exception e) {
			setCursor(Cursor.getDefaultCursor());
			if(e.getCause() instanceof BRSError) {
				BRSError error = (BRSError) e.getCause();
				if(error.getCode() == 5) {
					newAccount = true;
					// unknown account
					/* TODO: think about this auto-activation thing
					int ret = JOptionPane.showConfirmDialog(Main.this,
							"You have a new account, do you want to activate it\n"
							+ "with a complimentary message?", "Activate account",
							JOptionPane.YES_NO_OPTION);
					if(ret == JOptionPane.YES_OPTION) {
					*/
						// try to activate this account
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						try {
							Response response = g.activate();
							if(response.isSuccessful()) {
								Toast.makeText(this, "Great, in a few minutes your new account will be activated.", Toast.Style.SUCCESS).display();
								tabbedPane.setSelectedComponent(transactionsPanel);
							}
							else {
								Toast.makeText(this, 
										"Account activation failed, error code " + response.code() + ": " + response.message(), Toast.Style.ERROR).display();
							}
							response.close();
						}
						catch (Exception e1) {
							e1.printStackTrace();
							Toast.makeText(this, e1.getLocalizedMessage(), Toast.Style.ERROR).display();
						}
					//}
				}
			}
		}
		setCursor(Cursor.getDefaultCursor());
		
		statusLabel.setText(g.getNode());

		if(!newAccount)
			Toast.makeText(this, "Getting info from node...", 8000, Toast.Style.SUCCESS).display();
		update();
		Thread updateThread = new UpdateThread();
		updateThread.start();
	}
	
	private void browse(String url) {
		try {
			DesktopApi.browse(new URI(url));
			Toast.makeText(Main.this, "Opening " + url, Toast.Style.SUCCESS).display();
		} catch (Exception ex) {
			Toast.makeText(Main.this, ex.getMessage(), Toast.Style.ERROR).display();
		}
	}
	
	/**
	 * Signal an update should take place.
	 */
	public void update() {
		lastUpdated = 0;
	}

	public class UpdateThread extends Thread {
		
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				// Update at every 10 seconds
				if(System.currentTimeMillis() - lastUpdated < 10000) {
					try {
						sleep(100);
						continue;
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}				
				lastUpdated = System.currentTimeMillis();
				
				long balance = 0, locked = 0;
				try {
					Globals g = Globals.getInstance();
					try {
						Account ac = g.getNS().getAccount(g.getAddress()).blockingGet();
						balance = ac.getBalance().longValue();
						
						// All bids also go to the locked
						// TODO: iterate over all markets
						AssetOrder[] bids = g.getNS().getBidOrders(token.getTokenID()).blockingGet();
						for(AssetOrder o : bids) {
							if(o.getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
								continue;
							long price = o.getPrice().longValue();
							long amount = o.getQuantity().longValue();
							
							locked += amount*price;
						}
						
						balance -= locked;
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
					
					g.updateSuggestedFee();
					
					transactionsPanel.update();
					orderBook.update();
					historyPanel.update();
					
					Market tokenMarket = token;
					Market m = (Market) marketComboBox.getSelectedItem();
					if(m.getTokenID()!=null && m!=token)
						tokenMarket = m;
					AssetBalance[] accounts = g.getNS().getAssetBalances(tokenMarket.getTokenID()).blockingGet();
					long tokenBalance = 0;
					long tokenLocked = 0;
					for (AssetBalance aac : accounts) {
						if(aac.getAccountAddress().getSignedLongId() == g.getAddress().getSignedLongId()) {
							tokenBalance += aac.getBalance().longValue();
						}
					}
					
					AssetOrder[] asks = g.getNS().getAskOrders(token.getTokenID()).blockingGet();
					for(AssetOrder o : asks) {
						if(o.getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
							continue;
						tokenLocked += o.getQuantity().longValue();
					}
					tokenBalance -= tokenLocked;
					
					balanceLabelToken.setText(token.format(tokenBalance));
					balanceLabelTokenPending.setText("+ " + token.format(tokenLocked) + " locked");

					statusLabel.setText("");
					nodeButton.setIcon(ICON_CONNECTED);
				}
				catch (RuntimeException rex) {
					rex.printStackTrace();
					
					Toast.makeText(Main.this, rex.getMessage(), Toast.Style.ERROR).display();

					nodeButton.setIcon(ICON_DISCONNECTED);
					statusLabel.setText(rex.getMessage());
				}
				cardLayout.first(getContentPane());
			}
			
			System.err.println("Update thread finished!");
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
			
			if(m.getTokenID()==null) {
				Toast.makeText(this, "Cross-chain markets will be open only "
						+ "after TRT initial distribution is finished.", Toast.Style.ERROR).display();
				return;
			}
			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);
		}
		else if (e.getSource() == marketComboBox) {
			orderBook.setMarket(m);
			historyPanel.setMarket(m);
			
			if(m.getTokenID() == null) {
				// not a token market, show TRT in the token field 
				tokenDesc.setDesc(String.format("Balance (%s)", token));
				
				// FIXME: remove this when operational
				Toast.makeText(this, "Cross-chain markets will be open only "
						+ "after TRT initial distribution is finished.", Toast.Style.ERROR).display();
			}
			else {
				// this is a token market, show it on the token field 
				tokenDesc.setDesc(String.format("Balance (%s)", m));
				balanceLabelToken.setText(m.format(0));
				balanceLabelTokenPending.setText(" ");
				sendButtonToken.setToolTipText(String.format("Send %s...", m.toString()));
			}
			
			update();
		}
		else if (e.getSource() == sendButton) {
			SendDialog dlg = new SendDialog(this, null);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);			
		}
		else if (e.getSource() == sendButtonToken) {
			SendDialog dlg = new SendDialog(this, m.getTokenID()==null ? token : m);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);			
		}
		else if (e.getSource() == nodeButton) {
			
			Globals g = Globals.getInstance();
			
			String[] list = {BT.NODE_BURSTCOIN_RO, BT.NODE_BURST_ALLIANCE,
					BT.NODE_BURST_TEAM, "http://localhost:8125"};
			if(g.isTestnet()){
				list = new String[]{BT.NODE_TESTNET, BT.NODE_TESTNET_MEGASH, BT.NODE_LOCAL_TESTNET };
			}
			
			JComboBox<String> nodeComboBox = new JComboBox<String>(list);
			nodeComboBox.setEditable(true);
			int ret = JOptionPane.showConfirmDialog(this, nodeComboBox, "Select node", JOptionPane.OK_CANCEL_OPTION);
			
			if(ret == JOptionPane.OK_OPTION) {
				g.setNode(nodeComboBox.getSelectedItem().toString());
				try {
					g.saveConfs();
				} catch (Exception ex) {
					ex.printStackTrace();
					Toast.makeText(this, ex.getMessage(), Toast.Style.ERROR).display();
				}
				
				nodeButton.setText(g.getNode());
				update();
			}
		}
	}
}
