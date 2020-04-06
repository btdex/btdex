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

import com.google.gson.JsonObject;

import bt.BT;
import btdex.core.Account;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import btdex.sc.SellContract;
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
	JTextField amountField, priceField, totalField;
	JSlider security;

	ContractState contract;

	JTextPane conditions;
	JCheckBox acceptBox;

	JPasswordField pinField;

	private JButton okButton;
	private JButton cancelButton;
	private JButton disputeButton;

	private boolean isToken, isUpdate, isTake, isTaken;

	private JToggleButton buyToken;
	private JToggleButton sellToken;

	private FeeSuggestion suggestedFee;

	private BurstValue amountValue, priceValue;

	private Desc pinDesc;

	public PlaceOrderDialog(JFrame owner, Market market, AssetOrder order, ContractState contract) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.contract = contract;
		Globals g = Globals.getInstance();

		isToken = market.getTokenID()!=null;
		if(contract !=null && contract.getState()==SellContract.STATE_OPEN) {
			isUpdate =  contract.getCreator().equals(Globals.getInstance().getAddress());
			isTake = !isUpdate;
		}
		if(contract !=null && contract.hasStateFlag(SellContract.STATE_WAITING_PAYMT)) {
			isTake = isTaken = true;
		}

		if(isToken)
			setTitle(String.format("Exchange %s for BURST", market.toString()));
		else {
			setTitle(String.format((contract==null || contract.getCreator().equals(g.getAddress()) ?
					"Sell Burst for %s" : "Buy BURST with %s"), market.toString()));
			
			if(isTaken && contract.getCreator().equals(g.getAddress()))
				setTitle(String.format("Signal %s was received", market));
		}
		this.market = market;

		accountComboBox = new JComboBox<>();
		accountDetails = new JTextField(36);
		accountDetails.setEditable(false);

		JPanel accountPanel = new JPanel(new GridLayout(0, 1, 4, 4));
		accountPanel.setBorder(BorderFactory.createTitledBorder("Account to receive " + market.toString()));
		accountPanel.add(accountComboBox);
		accountPanel.add(accountDetails);

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amountField = new JFormattedTextField(isToken ? market.getNumberFormat().getFormat() : NumberFormatting.BURST.getFormat());
		priceField = new JFormattedTextField(isToken ? NumberFormatting.BURST.getFormat() : market.getNumberFormat().getFormat());
		totalField = new JTextField(16);
		totalField.setEditable(false);

		amountField.getDocument().addDocumentListener(this);
		priceField.getDocument().addDocumentListener(this);

		fieldPanel.setBorder(BorderFactory.createTitledBorder("Offer details"));

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
		fieldPanel.add(new Desc("Total (" + (isToken ? "BURST" : market) + ")", totalField));

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

					if(isUpdate || isTake)
						desc = "Security deposit";

					securityDesc.setDesc(desc);
					somethingChanged();
				}
			});
			security.getModel().setValue(5);

			if(isUpdate || isTake) {
				// These cannot be changed
				amountField.setEnabled(false);
				security.setEnabled(false);
				int securityValue = (int)(contract.getSecurityNQT()*100/contract.getAmountNQT());
				security.getModel().setValue(securityValue);

				priceField.setText(market.format(contract.getRate()));
				amountField.setText(NumberFormatting.BURST.format(contract.getAmountNQT()));
			}
			if(isTake) {
				// take cannot change this
				priceField.setEnabled(false);
				totalField.setEnabled(false);
			}
		}

		conditions = new JTextPane();
		//		conditions.setContentType("text/html");
		//		conditions.setEditable(false);
		//		conditions.setLineWrap(true);
		//		conditions.setWrapStyleWord(true);
		conditions.setPreferredSize(new Dimension(80, 140));

		acceptBox = new JCheckBox("I accept the terms and conditions");
		if(isTaken && contract.getCreator().equals(g.getAddress()))
			acceptBox.setText(String.format("I have received the %s amount due", market));

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pinField = new JPasswordField(12);
		pinField.addActionListener(this);

		cancelButton = new JButton("Cancel");
		okButton = new JButton("OK");
		disputeButton = new JButton("Open Dispute");

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		disputeButton.addActionListener(this);
		disputeButton.setVisible(false);

		buttonPane.add(new Desc(" ", disputeButton));
		buttonPane.add(pinDesc = new Desc("PIN", pinField));
		buttonPane.add(new Desc(" ", cancelButton));
		buttonPane.add(new Desc(" ", okButton));

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		// If not a token, we need the top panel
		if(!isToken && !isTake)
			content.add(accountPanel, BorderLayout.PAGE_START);

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

		if(isToken && order != null) {
			// taking this order
			if(order.getType() == AssetOrder.OrderType.BID)
				sellToken.setSelected(true);
			else
				buyToken.setSelected(true);

			priceField.setText(NumberFormatting.BURST.format(order.getPrice().longValue()*market.getFactor()));
			amountField.setText(market.format(order.getQuantity().longValue()));
			somethingChanged();
		}

		accountComboBox.addActionListener(this);
		if(accountComboBox.getItemCount() > 0)
			accountComboBox.setSelectedIndex(0);

		if(isTaken && !contract.getCreator().equals(g.getAddress())) {
			// If we have taken this offer, just show the market address to deposit or the dispute button
			acceptBox.setVisible(false);
			cancelButton.setVisible(false);
			pinDesc.setVisible(false);
		}
		if(isTaken) {
			disputeButton.setVisible(true);			
		}

		somethingChanged();
		pack();
	}

	@Override
	public void setVisible(boolean b) {
		if(b == true) {
			if(!Globals.getInstance().isTestnet() && !isToken) {
				JOptionPane.showMessageDialog(getParent(), "Cross-chain markets not open yet.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(accountComboBox.getItemCount()==0 && !isToken && contract==null) {
				JOptionPane.showMessageDialog(getParent(), "You need to register a " + market + " account first.",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if(contract==null && Contracts.getFreeContract() == null && !isToken) {
				int ret = JOptionPane.showConfirmDialog(getParent(),
						"You don't have a smart contract available.\nRegister a new one?",
						"Register Smart Contract", JOptionPane.YES_NO_OPTION);
				if(ret == JOptionPane.YES_OPTION) {
					// No available contract, show the option to register a contract first
					RegisterContractDialog dlg = new RegisterContractDialog(getOwner());
					dlg.setLocationRelativeTo(getOwner());
					dlg.setVisible(true);
				}			
				return;
			}
		}
		super.setVisible(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton) {
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

		if(e.getSource() == okButton || e.getSource() == pinField) {
			String error = null;
			Globals g = Globals.getInstance();

			if(contract!=null && contract.hasStateFlag(SellContract.STATE_WAITING_PAYMT) && contract.getTaker() == g.getAddress().getSignedLongId()) {
				// nothing to do, as this only shows the market address to deposit
				setVisible(false);
			}

			if(accountComboBox.getSelectedIndex() < 0 && !isToken && !isTake) {
				error = "You need to register an account first";
			}

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
				if(isToken) {
					if(sellToken.isSelected())
						utx = g.getNS().generatePlaceAskOrderTransaction(g.getPubKey(), market.getTokenID(),
								amountValue, priceValue, suggestedFee.getPriorityFee(), Constants.BURST_DEADLINE);
					else
						utx = g.getNS().generatePlaceBidOrderTransaction(g.getPubKey(), market.getTokenID(),
								amountValue, priceValue, suggestedFee.getPriorityFee(), Constants.BURST_DEADLINE);
				}
				else {
					if(contract == null) {
						// configure a new contract and place the deposit
						contract = Contracts.getFreeContract();

						// send the update transaction with the amount + security deposit
						long securityAmount = amountValue.longValue() * security.getValue() / 100;
						byte[] message = BT.callMethodMessage(Contracts.getContract().getMethod("update"), securityAmount);

						BurstValue amountToSend = amountValue.add(BurstValue.fromPlanck(securityAmount + contract.getActivationFee()));

						utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
								amountToSend, suggestedFee.getPriorityFee(),
								Constants.BURST_DEADLINE, message);

						Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
							byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsignedTransactionBytes);
							return g.getNS().broadcastTransaction(signedTransactionBytes);
						});
						tx.blockingGet();
					}

					if(isTaken && contract.getCreator().equals(g.getAddress())) {
						// we are signaling that we have received
						byte[] message = BT.callMethodMessage(Contracts.getContract().getMethod("reportComplete"));
						BurstValue amountToSend = BurstValue.fromPlanck(contract.getActivationFee());

						utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
								amountToSend, suggestedFee.getPriorityFee(),
								Constants.BURST_DEADLINE, message);						
					}
					else if(isTake) {
						// send the take transaction with the amount + security deposit
						byte[] message = BT.callMethodMessage(Contracts.getContract().getMethod("take"),
								contract.getSecurityNQT(), contract.getAmountNQT());

						BurstValue amountToSend = BurstValue.fromPlanck(contract.getSecurityNQT() + contract.getActivationFee());

						utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
								amountToSend, suggestedFee.getPriorityFee(),
								Constants.BURST_DEADLINE, message);
					}
					else {
						// now the configuration message
						JsonObject messageJson = new JsonObject();
						messageJson.addProperty("market", String.valueOf(market.getID()));
						messageJson.addProperty("rate", String.valueOf(priceValue.longValue()));
						messageJson.addProperty("account", accountDetails.getText());

						String messageString = Constants.GSON.toJson(messageJson);
						utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
								suggestedFee.getPriorityFee(),
								Constants.BURST_DEADLINE, messageString);
					}
				}

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

		Account account = isTake ? contract.getMarketAccount() : (Account) accountComboBox.getSelectedItem();

		if(priceField.getText().length()==0 || amountField.getText().length()==0)
			return;

		try {
			// For token, price is in BURST, others price is on the selected market
			if(isToken) {
				Number priceN = NumberFormatting.parse(priceField.getText());
				Number amountN = NumberFormatting.parse(amountField.getText());

				priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
				amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*market.getFactor()));

				double totalValue = priceN.doubleValue()*amountN.doubleValue();
				totalField.setText(NumberFormatting.BURST.format(totalValue));
			}
			else {
				Number priceN = NumberFormatting.parse(priceField.getText());
				Number amountN = NumberFormatting.parse(amountField.getText());

				priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
				amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*Market.BURST_TO_PLANCK));

				double totalValue = priceN.doubleValue()*amountN.doubleValue()*market.getFactor();
				totalField.setText(market.format((long)totalValue));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}


		String terms = null;
		if(isToken) {
			boolean isSell = sellToken.isSelected();

			terms = "You are placing a %s limit order for up to %s %s at a price of %s BURST each.\n\n"
					+ "This order can be partially filled and will be open until filled or cancelled. "
					+ "No trading fees apply, only a one time %s BURST transaction fee.";

			terms = String.format(terms,
					isSell ? "sell" : "buy",
							amountField.getText(),
							market,
							priceField.getText(),
							NumberFormatting.BURST.format(suggestedFee.getPriorityFee().longValue()));

		}
		else {
			boolean isNoDeposit = security.getValue() == 0;

			if(isNoDeposit) {
				terms = "You are selling BURST for %s at a price of %s %s each with no buyer "
						+ "security deposit.\n\n"
						+ "A smart contract will hold 10 times your selling amount of %s BURST as "
						+ "seller security deposity.\n\n"
						+ "Any taker (up to 8 simultaneous) have to deposit %s %s on your account '%s' "
						+ "and after that BTDEX will transfer %s BURST from your account (not from "
						+ "the smart contract) to the buyer's account.\n\n"
						+ "There is a 1%% fee when you withdraw your deposit and up to 40 BURST "
						+ "smart contract and transaction fees. "
						+ "It takes 2 blocks after this transaction confirms for your order to be available. "
						+ "\n\nThe buyer has up to %d hours to complete the %s transfer after taking your offer. "
						+ "After you receive the %s amount, you have up to 24 hours to finish "
						+ "the trade by signaling you received the %s.\n\n"
						+ "This order will be open until cancelled. If you do not follow "
						+ "this protocol, you might lose your security deposit.";

				terms = String.format(terms,
						market, priceField.getText(), market,
						amountField.getText(),
						totalField.getText(), market, accountDetails.getText(),
						amountField.getText(),
						market.getPaymentTimeout(account.getFields()), market, market, market
						);
			}
			else {
				if(isTaken) {
					if (contract.getCreator().equals(Globals.getInstance().getAddress())) {
						// Signaling that we have received the market amount
						terms = "You are signaling that you have received %s %s on the address '%s'.\n\n"
								+ "A smart contract is currently holding %s BURST plus your "
								+ "security deposity of %s BURST. "
								+ "You have up to 24 h after this offer was taken to make this confirmation. "
								+ "By confirming you have received, the BURST amount will be transfered "
								+ "to the buyer and your security deposit will be transfered back to you. "
								+ "This confirmation transaction will cost you %s BURST.\n\n"
								+ "If you have not received your %s, you can open a dispute. "
								+ "If you do not follow this protocol, you might lose your security deposit. "
								+ "The mediation system protects you in case of trade disputes.";
						terms = String.format(terms,
								totalField.getText(), market,
								market.simpleFormat(contract.getMarketAccount().getFields()),
								amountField.getText(), contract.getSecurity(),
								NumberFormatting.BURST.format(suggestedFee.getPriorityFee().longValue() +
										contract.getActivationFee()),
								market
								);
					}
					else {
						// Telling we need to transfer the market amount
						terms = "Now you MUST transfer %s %s to to the address '%s' within %s hours "
								+ "after you took the offer.\n\n"
								+ "A smart contract is currently holding %s BURST plus your "
								+ "security deposity of %s BURST. "
								+ "After the %s %s amount is confirmed on the seller's address, you will receive "
								+ "the BURST amount plus your security deposit back in up to 24 hours.\n\n"
								+ "If you do not follow this protocol, you might lose your security deposit. "
								+ "The mediation system protects you in case of trade disputes.";
						terms = String.format(terms,
								totalField.getText(), market,
								market.simpleFormat(contract.getMarketAccount().getFields()),
								market.getPaymentTimeout(account.getFields()),
								amountField.getText(), contract.getSecurity(),
								totalField.getText(), market
								);
					}
				}
				else if(isTake) {
					terms = "You are buying %s BURST for a price of %s %s each with 0.25%% trading fee.\n\n"
							+ "A smart contract is currently holding %s BURST plus a "
							+ "security deposity of %s BURST. "
							+ "By taking this offer you are now making the same security deposit on the smart contract "
							+ "plus %s BURST on smart contract and transaction fees. "
							+ "You MUST transfer %s %s within %s hours after this transaction confirms.\n\n"
							+ "After the %s amount is confirmed on the seller's address, you will receive "
							+ "the BURST amount plus your security deposit back in up to 24 hours.\n\n"
							+ "If you do not follow this protocol, you might lose your security deposit. "
							+ "The mediation system protects you in case of trade disputes.";					
					terms = String.format(terms,
							amountField.getText(), priceField.getText(), market,
							amountField.getText(), contract.getSecurity(),
							NumberFormatting.BURST.format(suggestedFee.getPriorityFee().longValue() +
									contract.getActivationFee()),
							totalField.getText(), market, market.getPaymentTimeout(account.getFields()),
							market
							);
				}
				else {
					if(isUpdate)
						terms = "You are updating your sell order of %s BURST to a price of %s %s each "
								+ "to be received on '%s'.\n\n"
								+ "This update transaction will cost you %s BURST. "
								+ "Your updated conditions will be effective after this transaction confirms.\n\n"
								+ "A smart contract is currently holding %s BURST of yours plus a "
								+ "security deposity of %s BURST. "
								+ "A taker have to make this same security deposit on the smart contract "
								+ "and must transfer %s %s to your address '%s'.\n\n"
								+ "When your offer is taken, the buyer "
								+ "has up to %d hours to complete the %s transfer. "
								+ "After the %s amount is confirmed on your address, you have up to 24 hours to signal "
								+ "the amount was received.\n\n"
								+ "This order will be open until taken or cancelled. If you do not follow "
								+ "this protocol, you might lose your security deposit. "
								+ "The mediation system protects you in case of trade disputes.";
					else
						terms = "You are selling %s BURST at a price of %s %s each "
								+ "to be received on '%s'.\n\n"
								+ "There are no trading fees for you, but %s BURST smart contract and transaction fees. "
								+ "Your offer will be available 2 blocks after this transaction confirms.\n\n"
								+ "A smart contract will hold your %s BURST plus a security deposity of %s BURST. "
								+ "The taker have to make the same security deposit on this smart contract "
								+ "and must transfer %s %s to your address '%s'.\n\n"
								+ "When your offer is taken, the buyer "
								+ "has up to %d hours to complete the %s transfer. "
								+ "After the %s amount is confirmed on your address, you have up to 24 hours to signal "
								+ "the amount was received.\n\n"
								+ "This order will be open until taken or cancelled. If you do not follow "
								+ "this protocol, you might lose your security deposit. "
								+ "The mediation system protects you in case of trade disputes.";

					terms = String.format(terms,
							amountField.getText(), priceField.getText(), market,
							accountDetails.getText(),
							NumberFormatting.BURST.format(
									isUpdate ? suggestedFee.getPriorityFee().longValue() :
										SellContract.ACTIVATION_FEE + 2*suggestedFee.getPriorityFee().longValue()),
							amountField.getText(),
							isUpdate ? contract.getSecurity() :
								NumberFormatting.BURST.format(security.getValue()*amountValue.longValue()/100),
								totalField.getText(),
								market, accountDetails.getText(),
								market.getPaymentTimeout(account.getFields()), market, market
							);
				}
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
