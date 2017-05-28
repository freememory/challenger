package xyz.arwx.challenger.irc.trigger;

/**
 * Created by macobas on 26/05/17.
 */
public class TriggerMessage {
    public String trigger;
    public String message;
    public String from;
    public String channel;

    public boolean isPrivateMessage()
    {
        return channel == null || channel.length() == 0;
    }

    public String returnTarget()
    {
        return isPrivateMessage() ? from : channel;
    }
}
