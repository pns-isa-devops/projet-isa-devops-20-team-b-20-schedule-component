package fr.polytech.schedule.components;

import javax.ejb.Local;

import fr.polytech.entities.Delivery;
import fr.polytech.schedule.exception.DroneNotFoundException;

@Local
public interface DeliveryOrganizer {
    Delivery getNextDelivery();

}
