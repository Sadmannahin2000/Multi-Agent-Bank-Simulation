package bankingsim;

import java.util.Random;

abstract class AgentBase implements Runnable {
    protected final String name;
    protected final Random rng;
    protected final int maxSleepMs;

    AgentBase(String name, int maxSleepMs, long seed) {
        this.name = name;
        this.maxSleepMs = Math.max(0, maxSleepMs);
        this.rng = new Random(seed ^ name.hashCode());
    }

    protected void rndSleep() {
        int ms = (maxSleepMs <= 0) ? 0 : rng.nextInt(maxSleepMs + 1); // uniform [0, max]
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
