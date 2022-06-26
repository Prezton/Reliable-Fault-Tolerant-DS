package com.cmu.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class GlobalConfig {

    public static final String SERVER1_ADDRESS;

    public static final String SERVER2_ADDRESS;

    public static final String SERVER3_ADDRESS;

    public static final Integer SERVER_PORT;

    public static final Integer LFD_PORT;

    public static final String CLIENT_ADDRESS;

    public static final Integer GFD_PORT;

    public static final Map<String, String> LFD_MAP;

    public static final Map<String, String> SERVER_MAP;

    public static final Integer RM_PORT;

    static {
        Properties properties = new Properties();
        try {
            properties.load(GlobalConfig.class.getClassLoader().getResourceAsStream("config.properties"));
            System.out.println("Global Config loads successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        SERVER1_ADDRESS = properties.getProperty("S1A");
        SERVER2_ADDRESS = properties.getProperty("S2A");
        SERVER3_ADDRESS = properties.getProperty("S3A");
        SERVER_PORT = Integer.parseInt(properties.getProperty("SP"));
        LFD_PORT = Integer.parseInt(properties.getProperty("LFDP"));
        CLIENT_ADDRESS = properties.getProperty("CLIENTA");
        GFD_PORT = Integer.parseInt(properties.getProperty("GFDP"));
        LFD_MAP = new HashMap<>();
        SERVER_MAP = new HashMap<>();
        LFD_MAP.put("lfd1", SERVER1_ADDRESS);
        LFD_MAP.put("lfd2", SERVER2_ADDRESS);
        LFD_MAP.put("lfd3", SERVER3_ADDRESS);
        SERVER_MAP.put("S1", SERVER1_ADDRESS);
        SERVER_MAP.put("S2", SERVER2_ADDRESS);
        SERVER_MAP.put("S3", SERVER3_ADDRESS);
        RM_PORT = Integer.parseInt(properties.getProperty("RMP"));
    }
}
