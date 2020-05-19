package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.aion.ledger.LedgerDevice;
import org.aion.ledger.LedgerUtilities;
import org.bouncycastle.util.encoders.Hex;

import btdex.core.BurstLedger;
import btdex.core.Globals;
import static btdex.locale.Translation.tr;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

public class Welcome extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	private JTextArea passphrase;

	private JLabel introText;

	private JPasswordField pin;
	private JPasswordField pinCheck;

	private JCheckBox acceptBox, recoverBox;

	private JButton okButton;
	private JButton calcelButton;
	private JButton useLedgerButton;
	private boolean resetPin;

	private int ret;

	public Welcome(JFrame owner) {
		this(owner, false);
	}
	
	public Welcome(JFrame owner, boolean resetPin) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.resetPin = resetPin;

		setTitle(tr(resetPin ? "welc_reset_pin" : "welc_welcome"));
		setResizable(false);

		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		if(!resetPin) {
			JPanel introPanel = new JPanel(new BorderLayout());
			String title = "<html><h2>" + tr("welc_intro_header") + "</h2>";
			introPanel.add(new JLabel(title), BorderLayout.PAGE_START);
			String intro = "<html>" + tr("welc_intro_text") + "<br><br>" + tr("welc_intro_pin");
			introText = new JLabel(intro);
			introText.setPreferredSize(new Dimension(60, 120));
			introPanel.add(introText, BorderLayout.PAGE_END);
			
			useLedgerButton = new JButton(tr("welc_use_ledger"));
			useLedgerButton.addActionListener(this);
			introPanel.add(useLedgerButton, BorderLayout.CENTER);
			topPanel.add(introPanel, BorderLayout.CENTER);
		}

		passphrase = new JTextArea(2, 40);
		passphrase.setWrapStyleWord(true);
		passphrase.setLineWrap(true);
		passphrase.setEditable(false);

		pin = new JPasswordField(12);
		pinCheck = new JPasswordField(12);

		acceptBox = new JCheckBox(tr("welc_wrote"));
		recoverBox = new JCheckBox(tr("welc_reuse"));

		recoverBox.addActionListener(this);

		JPanel recoveryPanel = new JPanel(new BorderLayout());
		recoveryPanel.setBorder(BorderFactory.createTitledBorder(tr(resetPin ?
				"welc_prhase_to_redefine" :
				"welc_recovery_phrase")));
		recoveryPanel.add(recoverBox, BorderLayout.PAGE_START);
		recoveryPanel.add(passphrase, BorderLayout.CENTER);
		recoveryPanel.add(acceptBox, BorderLayout.PAGE_END);

		topPanel.add(recoveryPanel, BorderLayout.PAGE_END);

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		calcelButton = new JButton(tr("dlg_cancel"));
		okButton = new JButton(tr("dlg_ok"));
		getRootPane().setDefaultButton(okButton);

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		panel.add(new Desc(tr("dlg_pin"), pin));
		panel.add(new Desc(tr("welc_reenter_pin"), pinCheck));

		//		buttonPane.add(acceptBox);
		buttonPane.add(calcelButton);
		buttonPane.add(okButton);

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		content.add(topPanel, BorderLayout.PAGE_START);
		content.add(panel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		if(resetPin) {
			passphrase.setText("");
			passphrase.setEditable(true);
			passphrase.requestFocus();
			
			recoverBox.setVisible(false);
			acceptBox.setSelected(true);
			acceptBox.setVisible(false);
		}
		else
			newPass();
		pack();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						acceptBox.requestFocusInWindow();
					}
				});
			}
		});
	}

	private void newPass() {
		String pass;
		StringBuilder sb = new StringBuilder();
		byte[] entropy = new byte[Words.TWELVE.byteLength()];
		new SecureRandom().nextBytes(entropy);
		new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);
		pass = sb.toString();

		passphrase.setText(pass);
	}

	public int getReturn() {
		return ret;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == useLedgerButton) {
			String error = null;
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				// Check for the Burst app
				if(!BurstLedger.isAppAvailable()) {
					// magic number do not match
					error = tr("ledger_error");
				}
				
				// TODO: think about asking the user which index should be used
				int index = 1;
				
				if(error == null) {
					// get the public key
					byte[] pubKey = BurstLedger.getPublicKey((byte)index);
					if(pubKey != null) {
						Globals.getInstance().setKeys(pubKey, index);
						Globals.getInstance().saveConfs();
						
						// all good
						ret = 1;
						setVisible(false);
					}
					else
						error = tr("ledger_error");
				}
				
			} catch (Exception e1) {
				error = tr("ledger_error");
				e1.printStackTrace();
			}
			setCursor(Cursor.getDefaultCursor());
			if(error != null)
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(useLedgerButton);
		}
		if(e.getSource() == recoverBox) {
			if(recoverBox.isSelected()) {
				passphrase.setText("");
				passphrase.setEditable(true);
				passphrase.requestFocus();
			}
			else {
				passphrase.setEditable(false);
				newPass();
			}
			acceptBox.setSelected(recoverBox.isSelected());
			acceptBox.setEnabled(!recoverBox.isSelected());
		}
		if(e.getSource() == calcelButton) {
			ret = 0;
			setVisible(false);
		}
		if(e.getSource() == okButton) {
			String error = null;
			String phrase = passphrase.getText();

			if(!acceptBox.isSelected()) {
				error = tr("welc_write_phrase");
				acceptBox.requestFocus();
			}
			else if(phrase.length()==0) {
				error = tr("welc_phrase_empty");
				passphrase.requestFocus();				
			}
			else if(pin.getPassword() == null || pin.getPassword().length < 4) {
				error = tr("welc_min_pin");
				pin.requestFocus();
			}
			else if(!Arrays.equals(pin.getPassword(), pinCheck.getPassword())) {
				pin.requestFocus();
				error = tr("welc_wrong_pin");
			}

			if(error == null) {
				Globals g = Globals.getInstance();
				
				// no error, so we have a new phrase
				byte[] privKey = Globals.BC.getPrivateKey(phrase);
				byte[] pubKey = Globals.BC.getPublicKey(privKey);
				
				try {
					if(resetPin) {
						if(!Arrays.equals(g.getPubKey(), pubKey)) {
							error = tr("welc_wrong_phrase");
						}
					}

					if(error == null) {
						g.setKeys(pubKey, privKey, pin.getPassword());
						g.saveConfs();
					}
				} catch (Exception e1) {
					error = e1.getMessage();
				}
			}
			
			if(error!=null) {
				Toast.makeText((JFrame) this.getOwner(), error, Toast.Style.ERROR).display(okButton);
			}
			else {
				ret = 1;
				setVisible(false);
			}
		}
	}
}
