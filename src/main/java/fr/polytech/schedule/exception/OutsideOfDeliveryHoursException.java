package fr.polytech.schedule.exception;

import java.io.Serializable;

import javax.xml.ws.WebFault;

@WebFault(targetNamespace = "http://www.polytech.unice.fr/si/4a/isa/dronedelivery/delivery")
public class OutsideOfDeliveryHoursException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    private int startingHour;
    private int closingHour;

    public OutsideOfDeliveryHoursException(int startingHour, int closingHour) {
        this.startingHour = startingHour;
        this.closingHour = closingHour;
    }

    public OutsideOfDeliveryHoursException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return "Forbidden : cannot schedule a delivery before " + startingHour + "h and after " + closingHour + "h";
    }

}
