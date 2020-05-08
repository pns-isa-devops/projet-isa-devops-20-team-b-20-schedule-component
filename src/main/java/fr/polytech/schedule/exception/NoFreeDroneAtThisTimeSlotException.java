package fr.polytech.schedule.exception;

import java.io.Serializable;

public class NoFreeDroneAtThisTimeSlotException extends Exception implements Serializable {

    public NoFreeDroneAtThisTimeSlotException() {
        super();
    }

    @Override
    public String getMessage() {
        return "There is no free drone for this time slot.";
    }

}
