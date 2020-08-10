package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bt.Contract;
import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import btdex.ledger.BurstLedger;
import btdex.ledger.LedgerService;
import btdex.ledger.LedgerService.SignCallBack;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class PlaceTokenOrderDialog extends JDialog implements ActionListener, DocumentListener, SignCallBack {
	private static final long serialVersionUID = 1L;

	Market market;

	JTextField amountField, priceField, totalField;

	JTextPane conditions;
	JCheckBox acceptBox;

	JPasswordField pinField;
	private JTextField ledgerStatus;
	private byte[] unsigned;

	private JButton okButton;
	private JButton cancelButton;

	private BurstValue suggestedFee;

	private BurstValue amountValue, priceValue;

	private boolean isAsk;

	public PlaceTokenOrderDialog(JFrame owner, Market market, AssetOrder order, boolean isAsk) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.isAsk = isAsk;

		setTitle(tr(isAsk ? "token_sell_for_burst" : "token_buy_with_burst", market));
		this.market = market;

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amountField = new JFormattedTextField(market.getNumberFormat().getFormat());
		priceField = new JFormattedTextField(NumberFormatting.BURST.getFormat());
		totalField = new JTextField(16);
		totalField.setEditable(false);

		amountField.getDocument().addDocumentListener(this);
		priceField.getDocument().addDocumentListener(this);

		fieldPanel.setBorder(BorderFactory.createTitledBorder(tr("token_offer_details")));

		fieldPanel.add(new Desc(tr("offer_price", "BURST"), priceField));
		fieldPanel.add(new Desc(tr("offer_size", market), amountField));
		fieldPanel.add(new Desc(tr("offer_total", "BURST"), totalField));

		conditions = new JTextPane();
		conditions.setContentType("text/html");
		conditions.setEditable(false);
		conditions.setPreferredSize(new Dimension(80, 200));

		acceptBox = new JCheckBox(tr("dlg_accept_terms"));

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pinField = new JPasswordField(12);
		pinField.addActionListener(this);

		cancelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr(isAsk ? "offer_confirm_limit_sell" : "offer_confirm_limit_buy"));
		getRootPane().setDefaultButton(okButton);

		cancelButton.addActionListener(this);
		okButton.addActionListener(this);

		if(Globals.getInstance().usingLedger()) {
			ledgerStatus = new JTextField(26);
			ledgerStatus.setEditable(false);
			buttonPane.add(new Desc(tr("ledger_status"), ledgerStatus));
			LedgerService.getInstance().setCallBack(this);
		}
		else
			buttonPane.add(new Desc(tr("dlg_pin"), pinField));
		buttonPane.add(new Desc(" ", okButton));
		buttonPane.add(new Desc(" ", cancelButton));

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder(tr("dlg_terms_and_conditions")));
		JScrollPane scroll = new JScrollPane(conditions);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(conditions.getPreferredSize());
		conditionsPanel.add(scroll, BorderLayout.CENTER);

		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(fieldPanel, BorderLayout.PAGE_START);
		centerPanel.add(conditionsPanel, BorderLayout.CENTER);

		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		suggestedFee = BurstNode.getInstance().getSuggestedFee().getPriorityFee();

		if(order != null) {
			// taking this order
			priceField.setText(NumberFormatting.BURST.format(order.getPrice().longValue()*market.getFactor()));
			amountField.setText(market.format(order.getQuantity().longValue()));
		}
		somethingChanged();
		pack();

		SwingUtilities.invokeLater(new Runnable() {  
			public void run() {
				priceField.requestFocusInWindow();
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cancelButton) {
			setVisible(false);
		}

		if(e.getSource() == okButton || e.getSource() == pinField) {
			String error = null;
			Globals g = Globals.getInstance();

			if(error == null && (priceValue == null || priceValue.longValue() <= 0)) {
				error = tr("offer_invalid_price");
			}
			if(error == null && (amountValue == null || amountValue.longValue() <= 0)) {
				error = tr("send_invalid_amount");
			}

			if(error == null && !acceptBox.isSelected()) {
				error = tr("dlg_accept_first");
				acceptBox.requestFocus();
			}

			if(error == null && !g.usingLedger() && !g.checkPIN(pinField.getPassword())) {
				error = tr("dlg_invalid_pin");
				pinField.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}
			
			if(g.usingLedger()) {
				if(BurstLedger.isAppAvailable())
					Toast.makeText((JFrame) this.getOwner(), tr("ledger_auth"), Toast.Style.NORMAL).display(okButton);
				else {
					Toast.makeText((JFrame) this.getOwner(), tr("ledger_error"), Toast.Style.ERROR).display(okButton);
					return;
				}
			}

			// all set, lets place the order
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				Single<byte[]> utx = null;

				if(this.isAsk)
					utx = g.getNS().generatePlaceAskOrderTransaction(g.getPubKey(), market.getTokenID(),
							amountValue, priceValue, suggestedFee, Constants.BURST_EXCHANGE_DEADLINE);
				else
					utx = g.getNS().generatePlaceBidOrderTransaction(g.getPubKey(), market.getTokenID(),
							amountValue, priceValue, suggestedFee, Constants.BURST_EXCHANGE_DEADLINE);

				unsigned = utx.blockingGet();
				if(g.usingLedger()) {
					LedgerService.getInstance().requestSign(unsigned, null, g.getLedgerIndex());
					okButton.setEnabled(false);
					priceField.setEnabled(false);
					amountField.setEnabled(false);
					
					Toast.makeText((JFrame) this.getOwner(), tr("ledger_authorize"), Toast.Style.NORMAL).display(okButton);
					
					return;
				}
				
				byte[] signedTransactionBytes = g.signTransaction(pinField.getPassword(), unsigned);
				reportSigned(signedTransactionBytes, null);
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

			long pricePlanck = (long) (priceN.doubleValue()*(Contract.ONE_BURST/market.getFactor()));
			priceValue = BurstValue.fromPlanck(pricePlanck);
			amountValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*market.getFactor()));

			double totalValue = priceN.doubleValue()*amountN.doubleValue();
			totalField.setText(NumberFormatting.BURST.format(totalValue));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		StringBuilder terms = new StringBuilder();
		terms.append(PlaceOrderDialog.HTML_STYLE);

		terms.append("<h3>").append(tr("token_terms_brief", isAsk ? tr("token_sell") : tr("token_buy"),
				amountField.getText(), market, priceField.getText())).append("</h3>");
		terms.append("<p>").append(tr("token_terms_details",
				NumberFormatting.BURST.format(suggestedFee.longValue()))).append("</p>");
		terms.append("<p>").append(tr("token_terms_closing")).append("</p>");


		if(!conditions.getText().equals(terms.toString())) {
			conditions.setText(terms.toString());
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

	@Override
	public void ledgerStatus(String txt) {
		ledgerStatus.setText(txt);
		ledgerStatus.setCaretPosition(0);
	}

	@Override
	public void reportSigned(byte[] signed, byte[] signed2) {
		if(!isVisible())
			return; // already closed by cancel, so we will not broadcast anyway
		
		if(signed == null) {
			// when coming from the hardware wallet
			okButton.setEnabled(true);
			priceField.setEnabled(true);
			amountField.setEnabled(true);

			setCursor(Cursor.getDefaultCursor());
			
			Toast.makeText((JFrame) this.getOwner(), tr("ledger_denied"), Toast.Style.ERROR).display(okButton);
			
			return;
		}
		TransactionBroadcast tb = Globals.getInstance().getNS().broadcastTransaction(signed).blockingGet();
		setVisible(false);

		Toast.makeText((JFrame) this.getOwner(),
				tr("send_tx_broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
	}
}
