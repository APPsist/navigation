package de.appsist.service.usermodel.model;

import java.util.*;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.appsist.service.usermodel.UserModelService;
import de.appsist.service.usermodel.connector.MongoDBConnector;
import de.appsist.service.usermodel.managers.UserManager;
public class User
{

    public enum CompetencyLevel
    {
        // competency levels
        COMPLVLLOW("Kenner"), COMPLVLMEDIUM("Könner"),
 COMPLVLHIGH("Experte"), COMPLVLUNKNOWN(
                "Nicht spezifiziert");
        
        public String fullname = "";
        
        CompetencyLevel(String val) {
            this.fullname = val;
        }

    }

    public enum EmployeeType {
        // different types of employees
        EMPLTYPE_AB("Anlagenbediener"), EMPLTYPE_AF("Anlagenführer"), EMPLTYPE_FK("Führungskraft"), EMPLTYPE_IH(
                "Instandhalter");

        public String fullname = "";

        EmployeeType(String val)
        {
            this.fullname = val;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(User.class);
    private static final String eventbusPrefix = "appsist:";
    // =================================================================
    // static user fields
    //
    // this section contains user fields which never change
    // or in rare occasions only
    //
    // =================================================================

    // user id
    public String id;

    // users first name
    private String firstname = "";

    // users middle name
    private String middlename = "";

    // users last name
    private String lastname = "";

    // users date of birth
    private Date dateofbirth = null;

    // users employer
    private String employer = "";

    // employeenumber (structure given by employer)
    private String employeenumber = "";

    // users mother tongue
    private String mothertongue = "";

    // users current level of training
    private String traininglevel = "";

    // specifies the type of the employee
    // see enum type definition at the top of class for possible values
    private String employeetype = null;

    // =================================================================
    // dynamic user fields
    //
    // this section contains user fields which never change often
    //
    // =================================================================

    // map of competencylevels for measures Map<String, String>
    // String#1 denotes measureId
    // String#2 one of (COMPLVLLOW, COMPLVLMEDIUM, COMPLVLHIGH)
    private Map<String, CompetencyLevel> measuresCompLvl;

    // viewed content map Map<String, Integer>
    // String denotes contentId
    // Integer is the number of views
    private Map<String, Integer> numContentViewed;

    // number of times a measure has been performed by the user
    // String denotes measureId
    // Integer is the number of times measure has been performed
    private final Map<String, Integer> numMeasuresPerformed;

    // list of measures user is allowed to perform
    private Set<String> measureClearance;

    // list of measures user is allowed to perform with assistance
    private Set<String> measureClearancesWithAssistance;

    // list of "Stationen" for which user is responsible
    private List<String> responsibleFor;

    // Workplacegroups user is assigned to
    private Set<String> workplaceGroups;

    // Developmentgoals which are set for user
    // consists of Occupationgroups and objects in production
    private EmployeeDevelopmentGoals developmentGoals;

    // current workstate
    // one of (Haupttätigkeit/Nebentätigkeit)
    private String currentWorkstate;

    public User()
    {
        // default constructor

        this.measuresCompLvl = new HashMap<String, CompetencyLevel>();
        this.numContentViewed = new HashMap<String, Integer>();
        this.numMeasuresPerformed = new HashMap<String, Integer>();
        this.setMeasureClearance(new HashSet<String>());
        this.setMeasureClearancesWithAssistances(new HashSet<String>());
        this.setResponsibleFor(new ArrayList<String>());
        this.setWorkplaceGroups(new HashSet<String>());
        this.developmentGoals = new EmployeeDevelopmentGoals();
        
        
    }

    // getter/setter methods for static user fields

    // returns (internal) user id

    public String getId()
    {
        return id;
    }

    // set user id
    public void setId(String id)
    {
        this.id = id;
        
    }

    // returns users firstname
    public String getFirstname()
    {
        return firstname;
    }

    // set users firstname
    public void setFirstname(String firstname)
    {
        this.firstname = firstname;
    }

    // returns users middlename
    public String getMiddlename()
    {
        return middlename;
    }

    // set users middlename
    public void setMiddlename(String middlename)
    {
        this.middlename = middlename;
    }

    // returns users lastname
    public String getLastname()
    {
        return lastname;
    }

    // set users lastname
    public void setLastname(String lastname)
    {
        this.lastname = lastname;
    }

    public String getFullName()
    {
        String temp = this.getFirstname() + " " + this.getLastname();
        return temp;
    }

    // returns date of birth
    public Date getDateofbirth()
    {
        return dateofbirth;
    }

    // set date of birth
    public void setDateofbirth(Date dateofbirth)
    {
        this.dateofbirth = dateofbirth;
    }

    // returns name of employer
    public String getEmployer()
    {
        return employer;
    }

    // set employer
    public void setEmployer(String employer)
    {
        this.employer = employer;
    }

    // returns employee type
    public String getEmployeeType()
    {
        return this.employeetype;
    }

    // set employee type
    public void setEmployeeType(String employeetype)
    {
        this.employeetype = employeetype;
    }

    // returns employeenumber
    public String getEmployeenumber()
    {
        return employeenumber;
    }

    // set employee number
    public void setEmployeenumber(String employeenumber)
    {
        this.employeenumber = employeenumber;
    }

    // returns the current traininglevel of user
    public String getTraininglevel()
    {
        return traininglevel;
    }

    // set the current traininglevel of user
    public void setTraininglevel(String traininglevel)
    {
        this.traininglevel = traininglevel;
    }

    // returns mother tongue
    public String getMothertongue()
    {
        return mothertongue;
    }

    // set mother tongue
    public void setMothertongue(String mothertongue)
    {
        this.mothertongue = mothertongue;
    }


    // ========================
    // CONTENT RELATED SECTION
    // ========================

    // returns map with amount of views for all content items
    public Map<String, Integer> getContentViewed()
    {
        return numContentViewed;
    }

    // set amount of views for multiple content items
    public void setContentViewed(Map<String, Integer> contentViewed)
    {
        this.numContentViewed = contentViewed;
    }

    // returns number of views for specified content item or -1 if we have a new item
    public int getNumContentViews(String contentItemId)
    {
        if (this.numContentViewed.containsKey(contentItemId)) {
            return numContentViewed.get(contentItemId);
        }
        return 0;	
    }

    // increases number of views for specified content item
    public void contentViewed(String contentItemId)
    {
        int numContentView = this.getNumContentViews(contentItemId);
        this.numContentViewed.put(contentItemId, ++numContentView);
    }

    // list contents seen by this user
    public String listContentsSeen()
    {
        String resultHTML = "<table>";
        for (String str : this.numContentViewed.keySet()) {
            resultHTML += "<tr><td>" + str + "</td><td>" + this.numContentViewed.get(str)
                    + "</td></tr>";
        }
        resultHTML += "</table>";
        return resultHTML;

    }

    // ==================
    // MEASURES SECTION
    // ==================

    // return map with competencylevels for all measures
    public Map<String, CompetencyLevel> getMeasuresCompLvls()
    {
        return measuresCompLvl;
    }

    // return competencylevel for specified measure
    public String getMeasureCompLvl(String measureId)
    {
        if (this.measuresCompLvl.containsKey(measureId)) {
            return this.measuresCompLvl.get(measureId).fullname;
        }
        return CompetencyLevel.COMPLVLUNKNOWN.fullname;
    }

    // set competencylevel for specified measure
    public void setMeasureCompLvl(String measure, CompetencyLevel lvl)
    {
        this.measuresCompLvl.put(measure, lvl);
    }

    // returns number of performances for the specified measure or 0 if we don't have one
    public int getNumMeasurePerformed(String measureId)
    {
        // for given measureId check if we already have a number of performances
        // if yes
        // if number is bigger than masteredTreshold return number
        // if number is lower ask LRS for updated number
        // ask LRS otherwise
        if (this.numMeasuresPerformed.containsKey(measureId)) {
            return numMeasuresPerformed.get(measureId);
                    }
        return 0;
    }

    // increases number of times a measure has been performed by the user
    public void measurePerformed(String measureId)
    {
        int numMeasurePerformed = this.getNumMeasurePerformed(measureId);
        log.info(measureId+" : "+numMeasurePerformed);
        numMeasurePerformed = numMeasurePerformed+1;
        log.info(measureId+" : "+numMeasurePerformed);
        this.numMeasuresPerformed.put(measureId, numMeasurePerformed);
        log.info(this.getNumMeasurePerformed(measureId));
        
        if (processItems.contains(measureId)){
        	this.interactedWithStationMontage++;
        	if ("ba66718d-607c-4c81-82d7-f1a55fbbef3f".equals(measureId)){
        		this.interactedWithPneumatikZylinder++;
        	}
        	// TODO: Replace with sendToKVD
        	sendKnowledgeEstimationToKVD();
        }
    }


    public void sendKnowledgeEstimationToKVD(){
    	Map<String, Integer> estimationMapToSend = this.getKnowledgeInformation();
    	JsonObject objectToSend = new JsonObject();
    	objectToSend.putString("userId", this.getId());
    	for (String item: estimationMapToSend.keySet()){
    		objectToSend.putNumber(item, estimationMapToSend.get(item));
    	}
    	//log.info(objectToSend.encodePrettily());
    	UserManager.getInstance().getEventBus().publish("appsist:services:kvdconnection:knowledgeestimation", objectToSend);
    }
    
    public void resetKnowledgeEstimation(){
    	this.numMeasuresPerformed.clear();
    	this.numContentViewed.clear();
    	this.interactedWithPneumatikZylinder=0;
    	this.interactedWithStationMontage=0;
    }
    // =======================================
    // MEASURE CLEARANCES (FREIGABEN) SECTION
    // =======================================

    // list measure clearances for user
    public String listMeasureClearances()
    {
        String resultHTML = "<table>";
        for (String str : this.measureClearance) {
            // resultHTML += "<tr><td>" + str + "</td><td>" + str + "</td></tr>";
            resultHTML += "<tr><td>" + str + "</td></tr>";
        }
        resultHTML += "</table>";
        return resultHTML;

    }

    // returns list containing all measure IDs the user is allowed to perform
    public Set<String> getMeasureClearance()
    {
        return measureClearance;
    }

    // set list of allowed measure IDs
    public void setMeasureClearance(Set<String> measureClearance)
    {
        this.measureClearance = measureClearance;
    }


    // add a new allowed measure here
    public void addMeasureClearance(String measureId)
    {
        // TODO: perform sanity check for measureId here
        if (true) {
            this.measureClearance.add(measureId);
        }

    }

    // ======================================================
    // MEASURE CLEARANCES WITH ASSISTANCE (FREIGABEN) SECTION
    // ======================================================

    // list measure clearances for user
    public String listMeasureClearancesWithAssistance()
    {
        String resultHTML = "<table>";
        for (String str : this.measureClearancesWithAssistance) {
            // resultHTML += "<tr><td>" + str + "</td><td>" + str + "</td></tr>";
            resultHTML += "<tr><td>" + str + "</td></tr>";
        }
        resultHTML += "</table>";
        return resultHTML;

    }

    // returns Set containing all measure IDs the user is allowed to perform
    public Set<String> getMeasureClearancesWithAssistance()
    {
        return this.measureClearancesWithAssistance;
    }

    // set list of allowed measure IDs
    public void setMeasureClearancesWithAssistances(Set<String> measureClearance)
    {
        this.measureClearancesWithAssistance = measureClearance;
    }

    // add a new allowed measure here
    public void addMeasureClearanceWithAssistance(String measureId)
    {
        // TODO: perform sanity check for measureId here
        if (true) {
            this.measureClearancesWithAssistance.add(measureId);
        }

    }

    // return list of stations
    public List<String> getResponsibleFor()
    {
        return responsibleFor;
    }

    // set stations user is responsibleFor
    public void setResponsibleFor(List<String> responsibleFor)
    {
        this.responsibleFor = responsibleFor;
    }

    ///////////////////////////////////////////////////////////
    // Arbeitsplatzgruppen (WorkplaceGroup section)

    // return list of workplacegroups users belongs to
    public Set<String> getWorkplaceGroups()
    {
        return this.workplaceGroups;
    }

    // set list of workplacegroups
    public void setWorkplaceGroups(Set<String> userWorkplaceGroups)
    {
        this.workplaceGroups = userWorkplaceGroups;
    }

    // add one workplacegroup
    public void addWorkplaceGroup(String newWorkplaceGroup)
    {
        this.workplaceGroups.add(newWorkplaceGroup);
    }

    //
    /////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////
    // Entwicklungsziele (DevlopementGoals section)

    // return list of developmentgoals users belongs to
    public String getCurrentWorkstate()
    {
        return this.currentWorkstate;
    }

    // add one workplacegroup
    public void setCurrentWorkstate(String userCurrentWorkstate)
    {
        this.currentWorkstate = userCurrentWorkstate;
    }

    //
    /////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////
    // Entwicklungsziele (DevlopementGoals section)

    // @return set with all given developmentgoals for user
    public Set<String> getDevelopmentGoals()
    {
        Set<String> allDGs = new HashSet<String>();
        allDGs.add(this.developmentGoals.getPosition());
        allDGs.addAll(this.developmentGoals.getContents());
        allDGs.addAll(this.developmentGoals.getItems());
        return allDGs;
    }

    // @return EmployeeDevelopmentgoals object
    public EmployeeDevelopmentGoals getDevelopmentGoalsObject()
    {
        return this.developmentGoals;
    }

    // set list of developmentgoals
    public void setDevelopmentGoals(EmployeeDevelopmentGoals userDevelopementGoals)
    {
        this.developmentGoals = userDevelopementGoals;
    }

    // add one workplacegroup
    public void addDevelopmentGoals(String newDevelopmentGoal)
    {
        this.workplaceGroups.add(newDevelopmentGoal);
    }

    ///////////////////////////////////////////////////////////
    // Prozesse

    // @return set with all given developmentgoals for user
    private Set<String> masteredProcesses = new HashSet<String>();

    public Set<String> getMasteredProcesses()
    {
        return this.masteredProcesses;
    }

    // set list of developmentgoals
    public void setMasteredProcesses(Collection<String> masteredProcessesCollection)
    {
        if (null != masteredProcesses) {
            this.masteredProcesses.addAll(masteredProcessesCollection);
        }
    }

    // add one workplacegroup
    public void addMasteredProcess(String processId, EventBus eb)
    {
        if (null != processId && !processId.equals("")) {
        	if (processId.startsWith("http://www.appsist.de/ontology")){
        		addMasteredFullProcessId(processId);
        	} else{
                JsonObject request = new JsonObject();

                final String sparqlQueryForProcessId = "PREFIX app: <http://www.appsist.de/ontology/> PREFIX terms: <http://purl.org/dc/terms/> "
                        + " SELECT DISTINCT ?uri WHERE { ?uri a ?_ FILTER (REGEX(str(?uri),'"
                        + "')) }";
                //log.debug("sparqlQueryForProcessId: "+ sparqlQueryForProcessId);
                JsonObject sQuery = new JsonObject();
                sQuery.putString("query", sparqlQueryForProcessId);
                request.putObject("sparql", sQuery);

                eb.send(eventbusPrefix + "requests:semwiki", request,
                        new Handler<Message<String>>()
                        {
                            public void handle(Message<String> reply)
                            {
                        List<String> foundProcessIds = new ArrayList<String>();
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(reply.body());
                            if (null != root) {
                                foundProcessIds = root.findValuesAsText("value");
                            }
                            if (!foundProcessIds.isEmpty()) {
                                String fullProcessId = foundProcessIds.get(0);
                                addMasteredFullProcessId(fullProcessId);
                            }
                        }
                        catch (Exception e) {
                            //e.printStackTrace();
                        	log.error(sparqlQueryForProcessId);
                        	log.error("Erroneous result from sparqlconnector:" + reply.body());
                        }
                    }
                });
        	}
        		


        }
    }

    private void addMasteredFullProcessId(String fullProcessId)
    {
        this.masteredProcesses.add(fullProcessId);
    }
    /////////////////////////////////////////////////////////////

    // takes a list of measures and returns list of cleared measures
    public ArrayList<String> filterMeasures(ArrayList<String> measureListToFilter)
    {
        ArrayList<String> resultList = new ArrayList<String>();
        for (String measureId : measureListToFilter) {
            if (this.measureClearance.contains(measureId)
                    || this.measureClearancesWithAssistance.contains(measureId)) {
                resultList.add(measureId);
            }
        }
        return resultList;
    }

    ///////////////////////////////////////////////////////////////

    // store user in database
    public void persistUser(String mongoPersistorAddress, EventBus eb)
    {
        // load employees from MongoDB collection usermodels
        MongoDBConnector mdbcon = new MongoDBConnector(mongoPersistorAddress, eb);
        // store mastered processes
        JsonObject criteriaObject = new JsonObject();
        criteriaObject.putString("id", this.getId());
        for (String masteredProcessId : this.masteredProcesses) {
            JsonObject newObject = new JsonObject();
            JsonObject newValueObject = new JsonObject();
            newValueObject.putString("masters", masteredProcessId);
            newObject.putObject("$addToSet", newValueObject);
            log.info("Storing new processId in MongoDB:" + masteredProcessId);
            mdbcon.update("usermodels", criteriaObject, newObject, false, false, null);
        }
    }
    
    // get information for 'Wissensaneignung'
    private List<String> knowledgeItems = null;
    private List<String> processItems = null;
    
    public void setKnowledgeItems(List<String> itemList){
    	knowledgeItems = new ArrayList<String>();
    	if (itemList.size()>0){
    		knowledgeItems.addAll(itemList);
    	}  
    }
    
    private List<String> getKnowledgeItems(){
    	return knowledgeItems;
    }
    
    public Map<String,Integer> getKnowledgeInformation(){
    	HashMap<String,Integer> knowledgeInformationMap = new HashMap<String,Integer>();
    	for(String knowledgeItem: knowledgeItems){
    		knowledgeInformationMap.put(knowledgeItem, getNumContentViews(knowledgeItem));
    	}
    	for(String knowledgeItem: processItems){
    		knowledgeInformationMap.put(knowledgeItem, getNumMeasurePerformed(knowledgeItem));
    	}
    	knowledgeInformationMap.put("StationMontage", interactedWithStationMontage);
    	knowledgeInformationMap.put("Pneumatikzylinder", interactedWithPneumatikZylinder);
    	
    	return knowledgeInformationMap;
    }

	public void setProcessItems(List<String> processItemList) {
		// TODO Auto-generated method stub
		processItems = new ArrayList<String>();
    	if (processItemList.size()>0){
    		processItems.addAll(processItemList);
    	}
	}
   
	private int interactedWithStationMontage = 0;
	private int interactedWithPneumatikZylinder = 0;
	
	public void presetKnowledgeEstimation(){
		// Übersicht Funktion der Roboterzelle
		this.numContentViewed.put("b2280e82-2d6f-4888-9edc-6cf5ccc8bd08",4);
		// Montageprozess in Roboterzelle
		this.numContentViewed.put("c4bc9f83-aeea-4680-8c79-488b0e7c6e49",1);
		// Im Detail: Funktion der Roboterzelle
		this.numContentViewed.put("59fc26ff-6339-4f6b-85c3-dfe5914b0f83",2);
		this.numContentViewed.put("file:///static/externalContent/7_Basiswissen_Pneumatik_HM/index.html?skipcheck",3);
		this.numContentViewed.put("file:///static/externalContent/WissenselementZelleGesamtExperte.pdf",1);
		this.numMeasuresPerformed.put("b55be936-8192-42bc-b7f3-cef947b6f4ce", 4);
		this.numMeasuresPerformed.put("ba66718d-607c-4c81-82d7-f1a55fbbef3f", 2);
		this.interactedWithPneumatikZylinder=2;
		this.interactedWithStationMontage=6;
	}
}
