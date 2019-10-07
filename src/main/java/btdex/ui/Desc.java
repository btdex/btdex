package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

class Desc extends JPanel {
	private static final long serialVersionUID = 1L;
	
	JLabel label;

	public Desc(String desc, Component child) {
		super(new BorderLayout());

		add(child, BorderLayout.CENTER);
		add(label = new JLabel(desc), BorderLayout.PAGE_START);
	}
	
	public void setDesc(String desc) {
		label.setText(desc);
	}
}