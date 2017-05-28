package xyz.arwx.challenger;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import xyz.arwx.challenger.irc.Events;
import xyz.arwx.challenger.irc.IrcVerticle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by macobas on 25/05/17.
 */
public class Challenger
{
    // THE GOD OBJECT
    public static Vertx vertx;

    public static void main(String[] args) throws IOException
    {
        init();
        JsonObject config = getConfig("challengeBot.json");

        // deploy the verticles
        DeploymentOptions dOpts = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle(IrcVerticle.class.getName(), dOpts, res -> {
            if (res.failed())
                System.exit(-1);
            else
            {
                vertx.eventBus().send(IrcVerticle.InboundAddress, new JsonObject().put("event", Events.Connect));
            }
        });
    }

    /**
     * Does any necessery preparations
     */
    private static void init()
    {
        vertx = Vertx.vertx();
    }

    /**
     * Reades the resource as a Json file
     *
     * @return
     * @throws IOException
     */
    private static JsonObject getConfig(String rez) throws IOException
    {
        InputStream is = Challenger.class.getClassLoader().getResourceAsStream(rez);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line);
        JsonObject config = new JsonObject(sb.toString());
        return config;
    }
}
