package bankingsim;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Name:Sadman Nahin
  Course: CNT 4714
  Assignment title: Project 2
  Due Date: September 26, 2025
*/

public class Main {

    // ---- Sleep tuning: withdrawals fastest; auditors slowest (spec) ----
    static final int MAX_SLEEP_WITHDRAW_MS = 60;    // fastest
    static final int MAX_SLEEP_DEPOSIT_MS  = 150;
    static final int MAX_SLEEP_TRANSFER_MS = 250;
    static final int MAX_SLEEP_INT_AUD_MS  = 1200;
    static final int MAX_SLEEP_TRE_AUD_MS  = 1500;  // slowest

    // Amount ranges (whole dollars, per spec)
    static final int MIN_DEP = 1,  MAX_DEP = 600;
    static final int MIN_WDR = 1,  MAX_WDR = 99;

    // CTR-style thresholds to flag
    static final int FLAG_DEP_OVER = 450;
    static final int FLAG_WDR_OVER = 90;

    public static void main(String[] args) throws Exception {
        // Transaction number (D/W/T only increment this)
        AtomicInteger txnCounter = new AtomicInteger(1);

        // Two accounts, start at $0
        BankAccount acc1 = new BankAccount(1);
        BankAccount acc2 = new BankAccount(2);
        BankAccount[] accounts = new BankAccount[]{acc1, acc2};

        // CSV logger for flagged transactions
        TransactionLogger logger = new TransactionLogger("transactions.csv");

        // Fixed thread pool with exactly 19 agents (5D + 10W + 2T + 1IA + 1TA)
        ExecutorService exec = Executors.newFixedThreadPool(19);

        Random seeder = new Random();

        // 5 Depositors
        for (int i = 1; i <= 5; i++) {
            exec.submit(new DepositorAgent(
                    "DT" + i, accounts, txnCounter, logger,
                    MIN_DEP, MAX_DEP, MAX_SLEEP_DEPOSIT_MS, seeder.nextLong()
            ));
        }

        // 10 Withdrawals
        for (int i = 1; i <= 10; i++) {
            exec.submit(new WithdrawalAgent(
                    "WT" + i, accounts, txnCounter, logger,
                    MIN_WDR, MAX_WDR, MAX_SLEEP_WITHDRAW_MS, seeder.nextLong()
            ));
        }

        // 2 Transfers (non-blocking; uses tryLock on both accts)
        for (int i = 1; i <= 2; i++) {
            exec.submit(new TransferAgent(
                    "TT" + i, acc1, acc2, txnCounter, logger,
                    MIN_WDR, MAX_WDR, MAX_SLEEP_TRANSFER_MS, seeder.nextLong()
            ));
        }

        // 1 Internal Audit
        exec.submit(new AuditAgent(
                "IA1", accounts, txnCounter, false, MAX_SLEEP_INT_AUD_MS, seeder.nextLong()
        ));

        // 1 Treasury Audit
        exec.submit(new AuditAgent(
                "TA1", accounts, txnCounter, true, MAX_SLEEP_TRE_AUD_MS, seeder.nextLong()
        ));

        // Graceful shutdown hook for CSV on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { logger.close(); } catch (Exception ignored) {}
            exec.shutdownNow();
        }));

        // Run forever (per spec)
        Thread.currentThread().join();
    }

    // Console helpers
    static void printTxn(String agent, String msg) {
        System.out.printf("[%s] %s%n", agent, msg);
    }

    static void printFlag(String agent, String msg) {
        System.out.printf("[%s] ⚠ FLAGGED: %s%n", agent, msg);
    }
}
