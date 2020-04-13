package btdex.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JButton;

import btdex.core.BurstNode;
import btdex.core.ContractState;
import btdex.core.Contracts;
import btdex.core.Globals;
import btdex.core.Market;
import btdex.core.Markets;
import btdex.locale.Translation;
import burst.kit.entity.response.AssetOrder;
import jiconfont.icons.font_awesome.FontAwesomeBrands;
import jiconfont.swing.IconFontSwing;

public class SocialButton extends JButton implements ActionListener {
	private static final long serialVersionUID = -7670367558338741748L;
	
	private static final String TWITTER_URL = "https://twitter.com/intent/tweet?text=";
	private static final String FACEBOOK_URL = "https://www.facebook.com/sharer/sharer.php?u=";
	// TODO: add other media
	
	public enum Type {
		TWITTER,
		FACEBOOK,
		INSTAGRAM,
		REDDIT,
		TELEGRAM,
		WHATSAPP,
		GOOGLE_PLUS
	}
	
	private Type type;
	private String tags;
	
	public SocialButton(Type type, Color color) {
		this.type = type;
		FontAwesomeBrands icon = null;
		String name = null;
		int size = 18;
		switch (type) {
		case FACEBOOK:
			icon = FontAwesomeBrands.FACEBOOK;
			name = "Facebook";
			break;
		case INSTAGRAM:
			icon = FontAwesomeBrands.INSTAGRAM;
			name = "Instagram";
			break;
		case GOOGLE_PLUS:
			icon = FontAwesomeBrands.GOOGLE_PLUS;
			name = "Google Plus";
			break;
		case REDDIT:
			icon = FontAwesomeBrands.REDDIT;
			name = "Reddit";
			break;
		case TELEGRAM:
			icon = FontAwesomeBrands.TELEGRAM;
			name = "Reddit";
			break;
		case WHATSAPP:
			icon = FontAwesomeBrands.WHATSAPP;
			name = "Reddit";
			break;
		default:
			icon = FontAwesomeBrands.TWITTER;
			name = "Twitter";
			tags = "\n#DEX #crypto @btdex_trade";
			break;
		}
		setIcon(IconFontSwing.buildIcon(icon, size, color));
		setToolTipText(Translation.tr("social_share", name));
		addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Globals g = Globals.getInstance();
		HashSet<String> markets = new HashSet<>();
		ArrayList<String> orders = new ArrayList<>();
		
		// Checking token orders
		for(Market m : Markets.getMarkets()) {
			if(orders.size() > 2)
				break;
			if(m.getTokenID()!=null) {
				markets.add(m.toString());
				for(AssetOrder o : BurstNode.getInstance().getAssetAsks(m)) {
					if(o.getAccountAddress().equals(g.getAddress())) {
						orders.add(o.getId().getID());
						break; // one order per market here
					}
				}
			}
		}
		
		// Smart contract based
		for(ContractState c : Contracts.getContracts()) {
			// limiting to 2 mentions for now
			if(orders.size() > 2)
				break;
			if(c.getState() > 0 && c.getCreator().equals(g.getAddress())) {
				orders.add(c.getAddress().getRawAddress());
				for(Market m : Markets.getMarkets()) {
					if(m.getID() == c.getMarket()) {
						markets.add(m.toString());
						break;
					}
				}
			}
		}
		
		String pairs = "";
		for(String m : markets) {
			if(pairs.length() > 0) pairs += ", ";
			pairs += "$" + m;
		}
		if(pairs.length() == 0)
			pairs = Translation.tr("social_crypto");
		
		String closing = "";
		for(String o : orders) {
			if(closing.length() > 0) closing += " ";
			closing += o;
		}
		closing += tags;
		
		String url;
		switch (type) {
		case FACEBOOK:
			url = FACEBOOK_URL;
			break;
			// TODO: add support for other medias
		default:
			url = TWITTER_URL;
			break;
		}
		try {
			url += URLEncoder.encode(Translation.tr("social_text", pairs, "https://btdex.trade", closing), StandardCharsets.UTF_8.toString());
			Main.getInstance().browse(url);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}
}
