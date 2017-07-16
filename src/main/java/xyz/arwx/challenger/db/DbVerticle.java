package xyz.arwx.challenger.db;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import xyz.arwx.challenger.config.DbConfig;
import xyz.arwx.challenger.utils.JsonMapper;

/**
 * Created by macobas on 28/05/17.
 */
public class DbVerticle extends AbstractVerticle
{
    private JDBCClient    client;
    private SQLConnection connection;
    private DbConfig      dbConfig;
    private static final Logger logger          = LoggerFactory.getLogger(DbVerticle.class);
    public static String OutboundAddress = DbVerticle.class.getName();
    public static String InboundAddress  = DbVerticle.class.getName() + ".query";

    public void start()
    {
        dbConfig = JsonMapper.objectFromJsonObject(config(), DbConfig.class);

        client = JDBCClient.createShared(
                vertx, new JsonObject()
                        .put("driver_class", dbConfig.dbDriver)
                        .put("url", dbConfig.dbUrl)
        );

        client.getConnection(res -> {
            if (res.succeeded())
            {
                connection = res.result();
                vertx.eventBus().publish(OutboundAddress, new JsonObject().put("dbReady", true));
            }
            else
            {
                vertx.eventBus().publish(OutboundAddress, new JsonObject().put("dbReady", false).put("exception", res.cause().getMessage()));
            }
        });

        setupEventHandlers();
    }

    private void setupEventHandlers()
    {
        vertx.eventBus().consumer(InboundAddress, this::handleQuery);
    }

    private void handleQuery(Message<JsonObject> queryMsg)
    {
        JsonObject queryParms = queryMsg.body();
        if (queryParms.getString("queryType").equals("UPDATE"))
        {
            connection.updateWithParams(queryParms.getString("query"), queryParms.getJsonArray("params"), res -> {
                if (res.succeeded())
                    queryMsg.reply(new JsonObject().put("succeeded", true));
                else
                {
                    queryMsg.reply(new JsonObject().put("succeeded", false).put("error", res.cause().getMessage()));
                    logger.error("Failed to update!", res.cause());
                }
            });
        }
        else
        {
            connection.queryWithParams(queryParms.getString("query"), queryParms.getJsonArray("params"), res -> {
                if (res.succeeded())
                {
                    ResultSet rs = res.result();
                    queryMsg.reply(new JsonObject().put("succeeded", true).put("result", new JsonArray(rs.getRows())));
                }
                else
                {
                    queryMsg.reply(new JsonObject().put("succeeded", false).put("error", res.cause().getMessage()));
                    logger.error("Failed to QUERY!", res.cause());
                }
            });
        }
    }

    public static void deploy(Vertx vx, DbConfig db)
    {
        DeploymentOptions dbOpts = new DeploymentOptions().setConfig(JsonMapper.objectToJsonObject(db));
        vx.deployVerticle(DbVerticle.class.getName(), dbOpts);
    }
}
