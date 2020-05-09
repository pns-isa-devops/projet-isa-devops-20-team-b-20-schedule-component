package fr.polytech.schedule.exception;

import java.io.Serializable;

import javax.xml.ws.WebFault;

@WebFault(targetNamespace = "http://www.polytech.unice.fr/si/4a/isa/dronedelivery/drone")
public class NoFreeDroneAtThisTimeSlotException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    private String time;

    public NoFreeDroneAtThisTimeSlotException(String time) {
        this.time = time;
    }

    public NoFreeDroneAtThisTimeSlotException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return "There is no free drone for the Timeslot : " + time;
    }

}
