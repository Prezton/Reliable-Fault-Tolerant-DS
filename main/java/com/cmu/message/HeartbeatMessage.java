package com.cmu.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;


@Data
@AllArgsConstructor
public class HeartbeatMessage implements Serializable {
    private String receiver;
    private String sender;
    private Integer num;
    private Direction direction;
    private Boolean primaryOrNot;

    public void incNum() {
        num++;
    }

    @Override
    public String toString() {
        return "heartbeat_count: " + num + " " + sender + " " + direction + " heartbeat from " + receiver;
    }
}
