package org.hypeastrum.arduino.due.slitscan;

import android.util.Log;

import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StatusCenter {
    private final ConcurrentHashMap<Object, AtomicLong> counters = new ConcurrentHashMap<Object, AtomicLong>();
    private final ConcurrentHashMap<Object, String> statuses = new ConcurrentHashMap<Object, String>();

    public void incCounter(Object key) {
        addCounter(key, 1);
    }

    public void addCounter(Object key, long count) {
        final AtomicLong atomicLong = counters.get(key);
        if (atomicLong == null) {
            counters.put(key, new AtomicLong(count));
        } else {
            atomicLong.addAndGet(count);
        }
    }

    public void setStatus(Object key, String value) {
        statuses.put(key, value);
        Log.d(getClass().getSimpleName(), String.format("%s %s", key, value));
    }

    public String getStatus(Object key) {
        return statuses.get(key);
    }

    public long getCounter(Object key) {
        final AtomicLong atomicLong = counters.get(key);
        return atomicLong != null ? atomicLong.get() : 0;
    }

    public Set<Object> getCounterKeys() {
        return counters.keySet();
    }

    public Set<Object> getStatusKeys() {
        return statuses.keySet();
    }

}
