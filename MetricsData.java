package org.example;

public class MetricsData {

    private String dateOfEvents;

    private Long incomingEventsForDateUTC;


    public String getDateOfEvents() {
        return dateOfEvents;
    }

    public void setDateOfEvents(String dateOfEvents) {
        this.dateOfEvents = dateOfEvents;
    }

    public Long getIncomingEventsForDateUTC() {
        return incomingEventsForDateUTC;
    }

    public void setIncomingEventsForDateUTC(Long incomingEventsForDateUTC) {
        this.incomingEventsForDateUTC = incomingEventsForDateUTC;
    }

    @Override
    public String toString() {
        return "MetricsData{" +
            "dateOfEvents='" + dateOfEvents + '\'' +
            ", incomingEventsForDateUTC=" + incomingEventsForDateUTC +
            '}';
    }

}

