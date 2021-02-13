package btdex.ui;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;


public class Toast extends JDialog {
	private static final long serialVersionUID = -1602907470843951525L;
	
	public enum Style { NORMAL, SUCCESS, ERROR };
	
	public static final int LENGTH_SHORT = 5000;
	public static final int LENGTH_LONG = 10000;
	public static final Color ERROR_RED = new Color(121, 0, 0);
	public static final Color SUCCESS_GREEN = new Color(22, 127, 57);
	public static final Color NORMAL_BLACK = new Color(0, 0, 0);
	
	private final float MAX_OPACITY = 0.8f;
	private final float OPACITY_INCREMENT = 0.05f;
	private final int FADE_REFRESH_RATE = 20;
	private final int WINDOW_RADIUS = 15;
	private final int DISTANCE_FROM_PARENT_TOP = 60;	
	
	private JFrame mOwner;
	private String mText;
	private int mDuration;
	private Color mBackgroundColor = Color.BLACK;
	private Color mForegroundColor = Color.WHITE;
	private Point mLocation;
    
	private boolean opacitySupported = true;
	
    public Toast(JFrame owner){
    	super(owner);
    	mOwner = owner;
    }

    private void createGUI(){
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), WINDOW_RADIUS, WINDOW_RADIUS));
            }
        });
        
        setAlwaysOnTop(true);
        setUndecorated(true);
        setFocusableWindowState(false);
        setModalityType(ModalityType.MODELESS);
        getContentPane().setBackground(mBackgroundColor);
        
        JLabel label = new JLabel(mText);
        label.setFont(label.getFont().deriveFont(16.0f));
        label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        label.setForeground(mForegroundColor);
        add(label);
        pack();
    }
	
	public void fadeIn() {
		final Timer timer = new Timer(FADE_REFRESH_RATE, null);
		timer.setRepeats(true);
		timer.addActionListener(new ActionListener() {
			private float opacity = 0;
			@Override public void actionPerformed(ActionEvent e) {
				opacity += OPACITY_INCREMENT;
				if(opacitySupported)
					setOpacity(Math.min(opacity, MAX_OPACITY));
				if (opacity >= MAX_OPACITY){
					timer.stop();
				}
			}
		});

		try {
			setOpacity(0);
		}
		catch (UnsupportedOperationException e) {
			// translucency not supported on some systems
			opacitySupported = false;
		}
		timer.start();
		
		Point loc = new Point(getToastLocation());
		setLocation(loc);
		setVisible(true);
	}

	public void fadeOut() {
		final Timer timer = new Timer(FADE_REFRESH_RATE, null);
		timer.setRepeats(true);
		timer.addActionListener(new ActionListener() {
			private float opacity = MAX_OPACITY;
			@Override public void actionPerformed(ActionEvent e) {
				opacity -= OPACITY_INCREMENT;
				if(opacitySupported)
					setOpacity(Math.max(opacity, 0));
				if (opacity <= 0) {
					timer.stop();
					setVisible(false);
					dispose();
				}
			}
		});

		if(opacitySupported)
			setOpacity(MAX_OPACITY);
		timer.start();
	}
	
	private Point getToastLocation(){
		if(mLocation!=null) {
			Point p = new Point(mLocation);
			p.x -= getWidth()/2;
			p.y -= getHeight();
			return p;
		}
		Point ownerLoc = mOwner.getLocation();		
		int x = (int) (ownerLoc.getX() + ((mOwner.getWidth() - this.getWidth()) / 2)); 
		int y = (int) (ownerLoc.getY() + mOwner.getHeight()*DISTANCE_FROM_PARENT_TOP/100);
		return new Point(x, y);
	}
	
	public void setText(String text){
		mText = text;
	}
	
	public void setDuration(int duration){
		mDuration = duration;
	}
	
	@Override
	public void setBackground(Color backgroundColor){
		mBackgroundColor = backgroundColor;
	}
	
	@Override
	public void setForeground(Color foregroundColor){
		mForegroundColor = foregroundColor;
	}
	
	public static Toast makeText(JFrame owner, String text){
		return makeText(owner, text, LENGTH_SHORT);
	}
	
	public static Toast makeText(JFrame owner, String text, Style style){
		return makeText(owner, text, LENGTH_SHORT, style);
	}
    
    public static Toast makeText(JFrame owner, String text, int duration){
    	return makeText(owner, text, duration, Style.NORMAL);
    }
    
    public static Toast makeText(JFrame owner, String text, int duration, Style style){
    	Toast toast = new Toast(owner);
    	toast.mText = text;
    	toast.mDuration = duration;
    	
    	if (style == Style.SUCCESS)
    		toast.mBackgroundColor = SUCCESS_GREEN;
    	if (style == Style.ERROR)
    		toast.mBackgroundColor = ERROR_RED;
    	if (style == Style.NORMAL)
    		toast.mBackgroundColor = NORMAL_BLACK;
    	
    	return toast;
    }
    
    public void display(Component c) {
		Point p = c.getLocationOnScreen();
		p.x += c.getWidth()/2;
		mLocation = p;

    	display();
    }
        
    public void display(){
        new Thread(new Runnable() {
            @Override
            public void run() {
            	try{
            		createGUI();
            		fadeIn();
	                Thread.sleep(mDuration);
	                fadeOut();
            	}
            	catch(Exception ex){
            		ex.printStackTrace();
            	}
            }
        }).start();
    }

    public static void main(String []args){
    	final JFrame frame = new JFrame();
    	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	frame.setSize(new Dimension(500, 300));
    	JButton b = new JButton("Toast!");

    	b.addActionListener(new ActionListener() {		
    		@Override
    		public void actionPerformed(ActionEvent e) {
    			Toast.makeText(frame, "Annotations were successfully saved.", Style.SUCCESS).display();
    		}
    	});

    	frame.add(b);
    	frame.setVisible(true);        
    }
}
