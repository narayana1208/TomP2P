/*
 * Copyright 2009 Thomas Bocek
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.tomp2p.peers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.tomp2p.utils.Timings;

/**
 * Keeps track of the statistics of a given peer.
 * 
 * @author Thomas Bocek
 * 
 */
public class PeerStatatistic implements Serializable {
    
	private static final long serialVersionUID = -6225586345726672194L;

	private final AtomicLong lastSeenOnline = new AtomicLong(0);

    private final long created = Timings.currentTimeMillis();

    private final AtomicInteger successfullyChecked = new AtomicInteger(0);

    private final AtomicInteger failed = new AtomicInteger(0);

    private PeerAddress peerAddress;

    /**
     * Constructor. Sets the peer address
     * 
     * @param peerAddress
     *            The peer address that belongs to this statistics
     */
    public PeerStatatistic(final PeerAddress peerAddress) {
        if (peerAddress == null) {
            throw new IllegalArgumentException("PeerAddress cannot be null");
        }
        this.peerAddress = peerAddress;
    }

    /**
     * Sets the time when last seen online to now.
     * 
     * @return The number of successful checks
     */
    public int successfullyChecked() {
        lastSeenOnline.set(Timings.currentTimeMillis());
        failed.set(0);
        return successfullyChecked.incrementAndGet();

    }

    /**
     * @return The time last seen online
     */
    public long getLastSeenOnline() {
        return lastSeenOnline.get();
    }

    /**
     * @return The number of times the peer has been successfully checked
     */
    public int getSuccessfullyChecked() {
        return successfullyChecked.get();
    }

    /**
     * Increases the failed counter.
     * 
     * @return The number of failed checks.
     */
    public int failed() {
        return failed.incrementAndGet();
    }

    /**
     * @return The time of creating this peer (statistic)
     */
    public long getCreated() {
        return created;
    }

    /**
     * @return The time that this peer is online
     */
    public int onlineTime() {
        return (int) (lastSeenOnline.get() - created);
    }

    /**
     * @return the peer address associated with this peer address
     */
    public PeerAddress getPeerAddress() {
        return peerAddress;
    }

    /**
     * Set the peer address only if the previous peer address that had the same peer ID.
     * 
     * @param peerAddress
     *            The updated peer ID
     * @return The old peer address
     */
    public PeerAddress setPeerAddress(final PeerAddress peerAddress) {
        if (!this.peerAddress.getPeerId().equals(peerAddress.getPeerId())) {
            throw new IllegalArgumentException("can only update the same peer address");
        }
        PeerAddress previousPeerAddress = this.peerAddress;
        this.peerAddress = peerAddress;
        return previousPeerAddress;
    }
}
