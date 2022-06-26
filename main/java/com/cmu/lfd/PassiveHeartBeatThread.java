package com.cmu.ldf;

import com.cmu.message.Direction;
import com.cmu.message.HeartbeatMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class PassiveHeartBeatThread implements Runnable{
    private final int port;

    /**
     * initialize PassiveHeartBeatThread with the port for heartbeat
     * @param port the port for heartbeat
     */
    public PassiveHeartBeatThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        Socket socket = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                try {
                    socket = serverSocket.accept();
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    objectInputStream = new ObjectInputStream(inputStream);
                    objectOutputStream = new ObjectOutputStream(outputStream);

                    Object input = objectInputStream.readObject();

                    if (input instanceof HeartbeatMessage) {
                        System.out.println("[" + System.currentTimeMillis() + "] " + input + " Received");
                        ((HeartbeatMessage) input).setDirection(Direction.REPLY);
                        objectOutputStream.writeObject(input);
                        System.out.println("[" + System.currentTimeMillis() + "] " + input + " Sent");
                    }

                    socket.shutdownOutput();
                    socket.close();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
