package com.cmu.gfd;

import com.cmu.ldf.ActiveHeartBeatAndReportThread;
import com.cmu.ldf.ActiveHeartBeatThread;
import com.cmu.message.MembershipMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.cmu.config.GlobalConfig.GFD_PORT;
import static com.cmu.config.GlobalConfig.LFD_PORT;
import static com.cmu.config.GlobalConfig.RM_PORT;
import static com.cmu.config.GlobalConfig.SERVER1_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER2_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER3_ADDRESS;

public class GlobalFaultDetector {
    private HashSet<String> membership;
    private int heartbeatFreq;
    private int port;
    private String name;
    private List<String> serverAddress;
    private List<Integer> serverPorts;

    public GlobalFaultDetector() {
        membership = new HashSet<>();
        this.heartbeatFreq = 5000;
        this.name = "GFD";
        this.port = GFD_PORT;
        serverPorts = new ArrayList<>();
        serverPorts.add(LFD_PORT);
        serverPorts.add(LFD_PORT);
        serverPorts.add(LFD_PORT);
        serverAddress = new ArrayList<>();
        serverAddress.add(SERVER1_ADDRESS);
        serverAddress.add(SERVER2_ADDRESS);
        serverAddress.add(SERVER3_ADDRESS);
    }

    public List<String> getServerAddress() {
        return serverAddress;
    }

    public List<Integer> getServerPorts() {
        return serverPorts;
    }

    public int getMemberCount() {
        return membership.size();
    }

    public void printMembershipInfo() {
        System.out.println("GFD: " + getMemberCount() + " members: " + membership.toString());
    }

    /**
     * create heartbeat thread towards local fault detector
     * @param port lfd remote port
     * @param lfdName lfd name (replica name)
     * @return new ActiveHeartBeatThread
     */
    public ActiveHeartBeatThread sendHeartbeat(int port, String lfdName, String address) {
        return new ActiveHeartBeatThread(this.heartbeatFreq, address, port, lfdName, this.name);
    }

    /**
     * update the membership
     * @param serverName the replica which happens with membership change
     * @param addOrRemove true = add, false = remove
     * @param message true = primary, false = backup
     */
    public void updateMembership(String serverName, boolean addOrRemove, MembershipMessage message) {
        if (addOrRemove) {
            membership.add(serverName);
            printMembershipInfo();
        } else if (membership.contains(serverName)) {
            membership.remove(serverName);
            printMembershipInfo();
        }
        ActiveHeartBeatAndReportThread prototype = new ActiveHeartBeatAndReportThread(
                0,
                "",
                0,
                "",
                "",
                "127.0.0.1",
                RM_PORT);
        Thread reportThread = new Thread(() -> prototype.report(message));
        reportThread.start();
    }

    public void listenToLFD() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            serverSocket = new ServerSocket(this.port);
            while (true) {
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                objectOutputStream = new ObjectOutputStream(outputStream);

                MembershipMessage input = (MembershipMessage) objectInputStream.readObject();

                System.out.println("Received: " + input);
                String serverName = input.getReplicaName();
                boolean addOrRemove = input.getAddOrRemove();
                updateMembership(serverName, addOrRemove, input);

                socket.shutdownOutput();
                socket.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Launching the GFD!");
        GlobalFaultDetector gfd = new GlobalFaultDetector();
        gfd.printMembershipInfo();
        // heartbeat different lfd
        for (int i = 0; i < gfd.getServerAddress().size(); i++) {
            new Thread(gfd.sendHeartbeat(gfd.getServerPorts().get(i)
                    , "lfd" + (i + 1)
                    , gfd.getServerAddress().get(i))).start();
        }
        // listen to membership change from lfd
        gfd.listenToLFD();
    }
}
