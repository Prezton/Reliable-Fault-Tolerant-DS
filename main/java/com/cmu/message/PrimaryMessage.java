package com.cmu.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Sent from RM to the designated new primary.
 * when you construct a PrimaryMessage object,
 * the constructor will be like this "PrimaryMessage(S1, S2)" in the case
 * that old primary S1 dies and RM wants S2 to be the new primary
 * @author
 */
@Data
@AllArgsConstructor
public class PrimaryMessage implements Serializable{
    private String oldPrimary;
    private String newPrimary;

    @Override
    public String toString() {
        return " <" + getOldPrimary() + "dies, " + getNewPrimary() + "arises" + ">";
    }
}
