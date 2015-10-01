package flabbergast.time;

import org.joda.time.DateTime;

interface ConsumeDateTime {
    void invoke(DateTime d);
}
