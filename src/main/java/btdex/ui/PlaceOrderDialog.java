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
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import btdex.core.Account;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class PlaceOrderDialog extends JDialog implements ActionListener, DocumentListener {
	private static final long serialVersionUID = 1L;

	Market market;

	JComboBox<Account> accountComboBox;
	JTextField accountDetails;
	JTextField amountField, priceField, total;
	JSlider security;

	JTextPane conditions;
	JCheckBox acceptBox;

	JPasswordField pin;

	private JButton okButton;
	private JButton calcelButton;

	private boolean isToken;

	private JToggleButton buyToken;
	private JToggleButton sellToken;

	private FeeSuggestion suggestedFee;

	private BurstValue amountValue, priceValue;

	public PlaceOrderDialog(JFrame owner, Market market, AssetOrder order) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		isToken = market.getTokenID()!=null;

		if(isToken)
			setTitle(String.format("Exchange %s for BURST", market.toString()));
		else
			setTitle(String.format("Sell BURST for %s", market.toString()));

		this.market = market;

		accountComboBox = new JComboBox<>();
		accountDetails = new JTextField(36);
		accountDetails.setEditable(false);

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));
		topPanel.setBorder(BorderFactory.createTitledBorder("Account to receive " + market.toString()));
		topPanel.add(accountComboBox);
		topPanel.add(accountDetails);

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amountField = new JFormattedTextField(Globals.NF_FULL);
		priceField = new JFormattedTextField(isToken ? market.getNumberFormat() : Globals.NF_FULL);
		total = new JTextField(16);
		total.setEditable(false);

		amountField.getDocument().addDocumentListener(this);
		priceField.getDocument().addDocumentListener(this);

		fieldPanel.setBorder(BorderFactory.createTitledBorder("Offer configuration"));

		if(isToken) {
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
		}

		fieldPanel.add(new Desc("Price (" + (isToken ? "BURST" : market) + ")", priceField));
		fieldPanel.add(new Desc("Size (" + (isToken ? market : "BURST") + ")", amountField));
		fieldPanel.add(new Desc("Total (" + (isToken ? "BURST" : market) + ")", total));

		if(!isToken) {
			ArrayList<Account> acs = Globals.getInstance().getAccounts();

			for (Account ac : acs) {
				if(ac.getMarket().equals(market.toString()))
					accountComboBox.addItem(ac);
			}
			
			// Only FIAT can have zero security (on-ramp special contracts)
			security = new JSlider(market.isFiat() ? 0 : 1, 20);

			Desc securityDesc = new Desc("", security);
			fieldPanel.add(securityDesc);

			security.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					int value = security.getValue();
					String desc = String.format("Security deposit %d %%", value);
					if(value==0)
						desc = "No buyer deposit offer";

					securityDesc.setDesc(desc);
					somethingChanged();
				}
			});
			security.getModel().setValue(5);
		}
		conditions = new JTextPane();
		//		conditions.setContentType("text/html");
		//		conditions.setEditable(false);
		//		conditions.setLineWrap(true);
		//		conditions.setWrapStyleWord(true);
		conditions.setPreferredSize(new Dimension(80, 120));

		acceptBox = new JCheckBox("I accept the terms and conditions");

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pin = new JPasswordField(12);
		pin.addActionListener(this);

		calcelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(new Desc("PIN", pin));
		buttonPane.add(new Desc(" ", calcelButton));
		buttonPane.add(new Desc(" ", okButton));

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		// If not a token, we need the top panel
		if(!isToken)
			content.add(topPanel, BorderLayout.PAGE_START);

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

		suggestedFee = Globals.getInstance().getSuggestedFee();
		
		if(order != null) {
			// taking this order
			if(order.getType() == AssetOrder.OrderType.BID)
				sellToken.setSelected(true);
			else
				buyToken.setSelected(true);
			
			priceField.setText(ContractState.format(order.getPrice().longValue()*market.getFactor()));
			amountField.setText(market.format(order.getQuantity().longValue()));
			somethingChanged();
		}

		accountComboBox.addActionListener(this);
		if(accountComboBox.getItemCount() > 0)
			accountComboBox.setSelectedIndex(0);

		pack();
	}

	@Override
	public void setVisible(boolean b) {
		if(accountComboBox.getItemCount()==0 && !isToken) {
			JOptionPane.showMessageDialog(this, "You need to register a " + market + " account first.",
					"Error", JOptionPane.ERROR_MESSAGE);
			dispose();

			// TODO open the settings dialog here
			return;
		}

		super.setVisible(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}
		
		if(e.getSource() == accountComboBox) {
			Account ac = (Account) accountComboBox.getSelectedItem();
			String details = market.simpleFormat(ac.getFields());
			accountDetails.setText(details);
			somethingChanged();
		}

		if(e.getSource()==buyToken || e.getSource()==sellToken) {
			somethingChanged();
		}

		if(e.getSource() == okButton || e.getSource() == pin) {
			String error = null;
			Globals g = Globals.getInstance();

			if(accountComboBox.getSelectedIndex() < 0 && !isToken) {
				error = "You need to register an account first";
			}

			if(error == null && priceValue == null) {
				error = "Invalid price";
			}
			if(error == null && amountValue == null) {
				error = "Invalid amount";
			}

			if(error == null && !acceptBox.isSelected()) {
				error = "You must accept the terms first";
				acceptBox.requestFocus();
			}
			
			if(error == null && !g.checkPIN(pin.getPassword())) {
				error = "Invalid PIN";
				pin.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}

			// all set, lets place the order
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				Single<byte[]> utx = null;
				if(isToken) {
					if(sellToken.isSelected())
						utx = g.getNS().generatePlaceAskOrderTransaction(g.getPubKey(), market.getTokenID(),
								amountValue, priceValue, suggestedFee.getPriorityFee(), 1440);
					else
						utx = g.getNS().generatePlaceBidOrderTransaction(g.getPubKey(), market.getTokenID(),
								amountValue, priceValue, suggestedFee.getPriorityFee(), 1440);
				}
				else {
				}

				Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
					byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
					return g.getNS().broadcastTransaction(signedTransactionBytes);
				});
				TransactionBroadcast tb = tx.blockingGet();
				tb.getTransactionId();
				setVisible(false);

				Toast.makeText((JFrame) this.getOwner(),
						String.format("Transaction %s has been broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
			}
			catch (Exception ex) {
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
		
		Account account = (Account) accountComboBox.getSelectedItem();

		if(priceField.getText().length()==0 || amountField.getText().length()==0)
			return;

		try {
			// For token, price is in BURST, others price is on the selected market
			if(isToken) {
				Number priceN = Globals.NF.parse(priceField.getText());
				Number amountN = Globals.NF.parse(amountField.getText());

				priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
				amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*market.getFactor()));

				double totalValue = priceN.doubleValue()*amountN.doubleValue();
				total.setText(Globals.NF_FULL.format(totalValue));
			}
			else {
				Number priceN = market.getNumberFormat().parse(priceField.getText());
				Number amountN = Globals.NF_FULL.parse(amountField.getText());

				priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
				amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*Market.BURST_TO_PLANCK));

				double totalValue = priceN.doubleValue()*amountN.doubleValue()*market.getFactor();
				total.setText(market.format((long)totalValue));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}


		String terms = null;
		if(isToken) {
			boolean isSell = sellToken.isSelected();

			terms = "You are %s up to %s %s %s BURST at a price of %s BURST each.\n\n"
					+ "This order can be partially filled and will be open until filled or cancelled. "
					+ "No trading fees apply, only a one time %s BURST transaction fee.";

			terms = String.format(terms,
					isSell ? "selling" : "buying",
							amountField.getText(),
							market,
							isSell ? "for" : "with",
									priceField.getText(), ContractState.format(suggestedFee.getPriorityFee().longValue()));

		}
		else {
			boolean isNoDeposit = security.getValue() == 0;
			
			if(isNoDeposit) {
				terms = "You are selling BURST for %s at a price of %s %s each.\n\n"
						+ "A smart contract will hold your %s BURST as security deposity. "
						+ "A taker have to deposit %s %s on your account '%s' "
						+ "and after that you will transfer %s BURST to the buyer's account.\n\n"
						+ "There is a 1%% fee when you withdraw your deposit and up to 40 BURST "
						+ "smart contract and transaction fees. "
						+ "It can take up to 4 blocks for your order to be available. "
						+ "You need to open BTDEX at least once every 24 hours so your account "
						+ "details can be sent to the buyer in case your offer is taken. \n\n"
						+ "After your account details are sent to the buyer, he/she "
						+ "has up to %d hours to complete the %s transfer. "
						+ "After you receive the %s amount, you have up to 24 hours to finish "
						+ "the trade by transfering the BURST.\n\n"
						+ "This order will be open until taken or cancelled. If you do not follow "
						+ "the protocol, you might lose your security deposit.";

				terms = String.format(terms,
						market, priceField.getText(), market,
						amountField.getText(),
						total.getText(), market, accountDetails.getText(),
						amountField.getText(),
						market.getPaymentTimeout(account.getFields()), market, market
						);
			}
			else {
				terms = "You are selling %s BURST for %s at a price of %s %s each.\n\n"
						+ "A smart contract will hold your %s BURST plus a security deposity of %d %%. "
						+ "The taker will deposit %s %s on your account '%s'.\n\n"
						+ "There are no trading fees for you, but up to 40 BURST smart contract and transaction fees. "
						+ "It can take up to 4 blocks for your order to be available. "
						+ "You need to open BTDEX at least once every 24 hours so your account details can be "
						+ "sent to the buyer in case your offer is taken. \n\n"
						+ "After your account details are sent to the buyer, he/she "
						+ "has up to %d hours to complete %s transfer. "
						+ "After the %s amount is transfered, you have up to 24 hours to signal "
						+ "the amount was received. "
						+ "This order will be open until taken or cancelled.";

				terms = String.format(terms,
						amountField.getText(), market, priceField.getText(), market,
						amountField.getText(), security.getValue(),
						total.getText(), market, accountDetails.getText(),
						market.getPaymentTimeout(account.getFields()), market, market
						);
			}
		}
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
