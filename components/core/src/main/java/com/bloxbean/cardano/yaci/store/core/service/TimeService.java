package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class TimeService {

    private long startTime = 1654041600;
    private String systemStartTime = "2017-09-23T21:44:51Z";
    private SimpleDateFormat format = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");

    private long convertSlotToTime(Era era, long slot) throws ParseException {
        Date date = format.parse(systemStartTime);
        long timeInMillis = date.getTime() + ((207 * 432000) + (slot - 4924800))*1000;
        return timeInMillis ;
    }

}
