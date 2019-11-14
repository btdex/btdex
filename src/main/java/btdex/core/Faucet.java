package btdex.core;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import bt.BT;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.FeeSuggestion;
import burst.kit.entity.response.TransactionBroadcast;
import burst.kit.entity.response.http.BRSError;
import burst.kit.service.BurstNodeService;
import io.reactivex.Single;

/**
 * An extremely simple Faucet for validating accounts.
 * 
 * @author jjos
 *
 */
public class Faucet extends Thread {
	
	public static final int PORT_TESTNET = 1978;
	public static final int PORT = 1980;

	public static final int TIMEOUT = 1000;

	static final BurstValue AMOUNT = BurstValue.fromPlanck(1);
	static BurstNodeService NS;
	static final BurstCrypto BC = BurstCrypto.getInstance();
	static String PASS = BT.PASSPHRASE;

	static final int MAX_REQUESTS_PER_BLOCK = 2;

	static BurstValue FEE = BurstValue.fromPlanck(735000);

	private Socket socket;
	private boolean exceeded;

	private Faucet(Socket s, boolean ex) {
		this.socket = s;
		this.exceeded = ex;
	}

	@Override
	public void run() {
		InputStream inp = null;
		DataOutputStream out = null;
		try {
			socket.setSoTimeout(TIMEOUT);
			inp = socket.getInputStream();
			out = new DataOutputStream(socket.getOutputStream());

			if(exceeded) {
				out.writeBytes("Maximum number of requests reached, try again in a few minutes.");
				out.flush();
				return;
			}

		} catch (IOException e) {
			return;
		}

		System.out.println("New client");

		try {
			byte []publicKey = new byte[32];
			inp.read(publicKey, 0, publicKey.length);
			BurstAddress address = BC.getBurstAddressFromPublic(publicKey);

			try {
				NS.getAccount(address).blockingGet();
				// if this account exists, the request is invalid
				out.writeBytes("Invalid request, account already activated.");
				out.flush();
			}
			catch (Exception e) {
				if(e.getCause() instanceof BRSError) {
					BRSError error = (BRSError) e.getCause();
					if(error.getCode() == 5) {							
						byte[] senderPublicKey = BC.getPublicKey(PASS);
						Single<byte[]> utx = NS.generateTransactionWithMessage(address, publicKey,
								senderPublicKey, AMOUNT, FEE,
								1000, "Congratulations, your new BTDEX account is now validated!");

						Single<TransactionBroadcast> tx = utx.flatMap(unsignedTransactionBytes -> {
							byte[] signedTransactionBytes = BC.signTransaction(BC.getPrivateKey(PASS), unsignedTransactionBytes);
							return NS.broadcastTransaction(signedTransactionBytes);
						});
						tx.blockingGet();
						out.writeBytes("success");
						out.flush();

						System.out.println("Pubkey served: " + BC.toHexString(publicKey));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String args[]) {
		String node = BT.NODE_TESTNET;
		int port = PORT_TESTNET;
		String confFile = "faucet.properties";

		for (int i = 0; i < args.length; i++) {
			if(args[i].equals("-n") && i < args.length -1)
				node = args[++i];
			if(args[i].equals("-p") && i < args.length -1)
				port = Integer.parseInt(args[++i]);
			if(args[i].equals("-f") && i < args.length -1)
				confFile = args[++i];
		}
		NS = BurstNodeService.getCompositeInstance(node);

		Properties conf = new Properties();
		File f = new File(confFile);
		if (f.exists() && f.isFile()) {
			try {
				FileInputStream input = new FileInputStream(confFile);
				conf.load(input);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		PASS = conf.getProperty("passphrase", BT.PASSPHRASE);

		ServerSocket serverSocket = null;

		long timer = System.currentTimeMillis();
		int nreq = 0;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Faucet service running on port: " + port);
		while (true) {
			try {
				long now = System.currentTimeMillis();
				if(now - timer > TimeUnit.MINUTES.toMillis(20)) {
					// 5 blocks, clear the counters
					timer = now;
					nreq = 0;

					// update fee
					// We only serve unknown accounts
					FeeSuggestion feeSugest = NS.suggestFee().blockingGet();
					FEE = feeSugest.getCheapFee();
				}
				// another request
				nreq ++;

				Socket s = serverSocket.accept();
				// new thread for every client
				new Faucet(s, nreq > MAX_REQUESTS_PER_BLOCK*5).start();
			} catch (IOException e) {
				System.out.println("I/O error: " + e.getMessage());
			}
		}
	}
}

