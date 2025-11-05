package bankingsim;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class DepositorAgent extends AgentBase {
    private final BankAccount[] accounts;
    private final AtomicInteger txnCounter;
    private final TransactionLogger logger;
    private final int minAmt, maxAmt;

    public DepositorAgent(String name, BankAccount[] accounts, AtomicInteger txnCounter,
                          TransactionLogger logger, int minAmt, int maxAmt,
                          int maxSleepMs, long seed) {
        super(name, maxSleepMs, seed);
        this.accounts = accounts;
        this.txnCounter = txnCounter;
        this.logger = logger;
        this.minAmt = minAmt; this.maxAmt = maxAmt;
    }

    @Override public void run() {
        while (true) {
            BankAccount acct = accounts[rng.nextInt(accounts.length)];
            int amount = rng.nextInt(maxAmt - minAmt + 1) + minAmt;

            ReentrantLock lock = acct.lock();
            lock.lock();
            try {
                int oldBal = acct.getBalanceUnsafe();
                int newBal = oldBal + amount;
                acct.setBalanceUnsafe(newBal);

                int txn = txnCounter.getAndIncrement();
                Main.printTxn(name, String.format(
                        "Deposit $%d into A%d | Bal: $%d → $%d | Txn #%d",
                        amount, acct.id(), oldBal, newBal, txn));

                if (amount > Main.FLAG_DEP_OVER) {
                    Main.printFlag(name, String.format(
                            "Deposit > $%d on A%d (amount=$%d, Txn #%d)",
                            Main.FLAG_DEP_OVER, acct.id(), amount, txn));
                    logger.logFlag(name, "DEPOSIT", txn, acct.id(), amount);
                }

                // Wake all blocked withdrawals on this account
                acct.fundsCondition().signalAll();
            } finally {
                lock.unlock();
            }
            rndSleep();
        }
    }
}
