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

package net.tomp2p.examples;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.tomp2p.futures.FutureDigest;
import net.tomp2p.futures.FutureGet;
import net.tomp2p.futures.FuturePut;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.rpc.SimpleBloomFilter;
import net.tomp2p.storage.Data;

/**
 * Example how to use bloom filters for efficient search.
 * 
 * @author Thomas Bocek
 * 
 */
public final class ExampleBloomFilter {
    private static final Random RND = new Random(42L);

    /**
     * Empty constructor.
     */
    private ExampleBloomFilter() {
    }

    /**
     * Start the examples.
     * 
     * @param args
     *            Empty
     * @throws Exception .
     */
    public static void main(final String[] args) throws Exception {
        final int peerNr = 100;
        final int port = 4001;
        bloomFilterBasics();
        Peer[] peers = null;
        try {
            peers = ExampleUtils.createAndAttachNodes(peerNr, port);
            ExampleUtils.bootstrap(peers);
            exampleBloomFilter(peers);
        } finally {
            // 0 is the master
            if (peers != null && peers[0] != null) {
                peers[0].shutdown();
            }
        }
    }

    /**
     * Prints out bloom filter basics, how a bloom filter looks like if it gets filled.
     */
    private static void bloomFilterBasics() {
        final int nrElements = 20;
        System.out.println("bloomfilter basics:");
        SimpleBloomFilter<Number160> sbf = new SimpleBloomFilter<Number160>(0.05, nrElements);
        System.out.println("false-prob. rate: " + sbf.expectedFalsePositiveProbability());
        System.out.println("init: " + sbf.getBitSet().size());
        for (int i = 0; i < nrElements; i++) {
            sbf.add(new Number160(i));
            System.out.printf("after %2d insert %s\n", (i + 1), sbf);
        }
    }

    /**
     * Starts the example.
     * 
     * @param peers
     *            All the peers
     * @throws IOException .
     */
    private static void exampleBloomFilter(final Peer[] peers) throws IOException {
        final int nrPeers = 1000;
        final int range1 = 800;
        final int range2 = 1800;
        final int peer10 = 10;
        final int peer20 = 20;
        final int peer30 = 30;
        final int peer60 = 60;
        //

        Number160 nr1 = new Number160(RND);

        Map<Number160, Data> contentMap = new HashMap<Number160, Data>();
        System.out.println("first we store 1000 items from 0-999 under key " + nr1);
        for (int i = 0; i < nrPeers; i++) {
            contentMap.put(new Number160(i), new Data("data " + i));
        }
        FuturePut futurePut = peers[peer30].put(nr1).setDataMapContent(contentMap)
                .setDomainKey(Number160.createHash("my_domain")).start();
        futurePut.awaitUninterruptibly();
        // store another one
        Number160 nr2 = new Number160(RND);
        contentMap = new HashMap<Number160, Data>();
        System.out.println("then we store 1000 items from 800-1799 under key " + nr2);
        for (int i = range1; i < range2; i++) {
            contentMap.put(new Number160(i), new Data("data " + i));
        }
        futurePut = peers[peer60].put(nr2).setDataMapContent(contentMap).setDomainKey(Number160.createHash("my_domain"))
                .start();
        futurePut.awaitUninterruptibly();
        // digest the first entry
        FutureDigest futureDigest = peers[peer20].digest(nr1).setAll().returnBloomFilter()
                .setDomainKey(Number160.createHash("my_domain")).start();
        futureDigest.awaitUninterruptibly();
        // we have the bloom filter for the content keys:
        SimpleBloomFilter<Number160> contentBF = futureDigest.getDigest().contentBloomFilter();
                
        System.out.println("We got bloomfilter for the first key: " + contentBF);
        //TODO: check keyBF.contains(new Number160(123));
        // query for nr2, but return only those that are in this bloom filter
        FutureGet futureGet1 = peers[peer10].get(nr2).setAll().setKeyBloomFilter(contentBF)
                .setDomainKey(Number160.createHash("my_domain")).start();
        futureGet1.awaitUninterruptibly();
        System.out.println("For the 2nd key we requested with this Bloom filer and we got "
                + futureGet1.getDataMap().size() + " items.");
        
        FutureGet futureGet2 = peers[peer10].get(nr2).setAll().bloomFilterIntersect().setKeyBloomFilter(contentBF)
                .setDomainKey(Number160.createHash("my_domain")).start();
        futureGet2.awaitUninterruptibly();
        System.out.println("For the 2nd key we requested with this Bloom filer and we got "
                + futureGet2.getDataMap().size() + " items.");
    }
}
