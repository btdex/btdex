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
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import static btdex.locale.Translation.tr;
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

		setTitle(tr("main_send", token==null ? "BURST" : token));

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		recipient = new JTextField(26);
		message = new JTextField(26);

		pin = new JPasswordField(12);
		pin.addActionListener(this);

		amount = new JFormattedTextField(token==null ? NumberFormatting.BURST.getFormat() : token.getNumberFormat().getFormat());
		fee = new JSlider(1, 4);

		topPanel.add(new Desc(tr("send_recipient"), recipient));
		topPanel.add(new Desc(tr("send_message"), message));
		message.setToolTipText(tr("send_empty_for_no_message"));

		panel.add(new Desc(tr("send_amount", token==null ? "BURST" : token), amount));
		Desc feeDesc = new Desc("", fee);
		panel.add(feeDesc);
		fee.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				String feeType;
				switch (fee.getValue()) {
				case 1:
					feeType = tr("fee_minimum");
					selectedFee = BurstValue.fromPlanck(Constants.FEE_QUANT);
					break;
				case 2:
					feeType = tr("fee_cheap");
					selectedFee = Globals.getInstance().getSuggestedFee().getCheapFee();
					break;
				case 3:
					feeType = tr("fee_standard");
					selectedFee = Globals.getInstance().getSuggestedFee().getStandardFee();
					break;
				default:
					feeType = tr("fee_priority");
					selectedFee = Globals.getInstance().getSuggestedFee().getPriorityFee();
					break;
				}
				feeDesc.setDesc(tr("send_fee", feeType, selectedFee.toUnformattedString()));
			}
		});

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		calcelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("dlg_ok"));

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		buttonPane.add(new Desc(tr("dlg_pin"), pin));
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
				error = tr("send_invalid_recipient");

			if(error == null && !g.checkPIN(pin.getPassword())) {
				error = tr("dlg_invalid_pin");
				pin.requestFocus();
			}
			
			String msg = null;
			if(message.getText().length()>0)
				msg = message.getText();

			Number amountNumber = null;
			if(error == null) {
				try {
					amountNumber = NumberFormatting.parse(amount.getText());
				} catch (ParseException e1) {
					amount.requestFocus();
					error = tr("send_invalid_amount");
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
								selectedFee, Constants.BURST_DEADLINE, msg);
					}
					else {
						utx = g.getNS().generateTransactionWithMessage(BurstAddress.fromId(recID), g.getPubKey(),
							BurstValue.fromBurst(amountNumber.doubleValue()),
							selectedFee, Constants.BURST_DEADLINE, msg);
					}

					Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
								byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
								return g.getNS().broadcastTransaction(signedTransactionBytes);
							});
					TransactionBroadcast tb = tx.blockingGet();
					setVisible(false);

					Toast.makeText((JFrame) this.getOwner(),
							tr("send_tx_broadcast", tb.getTransactionId().toString()), Toast.Style.SUCCESS).display();
				}
				catch (Exception ex) {
					Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
				}
				setCursor(Cursor.getDefaultCursor());
			}
		}
	}
}
