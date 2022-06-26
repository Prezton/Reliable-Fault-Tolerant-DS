package com.cmu.client;

import com.cmu.message.ClientServerMessage;
import com.cmu.message.Direction;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;


public class MessageThread implements Callable<ClientServerMessage> {

    private final String remoteAddress;

    private final Integer remotePort;

    private final ClientServerMessage message;

    private static long current = 0L;

    /**
     * MessageThread Constructor
     * @param remoteAddress the remote address this thread will connect with
     * @param remotePort the remote port this thread will connect with
     * @param clientServerMessage the cs-message this thread will transfer to the remote server replica
     */
    public MessageThread(String remoteAddress, Integer remotePort, ClientServerMessage clientServerMessage) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.message = clientServerMessage;
    }

    private boolean incAndGet(long curr) {
        if (curr <= current) {
            return false;
        }
        current = curr;
        return true;
    }

    @Override
    public ClientServerMessage call() {
        InetAddress inet;
        Socket socket = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            inet = InetAddress.getByName(remoteAddress);
            // increase the message counter first
            synchronized (message) {
                message.incRequestNum();
            }
            socket = new Socket(inet, remotePort);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(message);
            System.out.println("[" + System.currentTimeMillis() + "]" + " Sent " + message);

            socket.shutdownOutput();

            inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            Object input = objectInputStream.readObject();
            if (input instanceof ClientServerMessage) {
                // different messages shouldn't change current counter at the same time
                synchronized (MessageThread.class) {
                    message.setDirection(((ClientServerMessage) input).getDirection());
                    if (!incAndGet(message.getRequestNum())) {
                        System.out.println("[" + System.currentTimeMillis() + "] request_num "
                                + message.getRequestNum()
                                + ": Discarded duplicate reply from " + message.getServerName());
                    } else {
                        System.out.println("[" + System.currentTimeMillis() + "]" + " Received " + message);
                    }
                    message.setDirection(Direction.REQUEST);
                }
                return message;
            }
        } catch (IOException | ClassNotFoundException e) {
            //System.out.println("Connection between " + message.getClientName() + " and " + message.getServerName() + " failed.");
            //e.printStackTrace();
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
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
