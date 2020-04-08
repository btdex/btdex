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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bulenkov.darcula.DarculaLaf;

import bt.BT;
import btdex.core.Constants;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstID;
import burst.kit.entity.response.Account;
import burst.kit.entity.response.AssetBalance;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.Block;
import burst.kit.entity.response.http.BRSError;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.icons.font_awesome.FontAwesomeBrands;
import jiconfont.swing.IconFontSwing;
import okhttp3.Response;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	public static final int ICON_SIZE = 24;
	
	Image icon, iconMono;

	CardLayout cardLayout;
	boolean showingSplash;
	OrderBook orderBook;
	TransactionsPanel transactionsPanel;
	HistoryPanel historyPanel;
	AccountsPanel accountsPanel;

	JTabbedPane tabbedPane;

	JLabel statusLabel;
	JButton nodeSelector, explorerSelector;
	ExplorerWrapper explorer;
	
	Icon ICON_CONNECTED, ICON_DISCONNECTED, ICON_TESTNET;
	
	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;

	private JComboBox<Market> marketComboBox;

	private JButton sendButton;

	private ExplorerButton copyAddButton;

	private Desc tokenDesc;
	
	private JLabel balanceLabelToken;

	private JLabel balanceLabelTokenPending;

	private JButton sendButtonToken;

	private Market token;
	
	private long lastUpdated;

	private PulsingIcon pulsingButton;
	
	private static Main instance;
	
	public static Main getInstance() {
		return instance;
	}
	
	public Main() {
		super("BTDEX" + (Globals.getInstance().isTestnet() ? "-TESTNET" : ""));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		String version = "dev";
		instance = this;
		
		try {
			icon = ImageIO.read(Main.class.getResourceAsStream("/icon.png"));
			setIconImage(icon);
			iconMono = ImageIO.read(Main.class.getResourceAsStream("/icon-mono.png"));
			
			Properties versionProp = new Properties();
			versionProp.load(Main.class.getResourceAsStream("/version.properties"));
			version = versionProp.getProperty("version");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		// TODO try again later this other theme, now bugs with tooltip, long text buttons, etc.
		// LafManager.install(new DarculaTheme()); //Specify the used theme.
		// LafManager.getUserProperties().put(DarkTooltipUI.KEY_STYLE, DarkTooltipUI.VARIANT_PLAIN);
		try {			
			DarculaLaf laf = new DarculaLaf();
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		IconFontSwing.register(FontAwesome.getIconFont());
		IconFontSwing.register(FontAwesomeBrands.getIconFont());
		setBackground(Color.BLACK);
		
		Globals g = Globals.getInstance();
		
		tabbedPane = new JTabbedPane();
		tabbedPane.setOpaque(true);
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				update();
			}
		});

		JPanel topAll = new JPanel(new BorderLayout());
		JPanel bottomAll = new JPanel(new BorderLayout());

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		cardLayout = new CardLayout();
		getContentPane().setLayout(cardLayout);
		
		JPanel content = new JPanel(new BorderLayout());
		JPanel splash = new JPanel(new BorderLayout());
		pulsingButton = new PulsingIcon(new ImageIcon(icon));
		splash.add(pulsingButton, BorderLayout.CENTER);

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
		
		Icon iconBtdex = IconFontSwing.buildIcon(FontAwesome.HEART, ICON_SIZE, COLOR);
		if(iconMono!=null)
			iconBtdex = new ImageIcon(iconMono);
		JButton webButton = new JButton(iconBtdex);
		bottomRight.add(webButton, BorderLayout.LINE_END);
		webButton.setToolTipText("Opens the BTDEX website");
		webButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse("https://btdex.trade");
			}
		});

		Icon discordIcon = IconFontSwing.buildIcon(FontAwesomeBrands.DISCORD, ICON_SIZE, COLOR);
		JButton discordButton = new JButton(discordIcon);
		bottomRight.add(discordButton, BorderLayout.LINE_END);
		discordButton.setToolTipText("Chat on BTDEX discord channel...");
		discordButton.setVerticalAlignment(SwingConstants.CENTER);
		discordButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse("https://discord.gg/VQ6sFAY");
			}
		});
		
		Icon githubIcon = IconFontSwing.buildIcon(FontAwesomeBrands.GITHUB, ICON_SIZE, COLOR);
		JButton githubButton = new JButton(githubIcon);
		bottomRight.add(githubButton, BorderLayout.LINE_END);
		githubButton.setToolTipText("Check the source code...");
		githubButton.setVerticalAlignment(SwingConstants.CENTER);
		githubButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse("https://github.com/btdex/btdex");
			}
		});

		Icon signoutIcon = IconFontSwing.buildIcon(FontAwesome.SIGN_OUT, ICON_SIZE, COLOR);
		JButton signoutButton = new JButton(signoutIcon);
		signoutButton.setToolTipText("Exit and clear user data...");
		signoutButton.setVerticalAlignment(SwingConstants.CENTER);
		signoutButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int ret = JOptionPane.showConfirmDialog(Main.this,
						"Exit and clear all user data?\n"
						+ "ATTENTION: this cannot be undone.", "Exit and clear",
						JOptionPane.YES_NO_OPTION);
				if(ret == JOptionPane.YES_OPTION) {
					try {
						Globals.getInstance().clearConfs();
						System.exit(0);
					} catch (Exception ex) {
						ex.printStackTrace();
						Toast.makeText(Main.this, ex.getMessage(), Toast.Style.ERROR).display();
					}
				}
			}
		});
		
		Icon resetPinIcon = IconFontSwing.buildIcon(FontAwesome.LOCK, ICON_SIZE, COLOR);
		JButton resetPinButton = new JButton(resetPinIcon);
		resetPinButton.setToolTipText("Reset your pin...");
		resetPinButton.setVerticalAlignment(SwingConstants.CENTER);
		resetPinButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Welcome welcome = new Welcome(Main.this, true);
				
				welcome.setLocationRelativeTo(Main.this);
				welcome.setVisible(true);
			}
		});
				
		bottomRight.add(versionButton);
		bottomRight.add(resetPinButton);
		bottomRight.add(signoutButton);

		for(Market m : Markets.getMarkets())
			marketComboBox.addItem(m);
		token = Markets.getToken();

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook(this, (Market) marketComboBox.getSelectedItem());
		
		transactionsPanel = new TransactionsPanel();
		historyPanel = new HistoryPanel(this, (Market) marketComboBox.getSelectedItem());
		accountsPanel = new AccountsPanel(this);
		
		ICON_CONNECTED = IconFontSwing.buildIcon(FontAwesome.WIFI, ICON_SIZE, COLOR);
		ICON_TESTNET = IconFontSwing.buildIcon(FontAwesome.FLASK, ICON_SIZE, COLOR);
		ICON_DISCONNECTED = IconFontSwing.buildIcon(FontAwesome.EXCLAMATION, ICON_SIZE, COLOR);

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, ICON_SIZE, COLOR);
		Icon expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, ICON_SIZE, COLOR);
		copyAddButton = new ExplorerButton("", copyIcon, expIcon);
		copyAddButton.getMainButton().setFont(largeFont);

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, ICON_SIZE, COLOR);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);
		settingsButton.setVisible(false);

		Icon sendIcon = IconFontSwing.buildIcon(FontAwesome.PAPER_PLANE, ICON_SIZE, COLOR);

		sendButton = new JButton(sendIcon);
		sendButton.setToolTipText("Send BURST...");
		sendButton.addActionListener(this);

		sendButtonToken = new JButton(sendIcon);
		sendButtonToken.setToolTipText(String.format("Send %s...", token.toString()));
		sendButtonToken.addActionListener(this);

		content.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, ICON_SIZE, COLOR);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, ICON_SIZE, COLOR);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, historyPanel);

		if(g.isTestnet()) {
			// FIXME: accounts on testnet only for now
			Icon accountIcon = IconFontSwing.buildIcon(FontAwesome.USER_CIRCLE, ICON_SIZE, COLOR);
			tabbedPane.addTab("ACCOUNTS", accountIcon, accountsPanel);
		}
		
		Icon chatIcon = IconFontSwing.buildIcon(FontAwesome.COMMENT, ICON_SIZE, COLOR);
		tabbedPane.addTab("CHAT", chatIcon, new ChatPanel());

		Icon transactionsIcon = IconFontSwing.buildIcon(FontAwesome.LINK, ICON_SIZE, COLOR);
		tabbedPane.addTab("TRANSACTIONS", transactionsIcon, transactionsPanel);
		
		top.add(new Desc("Market", marketComboBox));
		top.add(new Desc("Your Burst address", copyAddButton));
		
		balanceLabel = new JLabel("0");
		balanceLabel.setToolTipText("Available balance");
		balanceLabel.setFont(largeFont);
		lockedBalanceLabel = new JLabel("0");
		lockedBalanceLabel.setToolTipText("Amount locked in orders");
		top.add(new Desc("Balance (BURST)", balanceLabel, lockedBalanceLabel));
		top.add(new Desc("  ", sendButton));

		balanceLabelToken = new JLabel("0");
		balanceLabelToken.setToolTipText("Available balance");
		balanceLabelToken.setFont(largeFont);
		balanceLabelTokenPending = new JLabel("0");
		balanceLabelTokenPending.setToolTipText("Amount locked in orders or rewards pending");
		top.add(tokenDesc = new Desc(String.format("Balance (%s)", token), balanceLabelToken, balanceLabelTokenPending));
		top.add(new Desc("  ", sendButtonToken));

		
		top.add(new Desc("  ", settingsButton));

		nodeSelector = new JButton(g.getNode());
		nodeSelector.setToolTipText("Select node...");
		nodeSelector.addActionListener(this);
		
		explorer = ExplorerWrapper.getExplorer(g.getExplorer());
		explorerSelector = new JButton(explorer.toString(),
				IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, ICON_SIZE, COLOR));
		explorerSelector.setToolTipText("Select explorer...");
		explorerSelector.addActionListener(this);
		
		statusLabel = new JLabel();

		bottom.add(nodeSelector);
		bottom.add(explorerSelector);
		bottom.add(statusLabel);

		pack();
		setMinimumSize(new Dimension(1280, 600));
		setLocationRelativeTo(null);
		cardLayout.last(getContentPane());
		showingSplash = true;
		setVisible(true);
		
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
		copyAddButton.getMainButton().setText(g.getAddress().getRawAddress());
		copyAddButton.setAddress(g.getAddress().getID(), g.getAddress().getFullAddress());

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
			Toast.makeText(this, "Getting info from node...", 4000, Toast.Style.SUCCESS).display();
		update();
		Thread updateThread = new UpdateThread();
		updateThread.start();
	}
	
	public void browse(String url) {
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
					
					// Check if the node has the expected block
					if(g.isTestnet()) {
						Block checkBlock = g.getNS().getBlock(BurstID.fromLong(Constants.CHECK_BLOCK_TESTNET)).blockingGet();
						if(checkBlock.getHeight() != Constants.CHECK_HEIGHT_TESTNET) {
							String error = g.getNode() + " is not a valid testnet node!";
							Toast.makeText(Main.this, error, Toast.Style.ERROR).display();

							nodeSelector.setIcon(ICON_DISCONNECTED);
							statusLabel.setText(error);
						}
					}
					
					try {
						Account ac = g.getNS().getAccount(g.getAddress()).blockingGet();
						balance = ac.getBalance().longValue();
						// Locked value in *market* and possibly other Burst coin stuff.
						locked = balance - ac.getUnconfirmedBalance().longValue();
						
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
					balanceLabel.setText(NumberFormatting.BURST.format(balance));
					lockedBalanceLabel.setText("+" + NumberFormatting.BURST.format(locked) + " locked");
					
					g.updateSuggestedFee();
					
					if(transactionsPanel.isVisible() || showingSplash)
						transactionsPanel.update();
					if(orderBook.isVisible() || showingSplash)
						orderBook.update();
					if(historyPanel.isVisible() || showingSplash)
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
					nodeSelector.setIcon(g.isTestnet() ? ICON_TESTNET : ICON_CONNECTED);
				}
				catch (RuntimeException rex) {
					rex.printStackTrace();
					
					// Avoid making the window pop up on connectivity problems
					// Toast.makeText(Main.this, rex.getMessage(), Toast.Style.ERROR).display();

					nodeSelector.setIcon(ICON_DISCONNECTED);
					statusLabel.setText("Error: " + rex.getMessage());
				}
				if(showingSplash) {
					showingSplash = false;
					pulsingButton.stopPulsing();
					cardLayout.first(getContentPane());
				}
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
		if (e.getSource() == marketComboBox) {
			orderBook.setMarket(m);
			historyPanel.setMarket(m);
			
			if(m.getTokenID() == null) {
				// not a token market, show TRT in the token field 
				tokenDesc.setDesc(String.format("Balance (%s)", token));
				
				if(!Globals.getInstance().isTestnet()) {
					// FIXME: remove this when operational
					Toast.makeText(this, "Cross-chain markets currently only on testnet.", Toast.Style.ERROR).display();
				}
				else if(Contracts.isLoading()) {
					Toast.makeText(this, "Cross-chain market information is still loading...", Toast.Style.NORMAL).display();					
				}
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
		else if (e.getSource() == nodeSelector) {
			
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
				
				nodeSelector.setText(g.getNode());
				update();
			}
		}
		
		else if (e.getSource() == explorerSelector) {
			Globals g = Globals.getInstance();
			
			JComboBox<ExplorerWrapper> explorerCombo = new JComboBox<ExplorerWrapper>();
			explorerCombo.addItem(ExplorerWrapper.burstDevtrue());
			if(!g.isTestnet())
				explorerCombo.addItem(ExplorerWrapper.burstcoinRo());
			explorerCombo.addItem(ExplorerWrapper.burstcoinNetwork());
			
			for (int i = 0; i < explorerCombo.getItemCount(); i++) {
				if(explorerCombo.getItemAt(i).toString().equals(g.getExplorer()))
					explorerCombo.setSelectedIndex(i);
			}
			
			int ret = JOptionPane.showConfirmDialog(this, explorerCombo, "Select explorer", JOptionPane.OK_CANCEL_OPTION);
			
			if(ret == JOptionPane.OK_OPTION) {
				explorer = (ExplorerWrapper) explorerCombo.getSelectedItem();
				explorerSelector.setText(explorer.toString());
				
				g.setExplorer(explorer.getKey());
				try {
					g.saveConfs();
				} catch (Exception ex) {
					ex.printStackTrace();
					Toast.makeText(this, ex.getMessage(), Toast.Style.ERROR).display();
				}
			}
		}
	}
}
