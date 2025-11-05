package bankingsim;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class TransferAgent extends AgentBase {
    private final BankAccount a1, a2;
    private final AtomicInteger txnCounter;
    private final TransactionLogger logger; // not used for transfers; spec only flags D/W
    private final int minAmt, maxAmt;

    public TransferAgent(String name, BankAccount a1, BankAccount a2,
                         AtomicInteger txnCounter, TransactionLogger logger,
                         int minAmt, int maxAmt, int maxSleepMs, long seed) {
        super(name, maxSleepMs, seed);
        this.a1 = a1; this.a2 = a2;
        this.txnCounter = txnCounter;
        this.logger = logger;
        this.minAmt = minAmt; this.maxAmt = maxAmt;
    }

    @Override public void run() {
        while (true) {
            // Choose source/dest randomly (must be distinct)
            boolean srcIsA1 = rng.nextBoolean();
            BankAccount src = srcIsA1 ? a1 : a2;
            BankAccount dst = srcIsA1 ? a2 : a1;

            int amount = rng.nextInt(maxAmt - minAmt + 1) + minAmt;

            ReentrantLock lSrc = src.lock();
            ReentrantLock lDst = dst.lock();

            // Non-blocking semantics: try to take both locks; if fail, release & retry later
            if (!lSrc.tryLock()) { rndSleep(); continue; }
            if (!lDst.tryLock()) { lSrc.unlock(); rndSleep(); continue; }

            try {
                if (src.getBalanceUnsafe() < amount) {
                    Main.printTxn(name, String.format(
                            "Transfer ABORT (insufficient) A%d→A%d amount $%d | SrcBal $%d",
                            src.id(), dst.id(), amount, src.getBalanceUnsafe()));
                } else {
                    int srcOld = src.getBalanceUnsafe();
                    int dstOld = dst.getBalanceUnsafe();
                    src.setBalanceUnsafe(srcOld - amount);
                    dst.setBalanceUnsafe(dstOld + amount);

                    int txn = txnCounter.getAndIncrement();
                    Main.printTxn(name, String.format(
                            "Transfer $%d A%d→A%d | A%d: $%d→$%d, A%d: $%d→$%d | Txn #%d",
                            amount, src.id(), dst.id(),
                            src.id(), srcOld, src.getBalanceUnsafe(),
                            dst.id(), dstOld, dst.getBalanceUnsafe(),
                            txn));
                }
            } finally {
                lDst.unlock();
                lSrc.unlock();
            }
            rndSleep();
        }
    }
}
