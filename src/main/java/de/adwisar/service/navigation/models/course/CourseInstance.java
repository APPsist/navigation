package de.adwisar.service.navigation.models.course;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import de.adwisar.service.navigation.managers.CourseInstanceManager;

public class CourseInstance {
	// a UUID representing the current course instance
	private UUID instanceId;
	
	// the courseId for which this course instance has been created
	private String courseId;
	
	// the user for whom the course instance has been created
	private String userId;
	
	// current position
	private long currentStepId=0;
	
	// currentNodeId
	private CourseNode currentNode = null;
	
	// array contains all CourseNode for the current course
	// with their nodeIds as Array key
	private Map<String, CourseNode> courseNodeIdNodeArray;
	
	// array contains all CourseNode for the current course
	// with their stepIds  as Array key
	// this will be needed to be able to do arbitrary jumps in the course
	private Map<Long, CourseNode> courseStepIdNodeArray;
	
	// this represents the UUID of the starting node of the course
	private String rootNodeId;
	
	// logging class 
    private static final Logger log = LoggerFactory.getLogger(CourseInstance.class);
	private JsonObject nodeContext; 
	
	public CourseInstance(Course course, JsonObject nodeContextObject){
		instanceId = UUID.randomUUID();
		courseId = course.getId();
		rootNodeId = course.getRootNode().getNodeId();
		courseNodeIdNodeArray = course.getCourseNodes();
		courseStepIdNodeArray = new HashMap<Long, CourseNode>();
		for(CourseNode cn : courseNodeIdNodeArray.values()){
			//log.info("Adding step id: "+cn.getCourseStepId());
			courseStepIdNodeArray.put(cn.getCourseStepId(), cn);
		}
		nodeContext=nodeContextObject;
	}
	
	public UUID getInstanceId(){
		return instanceId;
	}
		
	public CourseNode getNextCourseNode(){
		if (null != currentNode){
			if (currentNode.hasNext()){
				currentNode = courseNodeIdNodeArray.get(currentNode.getNextNodeId());
			}
			return currentNode;
		} 
		currentNode= courseNodeIdNodeArray.get("rootNode");
		return currentNode;
//		if (currentStepId< courseStepIdNodeArray.keySet().size()){
//			return courseNodeIdNodeArray.get(courseStepIdNodeArray.get(currentStepId++).getNextNodeId());
//		} 
//		 return null;
	}
	
	public CourseNode getPreviousCourseNode(){
		if (null != currentNode){
			if (currentNode.hasPrevious()){
				currentNode = courseNodeIdNodeArray.get(currentNode.getPreviousNodeId());
			}
			return currentNode;
		} 
		currentNode= courseNodeIdNodeArray.get("rootNode");
		return currentNode;
//		if (currentStepId>0){
//			return courseNodeIdNodeArray.get(courseStepIdNodeArray.get(currentStepId--).getPreviousNodeId());
//		} 
//		 return null;
	}
	
	public CourseNode getCourseNodeAtStep(long stepId){
		if (currentStepId>=0 && (currentStepId< courseStepIdNodeArray.keySet().size())){
			return courseStepIdNodeArray.get(stepId);
		} 
		 return null;
	}
	
	public CourseNode getCourseNodeForStepId(String stepId){
		currentNode= courseNodeIdNodeArray.get(stepId);
		return currentNode;
	}
}