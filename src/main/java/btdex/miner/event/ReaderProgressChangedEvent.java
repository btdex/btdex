
package btdex.miner.event;

import btdex.miner.Reader;

/**
 * The type Reader progress changed event.
 */
public class ReaderProgressChangedEvent {
	private long blockNumber;
	private long capacity;
	private long remainingCapacity;
	private long realCapacity;
	private long realRemainingCapacity;
	private long elapsedTime;

	public ReaderProgressChangedEvent(Reader source, long blockNumber, long capacity, long remainingCapacity,long realCapacity, long realRemainingCapacity,
			long elapsedTime) {
		this.blockNumber = blockNumber;
		this.capacity = capacity;
		this.remainingCapacity = remainingCapacity;
		this.realCapacity = realCapacity;
		this.realRemainingCapacity = realRemainingCapacity;
		this.elapsedTime = elapsedTime;
	}

	public long getBlockNumber()
	{
		return blockNumber;
	}

	public long getRemainingCapacity()
	{
		return remainingCapacity;
	}

	public long getCapacity()
	{
		return capacity;
	}

	public long getElapsedTime()
	{
		return elapsedTime;
	}

	public long getRealCapacity()
	{
		return realCapacity;
	}

	public long getRealRemainingCapacity()
	{
		return realRemainingCapacity;
	}
}
