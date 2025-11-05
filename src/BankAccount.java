package bankingsim;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BankAccount {
    private final int id;
    private final ReentrantLock lock = new ReentrantLock();   // no fairness policy (spec)
    private final Condition fundsAvailable = lock.newCondition();
    private int balance = 0;                                   // whole dollars only

    public BankAccount(int id) { this.id = id; }

    public int id() { return id; }
    public ReentrantLock lock() { return lock; }
    public Condition fundsCondition() { return fundsAvailable; }

    // Only call these while holding lock()
    public int getBalanceUnsafe() { return balance; }
    public void setBalanceUnsafe(int newBal) { balance = newBal; }
}
