
package btdex.miner;


import static signumj.crypto.plot.impl.MiningPlot.PLOT_SIZE;
import static signumj.crypto.plot.impl.MiningPlot.SCOOPS_PER_PLOT;
import static signumj.crypto.plot.impl.MiningPlot.SCOOPS_PER_PLOT_BIGINT;
import static signumj.crypto.plot.impl.MiningPlot.SCOOP_SIZE;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Platform;

import btdex.miner.event.CheckerResultEvent;
import btdex.miner.event.ReaderDriveFinishEvent;
import btdex.miner.event.ReaderLoadedPartEvent;
import net.smacke.jaydio.DirectRandomAccessFile;


/**
 * Executed once for every block ... reads scoops of drive plots
 */
public class ReaderLoadDriveTask implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(ReaderLoadDriveTask.class);

	private final MinerEventPublisher publisher;
	private ShaLibChecker shaLibChecker;

	private byte[] generationSignature;
	private PlotDrive plotDrive;
	private int[] scoopArray;
	private long blockNumber;

	public ReaderLoadDriveTask(MinerEventPublisher publisher) {
		this.publisher = publisher;
	}

	public void init(int[] scoopArray, long blockNumber, byte[] generationSignature, PlotDrive plotDrive) {
		this.scoopArray = scoopArray;
		this.blockNumber = blockNumber;
		this.generationSignature = generationSignature;
		this.plotDrive = plotDrive;

		//    if(!CoreProperties.isUseOpenCl()) {
		this.shaLibChecker = new ShaLibChecker();
		//    }
	}

	@Override
	public void run() {
		long startTime = new Date().getTime();
		Iterator<PlotFile> iterator = plotDrive.getPlotFiles().iterator();
		boolean interrupted = false;
		while(iterator.hasNext() && !interrupted) {
			PlotFile plotPathInfo = iterator.next();
			if(plotPathInfo.getStaggeramt() % plotPathInfo.getNumberOfParts() > 0) {
				LOG.warn("staggeramt " + plotPathInfo.getStaggeramt() + " can not be devided by " + plotPathInfo.getNumberOfParts());
				// fallback ... could lead to problems on optimized plot-files
				plotPathInfo.setNumberOfParts(1);
			}
			interrupted = load(plotPathInfo);
		}

		// ui event
		publisher.publishEvent(new ReaderDriveFinishEvent(plotDrive.getDirectory(), plotDrive.getSize(), new Date().getTime() - startTime, blockNumber));
	}

	/**
	 * A random access file wrapper that should be optimized for each system.
	 * 
	 * On Linux the page cache keeps being filled by the plots recently read,
	 * so we use direct-io in this case.
	 * 
	 * TODO: add optimized support for other platforms.
	 *
	 */
	public static class RandomAccessFileWrapper implements Closeable {
		DirectRandomAccessFile dra;
		RandomAccessFile ra;

		public RandomAccessFileWrapper(Path path) throws IOException {
			// TODO: ARM has a different O_DIRECT hint, needs to be adjusted
			if(Platform.isLinux() && !Platform.isARM())
				dra = new DirectRandomAccessFile(path.toFile(), "r");
			else
				ra = new RandomAccessFile(path.toFile(), "r");
		}

		@Override
		public void close() throws IOException {
			if(dra != null)
				dra.close();
			if(ra != null)
				ra.close();
		}

		public void seek(long l) throws IOException {
			if(dra != null)
				dra.seek(l);
			if(ra != null)
				ra.seek(l);
		}

		public void read(byte[] partBuffer, int i, int length) throws IOException {
			if(dra != null)
				dra.read(partBuffer, i, length);
			if(ra != null)
				ra.read(partBuffer, i, length);      
		}
	};

	private boolean load(PlotFile plotFile) {
		try (RandomAccessFileWrapper sbc = new RandomAccessFileWrapper(plotFile.getFilePath())) {
			int partSizeNonces = (int)(plotFile.getStaggeramt() / plotFile.getNumberOfParts());
			byte []partBuffer = new byte[partSizeNonces * SCOOP_SIZE];

			for(int partNumber = 0; partNumber < plotFile.getNumberOfParts(); partNumber++) {
				for (int scoopNumberPosition = 0; scoopNumberPosition < scoopArray.length; scoopNumberPosition++) {
					readPart(plotFile, sbc, partSizeNonces, partBuffer, partNumber, scoopNumberPosition, scoopArray);
				}

				if(Reader.blockNumber.get() != blockNumber || !Arrays.equals(Reader.generationSignature.get(), generationSignature)) {
					LOG.trace("loadDriveThread stopped!");
					sbc.close();
					return true;
				}
				else {
					BigInteger chunkPartStartNonce = plotFile.getStartnonce().add(BigInteger.valueOf(partNumber * partSizeNonces));
					final byte[] scoops = partBuffer;
					publisher.publishEvent(new ReaderLoadedPartEvent(blockNumber, generationSignature, scoops, scoopArray, chunkPartStartNonce, plotFile));

					if(//!CoreProperties.isUseOpenCl() &&
							shaLibChecker.getLoadError() == null) {
						int lowestNonce = shaLibChecker.findLowest(generationSignature, scoops);

						publisher.publishEvent(new CheckerResultEvent(blockNumber, generationSignature, chunkPartStartNonce, lowestNonce, scoopArray,
								plotFile, scoops));
					}
				}
			}
			sbc.close();
		}
		catch(NoSuchFileException exception) {
			LOG.error("File not found ... please restart to rescan plot-files, maybe set rescan to 'true': " + exception.getMessage());
		}
		catch(ClosedByInterruptException e) {
			// we reach this, if we do not wait for task on shutdown - ByteChannel closed by thread interruption
			LOG.trace("reader stopped cause of new block ...");
		}
		catch(IOException e) {
			e.printStackTrace();
			LOG.error("IOException in: " + plotFile.getFilePath().toString() + " -> " + e.getMessage());
		}
		return false;
	}

	public static void readPart(PlotFile plotFile, RandomAccessFileWrapper sbc, int partSizeNonces,
			byte []partBuffer, int partNumber, int scoopNumberPosition, int[] scoopArray) throws IOException {
		int noncesToSwitchScoop = SCOOPS_PER_PLOT/scoopArray.length;
		int bytesToSwitchScoop = PLOT_SIZE/scoopArray.length;

		BigInteger startNonce = plotFile.getStartnonce().add(BigInteger.valueOf(partNumber * partSizeNonces));

		int scoopAlignmetOffset = SCOOPS_PER_PLOT_BIGINT.subtract(
				startNonce.add(BigInteger.valueOf(scoopArray[0])).mod(SCOOPS_PER_PLOT_BIGINT)).intValue();

		int startOffsetNonces = (scoopAlignmetOffset + noncesToSwitchScoop * scoopNumberPosition) % SCOOPS_PER_PLOT;
		if(scoopArray.length == 1) {
			// single scoop, so we can speed things up
			startOffsetNonces = 0;
			bytesToSwitchScoop = partBuffer.length;
		}
		else if(startOffsetNonces > SCOOPS_PER_PLOT - noncesToSwitchScoop) {
			// this scoop number has a section on the beginning of the file, so we read it first
			int noncesToReadOnStart = startOffsetNonces - (SCOOPS_PER_PLOT - noncesToSwitchScoop);
			int bytesToReadOnStart = noncesToReadOnStart * SCOOP_SIZE;

			sbc.seek((partSizeNonces*partNumber + scoopArray[scoopNumberPosition] * plotFile.getStaggeramt()) * SCOOP_SIZE);
			sbc.read(partBuffer, 0, bytesToReadOnStart);
		}

		startOffsetNonces += partSizeNonces*partNumber;
		int startOffsetBytes = startOffsetNonces * SCOOP_SIZE;

		long filePositionBytes = startOffsetBytes + scoopArray[scoopNumberPosition] * plotFile.getStaggeramt() * SCOOP_SIZE;

		while(startOffsetBytes < partBuffer.length) {
			sbc.seek(filePositionBytes);

			int bytesToRead = Math.min(bytesToSwitchScoop, partBuffer.length - startOffsetBytes);
			sbc.read(partBuffer, startOffsetBytes, bytesToRead);
			if(scoopArray.length == 1) {
				// we are done already
				break;
			}

			startOffsetBytes += PLOT_SIZE;
			filePositionBytes += PLOT_SIZE;
		}
	}
}
