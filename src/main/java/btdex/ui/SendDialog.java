package btdex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class SendDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextField recipient;
	JTextField message;
	JTextField amount;
	JPasswordField pin;
	JSlider fee;

	private final int N_SLOTS = 1020;
	private final int FEE_QUANT = 735000;

	private JButton okButton;

	private JButton calcelButton;

	public SendDialog(JFrame owner, Market token) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle(String.format("Send %s", token==null ? "BURST" : token));

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		recipient = new JTextField(26);
		message = new JTextField(26);

		pin = new JPasswordField(12);

		amount = new JFormattedTextField(Globals.NF_FULL);
		fee = new JSlider(1, N_SLOTS);

		topPanel.add(new Desc("Recipient", recipient));
		topPanel.add(new Desc("Message", message));
		message.setToolTipText("Leave empty for no message");

		panel.add(new Desc(String.format("Amount (%s)", token==null ? "BURST" : token), amount));
		Desc feeDesc = new Desc("", fee);
		panel.add(feeDesc);
		fee.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				int value = fee.getValue() * FEE_QUANT;
				feeDesc.setDesc(String.format("Fee (%s) BURST", ContractState.format(value)));
			}
		});
		fee.getModel().setValue(10);

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

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

		content.add(topPanel, BorderLayout.PAGE_START);
		content.add(panel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		pack();

		try {
			FeeSuggestion suggested = Globals.getInstance().getNS().suggestFee().blockingGet();
			int feeInt = (int)suggested.getPriorityFee().longValue()/FEE_QUANT;
			fee.getModel().setValue(feeInt);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}
		if(e.getSource() == okButton) {
			Globals g = Globals.getInstance();

			String error = null;
			String sadd = recipient.getText();
			BurstID recID = null;
			if(sadd.startsWith("BURST-")) {
				try {
					recID = Globals.BC.rsDecode(sadd.substring(5));
				}
				catch (Exception ex) {
				}
			}
			if(recID == null)
				error = "Invalid recipient address";

			if(error == null && !g.checkPIN(pin.getPassword())) {
				error = "Invalid PIN";
				pin.requestFocus();
			}

			Number amountNumber = null;
			if(error == null) {
				try {
					amountNumber = Globals.NF_FULL.parse(amount.getText());
				} catch (ParseException e1) {
					amount.requestFocus();
					error = "Invalid amount";
				}
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
			}
			else {
				long feePlanck = FEE_QUANT * fee.getValue();

				try {
					// all set, lets make the transfer
					Single<TransactionBroadcast> tx = g.getNS().generateTransaction(BurstAddress.fromId(recID), g.getPubKey(),
							BurstValue.fromBurst(amountNumber.doubleValue()),
							BurstValue.fromPlanck(feePlanck), 1440)
							.flatMap(unsignedTransactionBytes -> {
								byte[] signedTransactionBytes = Globals.getInstance().signTransaction(pin.getPassword(), unsignedTransactionBytes);
								return Globals.getInstance().getNS().broadcastTransaction(signedTransactionBytes);
							});

					TransactionBroadcast tb = tx.blockingGet();
					tb.getTransactionId();
					setVisible(false);

					Toast.makeText((JFrame) this.getOwner(),
							String.format("Transaction %s broadcasted", tb.getTransactionId().toString())).display();
				}
				catch (Exception ex) {
					Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
				}
			}
		}
	}

}