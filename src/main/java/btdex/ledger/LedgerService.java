package btdex.ledger;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import btdex.locale.Translation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for monitoring and signing messages with Burstcoin Ledger app.
 *
 * There is a background thread running so calls are not blocking.
 * The signature reported back via the {@link SignCallBack} interface.
 *
 * @author jjos
 *
 */
public class LedgerService extends TimerTask {

	private static LedgerService instance;

	private PubKeyCallBack pubKeyCaller;
	private boolean waitingKey;
	private SignCallBack signCaller;
	private byte[] unsigned;
	private byte[] unsigned2;
	private int index;
	private byte[] signed;
	private byte[] signed2;

	private static Logger logger = LogManager.getLogger();

	public interface SignCallBack {
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

	public interface PubKeyCallBack {
		/**
		 * Should update the status
		 * @param txt
		 */
		public void returnedError(String error);

		/**
		 * Got the signature or null on an exception or denied.
		 * @param signed
		 */
		public void returnedKey(byte []pubKey, int index);
	}

	public static LedgerService getInstance() {
		if(instance == null)
			instance = new LedgerService();
		return instance;
	}

	private LedgerService() {
		try {
			// start the dongle monitoring thread
			Timer timer = new Timer("ledger status update");
			timer.schedule(this, 0, 1000);
			logger.info("Ledger dongle monitoring thread started");
		}
		catch (Exception e) {
			logger.error("Error: {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	public void setCallBack(SignCallBack caller) {
		this.signCaller = caller;
	}

	public void setCallBack(PubKeyCallBack caller, int index) {
		this.waitingKey = caller != null;
		this.index = index;
		this.pubKeyCaller = caller;
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
			if(signCaller == null && !waitingKey)
				return;

			if(waitingKey) {
				if(!BurstLedger.isDeviceAvailable()) {
					SwingUtilities.invokeLater(() -> pubKeyCaller.returnedError(Translation.tr("ledger_no_device")));
				}
				else if(!BurstLedger.isAppAvailable()) {
					SwingUtilities.invokeLater(() -> pubKeyCaller.returnedError(Translation.tr("ledger_no_app")));
				}
				else {
					byte[] pubKey = BurstLedger.showAddress((byte)index);
					if(pubKey!=null) {
						waitingKey = false;
						SwingUtilities.invokeLater(() -> pubKeyCaller.returnedKey(pubKey, index));
					}
				}
				return;
			}

			if(!BurstLedger.isDeviceAvailable()) {
				SwingUtilities.invokeLater(() -> signCaller.ledgerStatus(Translation.tr("ledger_no_device")));
				return;
			}
			if(!BurstLedger.isAppAvailable()) {
				SwingUtilities.invokeLater(() -> signCaller.ledgerStatus(Translation.tr("ledger_no_app")));
				return;
			}

			if(unsigned!=null) {
				SwingUtilities.invokeLater(() -> signCaller.ledgerStatus(Translation.tr("ledger_authorize")));
				try {
					signed = BurstLedger.sign(unsigned, (byte)index);
					unsigned = null;
					if(unsigned2 != null) {
						signed2 = BurstLedger.sign(unsigned2, (byte)index);
						unsigned2 = null;
					}
					SwingUtilities.invokeLater(() -> signCaller.reportSigned(signed, signed2));
					return;
				} catch (Exception e) {
					logger.error("Error: {}", e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			SwingUtilities.invokeLater(() -> signCaller.ledgerStatus(Translation.tr("ledger_available")));
		}
		catch (Exception e) {
			if(pubKeyCaller!=null)
				pubKeyCaller = null;
			logger.error("Error: {}", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
}
