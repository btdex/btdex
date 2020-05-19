package btdex.ui;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import btdex.core.BurstLedger;
import btdex.locale.Translation;

public class LedgerSigner extends TimerTask {
	
	private static LedgerSigner instance;
	
	private CallBack caller;
	private byte[] unsigned;
	private int index;
	private byte[] signed;
	
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
		public void reportSigned(byte []signed);
	}
	
	public static LedgerSigner getInstance() {
		if(instance == null)
			instance = new LedgerSigner();
		return instance;
	}
	
	private LedgerSigner() {
		try {
			// start the node updater thread
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
	
	public void requestSign(byte []unsigned, int index) {
		this.unsigned = unsigned;
		this.index = index;
		this.signed = null;
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
					SwingUtilities.invokeLater(() -> caller.reportSigned(signed));
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
