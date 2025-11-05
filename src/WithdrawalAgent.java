package bankingsim;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class WithdrawalAgent extends AgentBase {
    private final BankAccount[] accounts;
    private final AtomicInteger txnCounter;
    private final TransactionLogger logger;
    private final int minAmt, maxAmt;

    public WithdrawalAgent(String name, BankAccount[] accounts, AtomicInteger txnCounter,
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
                while (acct.getBalanceUnsafe() < amount) {
                    Main.printTxn(name, String.format(
                            "BLOCKED on A%d: need $%d, have $%d",
                            acct.id(), amount, acct.getBalanceUnsafe()));
                    try {
                        acct.fundsCondition().await(); // wait to be signaled by deposit
                    } catch (InterruptedException ignored) {
                        // loop re-check
                    }
                }

                int oldBal = acct.getBalanceUnsafe();
                int newBal = oldBal - amount;
                acct.setBalanceUnsafe(newBal);

                int txn = txnCounter.getAndIncrement();
                Main.printTxn(name, String.format(
                        "Withdraw $%d from A%d | Bal: $%d → $%d | Txn #%d",
                        amount, acct.id(), oldBal, newBal, txn));

                if (amount > Main.FLAG_WDR_OVER) {
                    Main.printFlag(name, String.format(
                            "Withdrawal > $%d on A%d (amount=$%d, Txn #%d)",
                            Main.FLAG_WDR_OVER, acct.id(), amount, txn));
                    logger.logFlag(name, "WITHDRAWAL", txn, acct.id(), amount);
                }
            } finally {
                lock.unlock();
            }
            rndSleep();
        }
    }
}
