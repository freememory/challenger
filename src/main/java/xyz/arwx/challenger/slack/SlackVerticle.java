package xyz.arwx.challenger.slack;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.arwx.challenger.config.SlackConfig;
import xyz.arwx.challenger.utils.JsonMapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by macobas on 16/07/17.
 */
public class SlackVerticle extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(SlackVerticle.class);
    private SlackConfig config;
    private HttpServer  server;
    private Router router;

    public void start()
    {
        logger.info("Deploying Slack Verticle");
        config = JsonMapper.objectFromJsonObject(config(), SlackConfig.class);
        deployHttp();
    }

    private JsonObject decodeRequest(String request)
    {
        JsonObject ret = new JsonObject();

        Arrays.stream(request.split("&")).forEach(param -> {
            String[] split = param.split("=");
            if(split.length != 2)
                return;

            try
            {
                ret.put(split[0], URLDecoder.decode(split[1], StandardCharsets.UTF_8.toString()));
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        });

        return ret;
    }

    private void deployHttp()
    {
        server = vertx.createHttpServer();
        router = Router.router(vertx);
        router.route("/slack").handler(BodyHandler.create());
        router.post("/slack").handler(ctx -> {
            String body = ctx.getBodyAsString();
            JsonObject r = decodeRequest(body);
            if(!verifyAndSend(r))
                ctx.response().setStatusCode(500).end("Bad token or unknown request?");
            else
            {
                logger.info("Request received: {}", r.encodePrettily());
                ctx.response().setStatusCode(200).end(new JsonObject().put("response_type", "in_channel").encode());
            }
        });

        server.requestHandler(router::accept).listen(config.restPort);
    }

    private boolean verifyAndSend(JsonObject event)
    {
        if(!event.getString("token").equals(config.verificationToken))
            return false;
        if(event.getString("command").equals("/strava"))
        {
            vertx.eventBus().send(StravaVerticle.InboundAddress, JsonMapper.objectToJsonObject(StravaVerticle.DefaultRequest()),
                    reply -> {
                       if(reply.succeeded())
                       {
                           JsonObject data = (JsonObject)reply.result().body();
                           logger.info("Sending response to Slack: {}", data.encodePrettily());
                           vertx.createHttpClient().postAbs(event.getString("response_url"), resp -> {
                               logger.info("Posting to slack, got resp {}", resp.statusCode());
                           }).end(new JsonObject().put("response_type", "in_channel")
                                   .put("text", "Here's a whole bunch of Strava shit because I'm too lazy to format it right now")
                                   .put("attachments", new JsonArray().add(new JsonObject().put("text", data.encodePrettily()))).encode());
                       }
                    });
        }
        return true;
    }

    public static void deploy(Vertx vx, SlackConfig sc)
    {
        vx.deployVerticle(SlackVerticle.class.getName(), new DeploymentOptions().setConfig(JsonMapper.objectToJsonObject(sc)));
    }
}
