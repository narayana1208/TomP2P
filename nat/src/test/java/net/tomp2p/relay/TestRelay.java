package net.tomp2p.relay;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import net.tomp2p.Utils2;
import net.tomp2p.connection.PeerConnection;
import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureChannelCreator;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.futures.FutureDone;
import net.tomp2p.futures.FuturePeerConnection;
import net.tomp2p.futures.FuturePut;
import net.tomp2p.nat.FutureRelayNAT;
import net.tomp2p.nat.PeerNAT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerMaker;
import net.tomp2p.p2p.Shutdown;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerMap;
import net.tomp2p.peers.PeerMapConfiguration;
import net.tomp2p.rpc.DispatchHandler;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

import org.junit.Assert;
import org.junit.Test;

public class TestRelay {

	@Test
	public void testSetupRelayPeers() throws Exception {
		final Random rnd = new Random(42);
		final int nrOfNodes = 200;
		Peer master = null;
		Peer unreachablePeer = null;
		try {
			// setup test peers
			Peer[] peers = Utils2.createNodes(nrOfNodes, rnd, 4001);
			master = peers[0];
			Utils2.perfectRouting(peers);
			for (Peer peer : peers) {
				new PeerNAT(peer);
			}

			// Test setting up relay peers
			unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(5000).makeAndListen();
			PeerAddress pa = unreachablePeer.getPeerBean().serverPeerAddress();
			pa = pa.changeFirewalledTCP(true).changeFirewalledUDP(true);
			unreachablePeer.getPeerBean().serverPeerAddress(pa);
			// find neighbors
			FutureBootstrap futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
			futureBootstrap.awaitUninterruptibly();
			Assert.assertTrue(futureBootstrap.isSuccess());
			//setup relay
			PeerNAT uNat = new PeerNAT(unreachablePeer);
			FutureRelay fr = uNat.startSetupRelay();
			fr.awaitUninterruptibly();
			Assert.assertTrue(fr.isSuccess());
			Assert.assertEquals(2, fr.relays().size());

			// Check if flags are set correctly
			Assert.assertTrue(unreachablePeer.getPeerAddress().isRelayed());
			Assert.assertFalse(unreachablePeer.getPeerAddress().isFirewalledTCP());
			Assert.assertFalse(unreachablePeer.getPeerAddress().isFirewalledUDP());

		} finally {
			if (master != null) {
				unreachablePeer.shutdown().await();
				master.shutdown().await();
			}
		}
	}
    
    @Test
    public void testBoostrap() throws Exception {
        final Random rnd = new Random(42);
        final int nrOfNodes = 10;
        Peer master = null;
        Peer unreachablePeer = null;
        try {
            // setup test peers
            Peer[] peers = Utils2.createNodes(nrOfNodes, rnd, 4001);
            master = peers[0];
            Utils2.perfectRouting(peers);
            for(Peer peer:peers) {
            	new PeerNAT(peer);
            }

            // Test setting up relay peers
         	unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(5000).makeAndListen();
         	PeerAddress upa = unreachablePeer.getPeerBean().serverPeerAddress();
         	upa = upa.changeFirewalledTCP(true).changeFirewalledUDP(true);
         	unreachablePeer.getPeerBean().serverPeerAddress(upa);
         	// find neighbors
         	FutureBootstrap futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
         	futureBootstrap.awaitUninterruptibly();
         	Assert.assertTrue(futureBootstrap.isSuccess());
         	//setup relay
			PeerNAT uNat = new PeerNAT(unreachablePeer);
			FutureRelay fr = uNat.startSetupRelay();
			fr.awaitUninterruptibly();
			// find neighbors again
         	futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
         	futureBootstrap.awaitUninterruptibly();
         	Assert.assertTrue(futureBootstrap.isSuccess());
            
         	boolean otherPeersHaveRelay = false;
            
            
            for(Peer peer:peers) {
            	if(peer.getPeerBean().peerMap().getAllOverflow().contains(unreachablePeer.getPeerAddress())) {
            		for(PeerAddress pa: peer.getPeerBean().peerMap().getAllOverflow()) {
            			if(pa.getPeerId().equals(unreachablePeer.getPeerID())) {
            				if(pa.getPeerSocketAddresses().size() > 0) {
            					otherPeersHaveRelay = true;
            				}
            				System.err.println("-->"+pa.getPeerSocketAddresses());
            				System.err.println("relay="+pa.isRelayed());
            			}
            		}
            		System.err.println("check 1! "+peer.getPeerAddress());
            	}
            	
            	
            	
            }
            Assert.assertTrue(otherPeersHaveRelay);
            
            //wait for maintenance
            Thread.sleep(3000);
            
            boolean otherPeersMe = false;
            for(Peer peer:peers) {
            	
            	if(peer.getPeerBean().peerMap().getAll().contains(unreachablePeer.getPeerAddress())) {
            		System.err.println("check 2! "+peer.getPeerAddress());
            		otherPeersMe = true;
            	}
            }
            Assert.assertTrue(otherPeersMe);
            

        } finally {
            if (master != null) {
                unreachablePeer.shutdown().await();
                master.shutdown().await();
            }
        }
    }

    @Test
    public void testRelaySendDirect() throws Exception {
        final Random rnd = new Random(42);
        final int nrOfNodes = 100;
        Peer master = null;
        Peer unreachablePeer = null;
        try {
            // setup test peers
            Peer[] peers = Utils2.createNodes(nrOfNodes, rnd, 4001);
            master = peers[0];
            Utils2.perfectRouting(peers);
            for(Peer peer:peers) {
            	new PeerNAT(peer);
            }

            // Test setting up relay peers
         	unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(13337).makeAndListen();
         	PeerAddress upa = unreachablePeer.getPeerBean().serverPeerAddress();
         	upa = upa.changeFirewalledTCP(true).changeFirewalledUDP(true);
         	unreachablePeer.getPeerBean().serverPeerAddress(upa);
         	// find neighbors
         	FutureBootstrap futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
         	futureBootstrap.awaitUninterruptibly();
         	Assert.assertTrue(futureBootstrap.isSuccess());
         	//setup relay
			PeerNAT uNat = new PeerNAT(unreachablePeer);
			FutureRelay fr = uNat.startSetupRelay();
			fr.awaitUninterruptibly();
			// find neighbors again
         	futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
         	futureBootstrap.awaitUninterruptibly();
         	Assert.assertTrue(futureBootstrap.isSuccess());
            
            
         	System.out.print("Send direct message to unreachable peer");
            final String request = "Hello ";
            final String response = "World!";
            
            unreachablePeer.setObjectDataReply(new ObjectDataReply() {
                public Object reply(PeerAddress sender, Object request) throws Exception {
                    Assert.assertEquals(request.toString(), request);
                    return response;
                }
            });
            
            FutureDirect fd = peers[42].sendDirect(unreachablePeer.getPeerAddress()).setObject(request).start().awaitUninterruptibly();
            Assert.assertEquals(response, fd.object());
            //make sure we did not receive it from the unreachable peer with port 13337
            Assert.assertEquals(fd.wrappedFuture().getResponse().getSender().tcpPort(), 4001);
            

        } finally {
            if (unreachablePeer != null) {
            	unreachablePeer.shutdown().await();
            }
            if (master != null) {
                master.shutdown().await();
            }
        }
    }
    
    @Test
    public void testRelaySendDirect2() throws Exception {
        final Random rnd = new Random(42);
        final int nrOfNodes = 100;
        Peer master = null;
        Peer unreachablePeer = null;
        try {
            // setup test peers
            Peer[] peers = Utils2.createNodes(nrOfNodes, rnd, 4001);
            master = peers[0];
            Utils2.perfectRouting(peers);
            for(Peer peer:peers) {
            	new PeerNAT(peer);
            }

            // Test setting up relay peers
         	unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(13337).makeAndListen();
         	PeerNAT uNat = new PeerNAT(unreachablePeer);
         	uNat.bootstrapBuilder(unreachablePeer.bootstrap().setPeerAddress(master.getPeerAddress()));
         	FutureRelayNAT fbn = uNat.startRelay();
         	fbn.awaitUninterruptibly();
         	Assert.assertTrue(fbn.isSuccess());
            
            
         	System.out.print("Send direct message to unreachable peer");
            final String request = "Hello ";
            final String response = "World!";
            
            unreachablePeer.setObjectDataReply(new ObjectDataReply() {
                public Object reply(PeerAddress sender, Object request) throws Exception {
                    Assert.assertEquals(request.toString(), request);
                    return response;
                }
            });
            
            FutureDirect fd = peers[42].sendDirect(unreachablePeer.getPeerAddress()).setObject(request).start().awaitUninterruptibly();
            //fd.awaitUninterruptibly();
            Assert.assertEquals(response, fd.object());
            //make sure we did not receive it from the unreachable peer with port 13337
            //System.err.println(fd.getWrappedFuture());
            Assert.assertEquals(fd.wrappedFuture().getResponse().getSender().tcpPort(), 4001);
            

        } finally {
            if (unreachablePeer != null) {
            	unreachablePeer.shutdown().await();
            }
            if (master != null) {
                master.shutdown().await();
            }
        }
    }

    @Test
    public void testRelayRouting() throws Exception {
        final Random rnd = new Random(42);
        final int nrOfNodes = 8; //test only works if total nr of nodes is < 8
        Peer master = null;
        Peer unreachablePeer = null;
        try {
            // setup test peers
            Peer[] peers = Utils2.createNodes(nrOfNodes, rnd, 4001);
            master = peers[0];
            Utils2.perfectRouting(peers);
            for(Peer peer:peers) {
            	new PeerNAT(peer);
            }

            // Test setting up relay peers
         	unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(13337).makeAndListen();
         	PeerAddress upa = unreachablePeer.getPeerBean().serverPeerAddress();
         	upa = upa.changeFirewalledTCP(true).changeFirewalledUDP(true);
         	unreachablePeer.getPeerBean().serverPeerAddress(upa);
         	// find neighbors
         	FutureBootstrap futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
         	futureBootstrap.awaitUninterruptibly();
         	Assert.assertTrue(futureBootstrap.isSuccess());
         	//setup relay
			PeerNAT uNat = new PeerNAT(unreachablePeer);
			FutureRelay fr = uNat.startSetupRelay();
			fr.awaitUninterruptibly();
			// find neighbors again
         	futureBootstrap = unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()).start();
         	futureBootstrap.awaitUninterruptibly();
         	Assert.assertTrue(futureBootstrap.isSuccess());
         	//
         	uNat.bootstrapBuilder(unreachablePeer.bootstrap().setPeerAddress(peers[0].getPeerAddress()));
         	Shutdown shutdown = uNat.startRelayMaintenance(fr);
         	
            PeerAddress relayPeer = fr.distributedRelay().relayAddresses().iterator().next().remotePeer();
            Peer found = null;
            for(Peer p:peers) {
            	if(p.getPeerAddress().equals(relayPeer)) {
            		found = p;
            		break;
            	}
            }
            
            Thread.sleep(3000);

            int nrOfNeighbors = getNeighbors(found).size();
            //we have in total 9 peers, we should find 8 as neighbors
            Assert.assertEquals(8, nrOfNeighbors);
            
            System.err.println("neighbors: "+nrOfNeighbors);
            for(PeerConnection pc:fr.distributedRelay().relayAddresses()) {
            	System.err.println("pc:"+pc.remotePeer());
            }
            Assert.assertEquals(5, fr.distributedRelay().relayAddresses().size());

            //Shut down a peer
            Thread.sleep(3000);
            peers[nrOfNodes - 1].shutdown().await();
            peers[nrOfNodes - 2].shutdown().await();
            peers[nrOfNodes - 3].shutdown().await();

            /*
             * needed because failure of a node is detected with periodic
             * heartbeat and the routing table of the relay peers are also
             * updated periodically
             */
            Thread.sleep(15000);

            Assert.assertEquals(nrOfNeighbors - 3, getNeighbors(found).size());
            Assert.assertEquals(5, fr.distributedRelay().relayAddresses().size());
            shutdown.shutdown();

        } finally {
            if (unreachablePeer != null) {
            	unreachablePeer.shutdown().await();
            }
            if (master != null) {
                master.shutdown().await();
            }
        }
    }

    @Test
    public void testRelayRPC() throws Exception {
        Peer master = null;
        Peer slave = null;
        try {
            final Random rnd = new Random(42);
            Peer[] peers = Utils2.createNodes(2, rnd, 4000);
            master = peers[0]; // the relay peer
        	new PeerNAT(master); // register relayRPC ioHandler
            slave = peers[1];

            // create channel creator
            FutureChannelCreator fcc = slave.getConnectionBean().reservation().create(1, PeerAddress.MAX_RELAYS);
            fcc.awaitUninterruptibly();

            final FuturePeerConnection fpc = slave.createPeerConnection(master.getPeerAddress());
            FutureDone<PeerConnection> rcf = new PeerNAT(slave).relayRPC().setupRelay(fcc.getChannelCreator(), fpc);
            rcf.awaitUninterruptibly();

            //Check if permanent peer connection was created
            Assert.assertTrue(rcf.isSuccess());
            Assert.assertEquals(master.getPeerAddress(), fpc.getObject().remotePeer());
            Assert.assertTrue(fpc.getObject().channelFuture().channel().isActive());
            Assert.assertTrue(fpc.getObject().channelFuture().channel().isOpen());

        } finally {
            master.shutdown().await();
            slave.shutdown().await();
        }
    }	public BaseFuture publishNeighbors() {
	    return null;
    }

    
    @Test
    public void testNoRelayDHT() throws Exception {
    	final Random rnd = new Random(42);
    	 Peer master = null;
         Peer slave = null;
         try {
             Peer[] peers = Utils2.createNodes(10, rnd, 4000);
             master = peers[0]; // the relay peer
             Utils2.perfectRouting(peers);
             for(Peer peer:peers) {
            	 new PeerNAT(peer);
             }
             PeerMapConfiguration pmc = new PeerMapConfiguration(Number160.createHash(rnd.nextInt()));
             slave = new PeerMaker(Number160.ONE).peerMap(new PeerMap(pmc)).ports(13337).makeAndListen();
             printMapStatus(slave, peers);
             FuturePut futurePut = peers[8].put(slave.getPeerID()).setData(new Data("hello")).start().awaitUninterruptibly();
             futurePut.getFutureRequests().awaitUninterruptibly();
             Assert.assertTrue(futurePut.isSuccess());
             Assert.assertFalse(slave.getPeerBean().storage().contains(
            		 new Number640(slave.getPeerID(), Number160.ZERO, Number160.ZERO, Number160.ZERO)));
             System.err.println("DONE!");
             
         } finally {
             master.shutdown().await();
             slave.shutdown().await();
         }
    }

	private void printMapStatus(Peer slave, Peer[] peers) {
	    for(Peer peer:peers) {
	    	 if(peer.getPeerBean().peerMap().getAllOverflow().contains(slave.getPeerAddress())) {
	    		 System.err.println("found relayed peer in overflow bag " + peer.getPeerAddress());
	    	 }
	     }
	     
	     for(Peer peer:peers) {
	    	 if(peer.getPeerBean().peerMap().getAll().contains(slave.getPeerAddress())) {
	    		 System.err.println("found relayed peer in regular bag" + peer.getPeerAddress());
	    	 }
	     }
    }
    
	@Test
    public void testRelayDHT() throws Exception {
        final Random rnd = new Random(42);
         Peer master = null;
         Peer unreachablePeer = null;
         try {
             Peer[] peers = Utils2.createNodes(10, rnd, 4000);
             master = peers[0]; // the relay peer
             Utils2.perfectRouting(peers);
             for(Peer peer:peers) {
            	 new PeerNAT(peer);
             }
             
             // Test setting up relay peers
 			unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(13337).makeAndListen();
 			PeerNAT uNat = new PeerNAT(unreachablePeer);
 			uNat.bootstrapBuilder(unreachablePeer.bootstrap().setPeerAddress(master.getPeerAddress()));
 			FutureRelayNAT fbn = uNat.startRelay();
 			fbn.awaitUninterruptibly();
 			Assert.assertTrue(fbn.isSuccess());
             
            // PeerMapConfiguration pmc = new PeerMapConfiguration(Number160.createHash(rnd.nextInt()));
            
            // slave = new PeerMaker(Number160.ONE).peerMap(new PeerMap(pmc)).ports(13337).makeAndListen();
            // FutureRelay rf = new RelayConf(slave).bootstrapAddress(master.getPeerAddress()).start().awaitUninterruptibly();
            // Assert.assertTrue(rf.isSuccess());
            // RelayManager manager = rf.relayManager();
            // System.err.println("relays: "+manager.getRelayAddresses());
            // System.err.println("psa: "+ slave.getPeerAddress().getPeerSocketAddresses());
             //wait for maintenance to kick in
             Thread.sleep(4000);
             
             printMapStatus(unreachablePeer, peers);
             
             FuturePut futurePut = peers[8].put(unreachablePeer.getPeerID()).setData(new Data("hello")).start().awaitUninterruptibly();
             //the relayed one is the slowest, so we need to wait for it!
             futurePut.getFutureRequests().awaitUninterruptibly();
             Assert.assertTrue(futurePut.isSuccess());
             //we cannot see the peer in futurePut.rawResult, as the relayed is the slowest and we finish earlier than that.
             Assert.assertTrue(unreachablePeer.getPeerBean().storage().contains(new Number640(unreachablePeer.getPeerID(), Number160.ZERO, Number160.ZERO, Number160.ZERO)));
             System.err.println("DONE!");
             
         } finally {
             master.shutdown().await();
             unreachablePeer.shutdown().await();
         }
    }
	
	@Test
	public void testVeryFewPeers() throws Exception {
		final Random rnd = new Random(42);
		Peer master = null;
		Peer unreachablePeer = null;
		try {
			Peer[] peers = Utils2.createNodes(3, rnd, 4000);
			master = peers[0]; // the relay peer
			Utils2.perfectRouting(peers);
			for (Peer peer : peers) {
				new PeerNAT(peer);
			}

			// Test setting up relay peers
			unreachablePeer = new PeerMaker(Number160.createHash(rnd.nextInt())).ports(13337).makeAndListen();
			PeerNAT uNat = new PeerNAT(unreachablePeer);
			uNat.bootstrapBuilder(unreachablePeer.bootstrap().setPeerAddress(master.getPeerAddress()));
			FutureRelayNAT fbn = uNat.startRelay();
			fbn.awaitUninterruptibly();
			Assert.assertTrue(fbn.isSuccess());

		} finally {
			master.shutdown().await();
			unreachablePeer.shutdown().await();
		}
	}
    

    private Collection<PeerAddress> getNeighbors(Peer peer) {
    	Map<Number160, DispatchHandler> handlers = peer.getConnectionBean().dispatcher().searchHandler(5);
    	for(Map.Entry<Number160, DispatchHandler> entry:handlers.entrySet()) {
    		if(entry.getValue() instanceof RelayForwarderRPC) {
    			return ((RelayForwarderRPC)entry.getValue()).getAll();  
    		}
    	}
    	return null;
    }
    
}
