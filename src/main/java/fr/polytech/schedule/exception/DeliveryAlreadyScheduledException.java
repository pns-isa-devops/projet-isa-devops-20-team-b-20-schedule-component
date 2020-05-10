package fr.polytech.schedule.exception;

import java.io.Serializable;
import java.util.GregorianCalendar;

import javax.xml.ws.WebFault;

import fr.polytech.entities.Delivery;

@WebFault(targetNamespace = "http://www.polytech.unice.fr/si/4a/isa/dronedelivery/delivery")
public class DeliveryAlreadyScheduledException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deliveryId;

    public DeliveryAlreadyScheduledException(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public DeliveryAlreadyScheduledException(Delivery delivery)
    {
        this(delivery.getDeliveryId());
    }

    @Override
    public String getMessage() {
        return "The delivery : " + deliveryId + " is already scheduled for another hour";
    }

}
