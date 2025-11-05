package bankingsim;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionLogger implements Closeable, Flushable {
    private final PrintWriter out;
    private final ReentrantLock lock = new ReentrantLock();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS");

    public TransactionLogger(String path) throws IOException {
        File f = new File(path);
        boolean append = f.exists();
        this.out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
        if (!append) {
            out.println("timestamp,txnNumber,agent,type,account,amount");
            out.flush();
        }
    }

    public void logFlag(String agent, String type, int txnNumber, int accountId, int amount) {
        String ts = ZonedDateTime.now().format(fmt);
        lock.lock();
        try {
            out.printf("%s,%d,%s,%s,%d,%d%n", ts, txnNumber, agent, type, accountId, amount);
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override public void flush() { out.flush(); }
    @Override public void close() { out.flush(); out.close(); }
}
