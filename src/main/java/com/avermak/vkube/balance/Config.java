package com.avermak.vkube.balance;

public class Config {
    private String host = null;
    private int port = 0;
    private boolean useTLS = true;
    private int thinkTime = 500;
    private int warmupCount = 3;

    public Config(String host, int port, boolean useTLS, int thinkTime) {
        this.host = host;
        this.port = port;
        this.useTLS = useTLS;
        this.thinkTime = thinkTime;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean usesTLS() {
        return useTLS;
    }

    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }

    public int getThinkTime() {
        return this.thinkTime;
    }

    public void setThinkTime(int thinkTime) {
        this.thinkTime = thinkTime;
    }

    public int getWarmupCount() {
        return warmupCount;
    }
    public void setWarmupCount(int warmupCount) {
        this.warmupCount = warmupCount;
    }

}
