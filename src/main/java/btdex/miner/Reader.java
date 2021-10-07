
package btdex.miner;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import btdex.core.Globals;
import btdex.miner.event.ReaderLoadedPartEvent;
import btdex.miner.event.ReaderProgressChangedEvent;
import btdex.miner.event.RoundStoppedEvent;
import btdex.ui.MiningPanel;

/**
 * The type Reader.
 */
public class Reader {
	private static final Logger LOG = LoggerFactory.getLogger(Reader.class);

	private final MinerEventPublisher publisher;
	private final ThreadPoolTaskExecutor readerPool;

	// data
	public static volatile AtomicLong blockNumber = new AtomicLong();
	public static volatile AtomicReference<byte[]> generationSignature = new AtomicReference<>();

	private Plots plots;

	private Map<BigInteger, Long> realCapacityLookup;
	private long realRemainingCapacity;
	private long realCapacity;

	private Map<BigInteger, Long> capacityLookup;
	private long remainingCapacity;
	private long capacity;

	private long readerStartTime;
	private int readerThreads;

	public Reader(MinerEventPublisher publisher, ThreadPoolTaskExecutor readerPool) {
		this.publisher = publisher;
		this.readerPool = readerPool;

		generationSignature.set(new byte[0]);
		readerThreads = 0;
		String cpuCoresProp = Globals.getInstance().getProperty(MiningPanel.PROP_MINE_CPU_CORES);
		if(cpuCoresProp != null) {
			readerThreads = Integer.parseInt(cpuCoresProp) + 1;
		}
		capacityLookup = new HashMap<>();
		realCapacityLookup = new HashMap<>();
	}

	/* starts reader (once per block) */
	public void read(long previousBlockNumber, long blockNumber, byte[] generationSignature,
			int[] scoopArray, int networkQuality) {
		Reader.blockNumber.set(blockNumber);
		Reader.generationSignature.set(generationSignature);

		// ensure plots are initialized
		plots = plots == null ? getPlots() : plots;

		if(readerPool.getActiveCount() > 0) {
			long elapsedTime = new Date().getTime() - readerStartTime;
			publisher.publishEvent(new RoundStoppedEvent(previousBlockNumber, capacity, remainingCapacity, elapsedTime, networkQuality));
		}

		// update reader thread count
		int poolSize = readerThreads <= 0 ? plots.getPlotDrives().size() : readerThreads;
		readerPool.setCorePoolSize(poolSize);
		readerPool.setMaxPoolSize(poolSize);

		// we use the startnonce of loaded (startnonce+chunk+part) as unique job identifier
		capacityLookup.clear();
		capacityLookup.putAll(plots.getChunkPartStartNonces());

		realCapacityLookup.clear();
		realCapacity = 0;

		Map<BigInteger, Long> capacityLookupCopy = new HashMap<>(capacityLookup);
		for(BigInteger chunkPartNonces : capacityLookupCopy.keySet()) {
			plots.getPlotFileByChunkPartStartNonce(chunkPartNonces);
			long realChunkPartNoncesCapacity = capacityLookupCopy.get(chunkPartNonces);
			realCapacityLookup.put(chunkPartNonces, realChunkPartNoncesCapacity);
			realCapacity += realChunkPartNoncesCapacity;
		}

		remainingCapacity = plots.getSize();
		capacity = plots.getSize();

		realRemainingCapacity = realCapacity;
		readerStartTime = new Date().getTime();

		// order by slowest and biggest drives first
		List<PlotDrive> orderedPlotDrives = new ArrayList<>(plots.getPlotDrives());
		orderedPlotDrives.sort((o1, o2) -> Long.compare(o2.getSize(), o1.getSize())); // order by size

		for(PlotDrive plotDrive : orderedPlotDrives) {
			ReaderLoadDriveTask readerLoadDriveTask = new ReaderLoadDriveTask(publisher);
			readerLoadDriveTask.init(scoopArray, blockNumber, generationSignature, plotDrive);
			readerPool.execute(readerLoadDriveTask);
		}
	}

	public Plots getPlots() {
		if( // CoreProperties.isScanPathsEveryRound() ||
				plots == null) {
			plots = new Plots();
		}
		return plots;
	}

	public boolean cleanupReaderPool() {
		// if no read thread running, pool will be increased on next round
		if(readerPool.getActiveCount() == 0) {
			readerPool.setCorePoolSize(1);
			readerPool.setMaxPoolSize(1);
			LOG.trace("cleanup was successful ...");
			return true;
		}
		LOG.trace("cleanup skipped ... retry in 1s");
		return false;
	}

	@EventListener
	public void handleMessage(ReaderLoadedPartEvent event) {
		if(blockNumber.get() == event.getBlockNumber() &&
				Arrays.equals(event.getGenerationSignature(), generationSignature.get())) {
			// update progress
			Long removedCapacity = capacityLookup.remove(event.getChunkPartStartNonce());
			Long realRemovedCapacity = realCapacityLookup.remove(event.getChunkPartStartNonce());
			if(removedCapacity != null && realRemovedCapacity != null) {
				remainingCapacity -= removedCapacity;
				realRemainingCapacity -= realRemovedCapacity;
				long elapsedTime = new Date().getTime() - readerStartTime;
				publisher.publishEvent(new ReaderProgressChangedEvent(this, event.getBlockNumber(), capacity, remainingCapacity,
						realCapacity, realRemainingCapacity, elapsedTime));
			}
			else {
				// just on debug, update progress is not 'mission' critical.
				LOG.debug("Error on update progress: ReaderPartLoadedEvent for unknown chunkPartStartNonce: '" + event.getChunkPartStartNonce() + "'!");
			}
		}
		else {
			LOG.trace("update reader progress skipped ... old block ...");
		}
	}
}
