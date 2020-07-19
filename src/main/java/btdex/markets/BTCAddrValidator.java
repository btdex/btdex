package btdex.markets;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Code from https://raw.githubusercontent.com/timqi/btc_address_validator/master/BTCAddrValidator.java
 */
public class BTCAddrValidator {
	private static Logger logger = LogManager.getLogger();

  public static final int[] BTC_HEADERS = {0, 5};
  public static final int[] DOGE_HEADERS = {0x1e, 0x16};

  public static void main(String[] args) {

    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i") == true;
    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62j") == false;
    assert validate("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nK9") == true;
    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62X") == false;
    assert validate("1ANNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i") == false;
    assert validate("1A Na15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i") == false;
    assert validate("BZbvjr") == false;
    assert validate("i55j") == false;
    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62!") == false;
    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62iz") == false;
    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62izz") == false;
    assert validate("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nJ9") == false;
    assert validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62I") == false;
    assert validate("3R2MPpTNQLCNs13qnHz89Rm82jQ27bAwft") == true;
    assert validate("34QjytsE8GVRbUBvYNheftqJ5CHfDHvQRD") == true;
    assert validate("3GsAUrD4dnCqtaTTUzzsQWoymHNkEFrgGF") == true;
    assert validate("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU") == true;


    assert validate("DBXu2kgc3xtvCUWFcxFE3r9hEYgmuaaCyD", DOGE_HEADERS) == true;
    assert validate("DU7XJ1xeE1Xg8cRJmNvxLRjePAqmgFHebR", DOGE_HEADERS) == true;
    assert validate("DQpqATKHvJvMtZyEyz9eTEbj4S3tWJdaak", DOGE_HEADERS) == true;
    assert validate("DQqpATKHvJvMtZyEyz9eTEbj4S3tWJdaak", DOGE_HEADERS) == false;
    assert validate("DU7XJ1xeE1X8gcRJmNvxLRjePAqmgFHebR", DOGE_HEADERS) == false;

    System.out.println("Test all passed");
  }

  private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

  private static final int[] INDEXES = new int[128];

  private static final MessageDigest digest;

  static {
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
    	logger.error("Error: {}", e.getLocalizedMessage());
      	throw new RuntimeException(e);
    }

    for (int i = 0; i < INDEXES.length; i++) {
      INDEXES[i] = -1;
    }
    for (int i = 0; i < ALPHABET.length; i++) {
      INDEXES[ALPHABET[i]] = i;
    }
  }

  public static boolean validate(String addr) {
    try {
      int addressHeader = getAddressHeader(addr);
      Boolean result = (addressHeader == 0 || addressHeader == 5);
      logger.trace("Address validate result: {}", result);
      return result;
    } catch (Exception x) {
    	logger.error("Error: {}", x.getLocalizedMessage());
//      x.printStackTrace();
    }
    return false;
  }

  public static boolean validate(String addr, int[]headers) {
	    try {
	      int addressHeader = getAddressHeader(addr);
	      Boolean result = (addressHeader == headers[0] || addressHeader == headers[1]);
	      logger.trace("Address validate result: {}", result);
	      return result;
	    } catch (Exception x) {
	    	logger.error("Error: {}", x.getLocalizedMessage());
//	      	x.printStackTrace();
	    }
	    return false;
	  }

  private static int getAddressHeader(String address) throws IOException {
    byte[] tmp = decodeChecked(address);
    return tmp[0] & 0xFF;
  }

  private static byte[] decodeChecked(String input) throws IOException {
    byte[] tmp = decode(input);
    if (tmp.length < 4){
    	logger.error("BTC AddressFormatException Input too short");
		throw new IOException("BTC AddressFormatException Input too short");
	}

    byte[] bytes = copyOfRange(tmp, 0, tmp.length - 4);
    byte[] checksum = copyOfRange(tmp, tmp.length - 4, tmp.length);

    tmp = doubleDigest(bytes);
    byte[] hash = copyOfRange(tmp, 0, 4);
    if (!Arrays.equals(checksum, hash)){
		logger.error("BTC AddressFormatException Checksum does not validate");
		throw new IOException("BTC AddressFormatException Checksum does not validate");
	}

    return bytes;
  }

  private static byte[] doubleDigest(byte[] input) {
    return doubleDigest(input, 0, input.length);
  }

  private static byte[] doubleDigest(byte[] input, int offset, int length) {
    synchronized (digest) {
      digest.reset();
      digest.update(input, offset, length);
      byte[] first = digest.digest();
      return digest.digest(first);
    }
  }

  private static byte[] decode(String input) throws IOException {
    if (input.length() == 0) {
      return new byte[0];
    }
    byte[] input58 = new byte[input.length()];
    // Transform the String to a base58 byte sequence
    for (int i = 0; i < input.length(); ++i) {
      char c = input.charAt(i);
      int digit58 = -1;
      if (c >= 0 && c < 128) {
        digit58 = INDEXES[c];
      }
      if (digit58 < 0) {
      	logger.error("Bitcoin AddressFormatException Illegal character " + c + " at " + i);
        throw new IOException("Bitcoin AddressFormatException Illegal character " + c + " at " + i);
      }

      input58[i] = (byte) digit58;
    }

    // Count leading zeroes
    int zeroCount = 0;
    while (zeroCount < input58.length && input58[zeroCount] == 0) {
      ++zeroCount;
    }
    // The encoding
    byte[] temp = new byte[input.length()];
    int j = temp.length;

    int startAt = zeroCount;
    while (startAt < input58.length) {
      byte mod = divmod256(input58, startAt);
      if (input58[startAt] == 0) {
        ++startAt;
      }

      temp[--j] = mod;
    }
    // Do no add extra leading zeroes, move j to first non null byte.
    while (j < temp.length && temp[j] == 0) {
      ++j;
    }

    return copyOfRange(temp, j - zeroCount, temp.length);
  }

  private static byte divmod256(byte[] number58, int startAt) {
    int remainder = 0;
    for (int i = startAt; i < number58.length; i++) {
      int digit58 = (int) number58[i] & 0xFF;
      int temp = remainder * 58 + digit58;

      number58[i] = (byte) (temp / 256);

      remainder = temp % 256;
    }
    return (byte) remainder;
  }

  private static byte[] copyOfRange(byte[] source, int from, int to) {
    byte[] range = new byte[to - from];
    System.arraycopy(source, from, range, 0, range.length);
    return range;
  }
}
