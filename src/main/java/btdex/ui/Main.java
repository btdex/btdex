package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PopupMenu;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bulenkov.darcula.DarculaLaf;

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.ContractType;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.NumberFormatting;
import btdex.locale.Translation;
import btdex.sc.SellContract;
import btdex.ui.orderbook.MarketPanel;
import btdex.ui.orderbook.TokenMarketPanel;
import signumj.entity.SignumAddress;
import signumj.entity.response.Account;
import signumj.entity.response.Block;
import signumj.entity.response.http.BRSError;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import dorkbox.util.OS;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.icons.font_awesome.FontAwesomeBrands;
import jiconfont.swing.IconFontSwing;
import okhttp3.Response;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private Image icon, iconMono;
	private Icon ICON_CONNECTED, ICON_DISCONNECTED, ICON_TESTNET;
	private PulsingIcon pulsingButton;

	private CardLayout cardLayout;
	private boolean showingSplash;
	private MarketPanel orderBook;
	private TokenMarketPanel orderBookToken;
	private MediationPanel mediationPanel;
	private TransactionsPanel transactionsPanel;

	private JTabbedPane tabbedPane;
	private JLabel statusLabel;
	private JButton nodeSelector, explorerSelector;

	private ExplorerWrapper explorer;
	private ExplorerButton copyAddButton;

	private JLabel balanceLabel;
	private JLabel lockedBalanceLabel;
	private JButton sendButton;

	private long lastUpdated;

	private JButton signoutButton;
	private Icons i;

	private JButton resetPinButton;

	private Logger logger = LogManager.getLogger();

	private MiningPanel miningPanel;

	private SystemTray systemTray;
	private TrayIcon windowsTrayIcon;

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
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
            	if(windowsTrayIcon != null) {
            		windowsTrayIcon.displayMessage(getTitle() + " " + Globals.getInstance().getVersion(), tr("tray_running"), MessageType.INFO);
            	}
            	if(windowsTrayIcon != null || systemTray != null) {
            		Toast.makeText(Main.this, tr("tray_running"), Toast.Style.SUCCESS).display(MouseInfo.getPointerInfo().getLocation());
            		Main.this.setVisible(false);
            	}
            	else {
		        	if(miningPanel != null)
		        		miningPanel.stop();
		            System.exit(0);
            	}
            }
        });

		instance = this;

		readLocalResources();
		setUIManager();

		IconFontSwing.register(FontAwesome.getIconFont());
		IconFontSwing.register(FontAwesomeBrands.getIconFont());
		setBackground(Color.BLACK);

		Globals g = Globals.getInstance();

		Translation.setLanguage(g.getLanguage());

		addSystemTray();
		
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

		Font largeFont = bottomRight.getFont().deriveFont(Font.BOLD, Constants.ICON_SIZE);
		Color COLOR = bottomRight.getForeground();
		i = new Icons(COLOR, Constants.ICON_SIZE);

		bottomRight.add(createWebButton());
		bottomRight.add(createDiscordButton());
		bottomRight.add(createRedditButton());
		bottomRight.add(createGithubButton());

		signoutButton = new JButton(i.get(Icons.RECYCLE));
		signoutButton.setToolTipText(tr("main_exit_tip"));
		signoutButton.setVerticalAlignment(SwingConstants.CENTER);
		signoutButton.addActionListener(this);

		bottomRight.add(createVersionButton());
		bottomRight.add(createResetPinButton());

		orderBook = new MarketPanel(this);
		orderBookToken = new TokenMarketPanel(this);

		transactionsPanel = new TransactionsPanel();

		ICON_CONNECTED = i.get(Icons.CONNECTED);
		ICON_TESTNET = i.get(Icons.TESTNET);
		ICON_DISCONNECTED = i.get(Icons.DISCONNECTED);

		copyAddButton = new ExplorerButton("", i.get(Icons.COPY), i.get(Icons.EXPLORER));
		copyAddButton.getMainButton().setFont(largeFont);

		sendButton = new JButton(i.get(Icons.SEND));
		sendButton.setToolTipText(tr("main_send", Constants.BURST_TICKER));
		sendButton.addActionListener(this);

		content.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		tabbedPane.addTab(tr("main_swaps"), i.get(Icons.SWAPS), orderBookToken);
		tabbedPane.addTab(tr("main_contracts"), i.get(Icons.CROSS_CHAIN), orderBook);
		tabbedPane.addTab(tr("main_mining"), i.get(Icons.MINING), miningPanel = new MiningPanel());

		boolean isMediator = g.getAddress()!=null && g.getMediators().isMediator(g.getAddress().getSignedLongId());

		if(isMediator){
			// this is a mediator, add the mediation tab
			tabbedPane.addTab(tr("main_mediation"), i.get(Icons.MEDIATION), mediationPanel = new MediationPanel(this));
		}

		tabbedPane.addTab(tr("main_transactions"), i.get(Icons.TRANSACTION), transactionsPanel);
		
		top.add(new Desc(tr("main_your_burst_address"), copyAddButton));

		balanceLabel = new JLabel(NumberFormatting.BURST.format(0));
		balanceLabel.setToolTipText(tr("main_available_balance"));
		balanceLabel.setFont(largeFont);
		lockedBalanceLabel = new JLabel(tr("main_plus_locked", NumberFormatting.BURST.format(0)));
		lockedBalanceLabel.setToolTipText(tr("main_amount_locked"));
		top.add(new Desc(tr("main_balance", Constants.BURST_TICKER), balanceLabel, lockedBalanceLabel));
		top.add(new Desc("  ", sendButton));

		topRight.add(new Desc("  ", createSettingsButton(largeFont)));
		topRight.add(new Desc("  ", resetPinButton));
		topRight.add(new Desc("  ", signoutButton));
		topRight.add(new Desc(tr("main_language_name"), createLangButton(largeFont, g)));
		topRight.add(new Desc("  ", createQuitButton()));

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
		copyAddButton.getMainButton().setText(printAddress(g.getAddress()));
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
					
					// the copy will use the extended address if a new account, just to be sure
					copyAddButton.setAddress(g.getAddress().getID(), g.getAddress().getExtendedAddress());
					
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
							Response response = g.activate("btdex-" + g.getVersion());
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
	
	@SuppressWarnings("serial")
	private void addSystemTray() {
		logger.debug("adding to system tray");
		Action showHideAction = new AbstractAction(tr("tray_show_hide")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				Main.this.setVisible(!Main.this.isVisible());
			}
		};
		Action quitAction = new AbstractAction(tr("tray_quit")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				doQuit();
			}
		};
		
		ArrayList<Action> actions = new ArrayList<>();
		actions.add(showHideAction);
		actions.add(quitAction);
		Globals g = Globals.getInstance();
				
		if(OS.isWindows()) {
			if(java.awt.SystemTray.isSupported()) {
				java.awt.SystemTray sysTray = java.awt.SystemTray.getSystemTray();
				PopupMenu popup = new PopupMenu();

				for (Action a : actions) {
					java.awt.MenuItem awtItem = new java.awt.MenuItem(a.getValue(Action.NAME).toString());
					awtItem.addActionListener(a);
					popup.add(awtItem);
				}

				windowsTrayIcon = new TrayIcon(icon, getTitle());
				windowsTrayIcon.setImageAutoSize(true);
				windowsTrayIcon.setPopupMenu(popup);
				try {
					sysTray.add(windowsTrayIcon);
					windowsTrayIcon.displayMessage(getTitle() + " " + g.getVersion(), tr("tray_running"), MessageType.INFO);
				} catch (AWTException e1) {
					e1.printStackTrace();
				}
			}
		}
		else {
			systemTray = SystemTray.get();
			if(systemTray != null) {
				systemTray.setImage(icon);
				systemTray.setTooltip(getTitle());
				systemTray.setStatus(getTitle() + " " + g.getVersion());

				for (Action a : actions) {
					systemTray.getMenu().add(new MenuItem(a.getValue(Action.NAME).toString(), a));
				}
			}
		}
	}

	private JButton createVersionButton() {
		JButton versionButton = new JButton(Globals.getInstance().getVersion(), i.get(Icons.VERSION));
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
					String country = l.getDisplayCountry(Translation.getCurrentLocale());
					JMenuItem item = new JMenuItem(l.getDisplayLanguage(Translation.getCurrentLocale())
							+ (country.length() > 0 ? "-" + country : ""));
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							logger.debug("Language selected");
							String country = l.getCountry();
							g.setLanguage(l.getLanguage() + (country.length() > 0 ? "-" + country : ""));
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
	
	public static String printAddress(SignumAddress address) {
		String fullAddress = address.getFullAddress();
		return fullAddress;
		//return fullAddress.substring(0, 4) + "..." + fullAddress.substring(19);
		//return fullAddress.substring(0, 6) + "..." + fullAddress.substring(17);
	}
	
	private void doQuit() {
		if(miningPanel != null)
    		miningPanel.stop();
		if(systemTray != null) {
			systemTray.shutdown();				
		}
		if(windowsTrayIcon != null) {
			java.awt.SystemTray.getSystemTray().remove(windowsTrayIcon);
		}
        System.exit(0);
	}

	private JButton createQuitButton() {
		JButton quitButton = new JButton(i.get(Icons.QUIT));
		quitButton.setToolTipText(tr("tray_quit"));
		quitButton.addActionListener(e -> {
			doQuit();
		});
		return quitButton;
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
			if(orderBook.isVisible() || orderBookToken.isVisible() || showingSplash) {
				orderBook.update();
				orderBookToken.update();
			}
			if(mediationPanel!=null && mediationPanel.isVisible())
				mediationPanel.update();
			if(miningPanel != null) {
				if(miningPanel.isVisible() || showingSplash) {
					miningPanel.update();
				}
			}

			nodeSelector.setIcon(g.isTestnet() ? ICON_TESTNET : ICON_CONNECTED);
			nodeSelector.setBackground(explorerSelector.getBackground());

			String nodeAddress = g.getNS().getAddress();
			if(!nodeSelector.getText().equals(nodeAddress)) {
				// if the best node changed
				nodeSelector.setText(nodeAddress);
				g.setNode(true, nodeAddress);
				try {
					g.saveConfs();
				} catch (Exception ex) {
					ex.printStackTrace();
					Toast.makeText(this, ex.getMessage(), Toast.Style.ERROR).display();
				}
			}

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
			
			// the copy will use the short address if we have a valid account
			copyAddButton.setAddress(g.getAddress().getID(), g.getAddress().getFullAddress());
			
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

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getSource() == signoutButton) {
			Globals g = Globals.getInstance();
			String response = JOptionPane.showInputDialog(this,
					tr(g.usingLedger() ? "main_exit_message_ledger" : "main_exit_message", g.getAddress().getFullAddress()),
					tr("main_exit"), JOptionPane.OK_CANCEL_OPTION);
			if(response != null) {
				String strAddress = g.getAddress().getFullAddress();
				if(!response.equalsIgnoreCase(strAddress.substring(strAddress.length()-5))) {
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
		else if (e.getSource() == sendButton) {
			SendDialog dlg = new SendDialog(this, null);

			dlg.setLocationRelativeTo(Main.this);
			dlg.setVisible(true);
		}
		else if (e.getSource() == nodeSelector) {

			Globals g = Globals.getInstance();
			String customNode = "";
			if(!g.isNodeAutomatic())
				customNode = g.getNode();

			JTextField customNodeField = new JTextField(20);
			customNodeField.setText(customNode);
			JPanel customNodePanel = new JPanel(new BorderLayout());
			customNodePanel.add(new JLabel(tr("main_select_node_text")), BorderLayout.PAGE_START);
			customNodePanel.add(customNodeField, BorderLayout.PAGE_END);
			int ret = JOptionPane.showConfirmDialog(this, customNodePanel, tr("main_select_node"), JOptionPane.OK_CANCEL_OPTION);

			if(ret == JOptionPane.OK_OPTION) {
				customNode = customNodeField.getText().trim();
				if(customNode.length() > 0) {
					g.setNode(false, customNode);
				}
				else {
					if(g.isNodeAutomatic())
						return;
					g.setNode(true, g.getNS().getAddress());
				}
				try {
					g.saveConfs();
				} catch (Exception ex) {
					ex.printStackTrace();
					Toast.makeText(this, ex.getMessage(), Toast.Style.ERROR).display();
				}

				JOptionPane.showMessageDialog(Main.this,
						tr("main_restart_to_apply_changes"), tr("main_select_node"),
						JOptionPane.OK_OPTION);
			}
		}

		else if (e.getSource() == explorerSelector) {
			Globals g = Globals.getInstance();

			JComboBox<ExplorerWrapper> explorerCombo = new JComboBox<ExplorerWrapper>();
			explorerCombo.addItem(ExplorerWrapper.burstcoinNetwork());
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
