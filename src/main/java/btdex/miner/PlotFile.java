
package btdex.miner;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import btdex.miner.ReaderLoadDriveTask.RandomAccessFileWrapper;
import signumj.crypto.SignumCrypto;
import signumj.crypto.plot.impl.MiningPlot;

public class PlotFile {
	private static Logger logger = LogManager.getLogger();

	// key -> size
	private Map<BigInteger, Long> chunkPartStartNonces;

	private Path filePath;
	private String accountID;
	private Long chunkPartNonces;
	private int numberOfParts;
	private long numberOfChunks;

	private String filename;
	private BigInteger startnonce;
	private long plots;
	private long staggeramt;

	private long size;

	PlotFile(Path filePath, Long chunkPartNonces) {
		this.filePath = filePath;
		this.chunkPartNonces = chunkPartNonces;
		this.filename = getFilename(filePath);
		String[] parts = filename.split("_");
		this.accountID = parts[0];
		this.startnonce = new BigInteger(parts[1]);
		this.plots = Long.valueOf(parts[2]);

		staggeramt = plots;
		this.numberOfParts = calculateNumberOfParts(staggeramt);
		this.numberOfChunks = 1;
		this.numberOfParts = 1;

		chunkPartStartNonces = new HashMap<>();

		size = numberOfChunks * staggeramt * MiningPlot.PLOT_SIZE;

		if(logger.isDebugEnabled()) {
			long fileSize = filePath.toFile().length();
			if(fileSize != size)
			{
				logger.debug("incomplete plotFile: " + filePath.toString() + " specified size '" + size + " bytes', size '" + fileSize + " bytes'.");
			}
		}

		long chunkPartSize = this.size / numberOfChunks / numberOfParts;
		for(int chunkNumber = 0; chunkNumber < numberOfChunks; chunkNumber++) {
			for(int partNumber = 0; partNumber < numberOfParts; partNumber++) {
				// register a unique key for identification
				BigInteger chunkPartStartNonce = startnonce.add(BigInteger.valueOf(chunkNumber * staggeramt + partNumber * (staggeramt / numberOfParts)));
				Long key = chunkPartStartNonces.put(chunkPartStartNonce, chunkPartSize);
				if(key != null) {
					logger.warn("possible overlapping plot-file '" + filePath + "', please check your plots.");
				}
			}
		}
	}

	public void check() {
		SignumCrypto crypto = SignumCrypto.getInstance();
		long account = Long.parseUnsignedLong(accountID);
		byte[] buffer = new byte[MiningPlot.PLOT_SIZE];

		try (RandomAccessFileWrapper sbc = new RandomAccessFileWrapper(getFilePath())) {

			// checking at 8 different positions for nonces and also scoops
			int N = 8;
			int[] scoopArray = new int[1];
			byte[] scoopRead = new byte[MiningPlot.SCOOP_SIZE];
			for (int i = 0; i < N; i++) {
				long nonceOffset = i * (plots-1) / (N - 1);
				long nonce = startnonce.longValue() + nonceOffset;
				crypto.plotNonce(account, nonce, (byte)2, buffer, 0);

				// and check a N scoops
				for (int j = 0; j < N; j++) {
					scoopArray[0] = j * (MiningPlot.SCOOPS_PER_PLOT-1) / (N -1);

					sbc.seek((scoopArray[0]*plots + nonceOffset)*MiningPlot.SCOOP_SIZE);
					sbc.read(scoopRead, 0, MiningPlot.SCOOP_SIZE);

					for (int k = 0; k < MiningPlot.SCOOP_SIZE; k++) {
						if(scoopRead[k] != buffer[scoopArray[0] * MiningPlot.SCOOP_SIZE + k]) {
							logger.error("plot '{}' is invalid, please replot", getFilename());
							return;
						}
					}
				}
			}
			logger.debug("plot '{}' checked", getFilename());
			sbc.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			logger.error("Exception while checking plot '{}': {}", getFilename(), e.getMessage());
		}
	}

	private String getFilename(Path filePath) {
		return filePath.getFileName().toString();
	}

	public long getSize() {
		return size;
	}

	public Path getFilePath() {
		return filePath;
	}

	String getFilename() {
		return filename;
	}

	public String getAccountID() {
		return accountID;
	}

	public BigInteger getStartnonce() {
		return startnonce;
	}

	public long getPlots() {
		return plots;
	}

	public long getStaggeramt() {
		return staggeramt;
	}

	public long getNumberOfChunks() {
		return numberOfChunks;
	}

	public int getNumberOfParts() {
		return numberOfParts;
	}

	public void setNumberOfParts(int numberOfParts) {
		this.numberOfParts = numberOfParts;
	}

	Map<BigInteger, Long> getChunkPartStartNonces() {
		return chunkPartStartNonces;
	}

	// splitting into parts is not needed, but it seams to improve speed and enables us
	// to have steps of nearly same size
	private int calculateNumberOfParts(long staggeramt) {
		int maxNumberOfParts = 100;

		long targetNoncesPerPart = chunkPartNonces != null ? chunkPartNonces : 960000;

		// TODO: review the GPU support later
		// for CPU it should be much lower, ensures less idle.
		targetNoncesPerPart = // !CoreProperties.isUseOpenCl() ?
				targetNoncesPerPart / 10
				// : targetNoncesPerPart
				;

		// calculate numberOfParts based on target
		int suggestedNumberOfParts = (int) (staggeramt / targetNoncesPerPart) + 1;

		// ensure stagger is dividable by numberOfParts, if not adjust numberOfParts
		while(staggeramt % suggestedNumberOfParts != 0 && suggestedNumberOfParts < maxNumberOfParts) {
			suggestedNumberOfParts += 1;
		}

		// fallback if number of parts could not be calculated in acceptable range
		if(suggestedNumberOfParts >= maxNumberOfParts) {
			suggestedNumberOfParts = (int) Math.floor(Math.sqrt(staggeramt));
			while(staggeramt % suggestedNumberOfParts != 0) {
				suggestedNumberOfParts--;
			}
		}
		return suggestedNumberOfParts;
	}
}
