package btdex.ui;

import java.awt.Color;
import java.awt.Image;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import btdex.core.Constants;
import jiconfont.IconCode;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.icons.font_awesome.FontAwesomeBrands;
import jiconfont.swing.IconFontSwing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Icons {
    public static IconCode BTDEX = FontAwesome.HEART;
    public static IconCode RESET_PIN = FontAwesome.LOCK;
    public static IconCode VERSION = FontAwesome.CODE_FORK;
    public static IconCode RECYCLE = FontAwesome.RECYCLE;
    public static IconCode GITHUB = FontAwesomeBrands.GITHUB;
    public static IconCode TRASH = FontAwesome.TRASH;
    public static IconCode CONNECTED = FontAwesome.WIFI;
    public static IconCode TESTNET = FontAwesome.FLASK;
    public static IconCode DISCONNECTED = FontAwesome.EXCLAMATION;
    public static IconCode COPY = FontAwesome.CLONE;
    public static IconCode EXPLORER = FontAwesome.EXTERNAL_LINK;
    public static IconCode SETTINGS = FontAwesome.COG;
    public static IconCode LANGUAGE = FontAwesome.LANGUAGE;
    public static IconCode SEND = FontAwesome.PAPER_PLANE;
    public static IconCode ORDER_BOOK = FontAwesome.BOOK;
    public static IconCode SWAPS = FontAwesome.RANDOM;
    public static IconCode CROSS_CHAIN = FontAwesome.HANDSHAKE_O;
    public static IconCode TRADE = FontAwesome.LINE_CHART;
    public static IconCode ACCOUNT = FontAwesome.USER_CIRCLE;
    public static IconCode CHAT = FontAwesome.COMMENT;
    public static IconCode TRANSACTION = FontAwesome.LINK;
    public static IconCode CANCEL = FontAwesome.TIMES;
    public static IconCode SPINNER = FontAwesome.SPINNER;
    public static IconCode EDIT = FontAwesome.PENCIL;
    public static IconCode WITHDRAW = FontAwesome.RECYCLE;
    public static IconCode MEDIATION = FontAwesome.GAVEL;
    public static IconCode NEW_TOKEN = FontAwesome.TAG;
    public static IconCode UP = FontAwesome.ARROW_UP;
    public static IconCode DOWN = FontAwesome.ARROW_DOWN;
    public static IconCode UNKNOWN = FontAwesome.QUESTION;

    public static IconCode DISCORD = FontAwesomeBrands.DISCORD;
    public static IconCode FACEBOOK = FontAwesomeBrands.FACEBOOK;
    public static IconCode INSTAGRAM = FontAwesomeBrands.INSTAGRAM;
    public static IconCode GOOGLE_PLUS = FontAwesomeBrands.GOOGLE_PLUS;
    public static IconCode REDDIT = FontAwesomeBrands.REDDIT;
    public static IconCode TELEGRAM = FontAwesomeBrands.TELEGRAM;
    public static IconCode WHATSAPP = FontAwesomeBrands.WHATSAPP;
    public static IconCode TWITTER = FontAwesomeBrands.TWITTER;

    public static IconCode LEDGER = FontAwesome.USB;

	private static Logger logger = LogManager.getLogger();
    private int size = Constants.ICON_SIZE;
    private Color color = Color.BLACK;
    private HashMap<IconCode, Icon> icons = new HashMap<>();

    public Icons(Color color, int size) {
    	this.color = color;
    	this.size = size;
    	logger.debug("Icons color {} and size {}", color, size);
    }

    public Icon get(IconCode icon) {
    	Icon ret = icons.get(icon);
    	if(ret == null) {
    		ret = IconFontSwing.buildIcon(icon, size, color);
    	}
    	return ret;
    }

    public static Image getIcon() {
        try {
            return ImageIO.read(Main.class.getResourceAsStream("/icon.png"));
        } catch (Exception ex) {
			logger.error("Error: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return null;
    }

    public static Image getIconMono() {
        try {
            return ImageIO.read(Main.class.getResourceAsStream("/icon-mono.png"));
        } catch (Exception ex) {
			logger.error("Error: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return null;
    }
}
