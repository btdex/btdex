package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
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
import burst.kit.entity.response.Account;
import burst.kit.entity.response.AssetAccount;
import burst.kit.entity.response.http.BRSError;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import okhttp3.Response;

public class Main extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	OrderBook orderBook;
	TransactionsPanel transactionsPanel;
	HistoryPanel historyPanel;
	AccountsPanel accountsPanel;

	JTabbedPane tabbedPane;

	JLabel nodeStatus;
	
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
		
		try {
			Image image = ImageIO.read(Main.class.getResourceAsStream("/icon.png"));
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
		
		Globals g = Globals.getInstance();
		
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
		
		for(Market m : g.getMarkets())
			marketComboBox.addItem(m);
		token = g.getToken();

		marketComboBox.addActionListener(this);
		orderBook = new OrderBook(this, (Market) marketComboBox.getSelectedItem());
		
		transactionsPanel = new TransactionsPanel();
		historyPanel = new HistoryPanel(this, (Market) marketComboBox.getSelectedItem());
		accountsPanel = new AccountsPanel(this);

		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 18, COLOR);
		copyAddButton = new CopyToClipboardButton("", copyIcon);
		copyAddButton.setToolTipText("Copy your Burst address to clipboard");
		copyAddButton.setFont(largeFont);

		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, 18, COLOR);
		JButton settingsButton = new JButton(settinsIcon);
		settingsButton.setToolTipText("Configure settings...");
		settingsButton.setFont(largeFont);
		settingsButton.setVisible(false);

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

		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, 18, COLOR);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, historyPanel);

		Icon accountIcon = IconFontSwing.buildIcon(FontAwesome.USER_CIRCLE, 18, COLOR);
		tabbedPane.addTab("ACCOUNTS", accountIcon, accountsPanel);

		Icon transactionsIcon = IconFontSwing.buildIcon(FontAwesome.LINK, 18, COLOR);
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
		top.add(tokenDesc = new Desc(String.format("Balance (%s)", token), balanceLabelToken, balanceLabelTokenPending));
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
		

		// check if this is a known account
		try {
			g.getNS().getAccount(g.getAddress()).blockingGet();
		}
		catch (Exception e) {
			if(e.getCause() instanceof BRSError) {
				BRSError error = (BRSError) e.getCause();
				if(error.getCode() == 5) {
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
						setCursor(Cursor.getDefaultCursor());
					//}
				}
			}
		}

		update();
		Thread updateThread = new UpdateThread();
		updateThread.start();
	}
	
	/**
	 * Signal an update should take place.
	 */
	public void update() {
		lastUpdated = 0;
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
					
					transactionsPanel.update();
					orderBook.update();
					historyPanel.update();
					
					Market tokenMarket = token;
					Market m = (Market) marketComboBox.getSelectedItem();
					if(m.getTokenID()!=null && m!=token)
						tokenMarket = m;
					AssetAccount[] accounts = g.getNS().getAssetAccounts(tokenMarket.getTokenID()).blockingGet();
					long tokenBalance = 0;
					for (AssetAccount aac : accounts) {
						if(aac.getAccount().getSignedLongId() == g.getAddress().getSignedLongId()) {
							tokenBalance += aac.getQuantity().longValue();
						}
					}
					balanceLabelToken.setText(token.format(tokenBalance));
					if(tokenMarket == token)
						balanceLabelTokenPending.setText("+" + token.format(0) + " pending");
					else
						balanceLabelTokenPending.setText(" ");

					nodeStatus.setText("Node: " + Globals.getConf().getProperty(Globals.PROP_NODE));
				}
				catch (RuntimeException rex) {
					rex.printStackTrace();
					
					Toast.makeText(Main.this, rex.getMessage(), Toast.Style.ERROR).display();

					nodeStatus.setText(rex.getMessage());
				}
				setCursor(Cursor.getDefaultCursor());
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
	}
}
