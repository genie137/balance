package com.avermak.vkube.balance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public static final String CONFIG_FILE_NAME = "balance.ini";

    private static volatile Config self = null;

    public String urlREST;
    public String urlGRPC;
    public int thinkTime;
    public int warmupCount;

    public boolean demoMode;

    public static Config getInstance() {
        if (self == null) {
            synchronized (Config.class) {
                if (self == null) {
                    self = loadConfig();
                    if (self == null) {
                        System.out.println("Unable to load saved config. Using defaults.");
                        self = new Config();
                    }
                }
            }
        }
        return self;
    }

    public Config() {
        this.urlREST = "https://localhost:8443/api";
        this.urlGRPC = "grpcs://localhost:8443";
        this.thinkTime = 1000;
        this.warmupCount = 1;
        this.demoMode = false;
    }

    private static Config loadConfig() {
        Gson gson = new Gson();
        File cf = new File(CONFIG_FILE_NAME);
        if (!cf.exists()) {
            return null;
        }
        try {
            FileReader fr = new FileReader(cf);
            return gson.fromJson(fr, Config.class);
        } catch (Exception ex) {
            System.out.println("Error loading config from file. " + ex);
        }
        return null;
    }
    private void save() {
        FileWriter fw = null;
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(this);
            fw = new FileWriter(CONFIG_FILE_NAME);
            fw.write(json);
        } catch (Exception ex) {
            System.out.println("Error saving config to file. " + ex);
        } finally {
            if (fw != null) {
                try { fw.close(); } catch (Exception ex) { System.out.println("Error closing file writer. " + ex); }
            }
        }
    }
    public String getUrlREST() {
        return urlREST;
    }

    public void setUrlREST(String urlREST) {
        this.urlREST = urlREST;
        save();
    }

    public String getUrlGRPC() {
        return urlGRPC;
    }

    public void setUrlGRPC(String urlGRPC) {
        this.urlGRPC = urlGRPC;
        save();
    }

    public int getThinkTime() {
        return this.thinkTime;
    }

    public void setThinkTime(int thinkTime) {
        this.thinkTime = thinkTime;
        save();
    }

    public int getWarmupCount() {
        return warmupCount;
    }
    public void setWarmupCount(int warmupCount) {
        this.warmupCount = warmupCount;
        save();
    }
    public boolean isDemoMode() {
        return demoMode;
    }
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
        save();
    }
}
