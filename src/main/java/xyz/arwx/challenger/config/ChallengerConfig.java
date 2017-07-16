package xyz.arwx.challenger.config;

import io.vertx.core.Vertx;
import xyz.arwx.challenger.slack.SlackVerticle;
import xyz.arwx.challenger.db.DbVerticle;
import xyz.arwx.challenger.irc.IrcVerticle;
import xyz.arwx.challenger.mail.MailVerticle;
import xyz.arwx.challenger.slack.StravaVerticle;

/**
 * Created by macobas on 16/07/17.
 */
public class ChallengerConfig
{
    public IrcConfig irc;
    public MailConfig mail;
    public DbConfig db;
    public TriggerConfig triggers;
    public SlackConfig slack;
    public StravaConfig strava;

    public void deployVerticles(Vertx vx)
    {
        if(irc != null && triggers != null)
            IrcVerticle.deploy(vx, irc, triggers);
        if(db != null)
            DbVerticle.deploy(vx, db);
        if(mail != null)
            MailVerticle.deploy(vx, mail);
        if(slack != null)
            SlackVerticle.deploy(vx, slack);
        if(strava != null)
            StravaVerticle.deploy(vx, strava);
    }
}
