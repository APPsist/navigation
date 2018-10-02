package de.adwisar.service.navigation.managers;

import java.util.*;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import de.adwisar.service.navigation.connector.MongoDBConnector;
import de.adwisar.service.navigation.models.course.*;


public class CourseInstanceManager
{
    private static CourseInstanceManager _instance = new CourseInstanceManager();
    
    // vertx related fields
    // verticle configuration 
    private JsonObject config = null;
    // logging class 
    private static final Logger log = LoggerFactory.getLogger(CourseInstanceManager.class);
    // vertx eventbus object
    Vertx vx;
    
    // Class fields
    // all known courses are listed here
    private Map<String, Course> knownCourses=null;
    
    // all known users are listed here
    private List<String> knownUserIds=null;
    
    // stores all active course instances with their IDs as keys
    private Map<UUID, CourseInstance> mapInstanceIdCI = null;
    // stores all 
    private Map<String,Map<String,UUID>> mapUserIdCI = null;
    
 	public static CourseInstanceManager getInstance()
    {
        return _instance;
    }

    private CourseInstanceManager()
    {
    	
    }
    
    public void init(Vertx vertx, JsonObject verticleConfig){
    	// destroy field variables
    	knownCourses = null;
    	knownUserIds = null;
    	mapInstanceIdCI = null;
    	mapUserIdCI = null;
    	vx=null;
    	// re-initialize
    	knownCourses = new HashMap<String, Course>();
    	knownUserIds = new ArrayList<String>();
    	mapInstanceIdCI = new HashMap<UUID, CourseInstance>();
    	mapUserIdCI = new HashMap<String, Map<String,UUID>>();
    	vx=vertx;
    	config=verticleConfig;
    	getKnownUsers();
    	loadCoursesFromDB();
    	
    }
    
    public Course getCourse(String courseId){
    	return this.knownCourses.get(courseId);
    }
    
    private void loadCoursesFromDB(){
    	 // load employees from MongoDB collection usermodels
    	log.info("Loading courses from database");
        MongoDBConnector mdbcon = new MongoDBConnector(config.getString("mongoPersistorAddress"),
                vx.eventBus());
        AsyncResultHandler<JsonArray> usermodelDataHandler = new AsyncResultHandler<JsonArray>()
        {

            @Override
            public void handle(AsyncResult<JsonArray> mongoDBRequest)
            {
                if (mongoDBRequest.succeeded()) {
                    JsonArray resultArray = mongoDBRequest.result();
                    if (null != resultArray) {
                        initCourses(resultArray);
                    }
                }
            }

        };
        mdbcon.find(config.getString("coursesCollection"), new JsonObject(), new JsonObject(), usermodelDataHandler);
    }
    
    private void initCourses(JsonArray courseArray) {
    	
	    for (Object courseObject : courseArray) {
	        if (courseObject instanceof JsonObject) {
	            JsonObject courseJsonObject = (JsonObject) courseObject;
	            //log.info(courseJsonObject.encodePrettily());
	            Course course = new Course(courseJsonObject);
	            this.knownCourses.put(course.getId(), course);
	            //log.info("Adding course: "+course.getId());
	        }
	        
	    }
    }
    // methods to manage list of users authenticated to the AppSist system
      
    // set configuration
    public void setConfiguration(JsonObject conf)
    {
        this.config = conf;
    }
    
    
    public boolean isCourseInstantiatedForUser(String userId, String courseId){
    	boolean _courseInstantiated = false;
		if (null != mapUserIdCI.get(userId) && null != mapUserIdCI.get(userId).get(courseId)){
			_courseInstantiated = true;
		}
    	return _courseInstantiated;
    }
    
    public boolean isUserValid(String userId){
    	if (!knownUserIds.contains(userId)){
    		knownUserIds.add(userId);
    	}
    	return knownUserIds.contains(userId);
    }
    
    public boolean instantiateCourse(JsonObject data, Course course){
    	log.info("Instantiating course: "+course.getId());
    	CourseInstance ci = new CourseInstance(course, data);
    	mapInstanceIdCI.put(ci.getInstanceId(), ci);
    	String instanceUserId =data.getString("userId"); 
    	if (mapUserIdCI.containsKey(instanceUserId)){
    		mapUserIdCI.get(instanceUserId).put(course.getId(), ci.getInstanceId());
    	} else{
    		Map<String, UUID> newMap = new HashMap<String, UUID>();
    		newMap.put(course.getId(), ci.getInstanceId());
    		mapUserIdCI.put(instanceUserId, newMap);
    	}
    	log.info("Done");
    	return true;
    }
    
    public CourseNode getNextCourseNode(JsonObject data, String courseId){
    	String requestUserId = data.getString("userId");
    	CourseInstance requestCourseInstance = mapInstanceIdCI.get(mapUserIdCI.get(requestUserId).get(courseId));
    	return requestCourseInstance.getNextCourseNode();
    }
    
    public CourseNode getPreviousCourseNode(JsonObject data, String courseId){
    	String requestUserId = data.getString("userId");
    	CourseInstance requestCourseInstance = mapInstanceIdCI.get(mapUserIdCI.get(requestUserId).get(courseId));
    	return requestCourseInstance.getPreviousCourseNode();
    }
    
    public CourseNode getCourseNodeAtStep(JsonObject data, String courseId, long stepId){
    	String requestUserId = data.getString("userId");
    	CourseInstance requestCourseInstance = mapInstanceIdCI.get(mapUserIdCI.get(requestUserId).get(courseId));
    	return requestCourseInstance.getCourseNodeAtStep(stepId);
    }
    
    public CourseNode getCourseNodeAtStepId(JsonObject data, String courseId, String stepId){
    	String requestUserId = data.getString("userId");
    	CourseInstance requestCourseInstance = mapInstanceIdCI.get(mapUserIdCI.get(requestUserId).get(courseId));
    	return requestCourseInstance.getCourseNodeForStepId(stepId);
    }
    
    // method asks a list of all known userIds from usermodel service 
    private void getKnownUsers(){
    	log.info("Requesting known users");
    	HttpClient client = vx.createHttpClient()
    			.setHost("localhost")
    			.setPort(8089);
    	client.getNow("/its/usermodel/knownUsers",  new Handler<HttpClientResponse>(){

			@Override
			public void handle(HttpClientResponse knownUsersResponse) {
				// fill knownUserIds List
				
				final Buffer body = new Buffer(0);
				knownUsersResponse.dataHandler(new Handler<Buffer>() {
		            public void handle(Buffer data) {
		                body.appendBuffer(data);
		            }
		        });
				 knownUsersResponse.endHandler(new VoidHandler() {
			            public void handle() {
			               // The entire response body has been received
			            	log.info(body.toString());
			               JsonArray jArray = new JsonArray(body.toString());
			               knownUserIds.addAll(jArray.toList());
			            }
			        });
			}
    	});
    	client.close();
    	log.debug("Done");
    }
//    public void saveDataToDB(JsonObject userData, JsonObject eventData){
//    	 MongoDBConnector mdbcon = new MongoDBConnector(config.getString("mongoPersistorAddress"), this.eb);
//         JsonObject matchObject = new JsonObject();
//         matchObject.putString("id",userData.getString("id"));
//         AsyncResultHandler<Void> usermodelDataHandler = new AsyncResultHandler<Void>()
//         {
//
//             @Override
//             public void handle(AsyncResult<Void> mongoDBRequest)
//             {
//           	  if (mongoDBRequest.succeeded()) {
//           		  
//                 }
//           	  if (mongoDBRequest.failed()){
//           		  log.info("Error storing user! Cause: "+mongoDBRequest.cause());
//           	  }
//             }
//         };
//         mdbcon.update(config.getString("usermodelCollection"),  matchObject, userData,true,false, usermodelDataHandler);
//         mdbcon.save(config.getString("eventCollection"), eventData, usermodelDataHandler);
//       }
}
