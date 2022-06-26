package com.cmu.client;

import com.cmu.message.ClientServerMessage;
import com.cmu.message.Direction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static com.cmu.config.GlobalConfig.SERVER1_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER2_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER3_ADDRESS;
import static com.cmu.config.GlobalConfig.SERVER_PORT;


@Data
@AllArgsConstructor
public class Client {

    private String clientName;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Please enter one client name!");
            return;
        }
        Client client = new Client(args[0]);
        System.out.println("Launching the Client!");
        client.transfer();
    }

    private void transfer() {
        List<ClientServerMessage> clientServerMessages = new ArrayList<>();
        // initialize cs-messages
        clientServerMessages.add(new ClientServerMessage(clientName, "S1", 0L, Direction.REQUEST));
        clientServerMessages.add(new ClientServerMessage(clientName, "S2", 0L, Direction.REQUEST));
        clientServerMessages.add(new ClientServerMessage(clientName, "S3", 0L, Direction.REQUEST));
        // initialize server-ports
        List<Integer> serverPorts = new ArrayList<>();
        serverPorts.add(SERVER_PORT);
        serverPorts.add(SERVER_PORT);
        serverPorts.add(SERVER_PORT);
        // initialize server-ports
        List<String> serverAddress = new ArrayList<>();
        serverAddress.add(SERVER1_ADDRESS);
        serverAddress.add(SERVER2_ADDRESS);
        serverAddress.add(SERVER3_ADDRESS);
        try {
            while (true) {
                // send manually
                new Scanner(System.in).nextLine();
                // create three tasks to connect with different server replicas
                List<FutureTask<ClientServerMessage>> futureTasks = new ArrayList<>();
                for (int i = 0; i < clientServerMessages.size(); i++) {
                    FutureTask<ClientServerMessage> task = new FutureTask<>(
                            new MessageThread(serverAddress.get(i)
                                    , serverPorts.get(i)
                                    , clientServerMessages.get(i)));
                    futureTasks.add(task);
                    new Thread(task).start();
                }
                ClientServerMessage message = null;
                Set<Integer> set = new HashSet<>();
                // try to find the task with good end
                while (message == null) {
                    int i = 0;
                    while (!futureTasks.get(i).isDone() || set.contains(i)) {
                        i++;
                        if (i == futureTasks.size()) {
                            i = 0;
                        }
                    }
                    message = futureTasks.get(i).get();
                    if (message == null) {
                        set.add(i);
                        if (set.size() == futureTasks.size()) {
                            System.out.println("Cannot receive primary response, But you still can try!");
                            break;
                        }
                    }
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Client End!");
        }
    }
}
