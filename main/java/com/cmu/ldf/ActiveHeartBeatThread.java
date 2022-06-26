package com.cmu.ldf;

import com.cmu.message.Direction;
import com.cmu.message.HeartbeatMessage;
import com.cmu.message.MembershipMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;


public class ActiveHeartBeatThread implements Runnable, Report{

    private final int heartbeatFreq;

    private final String remoteAddress;

    private final int remotePort;

    private final String replicaName;

    private final String sourceName;

    /**
     * true = live, false = dead
     */
    private boolean replicaStatus;

    /**
     * initialize the ActiveHeartBeatThread with the heartbeat frequency, remote address, port number and replica Id
     * @param heartbeatFreq heartbeatFreq heartbeat frequency
     * @param remoteAddress remoteAddress remote address without port number
     * @param remotePort remote port number
     * @param replicaName replica name
     * @param sourceName the name of this heartbeat thread
     */
    public ActiveHeartBeatThread(int heartbeatFreq, String remoteAddress, int remotePort, String replicaName, String sourceName) {
        this.heartbeatFreq = heartbeatFreq;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.replicaName = replicaName;
        this.sourceName = sourceName;
        replicaStatus = false;
    }

    @Override
    public void run() {
        InetAddress inet;
        Socket socket = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        HeartbeatMessage message = new HeartbeatMessage(sourceName, replicaName, 1, Direction.REQUEST, false);
        while (true) {
            boolean check = true;
            try {
                inet = InetAddress.getByName(remoteAddress);
                socket = new Socket(inet, remotePort);
                outputStream = socket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(message);
                System.out.println(System.currentTimeMillis() + " " + message + " Sent");

                socket.shutdownOutput();

                inputStream = socket.getInputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                Object input = objectInputStream.readObject();
                if (input instanceof HeartbeatMessage) {
                    System.out.println(System.currentTimeMillis() + " " + input + " Received");
                    message.incNum();
                    message.setPrimaryOrNot(((HeartbeatMessage) input).getPrimaryOrNot());
                }
                socket.close();
            } catch (IOException | ClassNotFoundException e) {
                check = false;
                System.out.println("HeartBeating the " + replicaName + " failed. Now try again.");
            } finally {
                // check the membership change
                if (check != replicaStatus) {
                    replicaStatus = check;
                    MembershipMessage membershipMessage = new MembershipMessage(replicaName, replicaStatus, message.getPrimaryOrNot());
                    // report replica status change to the higher level
                    report(membershipMessage);
                }
                try {
                    Thread.sleep(heartbeatFreq);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (socket != null) {
                    try {
                        socket.close();
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
            }
        }
    }
}
