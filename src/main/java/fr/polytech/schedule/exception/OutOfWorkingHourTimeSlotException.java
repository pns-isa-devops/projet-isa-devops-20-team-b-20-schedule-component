package fr.polytech.schedule.exception;

import java.io.Serializable;

public class OutOfWorkingHourTimeSlotException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    public OutOfWorkingHourTimeSlotException(String date) {
        super(date);
    }

    @Override
    public String getMessage() {
        return "The slot is not within working hours.";
    }
}
