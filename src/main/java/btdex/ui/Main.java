package btdex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import btdex.core.Config;
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

	public Main() {
		super("BTDEX");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		IconFontSwing.register(FontAwesome.getIconFont());
		
		try {
			Class<?> lafc = null;
			try {
				lafc = Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				lafc = Class.forName("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			}
			LookAndFeel laf = (LookAndFeel) lafc.getConstructor().newInstance();
			UIManager.setLookAndFeel(laf);
		} catch (Exception e) {
		}

		orderBook = new OrderBook();
		
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		getContentPane().add(top, BorderLayout.PAGE_START);
		
		JComboBox<Market> marketComboBox = new JComboBox<>();
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, 18);
		marketComboBox.setFont(largeFont);
		
		marketComboBox.addItem(new MarketBTC());
		marketComboBox.addItem(new MarketETH());
		marketComboBox.addItem(new MarketLTC());
		
		Icon copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, 18);
		JButton copyAddButton = new JButton(address.getFullAddress(), copyIcon);
		copyAddButton.setFont(largeFont);
		
		Icon settinsIcon = IconFontSwing.buildIcon(FontAwesome.COG, 18);
		JButton settingsButton = new JButton("Settings", settinsIcon);
		settingsButton.setFont(largeFont);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setFont(largeFont);
		
		Icon orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, 18);
		tabbedPane.addTab("ORDER BOOK", orderIcon, orderBook);
		
		Icon ongoinIcon = IconFontSwing.buildIcon(FontAwesome.HANDSHAKE_O, 18);
		tabbedPane.addTab("ONGOING TRADES", ongoinIcon, new JLabel());
		
		Icon tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, 18);
		tabbedPane.addTab("TRADE HISTORY", tradeIcon, new JLabel());
		
		Account ac = Globals.NS.getAccount(address).blockingGet();
		JLabel balanceLabel = new JLabel(ac.getBalance().toFormattedString());
		balanceLabel.setFont(largeFont);

//		JLabel marketLabel = new JLabel("Market: ");
//		marketLabel.setFont(largeFont);
//		top.add(marketLabel);
		top.add(marketComboBox);
		
		top.add(copyAddButton);
		top.add(balanceLabel);
		top.add(settingsButton);

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public static void main(String[] args) {
		new Main();
	}
}
