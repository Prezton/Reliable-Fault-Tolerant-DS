package com.cmu.server;

import com.cmu.message.Direction;
import com.cmu.message.HeartbeatMessage;
import com.cmu.message.MembershipMessage;
import com.cmu.message.ServerServerMessage;
import static com.cmu.server.Server.myState;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;


public class CheckpointThread implements Runnable {

    private final int checkpointFreq;

    private final String remoteAddress;

    private final int remotePort;

    private final String primaryName;

    private final String backupName;

    private long[] myStateReference;

    /**
     * @param checkpointFreq   checkpoint frequency
     * @param remoteAddress    remoteAddress remote address without port number
     * @param remotePort       remote port number
     * @param backupName         the name of the backup replica receving messages from
     *                         this thread
     * @param primaryName      the name of the primary replica
     * @param myStateReference myState in a long[]
     */
    public CheckpointThread(int checkpointFreq, String remoteAddress, int remotePort, String backupName,
            String primaryName, long[] myStateReference) {
        this.checkpointFreq = checkpointFreq;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.backupName = backupName;
        this.primaryName = primaryName;
        this.myStateReference = myStateReference;
    }

    @Override
    public void run() {
        InetAddress inet;
        Socket socket = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        // long myState = myStateReference[0];
        ServerServerMessage message = new ServerServerMessage(primaryName, backupName, myState, 1L, Direction.REQUEST);
        while (true) {
            boolean check = true;
            try {
                // renew myState by referencing the array every 5s
                synchronized (Server.class) {
                    message.setMyState(myState);
                }

                // What is this remoteAddress? the address of the server(in the SC model or the
                // address of the backup in this milestone3)?
                inet = InetAddress.getByName(remoteAddress);
                socket = new Socket(inet, remotePort);
                outputStream = socket.getOutputStream();
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(message);
                System.out.println(System.currentTimeMillis() + " " + message + " Sent");

                socket.shutdownOutput();
                // increment the request number after sending the checkpoint message
                message.incRequestNum();
                socket.close();
            } catch (IOException e) {
                // What should I write in this block when catching this exception above?
                System.out.println("Checkpoint the " + backupName + " failed. Now try again.");
            } finally {
                try {
                    Thread.sleep(checkpointFreq);
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
