package fr.polytech.schedule.exception;

import java.io.Serializable;

public class TimeslotUnvailableException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    public TimeslotUnvailableException(String date) {
        super(date);
    }

    @Override
    public String toString() {
        return "The time : " + getMessage() + "slot already has a delivery";
    }
}
