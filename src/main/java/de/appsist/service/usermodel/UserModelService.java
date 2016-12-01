package de.appsist.service.usermodel;

import java.io.File;
import java.util.*;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Verticle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.JSONPObject;

import de.appsist.commons.event.UserOfflineEvent;
import de.appsist.commons.event.UserOnlineEvent;
import de.appsist.commons.misc.StatusSignalConfiguration;
import de.appsist.commons.misc.StatusSignalSender;
import de.appsist.commons.util.EventUtil;
import de.appsist.service.usermodel.connector.MongoDBConnector;
import de.appsist.service.usermodel.managers.UserManager;
import de.appsist.service.usermodel.addresses.Addresses;
import de.appsist.service.usermodel.model.User;
public class UserModelService
    extends Verticle
{
    private JsonObject config;
    private String basePath = "";

    public static final int masteredTreshhold = 1;
    private static final Map<String, String> sessionUserIds = new HashMap<String, String>();

    // logger object
    private static final Logger log = LoggerFactory.getLogger(UserModelService.class);

    // Eventbus
    private EventBus eb;

    // list of contentIds increasing user knowledge
    private final List<String> importantContentIds = new ArrayList<String>();
 // list of contentIds increasing user knowledge
    private final List<String> importantProcessIds = new ArrayList<String>();
    
    // initializing usermanager

    final UserManager um = UserManager.getInstance();

    public void start()
    {
        this.eb = vertx.eventBus();
        if (container.config() != null && container.config().size() > 0) {
            config = container.config();

        }
        else {
            config = getDefaultConfiguration();
        }

        this.basePath = config.getObject("webserver").getString("basePath");
     // set important content
        JsonArray configImportantContents = config.getArray("importantcontents", null);
        if (null != configImportantContents){
        	for (Object importantContent: configImportantContents.asArray()){
            	importantContentIds.add(importantContent.toString().replaceAll(" ", ""));
            }
        }
        
        JsonArray configImportantProcesses = config.getArray("importantprocesses", null);
        log.info(configImportantProcesses.encodePrettily());
        if (null != configImportantProcesses){
        	for (Object importantProcess: configImportantProcesses.asArray()){
            	importantProcessIds.add(importantProcess.toString());
            }
        }
        
        setupHandlers();
        setupHttpHandlers();
        // show user details
        vertx.setTimer(2000, new Handler<Long>()
        {

            @Override
            public void handle(Long arg0)
            {
                    log.debug("Initializing UserManager");
                // UserManager um = UserManager.getInstance();
                if (null == um.getEventBus()) {
                    um.setEventBus(eb);
                }
                um.setConfiguration(config);
                um.setImportantContentIds(importantContentIds);
                um.setImportantProcessIds(importantProcessIds);
                um.init();
            }

        });
        
        
            log.info("Usermodel Service started");
            
            JsonObject statusSignalObject = config.getObject("statusSignal");
            StatusSignalConfiguration statusSignalConfig;
            if (statusSignalObject != null) {
              statusSignalConfig = new StatusSignalConfiguration(statusSignalObject);
            } else {
              statusSignalConfig = new StatusSignalConfiguration();
            }

            StatusSignalSender statusSignalSender =
              new StatusSignalSender("usermodel-service", vertx, statusSignalConfig);
            statusSignalSender.start();

    }
    
    /**
     * Create a configuration which used if no configuration is passed to the module.
     * 
     * @return Configuration object.
     */
    private static JsonObject getDefaultConfiguration()
    {
        JsonObject defaultConfig = new JsonObject();
        JsonObject webserverConfig = new JsonObject();
        webserverConfig.putNumber("port", 8089);
        webserverConfig.putString("statics", "www");
        webserverConfig.putString("basePath", "/services/usermodel");
        defaultConfig.putObject("webserver", webserverConfig);
        defaultConfig.putBoolean("test", true);
        return defaultConfig;

    }

    private void setupHandlers()
    {
        Handler<Message<JsonObject>> usermodelServiceHandler = new Handler<Message<JsonObject>>(){

            @Override
            public void handle(final Message<JsonObject> message)
            {
                final JsonObject messageBody = message.body();
                //log.info(messageBody.encodePrettily());
                String userId = messageBody.getString("userId", null);
                String sessionId = messageBody.getString("sid", null);
                if (null == sessionId){
                	sessionId = messageBody.getString("sessionId",null);
                }
                if (null != sessionId && null == userId) {
                	userId = sessionUserIds.get(sessionId);
                }
                String token = messageBody.getString("token", "");
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                UserManager userManager = UserManager.getInstance();
                
                final User user = userManager.getUser(userId);
                if (null != user) {
                	
                    switch (message.address()) {
                        case Addresses.PROCESS_CONTENT_SEEN:
                            String contentId = messageBody.getString("contentId","bla");
                            log.info("user " + user.getId() + " has seen content: " + contentId);
                            user.contentViewed(contentId);
                            user.sendKnowledgeEstimationToKVD();
                            /*if (importantContentIds.contains(contentId)){
                            	user.sendKnowledgeEstimationToKVD();
                            }*/
                            break;
                        case Addresses.UM_MASTERS_PROCESS:
                            String processId = messageBody.getString("processId", "");
                            boolean processMastered = false;
                            if (!"".equals(processId)) {
                                // check if we already know that the user masters this process
                                if (user.getMasteredProcesses().contains(processId)) {
                                    message.body().putBoolean("mastered", true);
                                    message.reply(message.body());
                                    processMastered = true;
                                }
                                if (!processMastered) {
                                    JsonObject numberStatementQuery = new JsonObject();
                                    numberStatementQuery.putString("agent", userId)
                                            .putString("verb",
                                                    "http://adlnet.gov/expapi/verbs/completed")
                                            .putString("activityId", processId);
                                    final String finalProcessId = processId;
                                    Handler<Message<JsonObject>> numberOfStatementsHandler = new Handler<Message<JsonObject>>()
                                    {
                                        public void handle(Message<JsonObject> innerMessage)
                                        {
                                            // retrieve query from json object
                                            final JsonObject body = innerMessage.body();

                                            int amount = (int) body.getNumber("amount");
                                            
                                                log.debug("[UserModelService] " + user.getId()
                                                        + " completed activity " + finalProcessId
                                                        + " " + amount + " times");
                                            
                                            if (amount >= masteredTreshhold) {
                                                user.addMasteredProcess(finalProcessId, eb);
                                                message.body().putBoolean("mastered", true);
                                            }
                                            else {
                                                message.body().putBoolean("mastered", false);
                                            }
                                            message.reply(message.body());
                                        }
                                    };
                                    
                                     log.debug("[UserModelService] - sending request to LRS: "
                                                + numberStatementQuery.encodePrettily());
                                    

                                    eb.send("appsist:query:lrs#getNumberOfStatements",
                                            numberStatementQuery, numberOfStatementsHandler);

                                }
                            }
                            else {
                                messageBody.putBoolean("mastered", false);
                                message.reply(messageBody);
                            }
                            break;
                        case Addresses.REMOVE_MASTERED_PROCESSES:
                            // expects array processList

                            JsonArray processList = new JsonArray(
                                    messageBody.getString("processList"));
                            Iterator<Object> processListIterator = processList.iterator();
                            Set<String> remainingProcesses = new HashSet<String>();
                            while (processListIterator.hasNext()) {
                                String processListItem = processListIterator.next().toString();
                                if (!user.getMasteredProcesses().contains(processListItem)) {
                                    remainingProcesses.add(processListItem);
                                }
                            }
                            try {
                                messageBody.putString("remainingProcesses",
                                        ow.writeValueAsString(remainingProcesses));
                            }
                            catch (JsonProcessingException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                            message.reply(messageBody);
                            break;
                        case Addresses.USER_GET_INFORMATION:
                            // attach user information to calling message and return
                            JsonObject result = new JsonObject();
                            log.debug("getUserInformation" + user);
                            result.putString("currentWorkstate", user.getCurrentWorkstate());
                            try{
                                result.putString("workplaceGroups",
                                        ow.writeValueAsString(user.getWorkplaceGroups()));
                                result.putString("developmentGoals",
                                        ow.writeValueAsString(user.getDevelopmentGoals()));
                                result.putString("developmentGoalsObject",
                                        ow.writeValueAsString(user.getDevelopmentGoalsObject()));
                                result.putString("allowedMeasures",
                                        ow.writeValueAsString(user.getMeasureClearance()));
                                result.putString("allowedMeasuresWithAssistance",
                                        ow.writeValueAsString(
                                                user.getMeasureClearancesWithAssistance()));
                                result.putString("employeeType", user.getEmployeeType());
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                            messageBody.putObject("userInformation", result);

                            message.reply(messageBody);
                            break;
                    }
                }
                else {
                    message.reply(new JsonObject());
                }

            }

        };

        

        this.eb.registerHandler(Addresses.PROCESS_CONTENT_SEEN, usermodelServiceHandler);
        this.eb.registerHandler(Addresses.UM_MASTERS_PROCESS,
                usermodelServiceHandler);
        this.eb.registerHandler(Addresses.REMOVE_MASTERED_PROCESSES,
                usermodelServiceHandler);
        this.eb.registerHandler(Addresses.USER_GET_INFORMATION,
                usermodelServiceHandler);
        
        Handler<Message<JsonObject>> userOnlineEventHandler = new Handler<Message<JsonObject>>()
        {
            public void handle(Message<JsonObject> jsonMessage)
            {
                UserOnlineEvent userOnlineEvent = EventUtil.parseEvent(jsonMessage.body().toMap(),
                        UserOnlineEvent.class);
                processUserOnlineEvent(userOnlineEvent);
            }
        };

        this.eb.registerHandler(Addresses.USER_ONLINE_EVENT, userOnlineEventHandler);
        
        Handler<Message<JsonObject>> userOfflineHandler = new Handler<Message<JsonObject>>(){

            @Override
            public void handle(final Message<JsonObject> jsonMessage)
            {
            	UserOfflineEvent userOfflineEvent = EventUtil.parseEvent(jsonMessage.body().toMap(), UserOfflineEvent.class);
                processUserOfflineEvent(userOfflineEvent);
             }
        };
        this.eb.registerHandler(Addresses.USER_OFFLINE_EVENT, userOfflineHandler);
    }
    


    private void setupHttpHandlers()
    {
        BasePathRouteMatcher rm = new BasePathRouteMatcher(this.basePath);
        // list all users
        rm.get("/users", new Handler<HttpServerRequest>()
        {
            public void handle(HttpServerRequest req)
            {
                // UserManager um = UserManager.getInstance();
                Map<String, User> userMap = um.getUserList();
                if (null == um.getEventBus()) {
                    um.setEventBus(eb);
                }
                if (userMap.isEmpty()) {
                    um.init();
                        log.debug(
                            "UserModelService - UserManager eventbus Object: " + um.getEventBus());

                    userMap = um.getUserList();
                }
                String ulString = "";
                /*for (int i : userMap.keySet()) {
                    User user = userMap.get(i);

                    if (null != user) {
                        ulString += "ID: " + user.getId() + ": " + user.getFirstname() + " "
                                + user.getLastname() + " | " + user.getEmployer() + "<br/>";
                    }
                }*/
                ObjectMapper mapper = new ObjectMapper();
                try {
                    ulString = mapper.writeValueAsString(userMap);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                req.response().end("<html><body>" + ulString + "</body></html>");
            }
        });


        // list all performed actions
        rm.get("/users/:uid/clearances", new Handler<HttpServerRequest>()
        {
            public void handle(HttpServerRequest req)
            {
                String userId = req.params().get("uid");
                // UserManager um = UserManager.getInstance();
                User user = um.getUser(userId);
                if (null != user) {
                    req.response().end(
                            "<html><body>" + user.listMeasureClearances() + "</body></html>");
                }
                else {
                    req.response().end("");
                }
            }
        });

        // increase number of measure performed
        rm.post("/users/:uid/measures/:mid", new Handler<HttpServerRequest>()
        {
            public void handle(HttpServerRequest req)
            {
                String userId = req.params().get("uid");
                String mId = req.params().get("mid");
                // UserManager um = UserManager.getInstance();
                User user = um.getUser(userId);
                if (null != user) {
                    user.measurePerformed(mId);
                    req.response().end(
                            "<html><body> Measure " + mId + " was performed</body></html>");
                    log.info(user.getId()+" completed process "+mId);
                    
                }
                else {
                    req.response().end("<html><body></body></html>");
                }
            }
        });


        // list all performed measures
        rm.get("/users/:uid/contents", new Handler<HttpServerRequest>()
        {
            public void handle(HttpServerRequest req)
            {
                String userId = req.params().get("uid");
                // UserManager um = UserManager.getInstance();
                User user = um.getUser(userId);
                if (null != user) {
                    req.response().end("<html><body>" + user.listContentsSeen() + "</body></html>");
                }
                else {
                    req.response().end("");
                }
            }
        });

        // show user details
        rm.get("/users/:uid", new Handler<HttpServerRequest>()
        {
            public void handle(HttpServerRequest req)
            {
                String userId = req.params().get("uid");
                // UserManager um = UserManager.getInstance();
                User user = um.getUser(userId);
                if (null != user) {
                    /*
                     * String cvStr = user.listContentsSeen(); req.response().end(
                     * "<html><body>" + "ID: " + userId + ": " + user.getFirstname() + " " +
                     * user.getLastname() + " | " + user.getEmployer() + "<br/>" +
                     * user.listMeasuresPerformed() + user.listActivitiesPerformed() +
                     * user.listActionsPerformed() + user.listAtomicStepsPerformed() + cvStr +
                     * "</body></html>");
                     */
                    try {
                        req.response().end(new ObjectMapper().writeValueAsString(user));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    req.response().end("{}");
                }
            }
        });

        // list all performed measures


        vertx.createHttpServer().requestHandler(rm)
                .listen(config.getObject("webserver").getInteger("port"));
    }
    
    public UserModelService()
    {
        //
    }

    // ==================================
    //
    // User on/offline handlers
    //
    // ==================================

    // after user logs in -> store user and session
    private void processUserOnlineEvent(UserOnlineEvent userOnlineEvent)
    {
        if (null != userOnlineEvent) { 
            String user = userOnlineEvent.getUserId();
            String session = userOnlineEvent.getSessionId();

            if (!sessionUserIds.containsKey(session)) {
                sessionUserIds.put(session, user);
                //log.info("Adding session and userId:"+ session+" | "+user);
            }
            else {
                if (!user.equals(sessionUserIds.get(session))) {
                    // most probably user logout was not received
                    // replace with new user Id
                    sessionUserIds.remove(session);
                    sessionUserIds.put(session, user);
                  //  log.info("Updating  session and userId:"+ session+" | "+user);
                }
            }
            if (user.equals("dana.exter@appsist.de")){
            	um.getUser(user).presetKnowledgeEstimation();
            }
            um.getUser(user).sendKnowledgeEstimationToKVD();
        }
        else {
            log.error("invalid user online event received");
        }
    }
    
    private void processUserOfflineEvent(UserOfflineEvent userOfflineEvent)
    {
        if (null != userOfflineEvent) {
            String user = userOfflineEvent.getUserId();
            String session = userOfflineEvent.getSessionId();
            	
            sessionUserIds.remove(session);
            
            um.getUser(user).resetKnowledgeEstimation();
            //um.getUser(user).persistUser(config.getString("mongoPersistorAddress"), eb);
        }
        else {
            log.error("invalid user online event received");
        }
    }


}
