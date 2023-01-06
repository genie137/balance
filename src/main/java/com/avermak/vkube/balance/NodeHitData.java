package com.avermak.vkube.balance;

import java.util.Hashtable;

public class NodeHitData {
    private Hashtable<String, Integer> data = null;

    public NodeHitData() {
        this.data = new Hashtable<>();
    }


    public synchronized int incrementHit(String nodeName) {
        Integer hits = this.data.get(nodeName);
        if (hits == null) {
            hits = new Integer(0);
        }
        hits++;
        this.data.put(nodeName, hits);
        return hits;
    }

    public synchronized int getHitsForNode(String nodeName) {
        Integer hits = this.data.get(nodeName);
        if (hits == null) {
            return -1;
        }
        return hits;
    }

    public synchronized String[] getNodeNames() {
        return this.data.keySet().toArray(new String[0]);
    }

    @Override
    public synchronized String toString() {
        return this.data == null ? "<null>" : this.data.toString();
    }
}
