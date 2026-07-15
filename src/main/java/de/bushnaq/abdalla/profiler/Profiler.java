/*
 *
 * Copyright (C) 2025-2025 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.bushnaq.abdalla.profiler;

import de.bushnaq.abdalla.util.date.DateUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Measures execution time from creation to closing.
 * Supports nested creation of such counters
 *
 * @author abdalla
 */
public class Profiler implements AutoCloseable {
    static final String BLACK_LEFT_POINTING_TRIANGLE  = "<";
    static final String BLACK_RIGHT_POINTING_TRIANGLE = ">";

    @Getter
    @Setter
    static boolean          abbreviatedReport = false;
    static boolean          enableTraceLogger = false;
    static ProfilerResult   intermediateResult;
    static SampleSet        intermediateSampleSet;
    static Logger           logger            = LoggerFactory.getLogger(Profiler.class);
    static SampleSet        sampleSet         = new SampleSet();
    static Deque<SampleSet> stack             = new ArrayDeque<>();
    static long             start;

    static {
        for (SampleType type : SampleType.values()) {
            sampleSet.putSample(type, new Sample(type));
        }
    }

    /**
     * Wrap all your code with a generic profiler catching all what other profilers are missing in the Othr category
     */
    public Profiler() {
        this(null, SampleType.OTHR);
    }

    public Profiler(SampleType type) {
        this(null, type);
    }

    public Profiler(String subject) {
        this(subject, SampleType.OTHR);
    }

    public Profiler(String subject, SampleType type) {
        if (start == 0) {
            if (enableTraceLogger) {
                logger.trace(String.format("start(%s)", type.name()));
            }
            //            sampleSet.setTotalStart();
        } else {
            if (enableTraceLogger) {
                logger.trace(String.format("pause(%s) start(%s)", Profiler.sampleSet.getType().name(), type.name()));
            }
            measure();
            push();
        }
        sampleSet.setSubject(subject);
        sampleSet.setType(type);
        start = System.nanoTime();
        logStart(subject);
    }

    @Override
    public void close() {
        measure();
        if (stack.size() == 0) {
            if (enableTraceLogger) {
                logger.trace(String.format("close(%s)", sampleSet.getType().name()));
            }
        } else {
            if (enableTraceLogger) {
                logger.trace(String.format("close(%s) resume(%s)", Profiler.sampleSet.getType().name(), stack.peek().getType().name()));
            }
            sampleSet = pop();
            start     = System.nanoTime();
        }
        if (intermediateSampleSet.getSubject() != null) {
            log(intermediateSampleSet.getSubject());
        }
    }

    public static void generateResult() {
        if (intermediateSampleSet == null) {

        }
        intermediateResult = new ProfilerResult(intermediateSampleSet.getTotalStart(), intermediateSampleSet);
    }

    public static void incrementCounter(String counterName, long count) {
        Long counter = sampleSet.getCounter(counterName);
        if (counter == null) {
            counter = Long.valueOf(0);
        }
        counter += count;
        sampleSet.put(counterName, counter);
    }

    public static boolean isEnableTraceLogger() {
        return enableTraceLogger;
    }

    /**
     * generates an intermediate result and logs it
     *
     * @param subject
     */
    public static void log(String subject) {
        generateResult();
        //        long totalDelta = System.nanoTime() - totalStart;
        //        long gcCount2 = 0;
        //        long gcTime2 = 0;
        //        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
        //            gcCount2 += gc.getCollectionCount();
        //            gcTime2 += gc.getCollectionTime();
        //        }

        logger.info("--------------------------------------------------");
        if (subject.length() > 0) {
            logger.info(String.format(BLACK_LEFT_POINTING_TRIANGLE + "[%s]", subject));
        }

        for (SampleType type : intermediateSampleSet.sampleKeySet()) {
            Sample sample = intermediateSampleSet.getSample(type);
            if (sample.getTimeNanoSec() > 0 || !abbreviatedReport)
                logger.info(String.format("[%4s] %s", sample.getType().name(), nanoToString(sample.getTimeNanoSec())));
        }
        logger.info(String.format("[%4s] %s (%d%%)", "?", nanoToString(intermediateResult.delta),
                (intermediateResult.delta * 100) / intermediateResult.totalDelta));
        logger.info(String.format("[%4s] %s", "All", nanoToString(intermediateResult.totalDelta)));
        for (String counterName : intermediateSampleSet.counterKeySet()) {
            Long counter = intermediateSampleSet.getCounter(counterName);
            logger.info(String.format("[%4s] %d", counterName, counter));
        }
        logger.info("--------------------------------------------------");
    }

    private void logStart(String subject) {
        if (subject != null) {
            logger.info("--------------------------------------------------");
            logger.info(String.format(BLACK_RIGHT_POINTING_TRIANGLE + "[%s]", subject));
            logger.info("--------------------------------------------------");
        }
    }

    private void measure() {
        if (start != 0) {
            final long end    = System.nanoTime();
            final long delta  = end - start;
            Sample     sample = sampleSet.getSample();
            sample.addTimeNanoSec(delta);
            if (enableTraceLogger) {
                logger.trace(String.format("%s %dms", sampleSet.getType().name(), delta / 1000000));
            }
            intermediateSampleSet = sampleSet;
        }
    }

    private static String nanoToString(long time) {
        return DateUtil.create24hDurationString(time / 1000000L, true, true, true, true, false);
    }

    private SampleSet pop() {
        SampleSet temp = stack.pop();
        temp.add(sampleSet);
        return temp;

    }

    private void push() {
        //---push current sample set on the stack
        try {
            stack.push(sampleSet.clone());
        } catch (CloneNotSupportedException e) {
            logger.error(e.getMessage(), e);
        }
        //---reset current sample set
        sampleSet.reset();
    }

    public static void setEnableTraceLogger(boolean enableTraceLogger) {
        Profiler.enableTraceLogger = enableTraceLogger;
    }

}
