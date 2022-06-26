package com.cmu.server;

import com.cmu.message.ClientServerMessage;
import com.cmu.message.Direction;
import com.cmu.message.HeartbeatMessage;
import com.cmu.message.PrimaryMessage;
import com.cmu.message.ServerServerMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.cmu.config.GlobalConfig.SERVER1_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER2_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER3_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER_MAP;
import static com.cmu.config.GlobalConfig.SERVER_PORT;

public class Server {

    public static long myState = -1;

    // I have to put myState in another array to make it accessible in
    // CheckpointThread
    private long[] myStateReference = new long[1];

    private final int port;
    private boolean primary;//the membership might be changed
    private final String myAddress;
    private List<String> serverAddress;
    private int checkpointFreq;
    private final String myName;

    // add two arguments:myAddress and myName. I also put myState in an array
    public Server(int port, boolean primary, String myAddress, String myName) {
        this.port = port;
        this.primary = primary;
        this.myAddress = myAddress;
        serverAddress = new ArrayList<>();
        serverAddress.add(SERVER1_ADDRESS);
        serverAddress.add(SERVER2_ADDRESS);
        serverAddress.add(SERVER3_ADDRESS);
        this.checkpointFreq = 5000;
        this.myName = myName;
        myStateReference[0] = myState;
    }

    public static void main(String[] args) {
        System.out.println("Launching the server!");
        boolean primary;
        // select S1 as the primary server
        String myName = args[0];
        String myAddress = SERVER_MAP.get(myName);
        primary = myAddress.equals(SERVER1_ADDRESS) ? true : false;
        if (args.length > 1) {
            primary = false;
        }
        Server server = new Server(SERVER_PORT, primary, myAddress, myName);
        server.transfer();
    }

    public void transfer() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        boolean ready = this.primary;

        try {
            // if server is primary need to send my state
            if (this.primary) {
                // sends myState to other server using server-server msg class and open two
                // threads(but how can I quiesce????)
                for (int i = 0; i < serverAddress.size(); i++) {
                    String address = serverAddress.get(i);
                    if (!address.equals(myAddress)) {
                        // What should be the address written here?
                        new Thread(new CheckpointThread(checkpointFreq, address, SERVER_PORT,
                                "S" + String.valueOf(i + 1), myName, myStateReference)).start();
                    }
                }
            }
            // if server is not primary, just do what it did in milestone2 but also check
            // the ServerServerMessage
            serverSocket = new ServerSocket(port);
            while (true) {
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                objectInputStream = new ObjectInputStream(inputStream);
                objectOutputStream = new ObjectOutputStream(outputStream);

                Object input = objectInputStream.readObject();

                if (input instanceof HeartbeatMessage) {
                    System.out.println("[" + System.currentTimeMillis() + "] " + input + " Received");
                    ((HeartbeatMessage) input).setDirection(Direction.REPLY);
                    ((HeartbeatMessage) input).setPrimaryOrNot(this.primary);
                    objectOutputStream.writeObject(input);
                    System.out.println("[" + System.currentTimeMillis() + "] " + input + " Sent");
                } else if (input instanceof ClientServerMessage) {
                    // only primary replica deals with client's request
                    synchronized (Server.class) {
                        if (this.primary) {
                            System.out.println("[" + System.currentTimeMillis() + "]" + " Received " + input);
                            System.out.println("[" + System.currentTimeMillis() + "]" + " my_state_"
                                    + ((ClientServerMessage) input).getServerName() + " = " + myState
                                    + " before processing " + input);
                            myState = ((ClientServerMessage) input).getRequestNum();

                            // the state in myStateReference should also be changed
                            myStateReference[0] = myState;
                            // Some problem with "my_state_s1". why is it always s1???
                            System.out.println("[" + System.currentTimeMillis() + "]" + " my_state_ = " + myState
                                    + " after processing " + input);
                            ((ClientServerMessage) input).setDirection(Direction.REPLY);
                            System.out.println("[" + System.currentTimeMillis() + "]" + " Sending " + input);
                            objectOutputStream.writeObject(input);
                        } else {
                            myState = ((ClientServerMessage) input).getRequestNum();
                        }
                    }
                } else if (input instanceof ServerServerMessage && !this.primary) {
                    // only backup deals with receiving checkpoints
                    // check if the input is server-server msg. If so, receive it and renew myState
                    System.out.println("[" + System.currentTimeMillis() + "]" + " Received " + input);
                    System.out.println("[" + System.currentTimeMillis() + "]" + " my_state_"
                            + ((ServerServerMessage) input).getBackupName() + " = " + myState + " before processing "
                            + input);
                    myState = ((ServerServerMessage) input).getMyState();

                    // the state in myStateReference should also be changed
                    // myStateReference[0] = myState;
                    System.out.println("[" + System.currentTimeMillis() + "]" + " my_state_"
                            + ((ServerServerMessage) input).getBackupName() + " = " + myState + " after processing "
                            + input);
                    if (!ready) {
                        ready = true;
                        System.out.println("Now " + myName + " is ready!");
                    }

                } else if (input instanceof PrimaryMessage && !this.primary) {
                    //the backup becomes the primary
                    System.out.println(System.currentTimeMillis() + " " + input + " Received");
                    this.primary = true;
                    for (int i = 0; i < serverAddress.size(); i++) {
                        //do what the primary did at the beginning
                        String address = serverAddress.get(i);
                        if (!address.equals(myAddress)) {
                            // What should be the address written here?
                            new Thread(new CheckpointThread(checkpointFreq, address, SERVER_PORT,
                                    "S" + String.valueOf(i + 1), myName, myStateReference)).start();
                        }
                    }
                }
                if (!ready) {
                    System.out.println("Still not ready!");
                }

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
            System.out.println("Server End!");
        }

    }

}
