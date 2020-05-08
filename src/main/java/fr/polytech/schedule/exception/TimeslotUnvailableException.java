package fr.polytech.schedule.exception;

import java.io.Serializable;

import javax.xml.ws.WebFault;

@WebFault(targetNamespace = "http://www.polytech.unice.fr/si/4a/isa/dronedelivery/schedule")
public class TimeslotUnvailableException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;
    private String reason;

    public TimeslotUnvailableException(String date) {
        this(date, "");
    }

    public TimeslotUnvailableException(String date, String reason) {
        this.date = date;
        this.reason = reason;
    }

    public TimeslotUnvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return "The time : " + date + " slot is not unvailable" + (reason != "" ? "[Reason : " + reason + "]" : "");
    }
}
