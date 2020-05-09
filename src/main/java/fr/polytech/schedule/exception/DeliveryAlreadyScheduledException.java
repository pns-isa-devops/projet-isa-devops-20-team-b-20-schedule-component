package fr.polytech.schedule.exception;

import java.io.Serializable;
import java.util.GregorianCalendar;

import javax.xml.ws.WebFault;

import fr.polytech.entities.Delivery;

@WebFault(targetNamespace = "http://www.polytech.unice.fr/si/4a/isa/dronedelivery/delivery")
public class DeliveryAlreadyScheduledException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deliveryId;
    private String date;

    public DeliveryAlreadyScheduledException(String deliveryId, String date) {
        this.deliveryId = deliveryId;
        this.date = date;
    }

    public DeliveryAlreadyScheduledException(Delivery delivery, GregorianCalendar date)
    {
        this(delivery.getDeliveryId(), date.toString());
    }

    public DeliveryAlreadyScheduledException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return "The delivery : " + deliveryId + " is already scheduled for " + date;
    }

}
