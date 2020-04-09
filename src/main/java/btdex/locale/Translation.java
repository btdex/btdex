package btdex.locale;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;

public class Translation {
	
	private static final String RESOURCE_FILE = "/locale/i18n.btdex";
	
	private static Locale locale = Locale.ENGLISH;
	private static Properties enResource = new Properties();
	private static Properties resource;
	
	static {
		try {
			enResource.load(Translation.class.getResourceAsStream(RESOURCE_FILE + ".properties"));
			resource = enResource;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getLanguage() {
		return locale.getDisplayLanguage();
	}
	
	public static void setLanguage(Locale newLocale) {
		try {
			InputStream stream = Translation.class.getResourceAsStream(RESOURCE_FILE + newLocale.getLanguage() + ".properties");
			if(stream != null) {
				locale = newLocale;
				resource = new Properties();		
				resource.load(stream);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String tr(String key, Object... arguments) {
        return MessageFormat.format(get(key), arguments);
    }
	
	private static String get(String key) {
		String txt = resource.getProperty(key);
		if(txt == null)
			txt = enResource.getProperty(key);
		if(txt == null)
			txt = key;
		
		return txt;
	}

}
