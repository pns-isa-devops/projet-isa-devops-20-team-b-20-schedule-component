package fr.polytech.schedule.components;

import javax.ejb.Local;

import fr.polytech.entities.Delivery;

@Local
public interface DeliveryOrganizer {
    Delivery getNextDelivery();

}
