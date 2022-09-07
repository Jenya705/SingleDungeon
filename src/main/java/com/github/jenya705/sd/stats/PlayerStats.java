package com.github.jenya705.sd.stats;

import lombok.*;

import java.util.UUID;

/**
 * About thread safety of this class:
 * All values will be updated in the main bukkit thread (guarantied by me).
 * So it is normal to set values like that: setMobKills(getMobKills() + 1).
 */
@Data
@Getter(onMethod_ = @Synchronized("lock"))
@Setter(onMethod_ = @Synchronized("lock"))
public class PlayerStats implements Cloneable {

    @Getter(AccessLevel.PRIVATE)
    private final Object lock = new Object[0];

    @Getter
    private final UUID uuid;

    private volatile int mobKills;
    private volatile int sessions;
    private volatile int deaths;

    public float getAverageKills() {
        synchronized (lock) {
            return mobKills / (float) sessions;
        }
    }

}
