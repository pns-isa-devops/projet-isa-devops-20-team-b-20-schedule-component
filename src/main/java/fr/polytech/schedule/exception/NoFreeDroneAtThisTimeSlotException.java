package fr.polytech.schedule.exception;

public class NoFreeDroneAtThisTimeSlotException extends Throwable {

    public NoFreeDroneAtThisTimeSlotException() {
        super();
    }

    @Override
    public String getMessage() {
        return "There is no free drone for this time slot.";
    }

    @Override
    public String toString() {
        return "There is no free drone for this time slot.";
    }

}
