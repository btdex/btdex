package btdex.miner;

import signumj.crypto.plot.impl.MiningPlot;
import signumj.util.LibShabal;

public class ShaLibChecker {

    public Throwable getLoadError() {
      return LibShabal.LOAD_ERROR;
    }

    public int findLowest(byte[] gensig, byte[] data) {
        return (int) LibShabal.shabal_findBestDeadline(data, data.length / MiningPlot.SCOOP_SIZE, gensig);
    }
}
