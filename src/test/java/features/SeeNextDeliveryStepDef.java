package features;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.polytech.entities.Delivery;
import fr.polytech.schedule.components.ScheduleBean;
import javax.inject.Inject;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class SeeNextDeliveryStepDef {

    @Inject
    private ScheduleBean schedule;

    private GregorianCalendar calendar;
    private Delivery delivery;
    private Delivery next;

    @Given("^actual time is (\\d+)/(\\d+)/(\\d+) (\\d+):(\\d+)$")
    public void init_environment(int day, int month, int year, int hour, int minute){
        calendar = new GregorianCalendar(year, month, day, hour, minute, 0);
        System.out.println("AFK: " + calendar.getTime().toString());
    }

    @And("^a delivery of id \"(.*?)\" is scheduled today in (\\d+) minutes$")
    public void schedule_delivery(String id, int minute){
        delivery = new Delivery();
        delivery.setDeliveryNumber(id);
        calendar.setTimeInMillis(calendar.getTimeInMillis() + (60000 * minute));
        schedule.scheduleDelivery(calendar, delivery);
    }

    @When("^the warehouseman push the button see-next-delivery$")
    public void push_next_button(){
        next = schedule.getNextDelivery();
    }

    @Then("^a delivery to prepare is found$")
    public void is_delivery_found(){
        assertNotNull(next);
    }

    @And("^the warehouseman see \"(.*?)\" on his screen$")
    public void display_next_delivery(String expectedID){
        assertEquals(expectedID, next.getDeliveryNumber());
    }
}
