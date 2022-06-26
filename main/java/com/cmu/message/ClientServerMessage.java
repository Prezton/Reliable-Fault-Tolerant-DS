package com.cmu.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;


@Data
@AllArgsConstructor
public class ClientServerMessage implements Serializable {
    private String clientName;
    private String serverName;
    private Long requestNum;
    private Direction direction;

    @Override
    public String toString() {
        return " <"+ getClientName()
                + ", " + getServerName() + ", " + getRequestNum() + ", " + getDirection() + ">" ;
    }

    public void incRequestNum() {
        requestNum++;
    }
}
