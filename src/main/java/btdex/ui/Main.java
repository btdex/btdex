package btdex.ui;

import static btdex.locale.Translation.tr;

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
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import btdex.core.*;
import btdex.ui.orderbook.OrderBook;
import com.bulenkov.darcula.DarculaLaf;

import bt.BT;
import btdex.locale.Translation;
import btdex.markets.MarketBTC;
import btdex.markets.MarketBurstToken;
import btdex.sc.SellContract;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	private Image icon, iconMono;
	private Icon ICON_CONNECTED, ICON_DISCONNECTED, ICON_TESTNET;
	private PulsingIcon pulsingButton;

	private CardLayout cardLayout;
	private boolean showingSplash;
	private OrderBook orderBook;
	private MediationPanel mediationPanel;
	private TransactionsPanel transactionsPanel;
	private HistoryPanel historyPanel;
	private AccountsPanel accountsPanel;

	private JTabbedPane tabbedPane;
	private JLabel statusLabel;
	private JButton nodeSelector, explorerSelector;

	private ExplorerWrapper explorer;
	private ExplorerButton copyAddButton;

	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;
	private JComboBox<Market> marketComboBox;
	private JButton removeTokenButton;
	private Market addMarketDummy, newMarketDummy;
	private JButton sendButton;
	private Market token;

	private Desc tokenDesc;

	private JLabel balanceLabelToken;
	private JLabel balanceLabelTokenPending;
	private JButton sendButtonToken;

	private long lastUpdated;

	private JButton signoutButton;
	private String version = "dev";
	private Icons i;

	private JButton resetPinButton;

	private Logger logger = LogManager.getLogger();

	private static Main instance;

	public static Main getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		byte[] entropy = new byte[Words.TWELVE.byteLength()];
		new SecureRandom().nextBytes(entropy);
		new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);

		new Main();
	}

	public Main() {
		super("BTDEX" + (Globals.getInstance().isTestnet() ? "-TESTNET" : ""));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		instance = this;

		readLocalResources();
		setUIManager();

		IconFontSwing.register(FontAwesome.getIconFont());
		IconFontSwing.register(FontAwesomeBrands.getIconFont());
		setBackground(Color.BLACK);

		Globals g = Globals.getInstance();

		Translation.setLanguage(g.getLanguage());

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
		JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
		topAll.add(topRight, BorderLayout.LINE_END);
		content.add(topAll, BorderLayout.PAGE_START);
		content.add(bottomAll, BorderLayout.PAGE_END);

		JPanel bottomRight = new JPanel();
		bottomAll.add(bottomRight, BorderLayout.LINE_END);
		bottomAll.add(bottom, BorderLayout.LINE_START);

		marketComboBox = new JComboBox<Market>();
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, Constants.ICON_SIZE);
		Color COLOR = marketComboBox.getForeground();
		i = new Icons(COLOR, Constants.ICON_SIZE);

		marketComboBox.setToolTipText(tr("main_select_market"));
		marketComboBox.setFont(largeFont);

		bottomRight.add(createWebButton());
		bottomRight.add(createDiscordButton());
		bottomRight.add(createRedditButton());
		bottomRight.add(createGithubButton());

		signoutButton = new JButton(i.get(Icons.SIGNOUT));
		signoutButton.setToolTipText(tr("main_exit_tip"));
		signoutButton.setVerticalAlignment(SwingConstants.CENTER);
		signoutButton.addActionListener(this);

		bottomRight.add(createVersionButton());
		bottomRight.add(createResetPinButton());
		bottomRight.add(signoutButton);

		for(Market m : Markets.getMarkets())
			marketComboBox.addItem(m);
		marketComboBox.addItem(addMarketDummy = new MarketBTC() {
			@Override
			public String toString() {
				return tr("main_add_token_sign");
			}
		});
		marketComboBox.addItem(newMarketDummy = new MarketBTC() {
			@Override
			public String toString() {
				return tr("main_new_token_sign");
			}
		});
		token = Markets.getToken();

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook(this, (Market) marketComboBox.getSelectedItem());

		removeTokenButton = new JButton(i.get(Icons.TRASH));
		removeTokenButton.setToolTipText(tr("main_remove_token_tip"));
		removeTokenButton.addActionListener(this);
		removeTokenButton.setVisible(false);

		transactionsPanel = new TransactionsPanel();
		historyPanel = new HistoryPanel(this, (Market) marketComboBox.getSelectedItem(), orderBook);
		accountsPanel = new AccountsPanel(this);

		ICON_CONNECTED = i.get(Icons.CONNECTED);
		ICON_TESTNET = i.get(Icons.TESTNET);
		ICON_DISCONNECTED = i.get(Icons.DISCONNECTED);

		copyAddButton = new ExplorerButton("", i.get(Icons.COPY), i.get(Icons.EXPLORER));
		copyAddButton.getMainButton().setFont(largeFont);

		sendButton = new JButton(i.get(Icons.SEND));
		sendButton.setToolTipText(tr("main_send", "BURST"));
		sendButton.addActionListener(this);

		sendButtonToken = new JButton(i.get(Icons.SEND));
		sendButtonToken.setToolTipText(tr("main_send", token.toString()));
		sendButtonToken.addActionListener(this);

		content.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		tabbedPane.addTab(tr("main_order_book"), i.get(Icons.ORDER_BOOK), orderBook);
		tabbedPane.addTab(tr("main_trade_history"), i.get(Icons.TRADE), historyPanel);

		if(!g.usingLedger()) {
			tabbedPane.addTab(tr("main_accounts"), i.get(Icons.ACCOUNT), accountsPanel);
			// tabbedPane.addTab(tr("main_chat"), i.get(Icons.CHAT), new ChatPanel());
		}

		if(g.getAddress()!=null && g.getMediators().isMediator(g.getAddress().getSignedLongId())){
			// this is a mediator, add the mediation tab
			tabbedPane.addTab(tr("main_mediation"), i.get(Icons.MEDIATION), mediationPanel = new MediationPanel(this));
		}

		tabbedPane.addTab(tr("main_transactions"), i.get(Icons.TRANSACTION), transactionsPanel);

		top.add(new Desc(tr("main_market"), marketComboBox));
		top.add(new Desc(" ", removeTokenButton));
		top.add(new Desc(tr("main_your_burst_address"), copyAddButton));

		balanceLabel = new JLabel("0");
		balanceLabel.setToolTipText(tr("main_available_balance"));
		balanceLabel.setFont(largeFont);
		lockedBalanceLabel = new JLabel("0");
		lockedBalanceLabel.setToolTipText(tr("main_amount_locked"));
		top.add(new Desc(tr("main_balance", "BURST"), balanceLabel, lockedBalanceLabel));
		top.add(new Desc("  ", sendButton));

		balanceLabelToken = new JLabel("0");
		balanceLabelToken.setToolTipText(tr("main_available_balance"));
		balanceLabelToken.setFont(largeFont);
		balanceLabelTokenPending = new JLabel("0");
		balanceLabelTokenPending.setToolTipText(tr("main_amount_locked"));
		top.add(tokenDesc = new Desc(tr("main_balance", token), balanceLabelToken, balanceLabelTokenPending));
		top.add(new Desc("  ", sendButtonToken));

		topRight.add(new Desc("  ", createSettingsButton(largeFont)));
		topRight.add(new Desc(tr("main_language_name"), createLangButton(largeFont, g)));

		nodeSelector = new JButton(g.getNode());
		nodeSelector.setToolTipText(tr("main_select_node"));
		nodeSelector.addActionListener(this);

		explorer = ExplorerWrapper.getExplorer(g.getExplorer());
		explorerSelector = new JButton(explorer.toString(),
				i.get(Icons.EXPLORER));
		explorerSelector.setToolTipText(tr("main_select_explorer"));
		explorerSelector.addActionListener(this);

		statusLabel = new JLabel();

		bottom.add(nodeSelector);
		bottom.add(explorerSelector);
		bottomAll.add(statusLabel, BorderLayout.CENTER);

		pack();
		setMinimumSize(new Dimension(1280, 600));
		setLocationRelativeTo(null);
		cardLayout.last(getContentPane());
		showingSplash = true;
		setVisible(true);

		if(g.getAddress()==null) {
			logger.debug("no public key or invalid, show the welcome screen");
			Welcome welcome = new Welcome(this);

			welcome.setLocationRelativeTo(this);
			welcome.setVisible(true);
			if(welcome.getReturn() == 0) {
				System.exit(0);
				logger.debug("welcome.getReturn() == 0, system.exit");
				return;
			}

			resetPinButton.setVisible(!g.usingLedger());
		}
		copyAddButton.getMainButton().setText(g.getAddress().getRawAddress());
		copyAddButton.setAddress(g.getAddress().getID(), g.getAddress().getFullAddress());

		// check if this is a known account
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		boolean newAccount = false;
		try {
			Account address = g.getNS().getAccount(g.getAddress()).blockingGet();
			logger.info("Active address {}", address.getId().getFullAddress());
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
								Toast.makeText(this, tr("main_account_activate"), Toast.Style.SUCCESS).display();
								tabbedPane.setSelectedComponent(transactionsPanel);
							}
							else {
								Toast.makeText(this, tr("main_activation_failed", response.code(), response.message()), Toast.Style.ERROR).display();
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

		marketComboBox.addActionListener(this);

		if(!newAccount)
			Toast.makeText(this, tr("main_getting_info_from_node"), 4000, Toast.Style.SUCCESS).display();
		update();

		Timer timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateUI();
			}
		});
		timer.start();
		logger.info("updateUI timer started");
	}

	public void showTransactionsPanel() {
		tabbedPane.setSelectedComponent(transactionsPanel);
	}

	public void browse(String url) {
		try {
			DesktopApi.browse(new URI(url));
			Toast.makeText(Main.this, tr("main_opening_url", url.substring(0, Math.min(url.length(), 40)) + "..."), Toast.Style.SUCCESS).display();
		} catch (Exception ex) {
			logger.error(ex.getLocalizedMessage());
			Toast.makeText(Main.this, ex.getMessage(), Toast.Style.ERROR).display();
		}
	}

	private void readLocalResources() {
		try {
			icon = Icons.getIcon();
			setIconImage(icon);
			iconMono = Icons.getIconMono();

			Properties versionProp = new Properties();
			versionProp.load(Main.class.getResourceAsStream("/version.properties"));
			version = versionProp.getProperty("version");
			logger.info("Local resources, Version {}", version);
		} catch (Exception ex) {
			logger.error("Error in reading local resources :" + ex.getLocalizedMessage());
			ex.printStackTrace();
		}
	}

	private void setUIManager() {
		// TODO try again later this other theme, now bugs with tooltip, long text buttons, etc.
		// LafManager.install(new DarculaTheme()); //Specify the used theme.
		// LafManager.getUserProperties().put(DarkTooltipUI.KEY_STYLE, DarkTooltipUI.VARIANT_PLAIN);
		try {
			DarculaLaf laf = new DarculaLaf();
			UIManager.setLookAndFeel(laf);
			logger.debug("UI manager {} created", laf.getDescription());
		} catch (UnsupportedLookAndFeelException e) {
			logger.error("Error: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private JButton createVersionButton() {
		JButton versionButton = new JButton(version, i.get(Icons.VERSION));
		versionButton.setToolTipText(tr("main_check_new_release"));
		versionButton.setVerticalAlignment(SwingConstants.CENTER);
		versionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Version Button clicked");
				browse(Constants.RELEASES_LINK);
			}
		});
		return versionButton;
	}

	private JButton createResetPinButton() {
		resetPinButton = new JButton(i.get(Icons.RESET_PIN));
		resetPinButton.setToolTipText(tr("main_reset_pin"));
		resetPinButton.setVerticalAlignment(SwingConstants.CENTER);
		resetPinButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Reset Pin Button clicked");
				Welcome welcome = new Welcome(Main.this, true);

				welcome.setLocationRelativeTo(Main.this);
				welcome.setVisible(true);
			}
		});
		resetPinButton.setVisible(!Globals.getInstance().usingLedger());
		return resetPinButton;
	}

	private JButton createWebButton() {
		Icon iconBtdex = i.get(Icons.BTDEX);
		if(iconMono!=null)
			iconBtdex = new ImageIcon(iconMono);
		JButton webButton = new JButton(iconBtdex);
		webButton.setToolTipText(tr("main_open_website"));
		webButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Web Button clicked");
				browse(Constants.WEBSITE_LINK);
			}
		});
		return webButton;
	}

	private JButton createDiscordButton() {
		JButton discordButton = new JButton(i.get(Icons.DISCORD));
		discordButton.setToolTipText(tr("main_chat_discord"));
		discordButton.setVerticalAlignment(SwingConstants.CENTER);
		discordButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Discord Button clicked");
				browse(Constants.DISCORD_LINK);
			}
		});
		return discordButton;
	}

	private JButton createRedditButton() {
		JButton discordButton = new JButton(i.get(Icons.REDDIT));
		discordButton.setToolTipText(tr("main_chat_reddit"));
		discordButton.setVerticalAlignment(SwingConstants.CENTER);
		discordButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Reddit Button clicked");
				browse(Constants.REDDIT_LINK);
			}
		});
		return discordButton;
	}

	private JButton createGithubButton() {
		JButton githubButton = new JButton(i.get(Icons.GITHUB));
		githubButton.setToolTipText(tr("main_check_source"));
		githubButton.setVerticalAlignment(SwingConstants.CENTER);
		githubButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Github Button clicked");
				browse(Constants.GITHUB_LINK);
			}
		});
		return githubButton;
	}

	private JButton createSettingsButton(Font largeFont) {
		JButton settingsButton = new JButton(i.get(Icons.SETTINGS));
		settingsButton.setToolTipText(tr("main_configure_settings"));
		settingsButton.setFont(largeFont);
		settingsButton.setVisible(false);
		return settingsButton;
	}

	private JButton createLangButton(Font largeFont, Globals g) {
		JButton langButton = new JButton(i.get(Icons.LANGUAGE));
		langButton.setToolTipText(tr("main_change_language"));
		langButton.setFont(largeFont);
		langButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logger.debug("Lang Button clicked");
				JPopupMenu menu = new JPopupMenu();
				for(Locale l : Translation.getSupportedLanguages()) {
					JMenuItem item = new JMenuItem(l.getDisplayLanguage(Translation.getCurrentLocale()));
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							logger.debug("Language selected");
							g.setLanguage(l.getLanguage());
							try {
								g.saveConfs();
							} catch (Exception e1) {
								logger.error("Error: {}", e1.getLocalizedMessage());
								e1.printStackTrace();
							}
							JOptionPane.showMessageDialog(Main.this,
									tr("main_restart_to_apply_changes"), tr("main_language_changed"),
									JOptionPane.OK_OPTION);
						}
					});
					menu.add(item);
				}
				menu.show(langButton, 0, langButton.getHeight());
			}
		});
		return langButton;
	}

	/**
	 * Signal an update should take place.
	 */
	public void update() {
		lastUpdated = 0;
	}

	private void updateUI() {
		// Update at every 10 seconds
		if(System.currentTimeMillis() - lastUpdated < Constants.UI_UPDATE_INTERVAL) {
			return;
		}
		lastUpdated = System.currentTimeMillis();
		logger.trace("Updating UI...");
		long balance = 0, locked = 0;
		try {
			Globals g = Globals.getInstance();
			logger.trace("Got globals instance, Node {}", g.getNode());
			BurstNode bn = BurstNode.getInstance();

			if(transactionsPanel.isVisible() || showingSplash)
				transactionsPanel.update();
			if(orderBook.isVisible() || historyPanel.isVisible() || showingSplash) {
				orderBook.update();
				historyPanel.update();
			}
			if(mediationPanel!=null && mediationPanel.isVisible())
				mediationPanel.update();

			nodeSelector.setIcon(g.isTestnet() ? ICON_TESTNET : ICON_CONNECTED);
			nodeSelector.setBackground(explorerSelector.getBackground());

			Exception nodeException = bn.getNodeException();
			if(nodeException != null) {
				logger.warn("nodeException {}", nodeException.getLocalizedMessage());
				if(!(nodeException.getCause() instanceof BRSError) || ((BRSError) nodeException.getCause()).getCode() != 5) {
					// not the unknown account exception, show the error
					nodeSelector.setIcon(ICON_DISCONNECTED);
					String errorMessage = tr("main_error", nodeException.getLocalizedMessage());

					if(nodeException.getCause() instanceof ConnectException ||
							nodeException.getCause() instanceof SocketTimeoutException) {
						errorMessage = tr("main_node_connection");
						nodeSelector.setBackground(Color.RED);
					}

					statusLabel.setText(errorMessage);
				}
				// otherwise all fine, just move on
				if(showingSplash) {
					showingSplash = false;
					pulsingButton.stopPulsing();
					cardLayout.first(getContentPane());
					update();
				}
				return;
			}

			// Check if the node has the expected block
			Block checkBlock = bn.getCheckBlock();
			if(checkBlock == null) {
				logger.debug("checkBlock equals to Null");
				return;
			}
			String checkBlockId = checkBlock.getId().getID();
			if(!checkBlockId.equals(g.isTestnet() ? Constants.CHECK_BLOCK_TESTNET : Constants.CHECK_BLOCK)) {
				logger.warn("Check block Id equals to {}, testnet {}, realnet {}", checkBlockId, Constants.CHECK_BLOCK_TESTNET, Constants.CHECK_BLOCK);
				String error = tr("main_invalid_node", g.getNode());
				Toast.makeText(Main.this, error, Toast.Style.ERROR).display();

				nodeSelector.setIcon(ICON_DISCONNECTED);
				nodeSelector.setBackground(Color.RED);
				statusLabel.setText(error);

				if(showingSplash) {
					showingSplash = false;
					pulsingButton.stopPulsing();
					cardLayout.first(getContentPane());
					logger.debug("Splash removed");
				}
				return;
			}

			Account ac = bn.getAccount();
			if(ac == null)
				return;
			balance = ac.getBalance().longValue();
			// Locked value in *market* and possibly other Burst coin stuff.
			locked = balance - ac.getUnconfirmedBalance().longValue();
			balance -= locked;

			// Add the amounts on smart contract trades on the locked balance
			for(ContractState s : Contracts.getContracts()) {
				if(s.getState() == SellContract.STATE_FINISHED)
					continue;
				if(s.getCreator().equals(g.getAddress())){
					if(s.getType() == ContractType.SELL)
						locked += s.getAmountNQT() + s.getSecurityNQT();
					else if(s.getType() == ContractType.BUY)
						locked += s.getSecurityNQT();
				}
				else if (s.getTaker() == g.getAddress().getSignedLongId()) {
					if(s.getType() == ContractType.SELL)
						locked += s.getSecurityNQT();
					else if(s.getType() == ContractType.BUY)
						locked += s.getAmountNQT() + s.getSecurityNQT();
				}
			}

			balanceLabel.setText(NumberFormatting.BURST.format(balance));
			lockedBalanceLabel.setText(tr("main_plus_locked", NumberFormatting.BURST.format(locked)));

			Market tokenMarket = token;
			Market m = (Market) marketComboBox.getSelectedItem();
			if(m.getTokenID()!=null && m!=token)
				tokenMarket = m;
			AssetBalance tokenBalanceAccount = bn.getAssetBalances(tokenMarket);

			long tokenBalance = 0;
			long tokenLocked = 0;
			if (tokenBalanceAccount != null) {
				tokenBalance += tokenBalanceAccount.getBalance().longValue();
			}

			AssetOrder[] asks = bn.getAssetAsks(tokenMarket);
			if(asks == null)
				return;
			for(AssetOrder o : asks) {
				if(!o.getAccountAddress().equals(g.getAddress()))
					continue;
				tokenLocked += o.getQuantity().longValue();
			}
			tokenBalance -= tokenLocked;

			balanceLabelToken.setText(tokenMarket.format(tokenBalance));
			balanceLabelTokenPending.setText(tr("main_plus_locked", tokenMarket.format(tokenLocked)));

			// all fine status label with the latest block
			statusLabel.setText("");
			nodeSelector.setIcon(g.isTestnet() ? ICON_TESTNET : ICON_CONNECTED);
			nodeSelector.setBackground(explorerSelector.getBackground());

			// check if the latest block is too much in the past
			Date back8Minutes = new Date(System.currentTimeMillis() - 8*60_000);
			if(bn.getLatestBlock().getTimestamp().getAsDate().before(back8Minutes)) {
				statusLabel.setText(tr("main_node_not_sync"));
				nodeSelector.setBackground(Color.RED);
			}
		}
		catch (RuntimeException rex) {
			rex.printStackTrace();

			nodeSelector.setIcon(ICON_DISCONNECTED);
			statusLabel.setText(tr("main_error", rex.getMessage()));
		}
		if(showingSplash) {
			showingSplash = false;
			pulsingButton.stopPulsing();
			cardLayout.first(getContentPane());

			Toast.makeText(this, tr("main_cross_chain_loading"), 8000, Toast.Style.SUCCESS).display();
		}
	}

	public void addMarket(MarketBurstToken newMarket) {
		// Add at the end of the list
		marketComboBox.removeItem(addMarketDummy);
		marketComboBox.removeItem(newMarketDummy);
		marketComboBox.addItem(newMarket);
		marketComboBox.addItem(addMarketDummy);
		marketComboBox.addItem(newMarketDummy);

		Globals.getInstance().addUserMarket(newMarket, true);
		BurstNode.getInstance().update();

		marketComboBox.setSelectedItem(newMarket);
		Toast.makeText(this, tr("main_add_token_success", newMarket.getTokenID().getID()), Toast.Style.SUCCESS).display();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Market m = (Market) marketComboBox.getSelectedItem();

		if(e.getSource() == signoutButton) {
			Globals g = Globals.getInstance();
			String response = JOptionPane.showInputDialog(this,
					tr(g.usingLedger() ? "main_exit_message_ledger" : "main_exit_message", g.getAddress().getRawAddress()),
					tr("main_exit"), JOptionPane.OK_CANCEL_OPTION);
			if(response != null) {
				if(!response.equalsIgnoreCase(g.getAddress().getRawAddress().substring(0, 4))) {
					Toast.makeText(this, tr("main_exit_error"), Toast.Style.ERROR).display();
					return;
				}
				try {
					g.clearConfs();
					System.exit(0);
				} catch (Exception ex) {
					ex.printStackTrace();
					Toast.makeText(Main.this, ex.getMessage(), Toast.Style.ERROR).display();
				}
			}
		}

		if(e.getSource() == removeTokenButton) {
			int response = JOptionPane.showConfirmDialog(this, tr("main_remove_token_message", m.toString()),
					tr("main_remove_token"), JOptionPane.YES_NO_OPTION);
			if(response == JOptionPane.YES_OPTION) {
				marketComboBox.setSelectedIndex(0);

				marketComboBox.removeItem(m);
				Globals.getInstance().removeUserMarket(m, true);
				BurstNode.getInstance().update();
				return;
			}
		}

		if (e.getSource() == marketComboBox) {
			if(m == addMarketDummy) {
				String response = JOptionPane.showInputDialog(this, tr("main_add_token_message"),
						tr("main_add_token"), JOptionPane.OK_CANCEL_OPTION);
				if(response != null) {
					MarketBurstToken newMarket = new MarketBurstToken(response, Globals.getInstance().getNS());
					if(newMarket.getFactor() != 0) {
						addMarket(newMarket);
						return;
					}
					else {
						Toast.makeText(this, tr("main_add_token_invalid", response), Toast.Style.ERROR).display();
					}
				}

				marketComboBox.setSelectedIndex(0);
				return;
			}
			if(m == newMarketDummy) {
				CreateTokenDialog dlg = new CreateTokenDialog(this);
				dlg.setLocationRelativeTo(this);
				dlg.setVisible(true);

				if(dlg.getReturnValue() == JOptionPane.CANCEL_OPTION)
					marketComboBox.setSelectedIndex(0);
				return;
			}

			orderBook.setMarket(m);
			historyPanel.setMarket(m);
			// this is a custom token
			removeTokenButton.setVisible(Markets.getUserMarkets().contains(m));

			if(m.getTokenID() == null) {
				// not a token market, show TRT in the token field
				tokenDesc.setDesc(tr("main_balance", token));

				if(Contracts.isLoading()) {
					Toast.makeText(this, tr("main_cross_chain_loading"), 8000, Toast.Style.NORMAL).display();
				}
			}
			else {
				// this is a token market, show it on the token field
				tokenDesc.setDesc(tr("main_balance", m));
				balanceLabelToken.setText(m.format(0));
				balanceLabelTokenPending.setText(" ");
				sendButtonToken.setToolTipText(tr("main_send", m.toString()));
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

			String[] list = {BT.NODE_BURST_ALLIANCE, BT.NODE_BURSTCOIN_RO, BT.NODE_BURST_TEAM, Constants.NODE_LOCALHOST};
			if(g.isTestnet()){
				list = new String[]{Constants.NODE_TESTNET2, BT.NODE_TESTNET, BT.NODE_TESTNET_MEGASH, BT.NODE_LOCAL_TESTNET };
			}

			JComboBox<String> nodeComboBox = new JComboBox<String>(list);
			nodeComboBox.setEditable(true);
			int ret = JOptionPane.showConfirmDialog(this, nodeComboBox, tr("main_select_node"), JOptionPane.OK_CANCEL_OPTION);

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
			explorerCombo.addItem(ExplorerWrapper.burstScanNet());
			if(!g.isTestnet())
				explorerCombo.addItem(ExplorerWrapper.burstcoinRo());
			explorerCombo.addItem(ExplorerWrapper.burstcoinNetwork());

			for (int i = 0; i < explorerCombo.getItemCount(); i++) {
				if(explorerCombo.getItemAt(i).toString().equals(g.getExplorer()))
					explorerCombo.setSelectedIndex(i);
			}

			int ret = JOptionPane.showConfirmDialog(this, explorerCombo, tr("main_select_explorer"), JOptionPane.OK_CANCEL_OPTION);

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

	public ExplorerWrapper getExplorer() {
		return explorer;
	}
}
