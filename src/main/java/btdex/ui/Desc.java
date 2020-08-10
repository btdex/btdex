package btdex.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class Desc extends JPanel {
	private static final long serialVersionUID = 1L;
	
	JLabel label;
	Component child;

	public Desc(String desc, Component child) {
		this(desc, child, null);
	}

	public Desc(String desc, Component child, Component childBottom) {
		super(new BorderLayout());

		this.child = child;
		if(desc != null)
			add(label = new JLabel(desc), BorderLayout.PAGE_START);
		add(child, BorderLayout.CENTER);
		if(childBottom!=null)
			add(childBottom, BorderLayout.PAGE_END);
	}
	
	public Desc(Component desc, Component child, Component childBottom) {
		super(new BorderLayout());
		
		this.child = child;
		if(desc != null)
			add(desc, BorderLayout.PAGE_START);
		add(child, BorderLayout.CENTER);
		if(childBottom!=null)
			add(childBottom, BorderLayout.PAGE_END);
	}

	public void setDesc(String desc) {
		label.setText(desc);
	}
	
	public Component getChild() {
		return child;
	}
}
