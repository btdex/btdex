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

import btdex.core.*;
import btdex.locale.Translation;
import burst.kit.entity.response.AssetOrder;
import jiconfont.IconCode;
import jiconfont.swing.IconFontSwing;

public class SocialButton extends JButton implements ActionListener {
	private static final long serialVersionUID = -7670367558338741748L;

	// TODO: add other media
	private static final String TWITTER_URL = "https://twitter.com/intent/tweet?text=";
	private static final String FACEBOOK_URL = "https://www.facebook.com/sharer/sharer.php?u=";
	
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
		IconCode icon = null;
		String name = null;
		int size = Constants.ICON_SIZE;
		switch (type) {
		case FACEBOOK:
			icon = Icons.FACEBOOK;
			name = "Facebook";
			break;
		case INSTAGRAM:
			icon = Icons.INSTAGRAM;
			name = "Instagram";
			break;
		case GOOGLE_PLUS:
			icon = Icons.GOOGLE_PLUS;
			name = "Google Plus";
			break;
		case REDDIT:
			icon = Icons.REDDIT;
			name = "Reddit";
			break;
		case TELEGRAM:
			icon = Icons.TELEGRAM;
			name = "Reddit";
			break;
		case WHATSAPP:
			icon = Icons.WHATSAPP;
			name = "Reddit";
			break;
		default:
			icon = Icons.TWITTER;
			name = "Twitter";
			tags = "\n#DEX #DeFi #NoKYCOnly #KYClite @btdex_trade";
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
			if(m.getTokenID()!=null) {
				for(AssetOrder o : BurstNode.getInstance().getAssetAsks(m)) {
					if(o.getAccountAddress().equals(g.getAddress())) {
						orders.add(o.getId().getID());
						markets.add(m.toString());
						break; // just one order per token
					}
				}
				for(AssetOrder o : BurstNode.getInstance().getAssetBids(m)) {
					if(o.getAccountAddress().equals(g.getAddress())) {
						orders.add(o.getId().getID());
						markets.add(m.toString());
						break; // just one order per token
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
			url += URLEncoder.encode(Translation.tr("social_text", pairs, Constants.WEBSITE_LINK, closing), StandardCharsets.UTF_8.toString());
			Main.getInstance().browse(url);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}
}
