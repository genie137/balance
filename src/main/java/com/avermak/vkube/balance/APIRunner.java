package com.avermak.vkube.balance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class APIRunner extends Thread {
    protected boolean shouldRun = true;
    Config config = null;
    protected NodeHitData hdata = null;
    protected ResponseTimeData rdata = null;

    private int sampleCount = 0;

    public APIRunner(Config cfg, NodeHitData hdata, ResponseTimeData rdata, int thinkTime) {
        this.config = cfg;
        this.hdata = hdata;
        this.rdata = rdata;
    }
    public void scheduleStop() {
        this.shouldRun = false;
        this.interrupt();
    }

    @Override
    public void run() {
        int errCount = 0;
        while (shouldRun) {
            try {
                call();
            } catch (Exception ex) {
                System.out.println("Error calling " + getClass().getName() + ". " + ex);
                errCount++;
            }
            if (errCount > 10) {
                System.out.println("Too many errors in " + getClass().getName() + ". Stopping run.");
                shouldRun = false;
            }
            try {Thread.sleep(this.config.getThinkTime());} catch (InterruptedException iex) {}
        }
    }

    protected void recordHitData(String nodeName, int responseTime) {
        if (this.sampleCount < this.config.getWarmupCount()) {
            System.out.println(this.getClass().getName() + ": Hit sample skipped during warmup (nodeName="+nodeName+", responseTime="+responseTime+")");
        } else {
            hdata.incrementHit(nodeName);
            rdata.addHitResponse(nodeName, responseTime);
        }
        this.sampleCount++;
    }

    protected abstract void call() throws Exception;

    public static int random(int min, int max) {
        return ((int) (Math.random() * (max - min  + 1)) + min);
    }

    public String readStreamText(InputStream stream) throws IOException {
        byte[] buf = new byte[4096];
        int bread = -1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((bread = stream.read(buf)) != -1) {
            baos.write(buf, 0, bread);
        }
        return new String(baos.toByteArray(), "UTF-8");
    }
}
