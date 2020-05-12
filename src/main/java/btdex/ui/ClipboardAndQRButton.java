package btdex.ui;

import static btdex.locale.Translation.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

public class ClipboardAndQRButton extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private String uri;
	
	public ClipboardAndQRButton(final Component parent, int iconSize, final Color fg) {
		super(new FlowLayout(FlowLayout.LEFT));
		this.setAlignmentY(0.65f);
		
		JLabel copyButton = new JLabel(IconFontSwing.buildIcon(FontAwesome.CLONE, iconSize, fg));
		JLabel qrButton = new JLabel(IconFontSwing.buildIcon(FontAwesome.QRCODE, iconSize, fg));
		
		copyButton.setToolTipText(tr("btn_copy_to_clipboard"));
		qrButton.setToolTipText(tr("btn_show_qr"));
		
		add(copyButton);
		add(qrButton);

		copyButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection stringSelection = new StringSelection(uri);
				clipboard.setContents(stringSelection, null);
				
				Toast.makeText(Main.getInstance(), tr("btn_copied_to_clipboard", uri), Toast.Style.SUCCESS).display(copyButton);
			}
		});

		qrButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				byte[] imageBytes = QRCode.from(uri).withColor(0, getIntFromColor(fg))
						.withSize(260, 260).to(ImageType.PNG).stream().toByteArray();
				JLabel label = new JLabel(new ImageIcon(imageBytes));
				
				JOptionPane.showMessageDialog(parent, label, "QR code", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
	
	private int getIntFromColor(Color c){
	    int red = (c.getRed() << 16) & 0x00FF0000; //Shift red 16-bits and mask out other stuff
	    int green = (c.getGreen() << 8) & 0x0000FF00; //Shift Green 8-bits and mask out other stuff
	    int blue = c.getBlue() & 0x000000FF; //Mask out anything not blue.

	    return 0xFF000000 | red | green | blue; //0xFF000000 for 100% Alpha. Bitwise OR everything together.
	}
	
	public void setURI(String uri) {
		// TODO: more elaborated URI as described for instance here:
		// https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki
		this.uri = uri;
	}
}
