package com.cmu.rm;

import com.cmu.message.MembershipMessage;
import com.cmu.message.PrimaryMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Optional;

import static com.cmu.config.GlobalConfig.RM_PORT;
import static com.cmu.config.GlobalConfig.SERVER_MAP;
import static com.cmu.config.GlobalConfig.SERVER_PORT;

public class ResourceManager {
    private HashMap<String, String> membership;
    private int port;
    private String name;

    public ResourceManager() {
        this.membership = new HashMap<>();
        this.port = RM_PORT;
        this.name = "Resource Manager";
    }

    public void printMembershipInfo() {
        System.out.println("RM: " + membership.size() + " members: " + membership);
    }

    /**
     * update the membership
     * @param serverName the replica which happens with membership change
     * @param addOrRemove true = add, false = remove
     * @param message true = primary, false = backup
     */
    public void updateMembership(String serverName, boolean addOrRemove, MembershipMessage message) {
        if (addOrRemove) {
            membership.put(serverName, message.getPrimaryOrNot() ? "Primary" : "BackUp");
        } else {
            membership.remove(serverName);
        }
        if (!addOrRemove && message.getPrimaryOrNot()) {
            Optional<String> first = membership.keySet().stream().findFirst();
            if (!first.isPresent()) {
                System.exit(0);
            }
            String backupName = first.get();
            PrimaryMessage primaryMessage = new PrimaryMessage(message.getReplicaName(), backupName);
            electNewPrimary(backupName, primaryMessage);
            membership.put(backupName, "Primary");
        }
        System.out.println(message);
        if (!addOrRemove) {
            recoverBackUp(serverName);
        }
        printMembershipInfo();
    }

    /**
     * recover backup
     * @param serverName the server needed to be recovered
     */
    public void recoverBackUp(String serverName) {
        System.out.println("Now trying recover " + serverName);
        char num = serverName.charAt(serverName.length() - 1);
        String address = SERVER_MAP.get(serverName);
        String command = "ssh -i ~/.ssh/mysql.pem ubuntu@" + address + " '/home/ubuntu/run" + num + ".sh'";
        try {
            Process process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            System.out.println("bootstrap " + address + " failed.");
            e.printStackTrace();
        }
        System.out.println("Recover success!");
    }

    /**
     * elect new primary from the backup
     * @param serverName elected backup
     * @param primaryMessage primary message
     */
    public void electNewPrimary(String serverName, PrimaryMessage primaryMessage) {
        String reportAddress = SERVER_MAP.get(serverName);
        int reportPort = SERVER_PORT;
        System.out.println("Now start to report membership change to: " + reportAddress + ":" + reportPort);
        InetAddress inet;
        Socket socket = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            inet = InetAddress.getByName(reportAddress);
            socket = new Socket(inet, reportPort);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            // report the membership change message to the higher level
            objectOutputStream.writeObject(primaryMessage);
            System.out.println(System.currentTimeMillis() + " " + primaryMessage + " Sent");

            socket.shutdownOutput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
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
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Choose new primary successfully!");
    }

    public void listToGfd() {
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
        ResourceManager resourceManager = new ResourceManager();
        resourceManager.listToGfd();
    }
}
