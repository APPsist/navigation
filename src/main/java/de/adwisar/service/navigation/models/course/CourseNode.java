package de.adwisar.service.navigation.models.course;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class CourseNode {
	// a UUID identifying the current node instance
	private String nodeId;
	
	// represent which step in a course the current node represents
	private long courseStepId;
	
	// each node is connected with a default content
	// the id of this content is stored in nodeContentId
	private String nodeContentId;
	
	// node Id of the next node in the course 
	// null if we course is finished
	private String nextNodeId;
	
	// node Id of the previous node in the course 
	// null if we are at the root of the course
	private String previousNodeId;
	
	// array contains all CourseNode for the current course
	// with their nodeIds as Array key
	private JsonObject contextData;
	
	  // logger object
    private static final Logger log = LoggerFactory.getLogger(CourseNode.class);

	public CourseNode(){
		
	}
	
	public CourseNode(JsonObject nodeObject){
		// default constructor
		nodeId = nodeObject.getString("nodeId");
		nodeContentId = nodeObject.getString("nodeContentId");
		nextNodeId=nodeObject.getString("nextNodeId");
		previousNodeId=nodeObject.getString("previousNodeId");
		contextData = nodeObject.getObject("contextData");
	}
	
	// public getters 
	public JsonObject getContextData(){
		return this.contextData;
	}
	
	public String getNodeId(){
		return this.nodeId;
	}
	
	public String getNextNodeId(){
		return this.nextNodeId;
	}
	
	public String getPreviousNodeId(){
		return this.previousNodeId;
	}
	
	public String getNodeContentId(){
		return this.nodeContentId;
	}
	public boolean hasNext(){
		return (null != nextNodeId);
	}
	
	public boolean hasPrevious(){
		return (null != previousNodeId);
	}
	
	public long getCourseStepId(){
		return this.courseStepId;
	}
	
	public void setCourseStepId(long stepId){
		this.courseStepId=stepId;
	}
	
	
	public static void createCourseNodes(){
		CourseNode cn = new CourseNode();
		cn.nodeId=UUID.randomUUID().toString();
		cn.nodeContentId="http://edtec.dfki.de/edtec-lab/";
		cn.previousNodeId="c231699a-d420-42f7-a77c-5222bf37adc4";
		cn.contextData=new JsonObject();
		
		Map<String, CourseNode>testMap = new HashMap<String,CourseNode>();
		testMap.put(cn.getNodeId(),cn);
		testMap.put("rootNode",cn);
		 ObjectMapper mapper = new ObjectMapper();
         try {
             String ulString = mapper.writeValueAsString(testMap);
             log.info(ulString);
         }
         catch (Exception e) {
             e.printStackTrace();
         }
	}
}
