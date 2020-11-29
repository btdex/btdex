package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bt.BT;
import btdex.core.*;
import btdex.markets.MarketCrypto;
import btdex.sc.SellContract;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class DisputeDialog extends JDialog implements ActionListener, ChangeListener {
	private static final long serialVersionUID = 1L;

	private static final String BUTTON_TEXT = "[B]";
	public static final String HTML_STYLE = "<style>body{font: Dialog, Arial, sans-serif;}</style>";

	Market market;

	JTextField amountField, priceField, totalField;
	JSlider security;
	JSlider yourAmountYouSlider, yourAmountOtherSlider;
	JSlider otherAmountYouSlider, otherAmountOtherSlider;
	JSlider mediatorAmountMakerSlider, mediatorAmountTakerSlider;
	Desc yourAmountYouDesc, yourAmountOtherDesc;
	Desc otherAmountYouDesc, otherAmountOtherDesc;
	Desc mediatorAmountMakerDesc, mediatorAmountTakerDesc;
	JLabel amountToFeeContractLabel;
	long amountToFeeContract;
	long amount, amountToCreator, amountToTaker;
	long suggestToYou, suggestToOther;

	ClipboardAndQRButton addressButton;

	ContractState contract;

	JTextPane conditions;
	JCheckBox acceptBox;
	JCheckBox acceptOtherTermsBox;
	JCheckBox acceptMakerTermsBox;

	JPasswordField pinField;

	private JButton okButton;
	private JButton cancelButton;
	private JButton supportDiscord, supportReddit;

	private boolean isBuy, isCreator, isMediator, isMediating;
	private boolean hasOtherSuggestion, hasYourSuggestion;

	private BurstValue suggestedFee;

	private StringBuilder terms;

	public DisputeDialog(Window owner, Market market, ContractState contract) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Globals g = Globals.getInstance();
		this.contract = contract;
		this.isBuy = contract.getType() == ContractType.BUY;
		this.isCreator = contract.getCreator().equals(g.getAddress());
		this.isMediator = g.getMediators().isMediator(g.getAddress().getSignedLongId());
		this.isMediating = contract.getState() > SellContract.STATE_DISPUTE &&
				(contract.getMediator1() == g.getAddress().getSignedLongId() ||
				contract.getMediator2() == g.getAddress().getSignedLongId());
		
		if(this.isMediator)
			this.isCreator = true; // so that "your" is the maker
		
		this.hasOtherSuggestion = (isCreator && contract.hasStateFlag(SellContract.STATE_TAKER_DISPUTE)) 
				|| (!isCreator && contract.hasStateFlag(SellContract.STATE_CREATOR_DISPUTE));
		this.hasYourSuggestion = (isCreator && contract.hasStateFlag(SellContract.STATE_CREATOR_DISPUTE)) 
				|| (!isCreator && contract.hasStateFlag(SellContract.STATE_TAKER_DISPUTE));

		// This makes sure we do not violate the limitation on the amount being requested by the parties
		amount = contract.getAmountNQT() + 2 * (contract.getSecurityNQT() - contract.getFeeNQT());
		// Start with the *standard* value
		amountToCreator = contract.getSecurityNQT();
		if(isBuy)
			amountToCreator += contract.getAmountNQT();
		amountToTaker = amount - amountToCreator;

		suggestToYou = isCreator ? amountToCreator : amountToTaker;
		suggestToOther = isCreator ? amountToTaker : amountToCreator;
		
		if(hasYourSuggestion) {
			// Get the value from the contract if there is a previous one
			suggestToYou = contract.getDisputeAmount(isCreator, isCreator);
			suggestToOther = contract.getDisputeAmount(isCreator, !isCreator);
		}

		setTitle(tr(isMediating ? "disp_title_mediate" : isMediator ? "disp_title_details" : "disp_title"));

		this.market = market;

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amountField = new JFormattedTextField(NumberFormatting.BURST.getFormat());
		priceField = new JFormattedTextField(market.getNumberFormat().getFormat());
		totalField = new JTextField(16);

		fieldPanel.setBorder(BorderFactory.createTitledBorder(tr("disp_original_offer")));

		fieldPanel.add(new Desc(tr("offer_price", market), priceField));
		fieldPanel.add(new Desc(tr("offer_size", "BURST"), amountField));
		fieldPanel.add(new Desc(tr("offer_total", market), totalField));

		addressButton = new ClipboardAndQRButton(this, 18, amountField.getForeground());

		// Only FIAT can have zero security (on-ramp special contracts)
		security = new JSlider(market.isFiat() ? 0 : 1, 30);

		Desc securityDesc = new Desc("", security);
		fieldPanel.add(securityDesc);

		// No field from the original order can be changed
		priceField.setEnabled(false);
		amountField.setEnabled(false);
		security.setEnabled(false);
		totalField.setEnabled(false);
		int securityValue = (int)(contract.getSecurityNQT()*100/contract.getAmountNQT());
		security.getModel().setValue(securityValue);
		securityDesc.setDesc(tr("offer_security_deposit_percent", security.getValue()));

		priceField.setText(market.format(contract.getRate()));
		amountField.setText(NumberFormatting.BURST.format(contract.getAmountNQT()));

		conditions = new JTextPane();
		conditions.setContentType("text/html");
		conditions.setPreferredSize(new Dimension(80, 200));
		conditions.setEditable(false);
		
		acceptBox = new JCheckBox(tr("dlg_accept_terms"));
		acceptOtherTermsBox = new JCheckBox(tr(isMediator ? "disp_accept_taker_suggestion" : "disp_accept_other_suggestion"));
		acceptOtherTermsBox.addActionListener(this);
		if(isMediating) {
			acceptMakerTermsBox = new JCheckBox(tr("disp_accept_maker_suggestion"));
			acceptMakerTermsBox.addActionListener(this);
		}

		// Dispute panels
		JPanel otherPanel = new JPanel(new GridLayout(0, 2));
		otherPanel.setBorder(BorderFactory.createTitledBorder(tr(isMediator ? "disp_what_taker_suggested" : "disp_what_other_suggested")));
		otherPanel.add(new JLabel(tr(isMediator ? "disp_maker_should_get" : "disp_you_should_get")));
		otherPanel.add(otherAmountYouDesc = new Desc("", otherAmountYouSlider = new JSlider(0, 100)));
		otherPanel.add(new JLabel(tr(isMediator ? "disp_taker_should_get" : "disp_other_should_get")));
		otherPanel.add(otherAmountOtherDesc = new Desc("", otherAmountOtherSlider = new JSlider(0, 100)));
		otherAmountOtherSlider.setValue((int)(contract.getDisputeAmount(!isCreator, !isCreator)*100 / amount));
		otherAmountYouSlider.setValue((int)(contract.getDisputeAmount(!isCreator, isCreator)*100 / amount));
		otherAmountOtherDesc.setDesc(NumberFormatting.BURST.format(amount*otherAmountOtherSlider.getValue() / 100) + " BURST");
		otherAmountYouDesc.setDesc(NumberFormatting.BURST.format(amount*otherAmountYouSlider.getValue() / 100) + " BURST");

		otherAmountOtherSlider.setEnabled(false);
		otherAmountYouSlider.setEnabled(false);

		JPanel yourPanel = new JPanel(new BorderLayout());
		yourPanel.setBorder(BorderFactory.createTitledBorder(tr(isMediator ? "disp_what_maker_suggested" : "disp_what_you_suggest")));
		JPanel yourSuggestionPanel = new JPanel(new GridLayout(0, 2));
		yourPanel.add(yourSuggestionPanel, BorderLayout.CENTER);
		if(hasOtherSuggestion && !isMediator) {
			yourPanel.add(acceptOtherTermsBox, BorderLayout.PAGE_START);
		}
		yourSuggestionPanel.add(new JLabel(tr(isMediator ? "disp_maker_should_get" : "disp_you_should_get")));
		yourSuggestionPanel.add(yourAmountYouDesc = new Desc("", yourAmountYouSlider = new JSlider(0, 100)));
		yourSuggestionPanel.add(new JLabel(tr(isMediator ? "disp_taker_should_get" : "disp_other_should_get")));
		yourSuggestionPanel.add(yourAmountOtherDesc = new Desc("", yourAmountOtherSlider = new JSlider(0, 100)));
		

		JPanel mediatorPanel = new JPanel(new BorderLayout());
		if(isMediating) {
			mediatorPanel.setBorder(BorderFactory.createTitledBorder(tr("disp_mediator_decision")));
			JPanel useSuggestionsPanel = new JPanel(new FlowLayout());
			mediatorPanel.add(useSuggestionsPanel, BorderLayout.PAGE_START);
			if(hasOtherSuggestion)
				useSuggestionsPanel.add(acceptOtherTermsBox);
			if(hasYourSuggestion)
				useSuggestionsPanel.add(acceptMakerTermsBox);
			
			JPanel mediatorSuggestionPanel = new JPanel(new GridLayout(0, 2));
			mediatorPanel.add(mediatorSuggestionPanel, BorderLayout.CENTER);
			mediatorSuggestionPanel.add(new JLabel(tr("disp_maker_should_get")));
			mediatorSuggestionPanel.add(mediatorAmountMakerDesc = new Desc("", mediatorAmountMakerSlider = new JSlider(0, 100)));
			mediatorSuggestionPanel.add(new JLabel(tr("disp_taker_should_get")));
			mediatorSuggestionPanel.add(mediatorAmountTakerDesc = new Desc("", mediatorAmountTakerSlider = new JSlider(0, 100)));
			mediatorSuggestionPanel.add(new JLabel(tr("disp_amount_to_fee")));
			mediatorSuggestionPanel.add(amountToFeeContractLabel = new JLabel());
			
			mediatorAmountMakerSlider.addChangeListener(this);
			mediatorAmountTakerSlider.addChangeListener(this);
			
			mediatorAmountMakerSlider.setValue(0);
			mediatorAmountTakerSlider.setValue(0);
		}
		
		if(isMediator) {
			yourAmountYouSlider.setEnabled(false);
			yourAmountOtherSlider.setEnabled(false);
		}
		yourAmountYouSlider.addChangeListener(this);
		yourAmountOtherSlider.addChangeListener(this);
		
		yourAmountYouSlider.setValue((int)(suggestToYou*100 / amount));
		yourAmountOtherSlider.setValue((int)(suggestToOther*100 / amount));

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pinField = new JPasswordField(12);
		pinField.addActionListener(this);

		Icons i = new Icons(amountField.getForeground(), Constants.ICON_SIZE_MED);
		supportDiscord = new JButton(tr("dlg_support"), i.get(Icons.DISCORD));
		supportDiscord.setToolTipText(tr("dlg_support_discord"));
		supportReddit = new JButton(tr("dlg_support"), i.get(Icons.REDDIT));
		supportReddit.setToolTipText(tr("dlg_support_reddit"));
		cancelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("disp_open_dispute"));
		if(contract.getState() > SellContract.STATE_DISPUTE)
			okButton.setText(tr("disp_update_dispute"));
		if(isMediating)
			okButton.setText(tr("disp_settle"));
		
		getRootPane().setDefaultButton(okButton);

		supportDiscord.addActionListener(this);
		supportReddit.addActionListener(this);
		cancelButton.addActionListener(this);
		okButton.addActionListener(this);

		if(!isMediator) {
			buttonPane.add(new Desc(" ", supportDiscord));
			buttonPane.add(new Desc(" ", supportReddit));
		}
		if(!isMediator || isMediating) {
			buttonPane.add(new Desc(tr("dlg_pin"), pinField));
			buttonPane.add(new Desc(" ", okButton));
		}
		buttonPane.add(new Desc(" ", cancelButton));

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder(tr("dlg_terms_and_conditions") +
				(isMediator ? " (YOU is the offer MAKER)" : "")));
		JScrollPane scroll = new JScrollPane(conditions);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(conditions.getPreferredSize());
		conditionsPanel.add(scroll, BorderLayout.CENTER);
		
		if(!isMediator) {
			conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);
		}

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(fieldPanel, BorderLayout.PAGE_START);
		centerPanel.add(conditionsPanel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		JPanel suggestionsPanel = new JPanel(new GridLayout(0, 1));
		bottomPanel.add(suggestionsPanel, BorderLayout.PAGE_START);
		// Only show the other side proposal when there is one
		if(hasOtherSuggestion)
			suggestionsPanel.add(otherPanel);
		if(!isMediator || hasYourSuggestion)
			suggestionsPanel.add(yourPanel);
		if(isMediating)
			suggestionsPanel.add(mediatorPanel);
		bottomPanel.add(buttonPane, BorderLayout.PAGE_END);

		content.add(centerPanel, BorderLayout.CENTER);
		content.add(bottomPanel, BorderLayout.PAGE_END);

		suggestedFee = BurstNode.getInstance().getSuggestedFee().getPriorityFee();

		somethingChanged();
		pack();
	}

	@Override
	public void setVisible(boolean b) {
		if(b == true) {
			if(Contracts.isLoading()) {
				JOptionPane.showMessageDialog(getParent(), tr("main_cross_chain_loading"),
						tr("offer_processing"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(contract.hasPending()) {
				JOptionPane.showMessageDialog(getParent(), tr("offer_wait_confirm"),
						tr("offer_processing"), JOptionPane.ERROR_MESSAGE);
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
		
	    if(e.getSource() == supportDiscord) {
	    	Main.getInstance().browse(Constants.DISCORD_LINK);
            return;
        }
	    if(e.getSource() == supportReddit) {
	    	Main.getInstance().browse(Constants.REDDIT_LINK);
            return;
        }
		
		if(e.getSource() == acceptOtherTermsBox) {
			if(isMediating) {
				if(acceptOtherTermsBox.isSelected()) {
					acceptMakerTermsBox.setSelected(false);
					mediatorAmountMakerSlider.setValue(otherAmountYouSlider.getValue());
					mediatorAmountTakerSlider.setValue(otherAmountOtherSlider.getValue());
				}
				mediatorAmountMakerSlider.setEnabled(!acceptOtherTermsBox.isSelected());
				mediatorAmountTakerSlider.setEnabled(!acceptOtherTermsBox.isSelected());
				return;
			}
			if(acceptOtherTermsBox.isSelected()) {
				yourAmountOtherSlider.setValue(otherAmountOtherSlider.getValue());
				yourAmountYouSlider.setValue(otherAmountYouSlider.getValue());
			}
			yourAmountOtherSlider.setEnabled(!acceptOtherTermsBox.isSelected());
			yourAmountYouSlider.setEnabled(!acceptOtherTermsBox.isSelected());
		}
		if(e.getSource() == acceptMakerTermsBox) {
			if(acceptMakerTermsBox.isSelected()) {
				acceptOtherTermsBox.setSelected(false);
				mediatorAmountMakerSlider.setValue(yourAmountYouSlider.getValue());
				mediatorAmountTakerSlider.setValue(yourAmountOtherSlider.getValue());
			}
			mediatorAmountMakerSlider.setEnabled(!acceptMakerTermsBox.isSelected());
			mediatorAmountTakerSlider.setEnabled(!acceptMakerTermsBox.isSelected());
		}

		if(e.getSource() == okButton || e.getSource() == pinField) {
			String error = null;
			Component errorComp = null;
			Globals g = Globals.getInstance();

			if(error == null) {
				// check if something changed
				//				if(priceValue.longValue() == contract.getRate() &&
				//						(accountDetails.getText().length()==0 || 
				//						accountDetails.getText().equals(contract.getMarketAccount()))
				//						)
				//					error = tr("offer_no_changes");
			}
			
			if(error == null && !isMediator && !acceptBox.isSelected()) {
				error = tr("dlg_accept_first");
				errorComp = acceptBox;
				acceptBox.requestFocus();
			}

			if(error == null && isMediating && amountToFeeContract < 0) {
				error = tr("med_invalid_amounts");
			}

			if(error == null && !g.checkPIN(pinField.getPassword())) {
				error = tr("dlg_invalid_pin");
				pinField.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(errorComp != null ? errorComp : okButton);
				return;
			}

			// all set, lets place the dispute update
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				long amountToCreator = amount * (isCreator ? yourAmountYouSlider.getValue() : yourAmountOtherSlider.getValue()) / 100;
				long amountToTaker = amount - amountToCreator;
				
				if(isMediating) {
					amountToCreator = amount*mediatorAmountMakerSlider.getValue() / 100;
					amountToTaker = amount*mediatorAmountTakerSlider.getValue() / 100;
				}

				// we are sending the dispute message with our amounts
				byte[] message = BT.callMethodMessage(contract.getMethod("dispute"), amountToCreator, amountToTaker);
				BurstValue amountToSend = BurstValue.fromPlanck(contract.getActivationFee());

				Single<byte[]> utx = g.getNS().generateTransactionWithMessage(contract.getAddress(), g.getPubKey(),
						amountToSend, suggestedFee,
						Constants.BURST_EXCHANGE_DEADLINE, message);

				Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
					byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsignedTransactionBytes);
					return g.getNS().broadcastTransaction(signedTransactionBytes);
				});
				TransactionBroadcast tb = tx.blockingGet();
				tb.getTransactionId();

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

	private void header(String header) {
		terms = new StringBuilder();
		terms.append(HTML_STYLE);
		terms.append("<h3>").append(header).append("</h3>");
	}

	private void append(String text) {
		terms.append("<p>").append(text).append("</p>");
	}

	private void somethingChanged(){
		MarketAccount account = market.parseAccount(contract.getMarketAccount());
		
		acceptBox.setSelected(false);

		if(priceField.getText().length()==0 || amountField.getText().length()==0)
			return;

		if(account != null && account.getFields().get(MarketCrypto.ADDRESS) != null)
			addressButton.setURI(account.getFields().get(MarketCrypto.ADDRESS));

		try {
			// Price is on the selected market
			Number priceN = NumberFormatting.parse(priceField.getText());
			Number amountN = NumberFormatting.parse(amountField.getText());

			double totalValue = priceN.doubleValue()*amountN.doubleValue()*market.getFactor();
			totalField.setText(market.format(Math.round(totalValue)));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		boolean unexpectedState = false;

		Calendar deadline = Calendar.getInstance();
		deadline.setTime(contract.getTakeTimestamp().getAsDate());
		deadline.add(Calendar.HOUR_OF_DAY, isBuy ? 24 : market.getPaymentTimeout(account.getFields()));
		Calendar deadline2 = Calendar.getInstance();
		deadline2.setTime(contract.getTakeTimestamp().getAsDate());
		deadline2.add(Calendar.HOUR_OF_DAY, isBuy ? 48 : 24);

		if(isBuy) {
			header(tr(isCreator ? "disp_orig_buy_creator" : "disp_orig_buy_taker",
					totalField.getText(), market,
					HistoryPanel.DATE_FORMAT.format(deadline.getTime()),
					contract.getMarketAccount(),
					HistoryPanel.DATE_FORMAT.format(deadline2.getTime())
					));
		}
		else {
			header(tr(isCreator ? "disp_orig_sell_creator" : "disp_orig_sell_taker",
					totalField.getText(), market,
					HistoryPanel.DATE_FORMAT.format(deadline.getTime()),
					contract.getMarketAccount(),
					HistoryPanel.DATE_FORMAT.format(deadline2.getTime())
					));
		}
		append(tr("disp_orig_burst_deposits",
				NumberFormatting.BURST.format(isCreator ? amountToCreator : amountToTaker),
				NumberFormatting.BURST.format(isCreator ? amountToTaker : amountToCreator)
				));
		append(tr("disp_dispute_terms", suggestedFee.add(BurstValue.fromPlanck(contract.getActivationFee())).toUnformattedString()));
		if(!isMediator)
			append(tr("disp_mediating", suggestedFee.add(BurstValue.fromPlanck(contract.getActivationFee())).toUnformattedString()));

		// checking it has the balance before requesting the deposit
		if(unexpectedState) {
			// the contract seems to be invalid, this should not happen
			header(tr("offer_invalid_contact_mediator"));
			return;
		}

		String termsText = terms.toString();
		if(!conditions.getText().equals(termsText)) {
			conditions.setText(termsText);

			// TODO: Apparently tags are not counted so we need to subtract 3
			int pos = termsText.indexOf(BUTTON_TEXT) - HTML_STYLE.length() - 3;
			if(pos > 0) {
				conditions.select(pos, pos + BUTTON_TEXT.length());
				conditions.insertComponent(addressButton);
			}

			conditions.setCaretPosition(0);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if(e.getSource() == yourAmountYouSlider) {
			yourAmountYouDesc.setDesc(NumberFormatting.BURST.format(amount*yourAmountYouSlider.getValue() / 100) + " BURST");
			yourAmountOtherSlider.setValue(100-yourAmountYouSlider.getValue());
		}
		if(e.getSource() == yourAmountOtherSlider) {
			yourAmountOtherDesc.setDesc(NumberFormatting.BURST.format(amount*yourAmountOtherSlider.getValue() / 100) + " BURST");
			yourAmountYouSlider.setValue(100-yourAmountOtherSlider.getValue());
		}
		if(e.getSource() == mediatorAmountMakerSlider) {
			mediatorAmountMakerDesc.setDesc(NumberFormatting.BURST.format(amount*mediatorAmountMakerSlider.getValue() / 100) + " BURST");
		}
		if(e.getSource() == mediatorAmountTakerSlider) {
			mediatorAmountTakerDesc.setDesc(NumberFormatting.BURST.format(amount*mediatorAmountTakerSlider.getValue() / 100) + " BURST");
		}
		if(e.getSource() == mediatorAmountMakerSlider || e.getSource() == mediatorAmountTakerSlider) {
			int amountToSides = mediatorAmountMakerSlider.getValue() + mediatorAmountTakerSlider.getValue();
			amountToFeeContract = (100-amountToSides)*amount / 100;
			amountToFeeContractLabel.setText(NumberFormatting.BURST.format(amountToFeeContract) + " BURST");
			amountToFeeContractLabel.setForeground(amountToSides > 100 ? Color.RED : mediatorAmountTakerSlider.getForeground());
		}
	}
}
