package btdex.ui;

import java.awt.BorderLayout;
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

import btdex.core.Globals;
import burst.kit.entity.BurstAddress;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

public class Welcome extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextArea passphrase;

	JLabel introText;

	JPasswordField pin;
	JPasswordField pinCheck;

	JCheckBox acceptBox, recoverBox;

	private JButton okButton;
	private JButton calcelButton;
	private boolean resetPin;

	private int ret;

	public Welcome(JFrame owner) {
		this(owner, false);
	}
	
	public Welcome(JFrame owner, boolean resetPin) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.resetPin = resetPin;

		setTitle(resetPin ? "Reset PIN" : "Welcome!");
		setResizable(false);

		JPanel topPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		if(!resetPin) {
			introText = new JLabel(
					"<html><h2>You have a new Wallet, read carefully</h2>"
							+ "You must write down your 12-word recovery phrase "
							+ "precisely and in the correct order and store "
							+ "it securely. Anyone who gets your recovery phrase "
							+ "can take your funds.<br><br>"
							+ "This installation is secured with a PIN, if "
							+ "you ever lose this PIN or this computer, "
							+ "your recovery phrase is your only backup."
					);
			introText.setPreferredSize(new Dimension(60, 180));
		}

		passphrase = new JTextArea(2, 40);
		passphrase.setWrapStyleWord(true);
		passphrase.setLineWrap(true);
		passphrase.setEditable(false);

		pin = new JPasswordField(12);
		pinCheck = new JPasswordField(12);

		if(introText!=null)
			topPanel.add(introText, BorderLayout.PAGE_START);

		acceptBox = new JCheckBox("I wrote drown my recovery phrase");
		recoverBox = new JCheckBox("I want to use an existing recovery phrase");

		recoverBox.addActionListener(this);

		JPanel recoveryPanel = new JPanel(new BorderLayout());
		recoveryPanel.setBorder(BorderFactory.createTitledBorder(resetPin ?
				"Enter your recovery phrase to redefine your PIN" :
				"Your recovery phrase"));
		recoveryPanel.add(recoverBox, BorderLayout.PAGE_START);
		recoveryPanel.add(passphrase, BorderLayout.CENTER);
		recoveryPanel.add(acceptBox, BorderLayout.PAGE_END);

		topPanel.add(recoveryPanel, BorderLayout.CENTER);

		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		calcelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		panel.add(new Desc("PIN", pin));
		panel.add(new Desc("Reenter PIN", pinCheck));

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
		String pass = null;

		while(true) {
			StringBuilder sb = new StringBuilder();
			byte[] entropy = new byte[Words.TWELVE.byteLength()];
			new SecureRandom().nextBytes(entropy);
			new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);

			Globals g = Globals.getInstance();

			pass = sb.toString();
			
			// Check if this account exists, otherwise create a new pass
			try {
				BurstAddress addresss = Globals.BC.getBurstAddressFromPassphrase(pass);
				g.getNS().getAccount(addresss).blockingGet();
			}
			catch (Exception e) {
				// got an exception
				break;
			}
		}
		passphrase.setText(pass);
	}

	public int getReturn() {
		return ret;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
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
				error = "Write down your recovery phrase first.";
				acceptBox.requestFocus();
			}
			else if(phrase.length()==0) {
				error = "Your passphrase is empty.";
				passphrase.requestFocus();				
			}
			else if(pin.getPassword() == null || pin.getPassword().length < 4) {
				error = "Enter a PIN with at least 4 characters.";
				pin.requestFocus();
			}
			else if(!Arrays.equals(pin.getPassword(), pinCheck.getPassword())) {
				pin.requestFocus();
				error = "PINs do not match.";
			}

			if(error == null) {
				Globals g = Globals.getInstance();
				
				// no error, so we have a new phrase
				byte[] privKey = Globals.BC.getPrivateKey(phrase);
				byte[] pubKey = Globals.BC.getPublicKey(privKey);
				
				try {
					if(resetPin) {
						if(!Arrays.equals(g.getPubKey(), pubKey)) {
							error = "Incorrect passphrase.";
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
