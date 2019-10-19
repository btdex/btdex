package btdex.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

public class Welcome extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextField passphrase;

	JPasswordField pin;
	JPasswordField pinCheck;
	
	JCheckBox acceptBox;

	private JButton okButton;
	private JButton calcelButton;
	
	private int ret;

	public Welcome(JFrame owner) {
		super(owner, ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle("Welcome!");

		JPanel topPanel = new JPanel(new GridLayout(0, 1, 4, 4));

		JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));

		passphrase = new JTextField(44);
		
		pin = new JPasswordField(12);
		pinCheck = new JPasswordField(12);
		
		topPanel.add(new Desc("Your recovery phrase", passphrase));
		
		acceptBox = new JCheckBox("I wrote drown my recovery 12-word phrase");
		
		// Create a button
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		calcelButton = new JButton("Cancel");
		okButton = new JButton("OK");

		calcelButton.addActionListener(this);
		okButton.addActionListener(this);

		panel.add(new Desc("PIN", pin));
		panel.add(new Desc("Reenter PIN", pinCheck));
		
		buttonPane.add(acceptBox);
		buttonPane.add(calcelButton);
		buttonPane.add(okButton);

		// set action listener on the button

		JPanel content = (JPanel)getContentPane();
		content.setBorder(new EmptyBorder(4, 4, 4, 4));

		content.add(topPanel, BorderLayout.PAGE_START);
		content.add(panel, BorderLayout.CENTER);
		content.add(buttonPane, BorderLayout.PAGE_END);

		newPass();
		
		pack();
	}
	
	private void newPass() {
		StringBuilder sb = new StringBuilder();
		byte[] entropy = new byte[Words.TWELVE.byteLength()];
		new SecureRandom().nextBytes(entropy);
		new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);
		passphrase.setText(sb.toString());
	}
	
	public int getReturn() {
		return ret;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == calcelButton) {
			ret = 0;
			setVisible(false);
		}
	}
}
