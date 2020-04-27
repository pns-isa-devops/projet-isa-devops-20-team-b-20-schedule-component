package fr.polytech.schedule.exception;

import javax.ws.rs.NotFoundException;

public class DroneNotFoundException extends NotFoundException {

    public DroneNotFoundException(String message){
        super(message);
    }

}
