package com.cmu.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;


@Data
@AllArgsConstructor
public class ServerServerMessage implements Serializable {
    private String primaryName;
    private String backupName;
    private long myState;
    private Long requestNum;
    private Direction direction;

    @Override
    public String toString() {
        return " <" + getPrimaryName() + ", " + getBackupName() + ", " + getMyState() + ", " + getRequestNum() + ", "
                + getDirection() + ">";
    }

    public void incRequestNum() {
        requestNum++;
    }
}
