package btdex.ui.orderbook;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bt.Contract;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.ContractType;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.sc.SellContract;
import btdex.ui.AccountsPanel;
import btdex.ui.Desc;
import btdex.ui.ExplorerButton;
import btdex.ui.HistoryPanel;
import btdex.ui.Icons;
import btdex.ui.Main;
import btdex.ui.RotatingIcon;
import btdex.ui.SocialButton;

public class MarketPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JTable tableBid, tableAsk;
	private DefaultTableModel modelBid, modelAsk;
	private Icon copyIcon, expIcon, cancelIcon, pendingIcon, editIcon;
	private RotatingIcon pendingIconRotating;
	private boolean registering;

	private JComboBox<Market> marketComboBox;
	private JCheckBox listOnlyMine;
	private JLabel lastPrice;
	
	private HistoryPanel historyPanel;
	
	private int ROW_HEIGHT;

	private ArrayList<ContractState> contracts = new ArrayList<>();
	private ArrayList<ContractState> contractsBuy = new ArrayList<>();

	private Market market = null, newMarket;

	private JScrollPane scrollPaneBid;

	private JScrollPane scrollPaneAsk;

	private static Logger logger = LogManager.getLogger();

	public MarketPanel(Main main) {
		super(new BorderLayout());

		marketComboBox = new JComboBox<Market>();
		marketComboBox.setToolTipText(tr("main_select_market"));
		
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, Constants.ICON_SIZE);
		marketComboBox.setFont(largeFont);
		
		for(Market m : Markets.getMarkets()) {
			if(m.getTokenID() == null)
				marketComboBox.addItem(m);
		}
		marketComboBox.addActionListener(this);
		
		this.market = marketComboBox.getItemAt(0);
		Desc priceDesc = new Desc(tr("book_last_price"), lastPrice = new JLabel());
		historyPanel = new HistoryPanel(Main.getInstance(), market, priceDesc);
		lastPrice.setFont(largeFont);
				
		listOnlyMine = new JCheckBox(tr("book_mine_only"));
		listOnlyMine.setFont(largeFont);
		listOnlyMine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.update();
				historyPanel.setFilter(listOnlyMine.isSelected());
			}
		});

		tableBid = new BookTable(modelBid = new TableModelMarket(this, OrderBookSettings.BID_COLS), OrderBookSettings.BID_COLS);
		tableAsk = new BookTable(modelAsk = new TableModelMarket(this, OrderBookSettings.ASK_COLS), OrderBookSettings.ASK_COLS);
		ROW_HEIGHT = tableBid.getRowHeight()+10;
		tableBid.setRowHeight(ROW_HEIGHT);
		tableAsk.setRowHeight(ROW_HEIGHT);
		tableBid.setRowSelectionAllowed(false);
		tableAsk.setRowSelectionAllowed(false);
		tableBid.getTableHeader().setReorderingAllowed(false);
		tableAsk.getTableHeader().setReorderingAllowed(false);

		Icons icons = new Icons(marketComboBox.getForeground(), Constants.ICON_SIZE);
		Icons iconsSmall = new Icons(tableBid.getForeground(), Constants.ICON_SIZE_SMALL);
		copyIcon = iconsSmall.get(Icons.COPY);
		expIcon = iconsSmall.get(Icons.EXPLORER);
		cancelIcon = iconsSmall.get(Icons.CANCEL);
		pendingIcon = iconsSmall.get(Icons.SPINNER);
		pendingIconRotating = new RotatingIcon(pendingIcon);
		editIcon = iconsSmall.get(Icons.EDIT);

		scrollPaneBid = new JScrollPane(tableBid);
		tableBid.setFillsViewportHeight(true);
		scrollPaneAsk = new JScrollPane(tableAsk);
		tableAsk.setFillsViewportHeight(true);

		// Center header and all columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for (int i = 0; i < tableBid.getColumnCount(); i++) {
			tableBid.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );
			tableBid.getColumnModel().getColumn(i).setPreferredWidth(OrderBookSettings.COL_REGULAR);
			tableAsk.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );
			tableAsk.getColumnModel().getColumn(i).setPreferredWidth(OrderBookSettings.COL_REGULAR);
		}
		JTableHeader jtableHeader = tableBid.getTableHeader();
		DefaultTableCellRenderer rend = (DefaultTableCellRenderer) tableBid.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);
		jtableHeader = tableAsk.getTableHeader();
		rend = (DefaultTableCellRenderer) tableAsk.getTableHeader().getDefaultRenderer();
		rend.setHorizontalAlignment(JLabel.CENTER);
		jtableHeader.setDefaultRenderer(rend);

		tableBid.setAutoCreateColumnsFromModel(false);
		tableAsk.setAutoCreateColumnsFromModel(false);

		tableBid.getColumnModel().getColumn(OrderBookSettings.BID_COLS[OrderBookSettings.COL_CONTRACT]).setPreferredWidth(OrderBookSettings.COL_WIDE);
		tableAsk.getColumnModel().getColumn(OrderBookSettings.ASK_COLS[OrderBookSettings.COL_CONTRACT]).setPreferredWidth(OrderBookSettings.COL_WIDE);

		JPanel top = new JPanel(new BorderLayout());
		JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(topLeft, BorderLayout.LINE_START);
		
		topLeft.add(new Desc(tr("main_market"), marketComboBox));
		
		topLeft.add(priceDesc);
		
		topLeft.add(new Desc(tr("book_filtering"), listOnlyMine));
		listOnlyMine.setFont(largeFont);
		
		JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		top.add(topRight, BorderLayout.LINE_END);
		topRight.add(new SocialButton(SocialButton.Type.TWITTER, tableBid.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.INSTAGRAM, table.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.FACEBOOK, table.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.GOOGLE_PLUS, table.getForeground()));

//		buyButton.setBackground(HistoryPanel.GREEN);
//		sellButton.setBackground(HistoryPanel.RED);
//		buyButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				JFrame f = (JFrame) SwingUtilities.getRoot(MarketPanel.this);
//				JDialog dlg = new PlaceOrderDialog(f, market, null, true);
//				dlg.setLocationRelativeTo(MarketPanel.this);
//				dlg.setVisible(true);
//			}
//		});
//		sellButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				JFrame f = (JFrame) SwingUtilities.getRoot(MarketPanel.this);
//				JDialog dlg = new PlaceOrderDialog(f, market, null, false);
//
//				dlg.setLocationRelativeTo(MarketPanel.this);
//				dlg.setVisible(true);
//			}
//		});
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setOpaque(true);
		tabbedPane.setFont(largeFont);

		add(top, BorderLayout.PAGE_START);

		JPanel tablesPanel = new JPanel(new GridLayout(0,2));
		tablesPanel.add(scrollPaneBid);
		tablesPanel.add(scrollPaneAsk);
		add(tabbedPane, BorderLayout.CENTER);
		
		AccountsPanel accountsPanel = new AccountsPanel(Main.getInstance());
		
		tabbedPane.addTab(tr("main_order_book"), icons.get(Icons.ORDER_BOOK), tablesPanel);
		tabbedPane.addTab(tr("main_trade_history"), icons.get(Icons.TRADE), historyPanel);
		tabbedPane.addTab(tr("main_accounts"), icons.get(Icons.ACCOUNT), accountsPanel);

		market = null;
		setMarket(marketComboBox.getItemAt(0));
	}

	public void setMarket(Market m) {
		logger.debug("Market {} set", m.toString());
		newMarket = m;
		historyPanel.setMarket(m);
	}

	public void update() {
		logger.trace("starting update");
		if(newMarket != market) {
			market = newMarket;

			String marketName = market.getTokenID() != null ? market.toString() : Constants.BURST_TICKER;
			String basisCurrency = market.getTokenID() != null ? Constants.BURST_TICKER : market.toString();
			scrollPaneBid.setBorder(BorderFactory.createTitledBorder(null, tr("book_people_buying",
					marketName, basisCurrency), TitledBorder.TRAILING, TitledBorder.DEFAULT_POSITION));
			scrollPaneAsk.setBorder(BorderFactory.createTitledBorder(tr("book_people_selling",
					marketName, basisCurrency)));

			modelBid.setRowCount(0);
			modelBid.fireTableDataChanged();
			modelAsk.setRowCount(0);
			modelAsk.fireTableDataChanged();

			// update the column headers
			for (int c = 0; c < OrderBookSettings.columnNames.length; c++) {
				tableBid.getColumnModel().getColumn(c).setHeaderValue(modelBid.getColumnName(c));
				tableAsk.getColumnModel().getColumn(c).setHeaderValue(modelAsk.getColumnName(c));
			}
			tableBid.getTableHeader().repaint();
			tableAsk.getTableHeader().repaint();
		}

		updateContracts();
		historyPanel.update();
	}
	
	private void updateContracts() {
		Globals g = Globals.getInstance();

		Collection<ContractState> allContracts = Contracts.getContracts();
		contracts.clear();
		contractsBuy.clear();
		boolean onlyMine = listOnlyMine.isSelected();

		for(ContractState s : allContracts) {
			if(s.getType() == ContractType.INVALID)
				continue;

			// add your own contracts (so you can withdraw no matter what)
			// this should never happen on normal circumstances
			if(s.getCreator().equals(g.getAddress()) && s.getBalance().longValue() > 0L && s.getMarket() == 0L) {
				if(s.getType() == ContractType.SELL)
					contracts.add(s);
				else if(s.getType() == ContractType.BUY)
					contractsBuy.add(s);
				continue;
			}

			if(!s.getCreator().equals(g.getAddress()) && (s.getAmountNQT() < Constants.MIN_OFFER || s.getAmountNQT() > Constants.MAX_OFFER))
				continue;

			// only contracts for this market
			if(s.getMarket() != market.getID() || !g.getMediators().areMediatorsAccepted(s))
				continue;

			if(onlyMine && !s.getCreator().equals(g.getAddress()) && s.getTaker()!=g.getAddress().getSignedLongId())
				continue;

			// FIXME: add more validity tests here
			if(s.hasPending() ||
					s.getAmountNQT() > 0 && s.getBalance().longValue() + s.getActivationFee() > s.getSecurityNQT() &&
					s.getRate() > 0 && (s.getMarketAccount() != null || s.getType() == ContractType.BUY) &&
					(s.getState() == SellContract.STATE_OPEN
					|| (s.getState()!= SellContract.STATE_FINISHED && s.getTaker() == g.getAddress().getSignedLongId())
					|| (s.getState()!= SellContract.STATE_FINISHED && s.getCreator().equals(g.getAddress())) ) ) {
				if(s.getType() == ContractType.BUY)
					contractsBuy.add(s);
				else
					contracts.add(s);
			}
		}

		// sort by rate
		contracts.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o1, ContractState o2) {
				int cmp = (int)(o1.getRate() - o2.getRate());
				if(cmp == 0)
					cmp = (int)(o1.getSecurityNQT() - o2.getSecurityNQT());
				return cmp;
			}
		});
		contractsBuy.sort(new Comparator<ContractState>() {
			@Override
			public int compare(ContractState o1, ContractState o2) {
				int cmp = (int)(o2.getRate() - o1.getRate());
				if(cmp == 0)
					cmp = (int)(o1.getSecurityNQT() - o2.getSecurityNQT());
				return cmp;
			}
		});

		modelAsk.setRowCount(contracts.size() + 1);
		modelBid.setRowCount(contractsBuy.size() + 1);
		addContracts(modelAsk, contracts, true);
		addContracts(modelBid, contractsBuy, false);
	}

	private void addContracts(DefaultTableModel model, ArrayList<ContractState> contracts, boolean isAsk) {
		Globals g = Globals.getInstance();
		pendingIconRotating.clearCells(model);

		int []cols = isAsk ? OrderBookSettings.ASK_COLS : OrderBookSettings.BID_COLS;
		
		// Update the contents
		int row = 0;
		
		// Add the "make" buttons
		JButton newOffer = new ActionButton(this, market, tr("book_make_offer"),
				null, null, isAsk, false, false);
		newOffer.setBackground(isAsk ? HistoryPanel.RED : HistoryPanel.GREEN);

		ContractState freeContract = isAsk ? Contracts.getFreeContract() : Contracts.getFreeBuyContract();
		// Flag as registering and turn back only when we have a free contract
		if(Contracts.isRegistering())
			registering = true;
		if(freeContract != null)
			registering = false;
		
		if(Contracts.isLoading()) {
			newOffer.setText(tr("book_loading_button"));
			newOffer.setIcon(pendingIconRotating);
			pendingIconRotating.addCell(model, row, cols[OrderBookSettings.COL_PRICE]);
		}
		else if(registering) {
			newOffer.setText(tr("book_registering"));
			newOffer.setIcon(pendingIconRotating);
			pendingIconRotating.addCell(model, row, cols[OrderBookSettings.COL_PRICE]);
		}
		model.setValueAt(newOffer, row++, cols[OrderBookSettings.COL_PRICE]);
		
		for (int i = 0; i < contracts.size(); i++, row++) {
			ContractState s = contracts.get(i);

			String priceFormated = market.format(s.getRate());
			Icon icon = s.getCreator().equals(g.getAddress()) ? editIcon : null; // takeIcon;
			JButton b = new ActionButton(this, market, "", s, false);
			if(s.hasPending()) {
				if(s.getRate() == 0)
					priceFormated = tr("book_pending_button");
				icon = pendingIconRotating;
				pendingIconRotating.addCell(model, row, cols[OrderBookSettings.COL_PRICE]);
			}
			else if(s.getState() > SellContract.STATE_DISPUTE &&
					(s.getTaker() == g.getAddress().getSignedLongId() || s.getCreator().equals(g.getAddress()))){
				priceFormated = tr("book_dispute_button");
				icon = null;
			}
			else if(s.getTaker() == g.getAddress().getSignedLongId() && s.hasStateFlag(SellContract.STATE_WAITING_PAYMT)) {
				priceFormated = tr(s.getType() == ContractType.BUY ? "book_confirm_dispute_button" : "book_deposit_dispute_button");
				icon = null;
			}
			else if(s.getCreator().equals(g.getAddress()) && s.hasStateFlag(SellContract.STATE_WAITING_PAYMT)) {
				priceFormated = tr(s.getType() == ContractType.BUY ? "book_deposit_dispute_button" : "book_confirm_dispute_button");
				icon = null;
			}
			b.setText(priceFormated);
			b.setIcon(icon);
			b.setBackground(s.getType() == ContractType.BUY ? HistoryPanel.GREEN : HistoryPanel.RED);
			model.setValueAt(b, row, cols[OrderBookSettings.COL_PRICE]);

			if(s.getSecurityNQT() > 0 && s.getAmountNQT() > 0 && s.getRate() > 0 &&
					s.getState() >= SellContract.STATE_OPEN) {
				long securityPercent = s.getSecurityNQT()*100L / s.getAmountNQT();
				String sizeString = s.getAmount() + " (" + securityPercent + "%)";

				model.setValueAt(sizeString, row, cols[OrderBookSettings.COL_SIZE]);
				double amount = ((double)s.getRate())*s.getAmountNQT();
				amount /= Contract.ONE_BURST;
				model.setValueAt(market.format((long)amount), row, cols[OrderBookSettings.COL_TOTAL]);
			}
			else {
				model.setValueAt(null, row, cols[OrderBookSettings.COL_SIZE]);
				model.setValueAt(null, row, cols[OrderBookSettings.COL_TOTAL]);
			}

			ExplorerButton exp = new ExplorerButton(s.getAddress().getRawAddress(), copyIcon, expIcon,
					ExplorerButton.TYPE_ADDRESS, s.getAddress().getID(), s.getAddress().getFullAddress(), null);
			if(s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId()
					&& s.getBalance().longValue() > 0 && s.getState() < SellContract.STATE_WAITING_PAYMT
					&& !s.hasPending()) {
				ActionButton withDrawButton = new ActionButton(this, market, "", s, true);
				withDrawButton.setToolTipText(tr("book_withdraw"));
				withDrawButton.setIcon(cancelIcon);
				exp.add(withDrawButton, BorderLayout.WEST);
			}
			model.setValueAt(exp, row, cols[OrderBookSettings.COL_CONTRACT]);

			//			model.setValueAt(new ExplorerButton(
			//					s.getCreator().getSignedLongId()==g.getAddress().getSignedLongId() ? "YOU" : s.getCreator().getRawAddress(), copyIcon, expIcon,
			//					ExplorerButton.TYPE_ADDRESS, s.getCreator().getID(), s.getCreator().getFullAddress(), OrderBook.BUTTON_EDITOR), row, COL_ACCOUNT);

			//			model.setValueAt(s.getSecurity(), row, COL_SECURITY);
		}
	}

	public Market getMarket() {
		return market;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Market m = (Market) marketComboBox.getSelectedItem();

		if (e.getSource() == marketComboBox) {
			setMarket(m);

			update();
		}
	}
}
