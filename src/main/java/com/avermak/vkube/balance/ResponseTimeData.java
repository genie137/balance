package com.avermak.vkube.balance;

import java.util.ArrayList;
import java.util.Hashtable;

public class ResponseTimeData {
    public static class ResponseTime {
        int dt = 0;
        int responseTime = 0;
        public ResponseTime(int dt, int responseTime) {this.dt = dt; this.responseTime = responseTime;}
        public String toString() {return "[dt="+dt+", rt="+responseTime+"]";}
    }
    private Hashtable<String, ArrayList<ResponseTime>> data = null;
    private long firstSampleTime = 0;
    private String name = "";

    public ResponseTimeData(String name) {
        this.name = name;
        this.data = new Hashtable<>();
    }
    public synchronized void addHitResponse(String nodeName, int responseMS) {
        var nodeData = this.data.get(nodeName);
        if (nodeData == null) {
            nodeData = new ArrayList<>();
            this.data.put(nodeName, nodeData);
        }
        long t = System.currentTimeMillis();
        if (firstSampleTime == 0) {
            firstSampleTime = t;
        }
        int dt = (int)(t - firstSampleTime);
        nodeData.add(new ResponseTime(dt, responseMS));
        System.out.println("[" + this.name + "]\tnodeName="+nodeName+"\telapsedTime="+dt+"\tresponseTime="+responseMS);
    }

    public synchronized ArrayList<ResponseTime> getResponseTimes(String nodeName) {
        var nodeData = this.data.remove(nodeName);
        if (nodeData == null) {
            return new ArrayList<>();
        }
        return nodeData;
    }
    public synchronized String[] getNodeNames() {
        return this.data.keySet().toArray(new String[0]);
    }

    @Override
    public synchronized String toString() {
        return this.data == null ? "<null>" : this.data.toString();
    }
}
