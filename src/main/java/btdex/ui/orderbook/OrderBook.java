package btdex.ui.orderbook;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import bt.Contract;
import btdex.core.*;
import btdex.sc.SellContract;
import btdex.ui.*;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.attachment.AskOrderCancellationAttachment;
import burst.kit.entity.response.attachment.AskOrderPlacementAttachment;
import burst.kit.entity.response.attachment.BidOrderCancellationAttachment;
import burst.kit.entity.response.attachment.BidOrderPlacementAttachment;

public class OrderBook extends JPanel {

	private static final long serialVersionUID = 1L;

	private JTable tableBid, tableAsk;
	private DefaultTableModel modelBid, modelAsk;
	private Icon copyIcon, expIcon, cancelIcon, pendingIcon, editIcon, withdrawIcon;
	private RotatingIcon pendingIconRotating;

	private JCheckBox listOnlyMine;
	private JLabel lastPrice;
	private JButton buyButton, sellButton;
	private AssetOrder firstBid, firstAsk;

	private int ROW_HEIGHT;

	private ArrayList<ContractState> contracts = new ArrayList<>();
	private ArrayList<ContractState> contractsBuy = new ArrayList<>();

	Market market = null, newMarket;

	private JScrollPane scrollPaneBid;

	private JScrollPane scrollPaneAsk;

	public static final ButtonCellRenderer BUTTON_RENDERER = new ButtonCellRenderer();
	public static final ButtonCellEditor BUTTON_EDITOR = new ButtonCellEditor();

	public OrderBook(Main main, Market m) {
		super(new BorderLayout());

		listOnlyMine = new JCheckBox(tr("book_mine_only"));
		listOnlyMine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				main.update();
			}
		});

		market = m;

		tableBid = new MyTable(modelBid = new MyTableModel(this, OrderBookSettings.BID_COLS), OrderBookSettings.BID_COLS);
		tableAsk = new MyTable(modelAsk = new MyTableModel(this, OrderBookSettings.ASK_COLS), OrderBookSettings.ASK_COLS);
		ROW_HEIGHT = tableBid.getRowHeight()+10;
		tableBid.setRowHeight(ROW_HEIGHT);
		tableAsk.setRowHeight(ROW_HEIGHT);
		tableBid.setRowSelectionAllowed(false);
		tableAsk.setRowSelectionAllowed(false);
		tableBid.getTableHeader().setReorderingAllowed(false);
		tableAsk.getTableHeader().setReorderingAllowed(false);

		Icons ics = new Icons(tableBid.getForeground(), 12);
		copyIcon = ics.get(Icons.COPY);
		expIcon = ics.get(Icons.EXPLORER);
		cancelIcon = ics.get(Icons.CANCEL);
		pendingIcon = ics.get(Icons.SPINNER);
		pendingIconRotating = new RotatingIcon(pendingIcon);
		editIcon = ics.get(Icons.EDIT);
		withdrawIcon = ics.get(Icons.WITHDRAW);

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
		topLeft.add(lastPrice = new JLabel());
		lastPrice.setToolTipText(tr("book_last_price"));
		topLeft.add(buyButton = new JButton());
		topLeft.add(sellButton = new JButton());
		topLeft.add(listOnlyMine);

		JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		top.add(topRight, BorderLayout.LINE_END);
		topRight.add(new SocialButton(SocialButton.Type.TWITTER, tableBid.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.INSTAGRAM, table.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.FACEBOOK, table.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.GOOGLE_PLUS, table.getForeground()));

		buyButton.setBackground(HistoryPanel.GREEN);
		sellButton.setBackground(HistoryPanel.RED);
		buyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
				JDialog dlg = market.getTokenID()!= null ? new PlaceTokenOrderDialog(f, market, firstAsk):
						new PlaceOrderDialog(f, market, null, true);
				dlg.setLocationRelativeTo(OrderBook.this);
				dlg.setVisible(true);
			}
		});
		sellButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);
				JDialog dlg = market.getTokenID()!= null ? new PlaceTokenOrderDialog(f, market, firstBid)
						: new PlaceOrderDialog(f, market, null, false);

				dlg.setLocationRelativeTo(OrderBook.this);
				dlg.setVisible(true);
			}
		});

		add(top, BorderLayout.PAGE_START);

		JPanel tablesPanel = new JPanel(new GridLayout(0,2));
		tablesPanel.add(scrollPaneBid);
		tablesPanel.add(scrollPaneAsk);
		add(tablesPanel, BorderLayout.CENTER);

		market = null;
		setMarket(m);
	}

	public class ActionButton extends JButton{

		private static final long serialVersionUID = 1L;
		private AssetOrder order;
		private ContractState contract;
		private boolean isToken;

		private boolean cancel;

		public ActionButton(String text, ContractState contract, boolean cancel) {
			this(text, null, contract, cancel, false);
		}

		public ActionButton(String text, AssetOrder order, boolean cancel) {
			this(text, order, null, cancel, true);
		}
		public ActionButton(String text, AssetOrder order, ContractState contract, boolean cancel, boolean isToken) {
			super(text);
			this.order = order;
			this.contract = contract;
			this.cancel = cancel;
			this.isToken = isToken;

			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFrame f = (JFrame) SwingUtilities.getRoot(OrderBook.this);

					if((isToken && order.getAssetId() == null) ||
							(!isToken && contract.hasPending())) {
						JOptionPane.showMessageDialog(getParent(), tr("offer_wait_confirm"),
								tr("offer_processing"), JOptionPane.WARNING_MESSAGE);
						return;
					}

					JDialog dlg = null;
					if(cancel) {
						dlg = new CancelOrderDialog(f, market, order, contract);
					}
					else {
						if(isToken)
							dlg = new PlaceTokenOrderDialog(f, market, order);
						else
							dlg = new PlaceOrderDialog(f, market, contract, false);
					}
					dlg.setLocationRelativeTo(OrderBook.this);
					dlg.setVisible(true);

					BUTTON_EDITOR.stopCellEditing();
				}
			});
		}

	}

	public void setMarket(Market m) {
		newMarket = m;
	}

	public void setLastPrice(String price, Icon icon, Color color) {
		lastPrice.setText(price);
		lastPrice.setIcon(icon);
		lastPrice.setForeground(color);
	}

	public void update() {
		if(newMarket != market) {
			market = newMarket;
			
			String marketName = market.getTokenID() != null ? market.toString() : "BURST";
			String basisCurrency = market.getTokenID() != null ? "BURST" : market.toString(); 
			scrollPaneBid.setBorder(BorderFactory.createTitledBorder(null, tr("book_people_buying",
					marketName, basisCurrency), TitledBorder.TRAILING, TitledBorder.DEFAULT_POSITION));
			scrollPaneAsk.setBorder(BorderFactory.createTitledBorder(tr("book_people_selling",
					marketName, basisCurrency)));
			
			modelBid.setRowCount(0);
			modelBid.fireTableDataChanged();
			modelAsk.setRowCount(0);
			modelAsk.fireTableDataChanged();
			
			if(market.getTokenID() != null) {
				buyButton.setText(tr("book_buy_button", market));
				sellButton.setText(tr("book_sell_button", market));
			}
			else {
				buyButton.setText(tr("book_buy_button", "BURST"));
				sellButton.setText(tr("book_sell_button", "BURST"));
			}
			
			// update the column headers
			for (int c = 0; c < OrderBookSettings.columnNames.length; c++) {
				tableBid.getColumnModel().getColumn(c).setHeaderValue(modelBid.getColumnName(c));
				tableAsk.getColumnModel().getColumn(c).setHeaderValue(modelAsk.getColumnName(c));
			}
			tableBid.getTableHeader().repaint();
			tableAsk.getTableHeader().repaint();
		}

		if(market.getTokenID()==null) {
			updateContracts();
		}
		else
			updateOrders();
	}

	private void updateOrders() {
		Globals g = Globals.getInstance();
		BurstNode bn = BurstNode.getInstance();

		boolean onlyMine = listOnlyMine.isSelected();

		ArrayList<AssetOrder> bidOrders = new ArrayList<>();
		ArrayList<AssetOrder> askOrders = new ArrayList<>();
		AssetOrder[] bids = bn.getAssetBids(market);
		AssetOrder[] asks = bn.getAssetAsks(market);
		
		for (int i = 0; asks != null && i < asks.length; i++) {
			if(onlyMine && asks[i].getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
				continue;
			askOrders.add(asks[i]);
		}
		for (int i = 0; bids != null && i < bids.length; i++) {
			if(onlyMine && bids[i].getAccountAddress().getSignedLongId() != g.getAddress().getSignedLongId())
				continue;
			bidOrders.add(bids[i]);
		}
		
		// Check for unconfirmed transactions with asset stuff
		Transaction[] utx = BurstNode.getInstance().getUnconfirmedTransactions();
		if(utx == null)
			return;
		for(Transaction tx : utx) {
			if(!tx.getSender().equals(g.getAddress()))
				continue;
			if(tx.getType() == 2) {
				switch (tx.getSubtype()) {
				case 2: // Ask Offer
					if(tx.getAttachment() instanceof AskOrderPlacementAttachment) {
						AskOrderPlacementAttachment order = (AskOrderPlacementAttachment) tx.getAttachment();
						if(order.getAsset().equals(market.getTokenID().getID()))
							askOrders.add(new AssetOrder(null, null, g.getAddress(),
									BurstValue.fromPlanck(order.getQuantityQNT()), BurstValue.fromPlanck(order.getPriceNQT()),
									tx.getBlockHeight(), AssetOrder.OrderType.ASK));
					}
					break;
				case 3: // Bid offer
					if(tx.getAttachment() instanceof BidOrderPlacementAttachment) {
						BidOrderPlacementAttachment order = (BidOrderPlacementAttachment) tx.getAttachment();
						if(order.getAsset().equals(market.getTokenID().getID()))
							bidOrders.add(new AssetOrder(null, null, g.getAddress(),
								BurstValue.fromPlanck(order.getQuantityQNT()), BurstValue.fromPlanck(order.getPriceNQT()),
								tx.getBlockHeight(), AssetOrder.OrderType.BID));
					}
					break;
				case 4: // Cancel ask
					if(tx.getAttachment() instanceof AskOrderCancellationAttachment) {
						AskOrderCancellationAttachment order = (AskOrderCancellationAttachment) tx.getAttachment();
						// we replace it with an order without the asset ID
						for (int i = 0; i < askOrders.size(); i++) {
							AssetOrder o = askOrders.get(i);
							if(o.getId()!=null && o.getId().getID().equals(order.getOrder())) {
								askOrders.set(i, new AssetOrder(o.getId(), null, o.getAccountAddress(), o.getQuantity(), o.getPrice(),
										o.getHeight(), o.getType()));
							}
						}
					}
					break;
				case 5: // Cancel bid
					if(tx.getAttachment() instanceof BidOrderCancellationAttachment) {
						BidOrderCancellationAttachment order = (BidOrderCancellationAttachment) tx.getAttachment();
						// we replace it with an order without the asset ID
						for (int i = 0; i < bidOrders.size(); i++) {
							AssetOrder o = bidOrders.get(i);
							if(o.getId()!=null && o.getId().getID().equals(order.getOrder())) {
								bidOrders.set(i, new AssetOrder(o.getId(), null, o.getAccountAddress(), o.getQuantity(), o.getPrice(),
										o.getHeight(), o.getType()));
							}
						}
					}
					break;
				}
			}
		}

		// sort by price
		bidOrders.sort(new Comparator<AssetOrder>() {
			@Override
			public int compare(AssetOrder o1, AssetOrder o2) {
				int cmp = (int)(o2.getPrice().longValue() - o1.getPrice().longValue());
				if(cmp == 0)
					cmp = o1.getHeight() - o2.getHeight();
				return cmp;
			}
		});
		askOrders.sort(new Comparator<AssetOrder>() {
			@Override
			public int compare(AssetOrder o1, AssetOrder o2) {
				int cmp = (int)(o1.getPrice().longValue() - o2.getPrice().longValue());
				if(cmp == 0)
					cmp = o1.getHeight() - o2.getHeight();
				return cmp;
			}
		});

		firstBid = bidOrders.size() > 0 ? bidOrders.get(0) : null;
		firstAsk = askOrders.size() > 0 ? askOrders.get(0) : null;

		modelBid.setRowCount(bidOrders.size());
		modelAsk.setRowCount(askOrders.size());

		addOrders(modelBid, bidOrders, OrderBookSettings.BID_COLS);
		addOrders(modelAsk, askOrders, OrderBookSettings.ASK_COLS);
	}

	private void addOrders(DefaultTableModel model, ArrayList<AssetOrder> orders, int[] cols) {
		Globals g = Globals.getInstance();
		pendingIconRotating.clearCells(model);
		int row = 0;
		for (; row < orders.size(); row++) {
			AssetOrder o = orders.get(row);

			// price always come in Burst, so no problem in this division using long's
			long price = o.getPrice().longValue();
			long amountToken = o.getQuantity().longValue();

			if(price == 0 || amountToken == 0)
				continue;

			String priceFormated = NumberFormatting.BURST.format(price*market.getFactor());
			JButton b = new ActionButton(priceFormated, o, false);
			if(o.getAccountAddress().equals(g.getAddress()) && o.getAssetId() != null) {
				b = new ActionButton(priceFormated, o, true);
				b.setIcon(cancelIcon);
			}
			if(o.getId() == null || o.getAssetId() == null) {
				b.setIcon(pendingIconRotating);
				pendingIconRotating.addCell(model, row, cols[OrderBookSettings.COL_PRICE]);
			}
			b.setBackground(o.getType() == AssetOrder.OrderType.ASK ? HistoryPanel.RED : HistoryPanel.GREEN);
			model.setValueAt(b, row, cols[OrderBookSettings.COL_PRICE]);

			model.setValueAt(market.format(amountToken), row, cols[OrderBookSettings.COL_SIZE]);
			model.setValueAt(NumberFormatting.BURST.format(amountToken*price), row, cols[OrderBookSettings.COL_TOTAL]);

			ExplorerButton exp = null;
			if(o.getId()!=null) {
				exp = new ExplorerButton(o.getId().getID(), copyIcon, expIcon, BUTTON_EDITOR);
				if(o.getAccountAddress().equals(g.getAddress()) && o.getAssetId() != null) {
					JButton cancel = new ActionButton("", o, true);
					cancel.setIcon(cancelIcon);
					exp.add(cancel, BorderLayout.WEST);
				}
			}
			model.setValueAt(exp, row, cols[OrderBookSettings.COL_CONTRACT]);
		}
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
			
			// add your own contracts but not yet configured if they have balance (so you can withdraw)
			// this should never happen on normal circumstances
			if(s.getCreator().equals(g.getAddress()) && s.getMarket() == 0 && s.getBalance().longValue() > 0L) {
				if(s.getType() == ContractType.SELL)
					contracts.add(s);
				else if(s.getType() == ContractType.BUY)
					contractsBuy.add(s);
				continue;
			}

			// only contracts for this market
			if(s.getMarket() != market.getID())
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

		modelAsk.setRowCount(contracts.size());
		modelBid.setRowCount(contractsBuy.size());
		addContracts(modelAsk, contracts, OrderBookSettings.ASK_COLS);
		addContracts(modelBid, contractsBuy, OrderBookSettings.BID_COLS);
	}
	
	private void addContracts(DefaultTableModel model, ArrayList<ContractState> contracts, int []cols) {
		Globals g = Globals.getInstance();
		pendingIconRotating.clearCells(model);

		// Update the contents
		int row = 0;
		for (; row < contracts.size(); row++) {
			ContractState s = contracts.get(row);

			String priceFormated = market.format(s.getRate());
			Icon icon = s.getCreator().equals(g.getAddress()) ? editIcon : null; // takeIcon;
			JButton b = new ActionButton("", s, false);
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

			if(s.getSecurityNQT() > 0 && s.getAmountNQT() > 0 && s.getRate() > 0) {
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
					ExplorerButton.TYPE_ADDRESS, s.getAddress().getID(), s.getAddress().getFullAddress(), BUTTON_EDITOR); 
			if(s.getCreator().getSignedLongId() == g.getAddress().getSignedLongId()
					&& s.getBalance().longValue() > 0 && s.getState() < SellContract.STATE_WAITING_PAYMT
					&& !s.hasPending()) {
				ActionButton withDrawButton = new ActionButton("", s, true);
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
}
