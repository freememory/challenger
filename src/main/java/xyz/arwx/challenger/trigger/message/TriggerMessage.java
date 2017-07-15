package xyz.arwx.challenger.trigger.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;

/**
 * Created by macobas on 26/05/17.
 */
public class TriggerMessage
{
    public enum Source {
        IRC, SLACK;

        public String ebAddress()
        {
            if(this == IRC)
                return IrcVerticle.InboundAddress;
            return "";
        }
    }

    public String trigger;
    public String message;
    public String from;
    public String channel;
    public Source source;

    @JsonIgnore
    public boolean isPrivateMessage()
    {
        return channel == null || channel.length() == 0;
    }

    public String returnTarget()
    {
        return isPrivateMessage() ? from : channel;
    }

    public SendableMessage<MessagePayload> constructResponse(MessagePayload p)
    {
        SendableMessage<MessagePayload> s = new SendableMessage<>();
        s.event = Events.Privmsg;
        s.source = this.source;
        s.target = returnTarget();
        s.message = p;

        return s;
    }

    public static SendableMessage<MessagePayload> constructResponse(String to, MessagePayload p, Source sc)
    {
        SendableMessage<MessagePayload> s = new SendableMessage<>();
        s.event = Events.Privmsg;
        s.source = sc;
        s.target = to;
        s.message = p;

        return s;
    }
}
