package org.example;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class HelperUtil {


    public static String convertToMMddYYYY(long epochTime){
       return DateTimeFormatter.ofPattern("MM-dd-yyyy")
            .withZone(ZoneId.of("UTC"))
            .format(Instant.ofEpochSecond(epochTime));
    }

    public static int getNumbersOfDaysSinceOnboarded(long epochEndTime, long createDate) {
        Instant instant1 = Instant.ofEpochSecond(epochEndTime);
        Instant instant2 = Instant.ofEpochSecond(createDate);

        return (int) ChronoUnit.DAYS.between(instant1, instant2);
    }
}

