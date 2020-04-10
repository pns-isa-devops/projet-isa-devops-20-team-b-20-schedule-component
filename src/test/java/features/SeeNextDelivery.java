package features;

import arquillian.AbstractScheduleTest;
import cucumber.api.CucumberOptions;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.api.java.en_scouse.An;
import cucumber.runtime.arquillian.CukeSpace;
import cucumber.runtime.arquillian.api.Features;
import fr.polytech.entities.Delivery;
import fr.polytech.schedule.components.DeliveryOrganizer;
import fr.polytech.schedule.components.DeliveryScheduler;
import fr.polytech.schedule.components.ScheduleBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(CukeSpace.class)
@Features({"src/test/resources/features/SeeNextDelivery.feature"})
public class SeeNextDelivery extends AbstractScheduleTest {

    @EJB(name = "schedule")
    private DeliveryScheduler deliveryScheduler;

    @EJB(name = "schedule")
    private DeliveryOrganizer deliveryOrganizer;

    @Inject
    private ScheduleBean schedule;

    private GregorianCalendar calendar;
    private Delivery delivery;
    private Delivery next;

    @Given("^actual time is (\\d+)/(\\d+)/(\\d+) (\\d+):(\\d+)$")
    public void init_environment(int day, int month, int year, int hour, int minute){
        calendar = new GregorianCalendar(year, month, day, hour, minute, 0);
    }

    @And("^a delivery of id (.*) is scheduled today in (\\d+) minutes$")
    public void schedule_delivery(String id, int minute){
        delivery = new Delivery();
        delivery.setDeliveryNumber(id);
        calendar.setTimeInMillis(calendar.getTimeInMillis() + (6000 * minute) );
        schedule.scheduleDelivery(calendar, delivery);
    }

    @When("^the warehouseman push the button \"([^\"]*)\"$")
    public void push_next_button(){
        next = schedule.getNextDelivery();
    }

    @Then("^a delivery to prepare is found$")
    public void is_delivery_found(){
        assertNotNull(next);
    }

    @And("^the warehouseman see (.*) on his screen$")
    public void display_next_delivery(String expectedID){
        assertEquals(next.getDeliveryNumber(), expectedID);
    }
}
