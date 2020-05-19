package btdex.core;

import java.nio.ByteBuffer;

import org.aion.ledger.LedgerDevice;
import org.aion.ledger.LedgerUtilities;

/**
 * Wrapper class for Burstcoin App on Ledger Nano S devices.
 * 
 * All calls are blocking, so you need to work around it.
 *  
 * @author jjos
 *
 */
public class BurstLedger {
	private static final byte CLA = (byte)0x80;
	private static final byte ZERO = (byte)0x00;
	
	private static final byte INS_GET_VERSION = (byte)0x01;
	private static final byte INS_AUTH_SIGN_TXN = (byte)0x03;
	// private static final byte INS_ENCRYPT_DECRYPT_MSG = (byte)0x04;
	// private static final byte INS_SHOW_ADDRESS = (byte)0x05;
	private static final byte INS_GET_PUBLIC_KEY = (byte)0x06;
	
	private static final byte P1_SIGN_INIT = (byte)0x01;
	private static final byte P1_SIGN_CONTINUE = (byte)0x02;
	private static final byte P1_SIGN_AUTHORIZE = (byte)0x10;
	private static final byte P1_SIGN_FINISH = (byte)0x03;
	
	private static LedgerDevice dev = null;
	
	private static void findDevice() throws Exception {
		if(dev == null) {
			dev = LedgerUtilities.findLedgerDevice();			
//			if(dev != null) {
//				Runtime.getRuntime().addShutdownHook(
//						new Thread(() -> dev.close())
//				);
//			}
		}
	}
	
	public static boolean isDeviceAvailable() {
		try {
			findDevice();
			
			return dev != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Checks if a Ledger device is available and the Burstcoin app is open.
	 * 
	 * @return true if the Burstcoin app is open on the device.
	 */
	public static boolean isAppAvailable() {
		if(!isDeviceAvailable())
			return false;
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
			return output.length == 7 && output[4] == 0x0a && output[5] == 0x0b && output[6] == 0x0c;
		} catch (Exception e) {
			return false;
		}
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
			buff.put(CLA);
			buff.put(INS_AUTH_SIGN_TXN);
			int P1 = pos == 0 ? P1_SIGN_INIT : P1_SIGN_CONTINUE;
			if(utx.length == pos + delta)
				P1 |= P1_SIGN_AUTHORIZE;
			buff.put((byte) P1); // P1
			buff.put(ZERO); // P2
			buff.put((byte) delta); // LEN
			buff.put(utx, 0, delta);
			buff.clear();
			byte []out = dev.exchange(buff.array());
			if(out.length < 1 || (out[0] != 0 && out[0] != 15))
				return null;
			
			pos += delta;
		}

		// Finish the call and get
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
		return null;
	}
}
