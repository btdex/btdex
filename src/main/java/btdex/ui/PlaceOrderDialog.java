package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Properties;

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
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.Order;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class PlaceOrderDialog extends JDialog implements ActionListener, DocumentListener {
	private static final long serialVersionUID = 1L;

	Market market;

	JComboBox<String> account;
	JTextField accountDetails;
	JTextField amount, price, total;
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

	private BurstValue totalValue, quantityValue, priceValue;

	public PlaceOrderDialog(JFrame owner, Market market, Order order) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		isToken = market.getTokenID()!=null;

		if(isToken)
			setTitle(String.format("Exchange %s for BURST", market.toString()));
		else
			setTitle(String.format("Sell BURST for %s", market.toString()));

		this.market = market;

		account = new JComboBox<>();
		accountDetails = new JTextField(36);
		accountDetails.setEditable(false);

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));
		topPanel.setBorder(BorderFactory.createTitledBorder("Account to receive " + market.toString()));
		topPanel.add(account);
		topPanel.add(accountDetails);

		JPanel fieldPanel = new JPanel(new GridLayout(0, 2, 4, 4));

		amount = new JFormattedTextField(Globals.NF_FULL);
		price = new JFormattedTextField(Globals.NF_FULL);
		total = new JTextField(16);
		total.setEditable(false);

		amount.getDocument().addDocumentListener(this);
		price.getDocument().addDocumentListener(this);

		fieldPanel.setBorder(BorderFactory.createTitledBorder("Offer configuration"));

		if(isToken) {
			buyToken = new JRadioButton(String.format("Buy %s with BURST", market), true);
			sellToken = new JRadioButton(String.format("Sell %s for BURST", market));

			fieldPanel.add(buyToken);
			fieldPanel.add(sellToken);

			ButtonGroup bgroup = new ButtonGroup();
			bgroup.add(buyToken);
			bgroup.add(sellToken);
			buyToken.addActionListener(this);
			sellToken.addActionListener(this);
		}

		fieldPanel.add(new Desc("Price (" + (isToken ? "BURST" : market) + ")", price));
		fieldPanel.add(new Desc("Size (" + (isToken ? market : "BURST") + ")", amount));
		fieldPanel.add(new Desc("Total (" + (isToken ? "BURST" : market) + ")", total));

		if(!isToken) {
			Properties conf = Globals.getConf();

			for (int i = 0; i < 100; i++) {
				String acNameKey = market.toString() + i + ".name";

				String acName = conf.getProperty(acNameKey, null);
				if(acName == null || acName.length()==0)
					break;
				account.addItem(acName);
			}

			security = new JSlider(0, 20);

			Desc securityDesc = new Desc("", security);
			fieldPanel.add(securityDesc);

			security.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					int value = security.getValue();
					String desc = String.format("Security deposit %d %%", value);
					if(value==0)
						desc = "Security deposit 0.1 %";

					securityDesc.setDesc(desc);
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
		conditionsPanel.add(new JScrollPane(conditions), BorderLayout.CENTER);

		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(fieldPanel, BorderLayout.CENTER);
		centerPanel.add(conditionsPanel, BorderLayout.PAGE_END);

		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		suggestedFee = Globals.getInstance().getNS().suggestFee().blockingGet();
		
		if(order != null) {
			// taking this order
			if(order.getType().equals("bid"))
				sellToken.setSelected(true);
			else
				buyToken.setSelected(true);
			
			price.setText(ContractState.format(order.getPrice().longValue()/market.getFactor()));
			amount.setText(market.format(order.getQuantity().longValue()));
			somethingChanged();
		}

		pack();
	}

	@Override
	public void setVisible(boolean b) {
		if(account.getItemCount()==0 && !isToken) {
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

		if(e.getSource()==buyToken || e.getSource()==sellToken) {
			somethingChanged();
		}

		if(e.getSource() == okButton || e.getSource() == pin) {
			String error = null;
			Globals g = Globals.getInstance();

			if(account.getSelectedIndex() < 0 && !isToken) {
				error = "You need to register an account first";
			}

			if(error == null && totalValue == null) {
				error = "Invalid price or amount";
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
				Single<byte[]> utx = null;
				if(isToken) {
					if(sellToken.isSelected())
						utx = g.getNS().generatePlaceAskOrderTransaction(g.getPubKey(), market.getTokenID(),
								quantityValue, priceValue, suggestedFee.getStandardFee(), 1440);
					else
						utx = g.getNS().generatePlaceBidOrderTransaction(g.getPubKey(), market.getTokenID(),
								quantityValue, priceValue, suggestedFee.getStandardFee(), 1440);
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
						String.format("Transaction %s broadcasted", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
			}
			catch (Exception ex) {
				Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
			}
		}
	}

	private void somethingChanged(){
		acceptBox.setSelected(false);

		totalValue = null;
		quantityValue = null;
		priceValue = null;

		if(price.getText().length()==0 || amount.getText().length()==0)
			return;

		try {
			Number priceN = Globals.NF.parse(price.getText());
			Number amountN = Globals.NF.parse(amount.getText());
			
			priceValue = BurstValue.fromPlanck((long)(priceN.doubleValue()*market.getFactor()));
			quantityValue = BurstValue.fromPlanck((long)(amountN.doubleValue()*market.getFactor()));

			totalValue = BurstValue.fromBurst(priceN.doubleValue()*amountN.doubleValue());
			total.setText(market.format(totalValue.longValue()/market.getFactor()));
		} catch (ParseException e) {
			e.printStackTrace();
		}


		if(isToken) {
			boolean isSell = sellToken.isSelected();

			String terms = "You are %s up to %s %s %s BURST at a price of %s BURST each.\n\n"
					+ "This order can be partially filled and will be open until filled or cancelled. "
					+ "No trading fees apply, only a one time %s BURST transaction fee.";

			terms = String.format(terms,
					isSell ? "selling" : "buying",
							amount.getText(),
							market,
							isSell ? "for" : "with",
									price.getText(), ContractState.format(suggestedFee.getStandardFee().longValue()));

			if(!conditions.getText().equals(terms))
				conditions.setText(terms);
		}
		else {

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
