package btdex.ledger;

import java.nio.ByteBuffer;

import org.aion.ledger.LedgerDevice;
import org.aion.ledger.LedgerUtilities;
import org.aion.ledger.exceptions.CommsException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wrapper class for Burstcoin App on Ledger Nano S devices.
 *
 * All calls are blocking, so you need to work around it, for instance using
 * the {@link LedgerService} class.
 *
 * @author jjos
 *
 */
public class BurstLedger {
	private static final byte CLA = (byte)0x80;
	private static final byte ZERO = (byte)0x00;

	private static final byte INS_GET_VERSION = (byte)0x01;
	private static final byte INS_AUTH_SIGN_TXN = (byte)0x03;
	private static final byte INS_SHOW_ADDRESS = (byte)0x05;
	private static final byte INS_GET_PUBLIC_KEY = (byte)0x06;

	private static final byte P1_SIGN_INIT = (byte)0x01;
	private static final byte P1_SIGN_CONTINUE = (byte)0x02;
	private static final byte P1_SIGN_AUTHORIZE = (byte)0x10;
	private static final byte P1_SIGN_FINISH = (byte)0x03;

	private static final byte P1_SHOW_ADDRES_RETURN_KEY = (byte)0x01;

	private static LedgerDevice dev = null;

	private static Logger logger = LogManager.getLogger();

	private static void findDevice() throws Exception {
		if(dev == null) {
			dev = LedgerUtilities.findLedgerDevice();
		}
	}

	public static boolean isDeviceAvailable() {
		try {
			findDevice();

			return dev != null;
		} catch (Exception e) {
			logger.error("Error: {}", e.getLocalizedMessage());
			return false;
		}
	}

	/**
	 * Checks if a Ledger device is available and the Burstcoin app is open.
	 *
	 * @return true if the Burstcoin app is open on the device.
	 */
	public static boolean isAppAvailable() {
		if(!isDeviceAvailable()) {
			logger.debug("Ledger device not available");
			return false;
		}
		try {
			ByteBuffer buff = ByteBuffer.allocate(5);
			buff.put(CLA);
			buff.put(INS_GET_VERSION);
			buff.put(ZERO); // P1
			buff.put(ZERO); // P2
			buff.put(ZERO); // LEN
			buff.clear();

			byte [] output = dev.exchange(buff.array());

			// Check for the magic numbers
			Boolean magicNumbers = (output.length == 7 && output[4] == 0x0a && output[5] == 0x0b && output[6] == 0x0c);
			logger.debug("Magic numbers status: {}", magicNumbers);
			return magicNumbers;
		}
		catch (CommsException e) {
			logger.error("CommsExc: {}", e.getResponseCode());
			if(e.getResponseCode() == CommsException.RESP_INCORRECT_APP)
				return false; // just the app is not open

			if(dev!=null) {
				// try to close and open again as it apparently cannot recover after this
				dev.close();
				dev = null;
			}
		}
		catch (Exception e) {
			logger.error("Error: {}", e.getLocalizedMessage());
			e.printStackTrace();
			if(dev!=null) {
				// try to close and open again as it apparently cannot recover after this
				dev.close();
				dev = null;
			}
		}
		logger.debug("After all checks Ledger device not available");
		return false;
	}

	/**
	 * @return the version of the app instaled on the Ledger device
	 * @throws Exception
	 */
	public static byte[] getVersion() throws Exception {
		if(!isDeviceAvailable())
			return null;

		ByteBuffer buff = ByteBuffer.allocate(5);

		buff.put(CLA);
		buff.put(INS_GET_VERSION);
		buff.put(ZERO); // P1
		buff.put(ZERO); // P2
		buff.put(ZERO); // LEN

		buff.clear();

		return dev.exchange(buff.array());
	}

	/**
	 * Shows the BURST- address for the given index on the device
	 * @param index
	 * @return the public key for the given account index
	 * @throws Exception
	 */
	public static byte[] showAddress(byte index) throws Exception {
		if(!isDeviceAvailable())
			return null;

		ByteBuffer buff = ByteBuffer.allocate(8);

		buff.put(CLA);
		buff.put(INS_SHOW_ADDRESS);
		buff.put(P1_SHOW_ADDRES_RETURN_KEY); // P1 (we want the key)
		buff.put(ZERO); // P2
		buff.put((byte) 3); // LEN
		buff.put(ZERO); // account
		buff.put(ZERO); // change
		buff.put(index); // index

		buff.clear();

		byte[] out = dev.exchange(buff.array());

		if(out.length > 32) {
			ByteBuffer key = ByteBuffer.allocate(32);
			key.put(out, 1, 32);
			key.clear();
			return key.array();
		}
		logger.debug("showAddress returns null");
		return null;
	}


	/**
	 * @param index
	 * @return the public key for the given account index
	 * @throws Exception
	 */
	public static byte[] getPublicKey(byte index) throws Exception {
		if(!isDeviceAvailable())
			return null;

		ByteBuffer buff = ByteBuffer.allocate(8);

		buff.put(CLA);
		buff.put(INS_GET_PUBLIC_KEY);
		buff.put(ZERO); // P1
		buff.put(ZERO); // P2
		buff.put((byte) 3); // LEN
		buff.put(ZERO); // account
		buff.put(ZERO); // change
		buff.put(index); // index

		buff.clear();

		byte [] output = dev.exchange(buff.array());

		if(output.length == 33 && output[0] == 0) {
			byte[] pubKey = new byte[32];
			System.arraycopy(output, 1, pubKey, 0, pubKey.length);
			return pubKey;
		}
		logger.debug("getPublicKey returns null");
		return null;
	}

	/**
	 * Signs the given transaction and return the signed version.
	 *
	 * @param utx the unsigned transaction bytes
	 * @param index the account index
	 * @return the signed byte array
	 *
	 * @throws Exception
	 */
	public static byte[] sign(byte []utx, byte index) throws Exception {
		if(!isDeviceAvailable())
			return null;

		ByteBuffer buff = ByteBuffer.allocate(255);

		// We will split it in multiple calls
		int pos = 0;
		while(utx.length > pos) {
			int delta = Math.min(250, utx.length-pos);

			int P1 = pos == 0 ? P1_SIGN_INIT : P1_SIGN_CONTINUE;
			if(utx.length == pos + delta)
				P1 |= P1_SIGN_AUTHORIZE;

			buff.put(CLA);
			buff.put(INS_AUTH_SIGN_TXN);
			buff.put((byte) P1); // P1
			buff.put(ZERO); // P2
			buff.put((byte) delta); // LEN
			buff.put(utx, pos, delta);
			buff.clear();
			byte []out = dev.exchange(buff.array());
			if(out.length < 1 || (out[0] != 0 && out[0] != 15))
				return null;

			pos += delta;
		}

		// Finish the call and get the signature
		buff.put(CLA);
		buff.put(INS_AUTH_SIGN_TXN);
		buff.put(P1_SIGN_FINISH); // P1
		buff.put(ZERO); // P2
		buff.put((byte) 3); // LEN
		buff.put(ZERO);
		buff.put(ZERO);
		buff.put(index);
		buff.clear();

		byte[] out = dev.exchange(buff.array());
		if(out.length == 65 && out[0] == 0) {
	        byte[] signedTransaction = new byte[utx.length];
	        System.arraycopy(utx, 0, signedTransaction, 0, utx.length); // Duplicate the transaction
	        System.arraycopy(out, 1, signedTransaction, 96, 64); // Insert the signature
	        return signedTransaction;
		}
		logger.debug("sign returns null");
		return null;
	}
}
