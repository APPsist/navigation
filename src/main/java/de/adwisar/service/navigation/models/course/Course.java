package de.adwisar.service.navigation.models.course;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Course {
	private String id=null;
	private Map<String, CourseNode> courseNodes;
	private CourseNode courseRootNode = new CourseNode();
	
	// logger object
    private static final Logger log = LoggerFactory.getLogger(Course.class);
	
	public Course(JsonObject courseObject){
		
		this.id = courseObject.getString("id");
		
		this.courseNodes=new HashMap<String, CourseNode>();
		JsonObject nodeList = courseObject.getObject("courseNodes");
		
		long courseStepId = 0;
		CourseNode rootNode = new CourseNode(nodeList.getObject("rootNode"));
		Set<String> nodeKeys =nodeList.toMap().keySet(); 
		
		if (null != nodeKeys){
			for (String nodekey :nodeKeys){
				CourseNode newNode = new CourseNode(nodeList.getObject(nodekey));
				courseNodes.put(nodekey, newNode);
				if (nodekey.equals("rootNode")){
					courseRootNode=newNode;
					courseRootNode.setCourseStepId(0);
				}
			}
//			courseRootNode=rootNode;
//			rootNode.setCourseStepId(courseStepId++);
//			courseNodes.put(rootNode.getNodeId(),rootNode);
//			CourseNode currentNode = rootNode;
//			while(currentNode.hasNext()){
//				currentNode = new CourseNode(nodeList.getObject(currentNode.getNextNodeId()));
//				currentNode.setCourseStepId(courseStepId++);
//				courseNodes.put(currentNode.getNodeId(), currentNode);
//			}
		} else {
			log.info("No rootNode found for course: "+ this.id);
		}
		
	}
	
	public String getId(){
		return this.id;
	}
	
	public CourseNode getRootNode(){
		return courseRootNode;
	}
	
	
	// return a map with all CourseNodes 
	public Map<String, CourseNode> getCourseNodes(){
		return courseNodes;
	}

}
