package ve.nottabaker.nottpay.transaction;

import java.util.UUID;

/**
 * Represents a single payment transaction between two players.
 */
public class Transaction {

    private final UUID sender;
    private final UUID receiver;
    private final String senderName;
    private final String receiverName;
    private final String currency;
    private final double amount;
    private final long timestamp;

    public Transaction(UUID sender, UUID receiver, String senderName, String receiverName,
                       String currency, double amount, long timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.currency = currency;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getCurrency() {
        return currency;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
