
package btdex.miner.event;

/**
 * The type Round stopped event.
 */
public class RoundStoppedEvent {
	private long blockNumber;

	private long capacity;
	private long remainingCapacity;
	private long elapsedTime;
	private int networkQuality;

	public RoundStoppedEvent(long blockNumber, long capacity, long remainingCapacity, long elapsedTime, int networkQuality) {

		this.blockNumber = blockNumber;
		this.capacity = capacity;
		this.remainingCapacity = remainingCapacity;
		this.elapsedTime = elapsedTime;
		this.networkQuality = networkQuality;
	}

	public long getBlockNumber()
	{
		return blockNumber;
	}

	public long getCapacity()
	{
		return capacity;
	}

	public long getRemainingCapacity()
	{
		return remainingCapacity;
	}

	public long getElapsedTime()
	{
		return elapsedTime;
	}

	public int getNetworkQuality()
	{
		return networkQuality;
	}
}

