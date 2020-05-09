package fr.polytech.schedule.exception;

import java.io.Serializable;

import javax.xml.ws.WebFault;

@WebFault(targetNamespace = "http://www.polytech.unice.fr/si/4a/isa/dronedelivery/delivery")
public class ZeroDronesInWarehouseException extends Exception implements Serializable {

    public ZeroDronesInWarehouseException() {
        super();
    }

    public ZeroDronesInWarehouseException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return "There is no registered drone in the warehouse.";
    }

}
