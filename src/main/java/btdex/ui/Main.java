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
import java.net.URI;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bulenkov.darcula.DarculaLaf;

import bt.BT;
import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.core.NumberFormatting;
import btdex.locale.Translation;
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

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	Image icon, iconMono;

	CardLayout cardLayout;
	boolean showingSplash, notifiedLoadingContracts;
	OrderBook orderBook;
	TransactionsPanel transactionsPanel;
	HistoryPanel historyPanel;
	AccountsPanel accountsPanel;

	JTabbedPane tabbedPane;

	JLabel statusLabel;
	JButton nodeSelector, explorerSelector;
	ExplorerWrapper explorer;
	
	private Icon ICON_CONNECTED, ICON_DISCONNECTED, ICON_TESTNET;

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

	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		byte[] entropy = new byte[Words.TWELVE.byteLength()];
		new SecureRandom().nextBytes(entropy);
		new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);
		System.out.println(sb.toString());

		new Main();
	}

	public Main() {
		super("BTDEX" + (Globals.getInstance().isTestnet() ? "-TESTNET" : ""));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		String version = "dev";
		instance = this;
		Icons i = new Icons();
		try {
			icon = i.getIcon();
			setIconImage(icon);
			iconMono = i.getIconMono();

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
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, i.getIconSize());

		Color COLOR = marketComboBox.getForeground();
		i.setColor(COLOR);

		marketComboBox.setToolTipText(tr("main_select_market"));
		marketComboBox.setFont(largeFont);

		JButton versionButton = new JButton(version, i.getVersionIcon());
		versionButton.setToolTipText(tr("main_check_new_release"));
		versionButton.setVerticalAlignment(SwingConstants.CENTER);
		versionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse(Constants.RELEASES_LINK);
			}
		});

		Icon iconBtdex = i.getIconBtdex();
		if(iconMono!=null)
			iconBtdex = new ImageIcon(iconMono);
		JButton webButton = new JButton(iconBtdex);
		bottomRight.add(webButton, BorderLayout.LINE_END);
		webButton.setToolTipText(tr("main_open_website"));
		webButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse(Constants.WEBSITE_LINK);
			}
		});

		JButton discordButton = new JButton(i.getDiscordIcon());
		bottomRight.add(discordButton, BorderLayout.LINE_END);
		discordButton.setToolTipText(tr("main_chat_discord"));
		discordButton.setVerticalAlignment(SwingConstants.CENTER);
		discordButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse(Constants.DISCORD_LINK);
			}
		});

		JButton githubButton = new JButton(i.getGithubIcon());
		bottomRight.add(githubButton, BorderLayout.LINE_END);
		githubButton.setToolTipText(tr("main_check_source"));
		githubButton.setVerticalAlignment(SwingConstants.CENTER);
		githubButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browse(Constants.GITHUB_LINK);
			}
		});

		JButton signoutButton = new JButton(i.getSignoutIcon());
		signoutButton.setToolTipText(tr("main_exit_and_clear"));
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

		JButton resetPinButton = new JButton(i.getResetPinIcon());
		resetPinButton.setToolTipText(tr("main_reset_pin"));
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
		historyPanel = new HistoryPanel(this, (Market) marketComboBox.getSelectedItem(), orderBook);
		accountsPanel = new AccountsPanel(this);

		ICON_CONNECTED = i.getICON_CONNECTED();
		ICON_TESTNET = i.getICON_TESTNET();
		ICON_DISCONNECTED = i.getICON_DISCONNECTED();

		copyAddButton = new ExplorerButton("", i.getCopyIcon(), i.getExpIcon());
		copyAddButton.getMainButton().setFont(largeFont);

		JButton settingsButton = new JButton(i.getSettingsIcon());
		settingsButton.setToolTipText(tr("main_configure_settings"));
		settingsButton.setFont(largeFont);
		settingsButton.setVisible(false);

		JButton langButton = new JButton(i.getLangIcon());
		langButton.setToolTipText(tr("main_change_language"));
		langButton.setFont(largeFont);
		langButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JPopupMenu menu = new JPopupMenu();
				for(Locale l : Translation.getSupportedLanguages()) {
					JMenuItem item = new JMenuItem(l.getDisplayLanguage(l));
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							g.setLanguage(l.getLanguage());
							try {
								g.saveConfs();
							} catch (Exception e1) {
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

		sendButton = new JButton(i.getSendIcon());
		sendButton.setToolTipText(tr("main_send", "BURST"));
		sendButton.addActionListener(this);

		sendButtonToken = new JButton(i.getSendIcon());
		sendButtonToken.setToolTipText(tr("main_send", token.toString()));
		sendButtonToken.addActionListener(this);

		content.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);

		tabbedPane.addTab(tr("main_order_book"), i.getOrderIcon(), orderBook);
		tabbedPane.addTab(tr("main_trade_history"), i.getTradeIcon(), historyPanel);

		if(g.isTestnet()) {
			// FIXME: accounts on testnet only for now
			tabbedPane.addTab(tr("main_accounts"), i.getAccountIcon(), accountsPanel);
			tabbedPane.addTab(tr("main_chat"), i.getChatIcon(), new ChatPanel());
		}

		tabbedPane.addTab(tr("main_transactions"), i.getTransactionsIcon(), transactionsPanel);

		top.add(new Desc(tr("main_market"), marketComboBox));
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


		topRight.add(new Desc("  ", settingsButton));
		topRight.add(new Desc(tr("main_language_name"), langButton));

		nodeSelector = new JButton(g.getNode());
		nodeSelector.setToolTipText(tr("main_select_node"));
		nodeSelector.addActionListener(this);

		explorer = ExplorerWrapper.getExplorer(g.getExplorer());
		explorerSelector = new JButton(explorer.toString(),
				i.getExpIcon());
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
	}

	public void browse(String url) {
		try {
			DesktopApi.browse(new URI(url));
			Toast.makeText(Main.this, tr("main_opening_url", url.substring(0, Math.min(url.length(), 40)) + "..."), Toast.Style.SUCCESS).display();
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

	private void updateUI() {
		// Update at every 10 seconds
		if(System.currentTimeMillis() - lastUpdated < Constants.UI_UPDATE_INTERVAL) {
			return;
		}
		lastUpdated = System.currentTimeMillis();

		long balance = 0, locked = 0;
		try {
			Globals g = Globals.getInstance();
			BurstNode bn = BurstNode.getInstance();
			
			Exception nodeException = bn.getNodeException();
			if(nodeException != null) {
				nodeSelector.setIcon(ICON_DISCONNECTED);
				String errorMessage = tr("main_error", nodeException.getLocalizedMessage());
				statusLabel.setText(errorMessage);
				if(showingSplash) {
					showingSplash = false;
					pulsingButton.stopPulsing();
					cardLayout.first(getContentPane());
					update();
				}
				return;
			}

			// Check if the node has the expected block
			if(g.isTestnet()) {
				Block checkBlock = bn.getCheckBlock();
				if(checkBlock == null)
					return;
				if(checkBlock.getHeight() != Constants.CHECK_HEIGHT_TESTNET) {
					String error = tr("main_invalid_testnet_node", g.getNode());
					Toast.makeText(Main.this, error, Toast.Style.ERROR).display();

					nodeSelector.setIcon(ICON_DISCONNECTED);
					statusLabel.setText(error);
				}
			}
			
			Account ac = bn.getAccount();
			if(ac == null)
				return;
			balance = ac.getBalance().longValue();
			// Locked value in *market* and possibly other Burst coin stuff.
			locked = balance - ac.getUnconfirmedBalance().longValue();
			
			// Add the amounts on smart contract trades on the locked balance
			for(ContractState s : Contracts.getContracts()) {
				if(s.getState() == SellContract.STATE_FINISHED)
					continue;
				if(s.getCreator().equals(g.getAddress())){
					if(s.getType() == ContractState.Type.SELL)
						locked += s.getAmountNQT() + s.getSecurityNQT();
					else if(s.getType() == ContractState.Type.BUY)
						locked += s.getSecurityNQT();
				}
				else if (s.getTaker() == g.getAddress().getSignedLongId()) {
					if(s.getType() == ContractState.Type.SELL)
						locked += s.getAmountNQT();
					else if(s.getType() == ContractState.Type.BUY)
						locked += s.getSecurityNQT() + s.getSecurityNQT();					
				}
			}

			balance -= locked;
			balanceLabel.setText(NumberFormatting.BURST.format(balance));
			lockedBalanceLabel.setText(tr("main_plus_locked", NumberFormatting.BURST.format(locked)));

			if(transactionsPanel.isVisible() || showingSplash)
				transactionsPanel.update();
			if(orderBook.isVisible() || historyPanel.isVisible() || showingSplash) {
				orderBook.update();
				historyPanel.update();
			}

			Market tokenMarket = token;
			Market m = (Market) marketComboBox.getSelectedItem();
			if(m.getTokenID()!=null && m!=token)
				tokenMarket = m;
			AssetBalance[] accounts = bn.getAssetBalances(tokenMarket);
			if(accounts == null)
				return; // not yet retrieved from node
			
			long tokenBalance = 0;
			long tokenLocked = 0;
			for (AssetBalance aac : accounts) {
				if(aac.getAccountAddress().getSignedLongId() == g.getAddress().getSignedLongId()) {
					tokenBalance += aac.getBalance().longValue();
				}
			}

			AssetOrder[] asks = bn.getAssetAsks(token);
			for(AssetOrder o : asks) {
				if(o.getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
					continue;
				tokenLocked += o.getQuantity().longValue();
			}
			tokenBalance -= tokenLocked;

			balanceLabelToken.setText(token.format(tokenBalance));
			balanceLabelTokenPending.setText(tr("main_plus_locked", token.format(tokenLocked)));

			statusLabel.setText("");
			nodeSelector.setIcon(g.isTestnet() ? ICON_TESTNET : ICON_CONNECTED);
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
		Market m = (Market) marketComboBox.getSelectedItem();
		if (e.getSource() == marketComboBox) {
			orderBook.setMarket(m);
			historyPanel.setMarket(m);
			
			if(m.getTokenID() == null) {
				// not a token market, show TRT in the token field 
				tokenDesc.setDesc(tr("main_balance", token));
				
				if(!Globals.getInstance().isTestnet()) {
					// FIXME: remove this when operational
					Toast.makeText(this, tr("main_cross_chain_testnet_only"), Toast.Style.ERROR).display();
				}
				else if(Contracts.isLoading()) {
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
			
			String[] list = {BT.NODE_BURSTCOIN_RO, BT.NODE_BURST_ALLIANCE,
					BT.NODE_BURST_TEAM, Constants.NODE_LOCALHOST};
			if(g.isTestnet()){
				list = new String[]{BT.NODE_TESTNET, BT.NODE_TESTNET_MEGASH, BT.NODE_LOCAL_TESTNET };
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
			explorerCombo.addItem(ExplorerWrapper.burstDevtrue());
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
}
