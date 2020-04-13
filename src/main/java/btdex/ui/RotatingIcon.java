package btdex.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public class RotatingIcon implements Icon {
	private final Icon delegateIcon;
	private double angleInDegrees = 90;
	private final Timer rotatingTimer;
	
	public RotatingIcon( Icon icon, final JComponent component, final DefaultTableModel model, final int row, final int col ) {
		delegateIcon = icon;
		rotatingTimer = new Timer( 100, new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				angleInDegrees = angleInDegrees + 10;
				if ( angleInDegrees == 360 ){
					angleInDegrees = 0;
				}
				component.repaint();
				if(model != null)
					model.fireTableCellUpdated(row, col);
			}
		} );
		rotatingTimer.setRepeats( false );
		rotatingTimer.start();
	}

	@Override
	public void paintIcon( Component c, Graphics g, int x, int y ) {
		rotatingTimer.stop();
		Graphics2D g2 = (Graphics2D )g.create();
		int cWidth = delegateIcon.getIconWidth() / 2;
		int cHeight = delegateIcon.getIconHeight() / 2;
		Rectangle r = new Rectangle(x, y, delegateIcon.getIconWidth(), delegateIcon.getIconHeight());
		g2.setClip(r);
		AffineTransform original = g2.getTransform();
		AffineTransform at = new AffineTransform();
		at.concatenate(original);
		at.rotate(Math.toRadians( angleInDegrees ), x + cWidth, y + cHeight);
		g2.setTransform(at);
		
		// trying to make it look better
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON) ;
	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC) ;
	    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY) ;

		delegateIcon.paintIcon(c, g2, x, y);
		g2.setTransform(original);
		rotatingTimer.start();
	}

	@Override
	public int getIconWidth() {
		return delegateIcon.getIconWidth();
	}

	@Override
	public int getIconHeight() {
		return delegateIcon.getIconHeight();
	}
}
