package xyz.arwx.challenger.slack;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.StravaConfig;
import xyz.arwx.challenger.utils.JsonMapper;

import java.text.MessageFormat;

import static xyz.arwx.challenger.slack.StravaVerticle.StravaRequest.StravaRequestType.Club;

/**
 * Created by macobas on 16/07/17.
 */
public class StravaVerticle extends AbstractVerticle
{
    public Logger logger = LoggerFactory.getLogger(StravaVerticle.class);
    public static final String InboundAddress = StravaVerticle.class.getName();
    public static final String DefaultClub = "~~DEFAULT~~";
    private  StravaConfig config;
    private HttpClient   client;
    private JsonObject cachedClubResults;

    public static void deploy(Vertx vx, StravaConfig strava)
    {
        vx.deployVerticle(StravaVerticle.class.getName(), new DeploymentOptions().setConfig(JsonMapper.objectToJsonObject(strava)));
    }

    public static class StravaRequest
    {
        public enum StravaRequestType
        {
            Club,
            Athlete
        }

        public StravaRequestType type;
        public String thingId;
    }

    public static StravaRequest DefaultRequest()
    {
        StravaRequest sr = new StravaRequest();
        sr.type = Club;
        sr.thingId = DefaultClub;
        return sr;
    }


    public void start()
    {
        config = JsonMapper.objectFromJsonObject(config(), StravaConfig.class);
        client = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        ebListen();
        /*
        if(config.pollForActivity)
            periodicPoll();
            */
    }

    public void ebListen()
    {
        vertx.eventBus().consumer(InboundAddress, m -> {
            JsonObject js = (JsonObject)m.body();
            StravaRequest r = JsonMapper.objectFromJsonObject(js, StravaRequest.class);
            if(r.thingId.equals(DefaultClub))
                r.thingId = config.defaultClubId;
            fetchAndReply(r, m);
        });
    }

    public void fetchAndReply(StravaRequest r, Message<Object> msg)
    {
        switch(r.type)
        {
            case Athlete:
                getAthleteAndStats(r.thingId, msg);
                break;
            case Club:
                getClubAndLeaderboad(r.thingId, msg);
                break;

        }
    }

    public static String S(String url, String token, String... allArgs)
    {
        String formatted = MessageFormat.format(url, allArgs);
        return formatted + "?access_token=" + token;
    }

    public void getAthleteAndStats(String athid, Message<Object> msg)
    {
        client.getAbs(S("https://www.strava.com/api/v3/athletes/{0}", config.accessToken, athid), res -> {
            if(res.statusCode() != 200)
                msg.fail(-1, "Unknown athlete");
            else
            {
                res.bodyHandler(buf -> {
                  JsonObject reply = new JsonObject().put("athleteInfo", buf.toJsonObject());
                  client.getAbs(S("http://www.strava.com/api/v3/athletes/{0}/stats", config.accessToken, athid), ires -> {
                     if(res.statusCode() == 200)
                     {
                         ires.bodyHandler(statsBuf -> {
                             reply.put("athleteStats", statsBuf.toJsonObject());
                             msg.reply(reply);
                         });
                     } else msg.reply(reply);
                  }).end();
                });
            }
        }).end();
    }

    public void getClubAndLeaderboad(String clubId, Message<Object> msg)
    {
        if(clubId.equals(config.defaultClubId) && config.pollForActivity && cachedClubResults != null)
            msg.reply(cachedClubResults);
        else
        {
            client.getAbs(S("https://www.strava.com/api/v3/clubs/{0}", config.accessToken, clubId), res->
            {
                if(res.statusCode() != 200)
                {
                    logger.error("Failed to retrieve club info, status {}, message {}", res.statusCode(), res.statusMessage());
                    msg.fail(-1, "Unknown club");
                }

                res.bodyHandler(clubInfoBuf -> {
                    JsonObject reply = new JsonObject().put("clubInfo", clubInfoBuf.toJsonObject());
                    client.getAbs("https://www.strava.com/clubs/" + clubId + "/leaderboard", ires -> {
                        if(ires.statusCode() == 200)
                        {
                            ires.bodyHandler(clublbbuf -> {
                                reply.put("leaderboard", clublbbuf.toJsonObject());
                                msg.reply(reply);
                            });
                        } else msg.reply(reply);
                    }).putHeader("Accept", "text/javascript").end();
                });
            }).end();
        }
    }
}
