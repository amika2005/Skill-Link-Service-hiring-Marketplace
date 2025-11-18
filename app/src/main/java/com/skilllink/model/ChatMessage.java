package com.skilllink.model;

public class ChatMessage {
    private long messageId;
    private long bookingId;
    private long senderId;
    private String messageType; // text, image, location, voice
    private String content;
    private String readStatus; // sent, delivered, read

    // Constructors
    public ChatMessage() {
    }

    public ChatMessage(long bookingId, long senderId, String messageType, 
                       String content, String readStatus) {
        this.bookingId = bookingId;
        this.senderId = senderId;
        this.messageType = messageType;
        this.content = content;
        this.readStatus = readStatus;
    }

    // Getters and Setters
    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }
}