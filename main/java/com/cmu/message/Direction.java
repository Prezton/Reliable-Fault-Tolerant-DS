package com.cmu.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;


@AllArgsConstructor
@Getter
public enum Direction implements Serializable {
    /**
     * direction of request
     */
    REQUEST(0),
    /**
     * direction of reply
     */
    REPLY(1);
    /**
     * direction code
     */
    private final int code;
}
