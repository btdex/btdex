package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
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

import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class SendDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextField recipient;
	JTextField message;
	JTextField amount;
	JPasswordField pin;
	JSlider fee;
	BurstValue selectedFee;

	private JButton okButton;

	private JButton calcelButton;

	private Market token;

	public SendDialog(JFrame owner, Market token) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.token = token;

		setTitle(String.format("Send %s", token==null ? "BURST" : token));

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		recipient = new JTextField(26);
		message = new JTextField(26);

		pin = new JPasswordField(12);
		pin.addActionListener(this);

		amount = new JFormattedTextField(NumberFormatting.NF(5, 8));
		fee = new JSlider(1, 4);

		topPanel.add(new Desc("Recipient", recipient));
		topPanel.add(new Desc("Message", message));
		message.setToolTipText("Leave empty for no message");

		panel.add(new Desc(String.format("Amount (%s)", token==null ? "BURST" : token), amount));
		Desc feeDesc = new Desc("", fee);
		panel.add(feeDesc);
		fee.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				String feeType;
				switch (fee.getValue()) {
				case 1:
					feeType = "minimum";
					selectedFee = BurstValue.fromPlanck(Constants.FEE_QUANT);
					break;
				case 2:
					feeType = "cheap";
					selectedFee = Globals.getInstance().getSuggestedFee().getCheapFee();
					break;
				case 3:
					feeType = "standard";
					selectedFee = Globals.getInstance().getSuggestedFee().getStandardFee();
					break;
				default:
					feeType = "priority";
					selectedFee = Globals.getInstance().getSuggestedFee().getPriorityFee();
					break;
				}
				feeDesc.setDesc(String.format("Fee (%s %s BURST)", feeType,
						ContractState.format(selectedFee.longValue())));
			}
		});

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

		fee.getModel().setValue(4);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}
		if(e.getSource() == okButton || e.getSource() == pin) {
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
			
			String msg = null;
			if(message.getText().length()>0)
				msg = message.getText();

			Number amountNumber = null;
			if(error == null) {
				try {
					amountNumber = NumberFormatting.NF(5, 8).parse(amount.getText());
				} catch (ParseException e1) {
					amount.requestFocus();
					error = "Invalid amount";
				}
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
			}
			else {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				try {
					// all set, lets make the transfer
					Single<byte[]> utx = null;
					if(token!=null) {
						utx = g.getNS().generateTransferAssetTransactionWithMessage(g.getPubKey(), BurstAddress.fromId(recID),
								token.getTokenID(), BurstValue.fromPlanck((long)(amountNumber.doubleValue()*token.getFactor())),
								selectedFee, 1440, msg);
					}
					else {
						utx = g.getNS().generateTransactionWithMessage(BurstAddress.fromId(recID), g.getPubKey(),
							BurstValue.fromBurst(amountNumber.doubleValue()),
							selectedFee, 1440, msg);
					}

					Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
								byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
								return g.getNS().broadcastTransaction(signedTransactionBytes);
							});
					TransactionBroadcast tb = tx.blockingGet();
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
	}

}
