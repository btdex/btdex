package btdex.ui.orderbook;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.core.NumberFormatting;
import btdex.markets.MarketBTC;
import btdex.markets.MarketBurstToken;
import btdex.ui.CreateTokenDialog;
import btdex.ui.Desc;
import btdex.ui.ExplorerButton;
import btdex.ui.HistoryPanel;
import btdex.ui.Icons;
import btdex.ui.Main;
import btdex.ui.RotatingIcon;
import btdex.ui.SendDialog;
import btdex.ui.SocialButton;
import btdex.ui.Toast;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetBalance;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.Transaction;
import burst.kit.entity.response.attachment.AskOrderCancellationAttachment;
import burst.kit.entity.response.attachment.AskOrderPlacementAttachment;
import burst.kit.entity.response.attachment.BidOrderCancellationAttachment;
import burst.kit.entity.response.attachment.BidOrderPlacementAttachment;

public class TokenMarketPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JTable tableBid, tableAsk;
	private DefaultTableModel modelBid, modelAsk;
	private Icon copyIcon, expIcon, cancelIcon, pendingIcon;
	private RotatingIcon pendingIconRotating;

	private JCheckBox listOnlyMine;
	private JLabel lastPrice;
	
	private ExplorerButton tokenIdButton;

	private int ROW_HEIGHT;

	private Market token;
	private JComboBox<Market> marketComboBox;
	private JButton removeTokenButton;
	private Market addMarketDummy, newMarketDummy;
	
	private Desc tokenDesc;
	private JLabel balanceLabelToken;
	private JLabel balanceLabelTokenPending;
	private JButton sendButtonToken;

	private Market market = null, newMarket;
	
	private HistoryPanel historyPanel;
	
	private JScrollPane scrollPaneBid;

	private JScrollPane scrollPaneAsk;

	private AssetOrder firstBid;

	private AssetOrder firstAsk;

	private static Logger logger = LogManager.getLogger();

	public static final ButtonCellRenderer BUTTON_RENDERER = new ButtonCellRenderer();

	public static final ButtonCellEditor BUTTON_EDITOR = new ButtonCellEditor();
	public TokenMarketPanel(Main main) {
		super(new BorderLayout());

		listOnlyMine = new JCheckBox(tr("book_mine_only"));
		listOnlyMine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				historyPanel.setFilter(listOnlyMine.isSelected());
				main.update();
			}
		});
		
		marketComboBox = new JComboBox<Market>();
		marketComboBox.setToolTipText(tr("main_select_market"));
		
		for(Market m : Markets.getMarkets()) {
			if(m.getTokenID() != null)
				marketComboBox.addItem(m);
		}
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
		
		Icons iconsSmall = new Icons(marketComboBox.getForeground(), Constants.ICON_SIZE_SMALL);
		Icons icons = new Icons(marketComboBox.getForeground(), Constants.ICON_SIZE);
		Font largeFont = marketComboBox.getFont().deriveFont(Font.BOLD, Constants.ICON_SIZE);
		marketComboBox.setFont(largeFont);
		
		removeTokenButton = new JButton(icons.get(Icons.TRASH));
		removeTokenButton.setToolTipText(tr("main_remove_token_tip"));
		removeTokenButton.addActionListener(this);
		removeTokenButton.setVisible(false);
		
		sendButtonToken = new JButton(icons.get(Icons.SEND));
		sendButtonToken.setToolTipText(tr("main_send", token.toString()));
		sendButtonToken.addActionListener(this);

		balanceLabelToken = new JLabel("0");
		balanceLabelToken.setFont(largeFont);
		balanceLabelToken.setToolTipText(tr("main_available_balance"));
		balanceLabelTokenPending = new JLabel("0");
		balanceLabelTokenPending.setToolTipText(tr("main_amount_locked"));
		
		this.market = marketComboBox.getItemAt(0);
		Desc priceDesc = new Desc(tr("book_last_price"), lastPrice = new JLabel());
		historyPanel = new HistoryPanel(Main.getInstance(), market, priceDesc);
		lastPrice.setFont(largeFont);
		
		tableBid = new BookTable(modelBid = new TableModelToken(this, OrderBookSettings.BID_COLS), OrderBookSettings.BID_COLS);
		tableAsk = new BookTable(modelAsk = new TableModelToken(this, OrderBookSettings.ASK_COLS), OrderBookSettings.ASK_COLS);
		ROW_HEIGHT = tableBid.getRowHeight()+10;
		tableBid.setRowHeight(ROW_HEIGHT);
		tableAsk.setRowHeight(ROW_HEIGHT);
		tableBid.setRowSelectionAllowed(false);
		tableAsk.setRowSelectionAllowed(false);
		tableBid.getTableHeader().setReorderingAllowed(false);
		tableAsk.getTableHeader().setReorderingAllowed(false);

		copyIcon = iconsSmall.get(Icons.COPY);
		expIcon = iconsSmall.get(Icons.EXPLORER);
		cancelIcon = iconsSmall.get(Icons.CANCEL);
		pendingIcon = iconsSmall.get(Icons.SPINNER);
		pendingIconRotating = new RotatingIcon(pendingIcon);

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
		topLeft.add(new Desc("  ", removeTokenButton));		
		topLeft.add(tokenDesc = new Desc(tr("main_balance", token), balanceLabelToken, balanceLabelTokenPending));
		topLeft.add(new Desc("  ", sendButtonToken));
		
		topLeft.add(priceDesc);
		
		topLeft.add(new Desc(tr("book_filtering"), listOnlyMine));
		listOnlyMine.setFont(largeFont);
				
		tokenIdButton = new ExplorerButton("", icons.get(Icons.COPY), icons.get(Icons.EXPLORER));

		JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		top.add(topRight, BorderLayout.LINE_END);
		topRight.add(new Desc(tr("main_token_id"), tokenIdButton));
		topRight.add(new Desc(" ", new SocialButton(SocialButton.Type.TWITTER, tableBid.getForeground())));
//		topRight.add(new SocialButton(SocialButton.Type.INSTAGRAM, table.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.FACEBOOK, table.getForeground()));
//		topRight.add(new SocialButton(SocialButton.Type.GOOGLE_PLUS, table.getForeground()));

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setOpaque(true);
		tabbedPane.setFont(largeFont);
		
		add(top, BorderLayout.PAGE_START);

		JPanel tablesPanel = new JPanel(new GridLayout(0,2));
		tablesPanel.add(scrollPaneBid);
		tablesPanel.add(scrollPaneAsk);
		add(tabbedPane, BorderLayout.CENTER);
		
		tabbedPane.addTab(tr("main_order_book"), icons.get(Icons.ORDER_BOOK), tablesPanel);
		tabbedPane.addTab(tr("main_trade_history"), icons.get(Icons.TRADE), historyPanel);

		market = null;
		setMarket(marketComboBox.getItemAt(0));
	}

	public void setMarket(Market m) {
		logger.debug("Market {} set", m.toString());
		newMarket = m;
		tokenIdButton.getMainButton().setText(m.getTokenID().getID());
		tokenIdButton.setTokenID(m.getTokenID().getID());
		
		historyPanel.setMarket(m);
		
		update();
	}

	public void setLastPrice(String price, Icon icon, Color color) {
		lastPrice.setText(price);
		lastPrice.setIcon(icon);
		lastPrice.setForeground(color);
		logger.trace("lastPrice set");
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
		
		BurstNode bn = BurstNode.getInstance();
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

		Globals g = Globals.getInstance();
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

		updateOrders();
		
		historyPanel.update();
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
				int cmp = o2.getPrice().compareTo(o1.getPrice());
				if(cmp == 0)
					cmp = o1.getHeight() - o2.getHeight();
				return cmp;
			}
		});
		askOrders.sort(new Comparator<AssetOrder>() {
			@Override
			public int compare(AssetOrder o1, AssetOrder o2) {
				int cmp = o1.getPrice().compareTo(o2.getPrice());
				if(cmp == 0)
					cmp = o1.getHeight() - o2.getHeight();
				return cmp;
			}
		});
		
		firstBid = firstAsk = null;
		for (AssetOrder o : bidOrders) {
			if(o.getAssetId()!=null) {
				firstBid = o;
				break;
			}
		}
		for (AssetOrder o : askOrders) {
			if(o.getAssetId()!=null) {
				firstAsk = o;
				break;
			}
		}

		modelBid.setRowCount(bidOrders.size()+1);
		modelAsk.setRowCount(askOrders.size()+1);

		addOrders(modelBid, bidOrders, OrderBookSettings.BID_COLS, false);
		addOrders(modelAsk, askOrders, OrderBookSettings.ASK_COLS, true);
	}

	private void addOrders(DefaultTableModel model, ArrayList<AssetOrder> orders, int[] cols, boolean ask) {
		Globals g = Globals.getInstance();
		pendingIconRotating.clearCells(model);
		int row = 0;
		
		// Add the "make" buttons
		JButton newOffer = new ActionButton(this, market, tr("book_make_offer"),
				(ask ? firstBid : firstAsk), ask, false);
		newOffer.setBackground(ask ? HistoryPanel.RED : HistoryPanel.GREEN);
		model.setValueAt(newOffer, row++, cols[OrderBookSettings.COL_PRICE]);
		
		for (int i = 0; i < orders.size(); i++, row++) {
			AssetOrder o = orders.get(i);

			// price always come in Burst, so no problem in this division using long's
			long price = o.getPrice().longValue();
			long amountToken = o.getQuantity().longValue();

			if(price == 0 || amountToken == 0)
				continue;

			String priceFormated = NumberFormatting.BURST.format(price*market.getFactor());
			JButton b = new ActionButton(this, market, priceFormated, o, !ask, false);
			if(o.getAccountAddress().equals(g.getAddress()) && o.getAssetId() != null) {
				b = new ActionButton(this, market, priceFormated, o, true);
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
				exp = new ExplorerButton(o.getId().getID(), copyIcon, expIcon);
				if(o.getAccountAddress().equals(g.getAddress()) && o.getAssetId() != null) {
					JButton cancel = new ActionButton(this, market, "", o, true);
					cancel.setIcon(cancelIcon);
					exp.add(cancel, BorderLayout.WEST);
				}
			}
			model.setValueAt(exp, row, cols[OrderBookSettings.COL_CONTRACT]);
		}
	}

	public Market getMarket() {
		return market;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Market m = (Market) marketComboBox.getSelectedItem();

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
						Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this),
								tr("main_add_token_invalid", response), Toast.Style.ERROR).display();
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

			setMarket(m);
			historyPanel.setMarket(m);
			// this is a custom token
			removeTokenButton.setVisible(Markets.getUserMarkets().contains(m));

			if(m.getTokenID() == null) {
				// not a token market, show TRT in the token field
				tokenDesc.setDesc(tr("main_balance", token));
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
		else if (e.getSource() == sendButtonToken) {
			SendDialog dlg = new SendDialog((JFrame) SwingUtilities.getWindowAncestor(this),
					m.getTokenID()==null ? token : m);

			dlg.setLocationRelativeTo(this);
			dlg.setVisible(true);
		}
		else if(e.getSource() == removeTokenButton) {
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
	}
	
	public void addMarket(MarketBurstToken newMarket) {
		int index = 0;
		for (; index < marketComboBox.getItemCount(); index++) {
			if(marketComboBox.getItemAt(index).getTokenID() == null)
				break;
		}
		// Insert as the latest token
		marketComboBox.insertItemAt(newMarket, index);

		Globals.getInstance().addUserMarket(newMarket, true);
		BurstNode.getInstance().update();

		marketComboBox.setSelectedItem(newMarket);
		setMarket(newMarket);
		Toast.makeText((JFrame) SwingUtilities.getWindowAncestor(this),
				tr("main_add_token_success", newMarket.getTokenID().getID()), Toast.Style.SUCCESS).display();
	}
}
