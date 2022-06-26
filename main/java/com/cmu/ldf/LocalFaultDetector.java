package com.cmu.ldf;

import java.util.Scanner;

import static com.cmu.config.GlobalConfig.CLIENT_ADDRESS;
import static com.cmu.config.GlobalConfig.GFD_PORT;
import static com.cmu.config.GlobalConfig.LFD_PORT;
import static com.cmu.config.GlobalConfig.SERVER_PORT;


public class LocalFaultDetector {
    private int port;
    private int serverPort;
    private String name;
    private String serverName;
    public static Boolean primaryServer;

    public LocalFaultDetector(String name, int port, String serverName, int serverPort) {
        this.name = name;
        this.port = port;
        this.serverPort = serverPort;
        this.serverName = serverName;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please enter correct args: lfdName (choose from lfd1, lfd2, lfd3)");
            return;
        }
        System.out.println("Launching the LDF!");
        LocalFaultDetector localFaultDetector = new LocalFaultDetector(args[0],
                LFD_PORT, "S" + args[0].substring(args[0].length() - 1), SERVER_PORT);
        localFaultDetector.transfer();
    }

    public void transfer() {
        boolean check = true;
        int heartbeatFreq = 1000;
        // input the heartbeat frequency
        while (check) {
            System.out.print("Scan the heartbeat frequency you need: ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            try {
                heartbeatFreq = Integer.parseInt(input);
                if (heartbeatFreq <= 0) {
                    System.out.println("please input an unsigned integer!");
                } else {
                    check = false;
                }
            } catch (NumberFormatException e) {
                System.out.println("please input an unsigned integer!");
            }
        }
        // open heartbeat entry point for GFD
        new Thread(new PassiveHeartBeatThread(this.port)).start();
        // start heartbeat local server replica
        new Thread(new ActiveHeartBeatAndReportThread(heartbeatFreq
                , "127.0.0.1"
                , serverPort
                , serverName
                , name
                , CLIENT_ADDRESS
                , GFD_PORT)).start();
    }
}
