package de.bushnaq.abdalla.kassandra.report.dao;

import de.bushnaq.abdalla.kassandra.Context;
import de.bushnaq.abdalla.util.Util;
import de.bushnaq.abdalla.util.date.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class BurnDownGuide {
    // work planned at that day and all days before that, first day has the total
    // and last day has the last work done
    // public BurnDownRate[] burnDownRate;
    // private DateUtil dateUtil = new DateUtil();
    private final int               days;
    // work planned at that day and all days before that, first day has the total
    // and last day has the last work done
    private       Duration[]        dwindlingWork = null;// work per day, were every day has the total amount reduced by the amount of
    private final Logger            logger        = LoggerFactory.getLogger(this.getClass());
    public final  DateTimeFormatter sdtf          = DateTimeFormatter.ofPattern("yyyy.MMM.dd HH:mm", Util.locale);
    // private Long sprintEndDate;
    private final LocalDateTime     sprintStartDate;
    // private String xlsxFile = null;
    private       Duration[]        work          = null;// work per day, were every day has the total amount reduced by the amount of

    public BurnDownGuide(Context context, LocalDateTime sprintStartDate, int startDayIndex, int endDayIndex)
            throws Exception {


        this.sprintStartDate = DateUtil.toDayPrecision(sprintStartDate);
//        this.sprintEndDate   = DateUtil.toDayPrecision(sprintEndDate);
//        days = DateUtil.calculateDays(sprintStartDate, sprintEndDate) + 1;
        days = endDayIndex + 1;
        work = new Duration[days];
        for (int i = 0; i < days; i++) {
            work[i] = Duration.ZERO;
        }
    }

    /**
     * Add milliseconds worked at that daz
     *
     * @param index
     * @param value
     */
    public void add(int index, Duration value) {
        if (index >= 0 && index < work.length) {
            work[index] = work[index].plus(value);
        }
    }

    public void convertToAccumulatedValues() {
        dwindlingWork = new Duration[days + 1];
        Duration total = Duration.ZERO;
        for (int i = 0; i < days; i++) {
            total = total.plus(work[i]);
        }

        dwindlingWork[0] = total;
        for (int i = 1; i <= days; i++) {
            dwindlingWork[i] = dwindlingWork[i - 1].minus(work[i - 1]);
        }
    }

    public Duration get(int index) {
        return dwindlingWork[index];
    }

    public int getSize() {
        return dwindlingWork.length;
    }

    public Duration getWork(int index) {
        return work[index];
    }

    public int getWorkSize() {
        return work.length;
    }

    public void print(PrintStream out) {
        LocalDateTime ssd = sprintStartDate.truncatedTo(ChronoUnit.DAYS).withHour(8);

        logger.info("-------------------------------------------------------------------------------------------------");
        for (int i = 0; i < days; i++) {
            logger.info(String.format("%02d %s %s", i, DateUtil.createDateString(ssd, sdtf), DateUtil.createDurationString(work[i], false, true, true)));
            ssd = ssd.plusDays(1);
        }
        logger.info("-------------------------------------------------------------------------------------------------");
    }

}
