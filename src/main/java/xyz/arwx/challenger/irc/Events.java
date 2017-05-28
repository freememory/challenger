package xyz.arwx.challenger.irc;

/**
 * Created by macobas on 23/05/17.
 */
public final class Events
{
    private Events()
    {
    }

    ;

    public static final String Disconnect = "DISCONNECT";
    public static final String Connect    = "CONNECT";
    public static final String Join       = "JOIN";
    public static final String Privmsg    = "PRIVMSG";
    public static final String Trigger    = "TRIGGER";
}
