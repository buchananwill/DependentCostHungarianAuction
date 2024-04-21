package HungarianAuction.TaskElements;

import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token of integer size to denote the means to enter an allocation auction.
 */
public class EntryToken<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    private final int size;

    private static final AtomicInteger tokenCounter = new AtomicInteger();

    private final int id;

    private boolean auctionIsLive = false;

    /** With serial numbers - a traceable audit trail for the allocation process.
     *
     */
    public EntryToken(int size) {
        this.size = size;
        id = tokenCounter.getAndIncrement();
    }

    public int size() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof EntryToken<?, ?> otherToken)
            return this.id == otherToken.getId();
        else return false;
    }

    private int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EntryToken[" +
                "size=" + size +
                ", ID="+ id + ']';
    }

    public boolean isAuctionIsLive() {
        return auctionIsLive;
    }

    public void setAuctionIsLive(boolean auctionIsLive) {
        this.auctionIsLive = auctionIsLive;
    }
}
