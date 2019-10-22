import btdex.core.Globals;
import btdex.ui.Main;

public class BTDEX {
	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if(args[i].equals("-f") && i < args.length -1)
				Globals.setConfFile(args[++i]);
		}
		new Main();
	}
}
