package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;

import bt.BT;
import btdex.core.Constants;
import btdex.core.ContractState;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.NumberFormatting;
import static btdex.locale.Translation.tr;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.AssetOrder;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.TransactionBroadcast;
import io.reactivex.Single;

public class CancelOrderDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	Market market;
	AssetOrder order;
	ContractState state;

	JTextPane conditions;
	JCheckBox acceptBox;

	JPasswordField pin;

	private JButton okButton;
	private JButton calcelButton;

	private boolean isToken;

	private FeeSuggestion suggestedFee;

	public CancelOrderDialog(JFrame owner, Market market, AssetOrder order, ContractState state) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setTitle(tr("canc_cancel_order"));

		isToken = market.getTokenID()!=null;

		this.market = market;
		this.order = order;
		this.state = state;

		conditions = new JTextPane();
		conditions.setPreferredSize(new Dimension(80, 120));
		conditions.setEditable(false);

		acceptBox = new JCheckBox(tr("dlg_accept_terms"));

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		pin = new JPasswordField(12);
		pin.addActionListener(this);

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

		JPanel conditionsPanel = new JPanel(new BorderLayout());
		conditionsPanel.setBorder(BorderFactory.createTitledBorder(tr("dlg_terms_and_conditions")));
		conditionsPanel.add(new JScrollPane(conditions), BorderLayout.CENTER);

		conditionsPanel.add(acceptBox, BorderLayout.PAGE_END);

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(conditionsPanel, BorderLayout.PAGE_END);

		content.add(centerPanel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		suggestedFee = Globals.getInstance().getNS().suggestFee().blockingGet();

		boolean isSell = order==null || order.getType() == AssetOrder.OrderType.ASK;
		
		StringBuilder terms = new StringBuilder();
		terms.append(tr("canc_terms_brief", isSell ? tr("token_sell") : tr("token_buy"), market,
				isToken ? order.getId() : state.getAddress().getFullAddress()));
		if(isToken) {
			terms.append("\n\n").append(tr("canc_terms_token",
					NumberFormatting.BURST.format(suggestedFee.getPriorityFee().longValue())));
		}
		else {
			terms.append("\n\n").append(tr("canc_terms_contract",
					state.getBalance().toUnformattedString(),
					NumberFormatting.BURST.format(state.getActivationFee() + suggestedFee.getPriorityFee().longValue())));			
		}
		
		conditions.setText(terms.toString());
		conditions.setCaretPosition(0);
		
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			setVisible(false);
		}

		if(e.getSource() == okButton || e.getSource() == pin) {
			String error = null;
			Globals g = Globals.getInstance();

			if(error == null && !acceptBox.isSelected()) {
				error = tr("dlg_accept_first");
				acceptBox.requestFocus();
			}
			
			if(error == null && !g.checkPIN(pin.getPassword())) {
				error = tr("dlg_invalid_pin");
				pin.requestFocus();
			}

			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
				return;
			}

			// all set, lets cancel the order
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				Single<byte[]> utx = null;

				if(isToken) {
					if(order.getType() == AssetOrder.OrderType.BID)
						utx = g.getNS().generateCancelBidOrderTransaction(g.getPubKey(), order.getId(),
								suggestedFee.getPriorityFee(), 1440);
					else
						utx = g.getNS().generateCancelAskOrderTransaction(g.getPubKey(), order.getId(),
							suggestedFee.getPriorityFee(), 1440);
				}
				else {
					// update the security to zero to withdraw all funds
					byte[] message = BT.callMethodMessage(Contracts.getContract().getMethod("update"), 0L);
					
					BurstValue amountToSend = BurstValue.fromPlanck(state.getActivationFee());

					utx = g.getNS().generateTransactionWithMessage(state.getAddress(), g.getPubKey(),
							amountToSend, suggestedFee.getPriorityFee(),
							Constants.BURST_DEADLINE, message);
				}
				
				Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
					byte[] signedTransactionBytes = g.signTransaction(pin.getPassword(), unsignedTransactionBytes);
					return g.getNS().broadcastTransaction(signedTransactionBytes);
				});

				TransactionBroadcast tb = tx.blockingGet();
				tb.getTransactionId();
				setVisible(false);

				Toast.makeText((JFrame) this.getOwner(),
						tr("send_tx_broadcast", tb.getTransactionId().toString()),
						Toast.Style.SUCCESS).display();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				Toast.makeText((JFrame) this.getOwner(), ex.getCause().getMessage(), Toast.Style.ERROR).display(okButton);
			}
			setCursor(Cursor.getDefaultCursor());
		}
	}
}
