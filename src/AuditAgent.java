package bankingsim;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class AuditAgent extends AgentBase {
    private final BankAccount[] accounts;
    private final AtomicInteger globalTxnCounter;
    private final boolean isTreasury; // false = internal bank; true = US Treasury
    private int lastSeenTxn = 0;

    public AuditAgent(String name, BankAccount[] accounts,
                      AtomicInteger globalTxnCounter, boolean isTreasury,
                      int maxSleepMs, long seed) {
        super(name, maxSleepMs, seed);
        this.accounts = accounts;
        this.globalTxnCounter = globalTxnCounter;
        this.isTreasury = isTreasury;
    }

    @Override public void run() {
        while (true) {
            ReentrantLock[] locks = new ReentrantLock[accounts.length];
            boolean[] held = new boolean[accounts.length];

            try {
                boolean all = true;
                for (int i = 0; i < accounts.length; i++) {
                    locks[i] = accounts[i].lock();
                    held[i] = locks[i].tryLock();        // non-blocking per spec
                    if (!held[i]) { all = false; break; }
                }
                if (!all) {
                    for (int i = 0; i < accounts.length; i++) {
                        if (held[i]) locks[i].unlock();
                    }
                    rndSleep();
                    continue;
                }

                // With all locks held, nothing else should interleave (atomic audit)
                int[] bals = new int[accounts.length];
                for (int i = 0; i < accounts.length; i++) {
                    bals[i] = accounts[i].getBalanceUnsafe();
                }
                int seenNow = globalTxnCounter.get();
                int sinceLast = seenNow - lastSeenTxn;
                lastSeenTxn = seenNow;

                String label = isTreasury ? "US TREASURY AUDIT" : "INTERNAL AUDIT";
                StringBuilder sb = new StringBuilder();
                sb.append(label).append(" | Txns since last: ").append(sinceLast);
                for (int i = 0; i < accounts.length; i++) {
                    sb.append(String.format(" | A%d Bal: $%d", accounts[i].id(), bals[i]));
                }
                Main.printTxn(name, sb.toString());
            } finally {
                for (int i = 0; i < accounts.length; i++) {
                    if (locks[i] != null && locks[i].isHeldByCurrentThread()) {
                        locks[i].unlock();
                    }
                }
            }

            rndSleep();
        }
    }
}
