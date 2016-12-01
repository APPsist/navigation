package de.appsist.service.usermodel.managers;

import java.util.*;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import de.appsist.service.usermodel.connector.MongoDBConnector;
import de.appsist.service.usermodel.model.EmployeeDevelopmentGoals;
import de.appsist.service.usermodel.model.User;
public class UserManager
{
    private static UserManager _instance = new UserManager();
    private JsonObject config;
    private Map<String, User> userList;
    private int nextUserId = 1;
    private EventBus eb = null;
    private List<String> loggedInUsers = null;
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);
    private List<String> itemList;
    private List<String> processItemList;

    public static UserManager getInstance()
    {
        return _instance;
    }

    private UserManager()
    {
        userList = new HashMap<String, User>();
        this.setLoggedInUsers(new ArrayList<String>());
    }

    public User getUser(String userId)
    {
        if (this.userList.containsKey(userId)) {
            return this.userList.get(userId);
        }
        return null;
    }

    // delete a user
    public void deleteUser(int userId)
    {
        this.userList.remove(userId);
    }

    // returns the next user id
    public int getNextUserId()
    {
        return this.nextUserId;
    }

    // returns the list of users
    public Map<String, User> getUserList()
    {
        return this.userList;
    }



    public EventBus getEventBus()
    {
        return this.eb;
    }

    public void setEventBus(EventBus eventBus)
    {
        this.eb = eventBus;
    }

    public void init()
    {
        // load employees from MongoDB collection usermodels
        MongoDBConnector mdbcon = new MongoDBConnector(config.getString("mongoPersistorAddress"),
                this.eb);
        AsyncResultHandler<JsonArray> usermodelDataHandler = new AsyncResultHandler<JsonArray>()
        {

            @Override
            public void handle(AsyncResult<JsonArray> mongoDBRequest)
            {
                if (mongoDBRequest.succeeded()) {
                    JsonArray resultArray = mongoDBRequest.result();
                    if (null != resultArray) {
                        initUsers(resultArray);
                    }
                }
            }

        };
        mdbcon.find("usermodels", new JsonObject(), new JsonObject(), usermodelDataHandler);
    }

    private void initUsers(JsonArray userArray)
    {

        for (Object userObject : userArray) {
            if (userObject instanceof JsonObject) {
                JsonObject userJsonObject = (JsonObject) userObject;
                User u = new User();
                u.setId(userJsonObject.getString("id"));
                u.setEmployeeType(userJsonObject.getString("employeetype"));
                // get list of mastered measures...
                JsonArray masteredProcesses = userJsonObject.getArray("masters");
                if (null != masteredProcesses) {
                    for (Object processId : masteredProcesses) {
                        if (!processId.equals("")) {
                            u.addMasteredProcess(processId.toString(), eb);
                        }

                    }
                }
                // get list of allowed measures
                JsonArray allowedMeasures = userJsonObject.getArray("allowed");
                if (null != allowedMeasures) {
                    for (Object amo : allowedMeasures) {
                        if (!amo.equals("")) {
                            u.addMeasureClearance(amo.toString());
                        }

                    }
                }
                // get list of allowed measures with assistance
                JsonArray allowedWithAssistanceMeasures = userJsonObject
                        .getArray("allowedWithAssistance");
                if (null != allowedWithAssistanceMeasures) {
                    for (Object awamo : allowedWithAssistanceMeasures) {
                        if (!awamo.equals("")) {
                            u.addMeasureClearanceWithAssistance(awamo.toString());
                        }
                    }
                }
                // get list of workplaceGroups
                JsonArray workplaceGroups = userJsonObject.getArray("wpgs");
                if (null != workplaceGroups) {
                    for (Object wpg : workplaceGroups) {
                        if (!wpg.equals("")) {
                            u.addWorkplaceGroup(wpg.toString());
                        }
                    }
                }
                EmployeeDevelopmentGoals edg = new EmployeeDevelopmentGoals();

                // get list of development goal positions
                JsonArray dgPos = userJsonObject.getArray("dgPosition");
                if (null != dgPos) {
                    if (dgPos.size() > 0)
                        if (!dgPos.get(0).equals("")) {
                            edg.setPosition(dgPos.get(0).toString());
                        }
                }
                // get list of development goal positions
                JsonArray dgItems = userJsonObject.getArray("dgItems");
                if (null != dgItems) {
                    for (Object dgItem : dgItems) {
                        if (!dgItem.equals("")) {
                            edg.addItem(dgItem.toString());
                        }
                    }
                }
                // get list of development goal positions
                JsonArray dgContents = userJsonObject.getArray("dgContents");
                if (null != dgContents) {
                    for (Object dgContent : dgContents) {
                        if (!dgContent.equals("")) {
                            edg.addContent(dgContent.toString());
                        }
                    }
                }
                u.setDevelopmentGoals(edg);
                u.setKnowledgeItems(itemList);
                u.setProcessItems(processItemList);
                this.userList.put(u.getId(), u);
                

            }
        }

    }
    // methods to manage list of users authenticated to the AppSist system

    // get all users who are logged in currently
    public List<String> getLoggedInUsers()
    {
        return loggedInUsers;
    }

    // setter function for logged in users
    public void setLoggedInUsers(List<String> loggedInUsers)
    {
        this.loggedInUsers = loggedInUsers;
    }

    // add user who logged in (only if not already in the list)
    public void addLoggedInUser(String loggedInUser)
    {
        if (!this.loggedInUsers.contains(loggedInUser)) {
            this.loggedInUsers.add(loggedInUser);
        }
    }

    // remove a user (only if logged in at all)
    public void removeLoggedInUser(String loggedInUser)
    {
        if (this.loggedInUsers.contains(loggedInUser)) {
            this.loggedInUsers.remove(loggedInUser);
        }
    }

    // set configuration
    public void setConfiguration(JsonObject conf)
    {
        this.config = conf;
    }
    
    // set itemList 
    public void setImportantContentIds(List<String> items){
    	this.itemList=items;
    }
 // set itemList 
    public void setImportantProcessIds(List<String> items){
    	this.processItemList=items;
    }
}
