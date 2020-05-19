package btdex.ledger;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import btdex.locale.Translation;

/**
 * A class for monitoring and signing messages with Burstcoin Ledger app.
 * 
 * There is a background thread running so calls are not blocking.
 * The signature reported back via the {@link CallBack} interface.
 * 
 * @author jjos
 *
 */
public class LedgerSigner extends TimerTask {
	
	private static LedgerSigner instance;
	
	private CallBack caller;
	private byte[] unsigned;
	private byte[] unsigned2;
	private int index;
	private byte[] signed;
	private byte[] signed2;
	
	public interface CallBack {
		/**
		 * Should update the status
		 * @param txt
		 */
		public void ledgerStatus(String txt);
		
		/**
		 * Got the signature or null on an exception or denied.
		 * @param signed
		 */
		public void reportSigned(byte []signed, byte []signed2);
	}
	
	public static LedgerSigner getInstance() {
		if(instance == null)
			instance = new LedgerSigner();
		return instance;
	}
	
	private LedgerSigner() {
		try {
			// start the dongle monitoring thread
			Timer timer = new Timer("ledger status update");
			timer.schedule(this, 0, 1000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setCallBack(CallBack caller) {
		this.caller = caller;
	}

	/**
	 * Request up to two signatures (second is optional) for the given account index.
	 * 
	 * @param unsigned
	 * @param unsigned2
	 * @param index
	 */
	public void requestSign(byte []unsigned, byte unsigned2[], int index) {
		this.unsigned = unsigned;
		this.unsigned2 = unsigned2;
		this.index = index;
		this.signed = this.signed2 = null;
	}
	
	@Override
	public void run() {
		try {
			if(caller == null)
				return;

			if(!BurstLedger.isDeviceAvailable()) {
				SwingUtilities.invokeLater(() -> caller.ledgerStatus(Translation.tr("ledger_no_device")));
				return;
			}
			if(!BurstLedger.isAppAvailable()) {
				SwingUtilities.invokeLater(() -> caller.ledgerStatus(Translation.tr("ledger_no_app")));
				return;
			}

			if(unsigned!=null) {
				SwingUtilities.invokeLater(() -> caller.ledgerStatus(Translation.tr("ledger_authorize")));
				try {
					signed = BurstLedger.sign(unsigned, (byte)index);
					unsigned = null;
					if(unsigned2 != null) {
						signed2 = BurstLedger.sign(unsigned2, (byte)index);
						unsigned2 = null;
					}
					SwingUtilities.invokeLater(() -> caller.reportSigned(signed, signed2));
					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			SwingUtilities.invokeLater(() -> caller.ledgerStatus(Translation.tr("ledger_available")));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
