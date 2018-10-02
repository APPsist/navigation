package de.adwisar.service.navigation;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Verticle;
import de.appsist.commons.misc.StatusSignalConfiguration;
import de.appsist.commons.misc.StatusSignalSender;

import de.adwisar.service.navigation.managers.CourseInstanceManager;
import de.adwisar.service.navigation.models.course.Course;
import de.adwisar.service.navigation.models.course.CourseNode;

public class Navigation
    extends Verticle
{
    private JsonObject config;
    private String basePath = "";

    // logger object
    private static final Logger log = LoggerFactory.getLogger(Navigation.class);
   
    private CourseInstanceManager ciManager;
   
    public void start()
    {
         if (container.config() != null && container.config().size() > 0) {
            config = container.config();

        }
        else {
            config = getDefaultConfiguration();
        }
        container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", config.getObject("mod-mongo-persistor"));
        this.basePath = config.getObject("webserver").getString("basePath");
        // set important content
        
        // start CourseInstanceManager
        this.ciManager = CourseInstanceManager.getInstance();
        this.ciManager.init(vertx, config);
        //setupHandlers();
        setupHttpHandlers();
        
                
            log.info("--- adwisar navigation started ---");
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
        webserverConfig.putNumber("port", 8088);
        webserverConfig.putString("statics", "www");
        webserverConfig.putString("basePath", "/its/navigate");
        defaultConfig.putObject("webserver", webserverConfig);
        defaultConfig.putString("mongoPersistorAddress","vertx.mongopersistor");
        defaultConfig.putString("coursesCollection", "its_courses");
        defaultConfig.putString("jwt_secret", "your_jwt_secret");
        
        JsonObject mongoDBConfig =  new JsonObject();
        mongoDBConfig.putString("address", "vertx.mongopersistor");
        mongoDBConfig.putString("host", "localhost"); 
        mongoDBConfig.putNumber("port", 27017);
        mongoDBConfig.putNumber("pool_size", 10);
        mongoDBConfig.putString("db_name", "ulm_its");
        defaultConfig.putObject("mod-mongo-persistor", mongoDBConfig);
        return defaultConfig;

    }

    


    private void setupHttpHandlers()
    {
        BasePathRouteMatcher rm = new BasePathRouteMatcher(this.basePath);

      rm.put("/:courseId/next", new Handler<HttpServerRequest>()
      {
          public void handle(final HttpServerRequest req)
          {
              final String courseId = req.params().get("courseId");
              final Course course = ciManager.getCourse(courseId);
              if (null != course) {
            	 processNavigateRequest(req, course, "next");
              }
              else {
                  req.response().end("{\"error\":\"Unknown course id:"+req.params().get("courseId")+"\"}");
              }
          }
      });
      
      rm.options("/:courseId/next", new Handler<HttpServerRequest>(){

			@Override
			public void handle(HttpServerRequest event) {
				// TODO return website showing module configuration
				event.response().end();
			}
      });
      
      rm.put("/:courseId/back", new Handler<HttpServerRequest>()
      {
          public void handle(final HttpServerRequest req)
          {
              final String courseId = req.params().get("courseId");
              final Course course = ciManager.getCourse(courseId);
            
              if (null != course) {
            	 processNavigateRequest(req, course, "back");
              }
              else {
                  req.response().end("{\"error\":\"Unknown course id:"+req.params().get("courseId")+"\"}");
              }
          }
      });
      
      rm.options("/:courseId/back", new Handler<HttpServerRequest>(){

			@Override
			public void handle(HttpServerRequest event) {
				// TODO return website showing module configuration
				event.response().end();
			}
    });
      
      rm.put("/:courseId/go/:step", new Handler<HttpServerRequest>()
      {
          public void handle(final HttpServerRequest req)
          {
              final String courseId = req.params().get("courseId");
              final Course course = ciManager.getCourse(courseId);
              String stepId = req.params().get("step");
            
              if (null != course) {
            	 processNavigateRequest(req, course, stepId);
              }
              else {
                  req.response().end("{\"error\":\"Unknown course id:"+req.params().get("courseId")+"\"}");
              }
          }
      });
     
      rm.options("/:courseId/go/:step", new Handler<HttpServerRequest>(){

			@Override
			public void handle(HttpServerRequest event) {
				// TODO return website showing module configuration
				event.response().end();
			}
      });
      
        vertx.createHttpServer().requestHandler(rm)
                .listen(config.getObject("webserver").getInteger("port"));
    }
    
    
    private void sendResponse(CourseNode courseNode, HttpServerRequest request){
    	if (null != courseNode){
    		JsonObject response = new JsonObject();
    		response.putString("nextStepContentId", courseNode.getNodeContentId());
    		response.putString("nextStepPreviousId", courseNode.getPreviousNodeId());
    		response.putString("nextStepNextId", courseNode.getNextNodeId());
    		if (courseNode.getNextNodeId().indexOf("_SPA_") != -1){
    			response.putBoolean("isNextStepInternal", true);
    		}
    		if (courseNode.getPreviousNodeId().indexOf("_SPA_") != -1){
    			response.putBoolean("isPreviousStepInternal", true);
    		}
    		request.response().end(response.toString());
    	} else{
    		request.response().end("{\"error\":\"No previous/next node available\"}");
    	}
    	
    }
        
    
    
    public Navigation()
    {
        //
    }

    
    private void processNavigateRequest(final HttpServerRequest request, final Course course, final String direction){
    	 // extract data from request and continue
  	  final Buffer body = new Buffer(0);
			request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer data) {
                body.appendBuffer(data);
            }
        });
  	  
  	  request.endHandler(new Handler<Void>(){

			@Override
			public void handle(Void dataEndEvent) {
				JsonObject dataObject = new JsonObject(body.toString());
				String receivedUserId = JWTSecurity.extractUserId(dataObject.getString("jwt"), config.getString("jwt_secret"));
				if ("_UNDEFINED_".equals(receivedUserId)){
					// no userId provided with request. Stop execution
					request.response().end("{\"error\":\"User id not provided\"}");
				}
				// add receivedUserId
				dataObject.putString("userId", receivedUserId);
				executeProcessRequest(request, course, direction, dataObject, receivedUserId);
				
								
			}
  	  });
    }
    
    public void executeProcessRequest(final HttpServerRequest request, final Course course, final String direction, final JsonObject dataObject, final String receivedUserId){
    	if (!ciManager.isCourseInstantiatedForUser(receivedUserId, course.getId())){
			ciManager.instantiateCourse(dataObject, course);
			vertx.setTimer(1000, new Handler<Long>(){

				@Override
				public void handle(Long event) {
					// TODO Auto-generated method stub
					executeProcessRequest(request, course, direction, dataObject, receivedUserId);
				}
				
			});
			
		} else{
			switch(direction){
			case "next": sendResponse(ciManager.getNextCourseNode(dataObject, course.getId()), request);
			break;
			case "back": sendResponse(ciManager.getPreviousCourseNode(dataObject, course.getId()), request);
			break;
				default:
					try {
						Long l = Long.parseLong(direction);
						sendResponse(ciManager.getCourseNodeAtStep(dataObject, course.getId(), l), request);
					} catch(NumberFormatException nfe){
						log.info("String "+direction+" cannot be parsed. Requesting as stepId");
						sendResponse(ciManager.getCourseNodeAtStepId(dataObject, course.getId(), direction), request);
					}
			}
		}
    }

  

}
