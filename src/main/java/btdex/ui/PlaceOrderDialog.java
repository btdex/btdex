package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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
import bt.Contract;
import btdex.core.MarketAccount;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import static btdex.locale.Translation.tr;
import btdex.sc.SellContract;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class PlaceOrderDialog extends JDialog implements ActionListener, DocumentListener {
	private static final long serialVersionUID = 1L;

	Market market;

	JComboBox<MarketAccount> accountComboBox;
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

	private boolean isUpdate, isTake, isTaken, isBuy, isSignal, isDeposit;

	private JToggleButton buyToken;
	private JToggleButton sellToken;

	private BurstValue suggestedFee;

	private BurstValue amountValue, priceValue;

	private Desc pinDesc;

	public PlaceOrderDialog(JFrame owner, Market market, ContractState contract, boolean buy) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.contract = contract;
		Globals g = Globals.getInstance();
		
		this.isBuy = buy;
		if(contract !=null && contract.getType() == ContractState.Type.BUY)
			this.isBuy = true;

		if(contract !=null && contract.getState()==SellContract.STATE_OPEN) {
			isUpdate =  contract.getCreator().equals(Globals.getInstance().getAddress());
			isTake = !isUpdate;
		}
		if(contract !=null && contract.hasStateFlag(SellContract.STATE_WAITING_PAYMT)) {
			isTake = isTaken = true;
		}
		isSignal = isDeposit = false;
		if(isTaken) {
			isDeposit = isBuy ? contract.getCreator().equals(g.getAddress()) : contract.getTaker() == g.getAddress().getSignedLongId();
			isSignal = !isDeposit;
		}

		setTitle(tr((isBuy && !isTake) || (!isBuy && isTake) ? "offer_buy_burst_with" : "offer_sell_burst_for", market.toString()));

		if(isTaken)
			setTitle(tr(isDeposit ? "offer_deposit" : "offer_signal_was_received", market));

		this.market = market;

		accountComboBox = new JComboBox<>();
		accountDetails = new JTextField(36);
		accountDetails.setEditable(false);

		JPanel accountPanel = new JPanel(new GridLayout(0, 1, 4, 4));
		accountPanel.setBorder(BorderFactory.createTitledBorder(tr("offer_account_to_receive", market)));
		accountPanel.add(accountComboBox);
		accountPanel.add(accountDetails);

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amountField = new JFormattedTextField(NumberFormatting.BURST.getFormat());
		priceField = new JFormattedTextField(market.getNumberFormat().getFormat());
		totalField = new JTextField(16);
		totalField.setEditable(false);

		amountField.getDocument().addDocumentListener(this);
		priceField.getDocument().addDocumentListener(this);

		fieldPanel.setBorder(BorderFactory.createTitledBorder(tr("offer_offer_details")));

		fieldPanel.add(new Desc(tr("offer_price", market), priceField));
		fieldPanel.add(new Desc(tr("offer_size", "BURST"), amountField));
		fieldPanel.add(new Desc(tr("offer_total", market), totalField));

		ArrayList<MarketAccount> acs = Globals.getInstance().getMarketAccounts();

		for (MarketAccount ac : acs) {
			if(ac.getMarket().equals(market.toString()))
				accountComboBox.addItem(ac);
		}

		// Only FIAT can have zero security (on-ramp special contracts)
		security = new JSlider(market.isFiat() ? 0 : 1, 30);

		Desc securityDesc = new Desc("", security);
		fieldPanel.add(securityDesc);

		security.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				int value = security.getValue();
				String desc = tr("offer_security_deposit_percent", value);
				if(value==0)
					desc = tr("offer_no_security");

				if(isUpdate || isTake)
					desc = tr("offer_security_deposit");

				securityDesc.setDesc(desc);
				somethingChanged();
			}
		});
		security.setValue(15);
		securityDesc.setDesc(tr("offer_security_deposit_percent", security.getValue()));
		

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

		conditions = new JTextPane();
		//		conditions.setContentType("text/html");
		//		conditions.setLineWrap(true);
		//		conditions.setWrapStyleWord(true);
		conditions.setPreferredSize(new Dimension(80, 140));
		conditions.setEditable(false);

		acceptBox = new JCheckBox("I accept the terms and conditions");
		if(isSignal)
			acceptBox.setText(tr("offer_received_coin", market));

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pinField = new JPasswordField(12);
		pinField.addActionListener(this);

		cancelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("dlg_ok"));
		disputeButton = new JButton(tr("offer_open_dispute"));

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		disputeButton.addActionListener(this);
		disputeButton.setVisible(false);

		buttonPane.add(new Desc(" ", disputeButton));
		buttonPane.add(pinDesc = new Desc(tr("dlg_pin"), pinField));
		buttonPane.add(new Desc(" ", cancelButton));
		buttonPane.add(new Desc(" ", okButton));

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		// We need the top panel
		if(!isTake && !isBuy)
			content.add(accountPanel, BorderLayout.PAGE_START);

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder(tr("dlg_terms_and_conditions")));
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

		accountComboBox.addActionListener(this);
		if(accountComboBox.getItemCount() > 0)
			accountComboBox.setSelectedIndex(0);

		if(isTaken && !isSignal) {
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
			if(!Globals.getInstance().isTestnet()) {
				JOptionPane.showMessageDialog(getParent(), tr("offer_not_open_yet"),
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(Contracts.isLoading()) {
				JOptionPane.showMessageDialog(getParent(), tr("main_cross_chain_loading"),
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(accountComboBox.getItemCount()==0 && (isBuy && isTake || !isBuy && !isTake)) {
				JOptionPane.showMessageDialog(getParent(), tr("offer_register_account_first", market),
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(contract!=null && contract.hasPending()) {
				JOptionPane.showMessageDialog(getParent(), tr("offer_wait_confirm"),
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if(contract == null)
				contract = isBuy ? Contracts.getFreeBuyContract() : Contracts.getFreeContract();
			if(contract == null) {
				int ret = JOptionPane.showConfirmDialog(getParent(),
						tr("offer_no_contract_available"),
						tr("reg_register"), JOptionPane.YES_NO_OPTION);
				if(ret == JOptionPane.YES_OPTION) {
					// No available contract, show the option to register a contract first
					RegisterContractDialog dlg = new RegisterContractDialog(getOwner(), isBuy);
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
			return;
		}

		if(e.getSource() == accountComboBox) {
			MarketAccount ac = (MarketAccount) accountComboBox.getSelectedItem();
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

			if(!pinDesc.isVisible()) {
				// nothing to do, as this is only showing information
				setVisible(false);
				return;
			}

			if(accountComboBox.getSelectedIndex() < 0 && !isTake && !isBuy) {
				error = tr("offer_select_account");
			}

			if(error == null && (priceValue == null || priceValue.longValue() <= 0)) {
				error = tr("offer_invalid_price");
			}
			if(error == null && (amountValue == null || amountValue.longValue() <= 0)) {
				error = tr("send_invalid_amount");
			}
			if(error == null && amountValue.longValue() <= 10 * Contract.ONE_BURST) {
				error = tr("offer_too_small");
			}

			if(error == null && !acceptBox.isSelected()) {
				error = tr("dlg_accept_first");
				acceptBox.requestFocus();
			}
			
			if(error == null && isUpdate) {
				// check if something changed
				if(priceValue.longValue() == contract.getRate() &&
						(accountDetails.getText().length()==0 || 
						accountDetails.getText().equals(market.simpleFormat(contract.getMarketAccount().getFields())))
						)
					error = tr("offer_no_changes");
			}

			if(error == null && !g.checkPIN(pinField.getPassword())) {
				error = tr("dlg_invalid_pin");
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
				Single<TransactionBroadcast> updateTx = null;

				BurstValue configureFee = suggestedFee;
				if(!isUpdate && !isTake && !isTaken) {
					// configure a new contract and place the deposit
					contract = isBuy ? Contracts.getFreeBuyContract() : Contracts.getFreeContract();
					if(contract == null) {
						// This should not happen, since we checked already when opening the dialog
						Toast.makeText((JFrame) this.getOwner(), tr("offer_no_contract_error"), Toast.Style.ERROR).display(okButton);
						setCursor(Cursor.getDefaultCursor());
						return;
					}

					// send the update transaction
					long securityAmount = amountValue.longValue() * security.getValue() / 100L;
					byte[] message = BT.callMethodMessage(contract.getMethod("update"), isBuy? amountValue.longValue() : securityAmount);

					BurstValue amountToSend = BurstValue.fromPlanck(securityAmount + contract.getNewOfferFee());
					if(!isBuy)
						amountToSend = amountToSend.add(amountValue);

					utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
							amountToSend, suggestedFee,
							Constants.BURST_DEADLINE, message);

					updateTx = utx.flatMap(unsignedTransactionBytes -> {
						byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsignedTransactionBytes);
						return g.getNS().broadcastTransaction(signedTransactionBytes);
					});

					// make sure the price setting goes first setting an extra priority for it
					configureFee = suggestedFee.add(BurstValue.fromPlanck(Constants.FEE_QUANT));
				}

				if(isTaken && isSignal) {
					// we are signaling that we have received
					byte[] message = BT.callMethodMessage(contract.getMethod("reportComplete"));
					BurstValue amountToSend = BurstValue.fromPlanck(contract.getActivationFee());

					utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
							amountToSend, suggestedFee,
							Constants.BURST_DEADLINE, message);						
				}
				else if(isTake) {
					// send the take transaction with the security deposit (+ amount if a buy order)
					BurstValue amountToSend = BurstValue.fromPlanck(contract.getSecurityNQT() + contract.getActivationFee());
					if(isBuy) {
						amountToSend = amountToSend.add(BurstValue.fromPlanck(contract.getAmountNQT()));
						
						// also send the address we want to receive the amount
						JsonObject messageJson = new JsonObject();
						messageJson.addProperty("account", accountDetails.getText());

						String messageString = Constants.GSON.toJson(messageJson);
						utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
								configureFee,
								Constants.BURST_DEADLINE, messageString);
						
						utx.flatMap(unsignedTransactionBytes -> {
							byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsignedTransactionBytes);
							return g.getNS().broadcastTransaction(signedTransactionBytes);
						}).blockingGet();
						// increase the fee to make sure the "take" will go first than this one
						configureFee = configureFee.add(BurstValue.fromPlanck(Constants.FEE_QUANT));
					}

					byte[] message = BT.callMethodMessage(contract.getMethod("take"),
							contract.getSecurityNQT(), contract.getAmountNQT());

					utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
							amountToSend, configureFee,
							Constants.BURST_DEADLINE, message);
				}
				else {
					// now the configuration message
					JsonObject messageJson = new JsonObject();
					messageJson.addProperty("market", String.valueOf(market.getID()));
					messageJson.addProperty("rate", String.valueOf(priceValue.longValue()));
					if(!isBuy)
						messageJson.addProperty("account", accountDetails.getText());

					String messageString = Constants.GSON.toJson(messageJson);
					utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
							configureFee,
							Constants.BURST_DEADLINE, messageString);
				}					

				Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
					byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsignedTransactionBytes);
					return g.getNS().broadcastTransaction(signedTransactionBytes);
				});
				TransactionBroadcast tb = tx.blockingGet();
				tb.getTransactionId();

				// Send the update transaction only if the price setting was already sent
				if(updateTx!=null) {
					updateTx.blockingGet();
				}

				setVisible(false);
				Toast.makeText((JFrame) this.getOwner(),
						tr("send_tx_broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
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

		MarketAccount account = isTake ? contract.getMarketAccount() : (MarketAccount) accountComboBox.getSelectedItem();

		if(priceField.getText().length()==0 || amountField.getText().length()==0)
			return;

		try {
			// Price is on the selected market
			Number priceN = NumberFormatting.parse(priceField.getText());
			Number amountN = NumberFormatting.parse(amountField.getText());

			priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
			amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*Market.BURST_TO_PLANCK));

			double totalValue = priceN.doubleValue()*amountN.doubleValue()*market.getFactor();
			totalField.setText(market.format(Math.round(totalValue)));
		} catch (ParseException e) {
			e.printStackTrace();
		}


		StringBuilder terms = new StringBuilder();
		boolean isNoDeposit = security.getValue() == 0;
		boolean unexpectedState = false;

		if(isNoDeposit) {
			terms.append(tr("offer_terms_no_deposit",
					market, priceField.getText(), market,
					amountField.getText()));
			terms.append("\n\n").append(tr("offer_terms_no_deposit_maker",
					totalField.getText(), market, accountDetails.getText(),
					amountField.getText()));
			terms.append("\n\n").append(tr("offer_terms_no_deposit_taker",
					market.getPaymentTimeout(account.getFields()),
					market, market, market
					));
			terms.append("\n\n").append(tr("offer_terms_closing"));
		}
		else {
			if(isTaken) {
				// we have to either signal we have received or make the deposit
				if (isSignal) {
					// Signaling that we have received the market amount
					terms.append(tr("offer_terms_signaling",
							totalField.getText(), market,
							market.simpleFormat(contract.getMarketAccount().getFields())));
					terms.append("\n\n").append(tr("offer_terms_signaling_details",
							amountField.getText(), contract.getSecurity(),
							NumberFormatting.BURST.format(suggestedFee.longValue() +
									contract.getActivationFee()),
							market
							));
				}
				else {
					// is deposit
					if(isBuy) {
						terms.append(tr("offer_terms_buy_deposit",
								totalField.getText(), market,
								market.simpleFormat(contract.getMarketAccount().getFields())));
					}
					else {
						terms.append(tr("offer_terms_need_transfer",
								totalField.getText(), market,
								market.simpleFormat(contract.getMarketAccount().getFields()),
								market.getPaymentTimeout(account.getFields())));
					}
					terms.append("\n\n").append(tr("offer_terms_need_transfer_details",
							amountField.getText(), contract.getSecurity(),
							totalField.getText(), market
							));
				}
				terms.append("\n\n").append(tr("offer_terms_protocol"));
				// checking it actually has the balance
				if(contract.getBalance().longValue() + contract.getActivationFee() <
						contract.getAmountNQT() + 2*contract.getSecurityNQT())
					unexpectedState = true;
			}
			else if(isTake) {
				if(isBuy) {
					terms.append(tr("offer_terms_take_buy",
							amountField.getText(), priceField.getText(), market));
					terms.append("\n\n").append(tr("offer_terms_take_buy_details",
							contract.getSecurity(),
							amountField.getText(),
							NumberFormatting.BURST.format(suggestedFee.longValue()*2 +
									contract.getActivationFee()),
							totalField.getText(), market,
							accountDetails.getText(), market
							));
				}
				else {
					terms.append(tr("offer_terms_take_sell",
							amountField.getText(), priceField.getText(), market));
					terms.append("\n\n").append(tr("offer_terms_take_sell_details",
							amountField.getText(), contract.getSecurity(),
							NumberFormatting.BURST.format(suggestedFee.longValue() +
									contract.getActivationFee()),
							totalField.getText(), market, market.getPaymentTimeout(account.getFields()),
							market
							));
				}
				terms.append("\n\n").append(tr("offer_terms_protocol"));				
			}
			else if (isBuy){
				if(!isUpdate)
					contract = Contracts.getFreeBuyContract();

				terms.append(tr(isUpdate ? "offer_terms_update_buy" : "offer_terms_buy",
						amountField.getText(), priceField.getText(), market));
				
				terms.append("\n\n").append(tr(isUpdate ? "offer_terms_update_buy_details" : "offer_terms_buy_details",
						NumberFormatting.BURST.format(isUpdate ? suggestedFee.longValue() :
									contract.getNewOfferFee() + 2*suggestedFee.longValue() + Constants.FEE_QUANT),
						isUpdate ? contract.getSecurity() :
							NumberFormatting.BURST.format(security.getValue()*amountValue.longValue()/100) ));
				terms.append("\n\n").append(tr("offer_terms_buy_taker",
						amountField.getText(),
						totalField.getText(), market, market));
				terms.append("\n\n").append(tr("offer_terms_protocol"));				
			}
			else {
				// sell contract (new or update)
				if(!isUpdate)
					contract = Contracts.getFreeContract();

				terms.append(tr(isUpdate ? "offer_terms_update_sell" : "offer_terms_sell",
						amountField.getText(), priceField.getText(), market,
						accountDetails.getText()));
				
				terms.append("\n\n").append(tr(isUpdate ? "offer_terms_update_sell_details" : "offer_terms_sell_details",
						NumberFormatting.BURST.format(isUpdate ? suggestedFee.longValue() :
									contract.getNewOfferFee() + 2*suggestedFee.longValue() + Constants.FEE_QUANT),
						amountField.getText(),
						isUpdate ? contract.getSecurity() :
							NumberFormatting.BURST.format(security.getValue()*amountValue.longValue()/100) ));
				terms.append("\n\n").append(tr("offer_terms_sell_taker",
							totalField.getText(),
							market, accountDetails.getText(),
							market.getPaymentTimeout(account.getFields()), market
						));
				terms.append("\n\n").append(tr("offer_terms_protocol"));				
			}
		}
		
		// checking it has the balance before requesting the deposit
		if(unexpectedState) {
			// the contract seems to be invalid, this should not happen
			terms = new StringBuilder();
			terms.append(tr("offer_invalid_contact_mediator"));
		}

		String termsText = terms.toString();
		if(!conditions.getText().equals(termsText)) {
			conditions.setText(termsText);
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
