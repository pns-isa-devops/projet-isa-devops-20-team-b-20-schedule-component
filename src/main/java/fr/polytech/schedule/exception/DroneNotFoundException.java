package fr.polytech.schedule.exception;

import java.io.Serializable;

public class DroneNotFoundException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    public DroneNotFoundException(String droneId) {
        super(droneId);
    }

    @Override
    public String toString() {
        return "The drone : " + getMessage() + " has not been found";
    }

}
