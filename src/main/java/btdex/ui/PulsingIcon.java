package btdex.ui;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Timer;


@SuppressWarnings("serial")
public class PulsingIcon extends JLabel {

	private static final int MAX_ALPHAS = 20;
	private float alpha = 1.0f;
	private float[] alphas = new float[MAX_ALPHAS];
	private Timer timer;

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setComposite(((AlphaComposite)g2.getComposite()).derive(alpha));
		super.paintComponent(g2);
		
		Toolkit.getDefaultToolkit().sync();
	};

	public PulsingIcon(ImageIcon icon) {
		super(icon);
		for (int i = 0; i < alphas.length; i++) {
			double theta = (Math.PI * 2 * i) / alphas.length;
			alphas[i] = (float) (0.2 + 0.8 * (Math.cos(theta) + 1) / 2.0);
		}

		int bpm = 60;
		timer = new Timer(setTimerDelay(bpm), new TimerListener());
		timer.start();
	}
	
	public void stopPulsing() {
		timer.stop();
	}

	private int setTimerDelay(int bpm) {
		int milisecondsInMinute = 60 * 1000;
		int delay = milisecondsInMinute / (bpm * alphas.length);
		if (timer != null) {
			timer.setDelay(delay);
		}
		return delay;
	}

	private class TimerListener implements ActionListener {
		int index = 0;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			alpha = alphas[index];
			index++;
			index %= alphas.length;
			
			repaint();
		}
	}
}
