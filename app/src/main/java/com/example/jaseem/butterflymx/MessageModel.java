package com.example.jaseem.butterflymx;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Jaseem on 4/12/18.
 */

public class MessageModel {
    @SerializedName("from")
    public String messageSender;
    @SerializedName("body")
    public String messageBody;

    public long timestamp;

    public MessageModel() {
    }

    public String getMessageSender() { return messageSender; }

    public void setMessageSender(String messageSender) {
        this.messageSender = messageSender;
    }

    public String getMessageText() {
        return messageBody;
    }

    public void setMessageText(String messageText) {
        this.messageBody = messageText;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
