package btdex.ui;

import static btdex.locale.Translation.tr;

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

import btdex.core.BurstNode;
import btdex.core.Constants;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import btdex.ledger.LedgerService;
import btdex.ledger.LedgerService.SignCallBack;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstID;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class SendDialog extends JDialog implements ActionListener, SignCallBack {
	private static final long serialVersionUID = 1L;

	private JTextField recipient;
	private JTextField message;
	private JTextField amount;
	private JTextField ledgerStatus;
	private byte[] unsigned;
	private JPasswordField pin;
	private JSlider fee;
	private BurstValue selectedFee;

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
		FeeSuggestion suggestedFee = BurstNode.getInstance().getSuggestedFee();
		
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
					selectedFee = suggestedFee.getCheapFee();
					break;
				case 3:
					feeType = tr("fee_standard");
					selectedFee = suggestedFee.getStandardFee();
					break;
				default:
					feeType = tr("fee_priority");
					selectedFee = suggestedFee.getPriorityFee();
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

		if(Globals.getInstance().usingLedger()) {
			ledgerStatus = new JTextField(26);
			ledgerStatus.setEditable(false);
			buttonPane.add(new Desc(tr("ledger_status"), ledgerStatus));
			LedgerService.getInstance().setCallBack(this);
		}
		else
			buttonPane.add(new Desc(tr("dlg_pin"), pin));
		buttonPane.add(new Desc(" ", okButton));
		buttonPane.add(new Desc(" ", calcelButton));

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

			if(error == null && !g.usingLedger() && !g.checkPIN(pin.getPassword())) {
				error = tr("dlg_invalid_pin");
				pin.requestFocus();
			}
			
			String msg = null;
			if(message.getText().length()>0) {
				msg = message.getText();
				if(msg.length() > 1000) {
					error = tr("send_invalid_message");
				}
			}

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
								selectedFee, Constants.BURST_SEND_DEADLINE, msg);
					}
					else {
						utx = g.getNS().generateTransactionWithMessage(BurstAddress.fromId(recID), g.getPubKey(),
							BurstValue.fromBurst(amountNumber.doubleValue()),
							selectedFee, Constants.BURST_SEND_DEADLINE, msg);
					}
					
					unsigned = utx.blockingGet();
					if(g.usingLedger()) {
						LedgerService.getInstance().requestSign(unsigned, null, g.getLedgerIndex());
						okButton.setEnabled(false);
						recipient.setEnabled(false);
						message.setEnabled(false);
						amount.setEnabled(false);
						fee.setEnabled(false);
						
						Toast.makeText((JFrame) this.getOwner(), tr("ledger_authorize"), Toast.Style.NORMAL).display(okButton);
						
						return;
					}
					byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsigned);
					reportSigned(signedTransactionBytes, null);
				}
				catch (Exception ex) {
					Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
				}
				setCursor(Cursor.getDefaultCursor());
			}
		}
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
			recipient.setEnabled(true);
			message.setEnabled(true);
			amount.setEnabled(true);
			fee.setEnabled(true);

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
