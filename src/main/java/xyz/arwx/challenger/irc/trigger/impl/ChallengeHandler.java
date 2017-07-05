package xyz.arwx.challenger.irc.trigger.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.HandlerConfig;
import xyz.arwx.challenger.config.IrcConfig;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.irc.trigger.TriggerHandler;
import xyz.arwx.challenger.irc.trigger.TriggerMessage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by macobas on 01/06/17.
 */
public class ChallengeHandler extends TriggerHandler
{
    private static final Logger logger = LoggerFactory.getLogger(ChallengeHandler.class);
    Pattern p = null;
    public ChallengeHandler(Vertx v, String name, IrcConfig.Trigger trigger, HandlerConfig hc)
    {
        super(v, name, trigger, hc);
        p = Pattern.compile((String)hc.config.get("regex"));
    }

    @Override
    public void handleTrigger(TriggerMessage trigger)
    {
        Matcher m = p.matcher(trigger.message);
        if(!m.matches())
            return;

        if(m.groupCount() != 3)
            return;

        if(trigger.isPrivateMessage())
            return;

        String nick = m.group(1);
        String date = m.group(2);
        String challenge = m.group(3);

        LocalDate ld = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);

        v.eventBus().send(IrcVerticle.InboundAddress, new JsonObject().put("event", Events.Who).put("channel", trigger.channel), reply -> {
            if(reply.failed())
                return;

            JsonArray users = (JsonArray) reply.result().body();
            if(users.contains(nick))
            {
                pendChallenge(trigger.from, nick, date, challenge, trigger.channel);
            }
        });
    }

    private void pendChallenge(String from, String nick, String date, String challenge, String channel)
    {

    }
}
