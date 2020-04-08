package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class PlaceTokenOrderDialog extends JDialog implements ActionListener, DocumentListener {
	private static final long serialVersionUID = 1L;

	Market market;

	JTextField amountField, priceField, totalField;

	JTextPane conditions;
	JCheckBox acceptBox;

	JPasswordField pinField;

	private JButton okButton;
	private JButton cancelButton;

	private JToggleButton buyToken;
	private JToggleButton sellToken;

	private BurstValue suggestedFee;

	private BurstValue amountValue, priceValue;

	public PlaceTokenOrderDialog(JFrame owner, Market market, AssetOrder order) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle(String.format("Exchange %s for BURST", market.toString()));
		this.market = market;

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amountField = new JFormattedTextField(market.getNumberFormat().getFormat());
		priceField = new JFormattedTextField(NumberFormatting.BURST.getFormat());
		totalField = new JTextField(16);
		totalField.setEditable(false);

		amountField.getDocument().addDocumentListener(this);
		priceField.getDocument().addDocumentListener(this);

		fieldPanel.setBorder(BorderFactory.createTitledBorder("Offer details"));

		buyToken = new JRadioButton(String.format("Buy %s with BURST", market), true);
		sellToken = new JRadioButton(String.format("Sell %s for BURST", market));

		buyToken.setBackground(HistoryPanel.GREEN);
		sellToken.setBackground(HistoryPanel.RED);
		buyToken.setForeground(Color.WHITE);
		sellToken.setForeground(Color.WHITE);

		fieldPanel.add(buyToken);
		fieldPanel.add(sellToken);

		ButtonGroup bgroup = new ButtonGroup();
		bgroup.add(buyToken);
		bgroup.add(sellToken);
		buyToken.addActionListener(this);
		sellToken.addActionListener(this);

		fieldPanel.add(new Desc("Price (" + "BURST" + ")", priceField));
		fieldPanel.add(new Desc("Size (" + market + ")", amountField));
		fieldPanel.add(new Desc("Total (" + "BURST" + ")", totalField));

		conditions = new JTextPane();
		//		conditions.setContentType("text/html");
		//		conditions.setEditable(false);
		//		conditions.setLineWrap(true);
		//		conditions.setWrapStyleWord(true);
		conditions.setPreferredSize(new Dimension(80, 140));

		acceptBox = new JCheckBox("I accept the terms and conditions");

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pinField = new JPasswordField(12);
		pinField.addActionListener(this);

		cancelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(new Desc("PIN", pinField));
		buttonPane.add(new Desc(" ", cancelButton));
		buttonPane.add(new Desc(" ", okButton));

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder("Terms and conditions"));
		JScrollPane scroll = new JScrollPane(conditions);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(conditions.getPreferredSize());
		conditionsPanel.add(scroll, BorderLayout.CENTER);

		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(fieldPanel, BorderLayout.CENTER);
		centerPanel.add(conditionsPanel, BorderLayout.PAGE_END);

		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		suggestedFee = Globals.getInstance().getSuggestedFee().getPriorityFee();

		if(order != null) {
			// taking this order
			if(order.getType() == AssetOrder.OrderType.BID)
				sellToken.setSelected(true);
			else
				buyToken.setSelected(true);

			priceField.setText(NumberFormatting.BURST.format(order.getPrice().longValue()*market.getFactor()));
			amountField.setText(market.format(order.getQuantity().longValue()));
			somethingChanged();
		}

		somethingChanged();
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton) {
			setVisible(false);
		}

		if(e.getSource()==buyToken || e.getSource()==sellToken) {
			somethingChanged();
		}

		if(e.getSource() == okButton || e.getSource() == pinField) {
			String error = null;
			Globals g = Globals.getInstance();

			if(error == null && (priceValue == null || priceValue.longValue() <= 0)) {
				error = "Invalid price";
			}
			if(error == null && (amountValue == null || amountValue.longValue() <= 0)) {
				error = "Invalid amount";
			}

			if(error == null && !acceptBox.isSelected()) {
				error = "You must accept the terms first";
				acceptBox.requestFocus();
			}

			if(error == null && !g.checkPIN(pinField.getPassword())) {
				error = "Invalid PIN";
				pinField.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}

			// all set, lets place the order
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				Single<byte[]> utx = null;

				if(sellToken.isSelected())
					utx = g.getNS().generatePlaceAskOrderTransaction(g.getPubKey(), market.getTokenID(),
							amountValue, priceValue, suggestedFee, Constants.BURST_DEADLINE);
				else
					utx = g.getNS().generatePlaceBidOrderTransaction(g.getPubKey(), market.getTokenID(),
							amountValue, priceValue, suggestedFee, Constants.BURST_DEADLINE);

				Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
					byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsignedTransactionBytes);
					return g.getNS().broadcastTransaction(signedTransactionBytes);
				});
				TransactionBroadcast tb = tx.blockingGet();
				tb.getTransactionId();

				setVisible(false);

				Toast.makeText((JFrame) this.getOwner(),
						String.format("Transaction %s has been broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
			}
			setCursor(Cursor.getDefaultCursor());
		}
	}

	private void somethingChanged(){
		if(acceptBox == null)
			return;

		acceptBox.setSelected(false);

		amountValue = null;
		priceValue = null;

		if(priceField.getText().length()==0 || amountField.getText().length()==0)
			return;

		try {
			// For token, price is in BURST, others price is on the selected market
			Number priceN = NumberFormatting.parse(priceField.getText());
			Number amountN = NumberFormatting.parse(amountField.getText());

			priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
			amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*market.getFactor()));

			double totalValue = priceN.doubleValue()*amountN.doubleValue();
			totalField.setText(NumberFormatting.BURST.format(totalValue));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		String terms = null;
		boolean isSell = sellToken.isSelected();

		terms = "You are placing a %s limit order for up to %s %s at a price of %s BURST each.\n\n"
				+ "This order can be partially filled and will be open until filled or cancelled. "
				+ "No trading fees apply, only a one time %s BURST transaction fee.";

		terms = String.format(terms,
				isSell ? "sell" : "buy",
						amountField.getText(),
						market,
						priceField.getText(),
						NumberFormatting.BURST.format(suggestedFee.longValue()));

		if(!conditions.getText().equals(terms)) {
			conditions.setText(terms);
			conditions.setCaretPosition(0);
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		somethingChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		somethingChanged();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		somethingChanged();
	}
}
