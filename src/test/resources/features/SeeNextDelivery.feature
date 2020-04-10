Feature: See Next Delivery

  This feature support the way a Warehouse person consults the next delivery they have to prepare

  Background:
    Given actual time is 24/02/2019 09:00
    And a delivery of id 0123456789 is scheduled in 40 minutes

  Scenario: Seeing delivery to prepare
    When the warehouseman push the button "see next delivery"
    Then a delivery to prepare is found
    And the warehouseman see 0123456789 on his screen
