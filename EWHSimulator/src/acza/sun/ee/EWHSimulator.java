package acza.sun.ee;


import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import acza.ee.sun.geyserM2M.Geyser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import fr.lissi.belilif.om2m.model.Container;
import fr.lissi.belilif.om2m.model.ContentInstance;
import fr.lissi.belilif.om2m.model.ContentInstancesResponse;
import fr.lissi.belilif.om2m.model.app.Application;
import fr.lissi.belilif.om2m.oao.ApplicationManager;
import fr.lissi.belilif.om2m.oao.ContainerManager;
import fr.lissi.belilif.om2m.oao.ContentInstanceManager;
import fr.lissi.belilif.om2m.oao.Om2mManagersFactorty;





public class EWHSimulator {

	private static final Logger logger = LogManager.getLogger(EWHSimulator.class);
	
	private static String BASE_URL;
	private static String BASIC_REQENTITY;
	private static String APP_ID;
	private static String DATA_CONTAINER_ID = "DATA";
	private static String SETTINGS_CONTAINER_ID = "SETTING";
	
	private static double THERMAL_RESISTANCE;
	private static double TANK_VOLUME;
	private static double START_TEMPERATURE;
	private static double HIGH_SETPOINT;
	private static double LOW_SETPOINT;
	private static double ELEMENT_POWER_KW;
	
	private static String JSON_INPUT_FILEPATH;
	
	private static LinkedList<InputPoint> input_points = null;
	
	
	
	public static void main(String[] args) {

		
		// ---------------------- Reading and sanity checking configuration parameters -------------------------------------------
    	Properties configFile = new Properties();
    	try {
    		configFile.load(EWHSimulator.class.getClassLoader().getResourceAsStream("config.properties"));
    		
    		BASE_URL = configFile.getProperty("BASE_URL");
    		BASIC_REQENTITY = configFile.getProperty("BASIC_REQENTITY");
    		APP_ID = configFile.getProperty("APP_ID");
			
    		THERMAL_RESISTANCE = new Double(configFile.getProperty("THERMAL_RESISTANCE").trim());
    		TANK_VOLUME = new Double(configFile.getProperty("TANK_VOLUME").trim());
    		START_TEMPERATURE = new Double(configFile.getProperty("START_TEMPERATURE").trim());
    		HIGH_SETPOINT = new Double(configFile.getProperty("HIGH_SETPOINT").trim());
    		LOW_SETPOINT = new Double(configFile.getProperty("LOW_SETPOINT").trim());
    		ELEMENT_POWER_KW = new Double(configFile.getProperty("ELEMENT_POWER_KW").trim());
    		
    		JSON_INPUT_FILEPATH = configFile.getProperty("JSON_INPUT_FILEPATH");

    	} catch (IOException e) {
    		logger.fatal("Error in interpereting configuration file \"config.properties\"", e);
    		return;
    	} 
    	//-------------------------------------------------------------------------------------------------------
    	logger.info("EWH simulator started with parameters: " + configFile.toString());
		
		
		try
		{
			Om2mManagersFactorty.configure(BASE_URL, BASIC_REQENTITY);
	
			
			//Create the application
			ApplicationManager appManager = (ApplicationManager)   Om2mManagersFactorty.getManager(Om2mManagersFactorty.APP_MANAGER);
			Application myApp1 = new Application(APP_ID);
			
			if(!appManager.exist(myApp1))
				appManager.create(myApp1);
			
			
			//Create DATA container
			ContainerManager containerManager = (ContainerManager) Om2mManagersFactorty.getManager(Om2mManagersFactorty.CONTAINER_MANAGER);
			Container data_container = new Container(APP_ID, DATA_CONTAINER_ID);
			if(!containerManager.exist(data_container))
				containerManager.create(data_container);
	
				
				ContentInstanceManager contentInstanceManager = (ContentInstanceManager) Om2mManagersFactorty.getManager(Om2mManagersFactorty.CONTENT_INSTANCE_MANAGER);
			
	    	
	    	//Create and initialise new Geyser object
	    	Geyser ewh = new Geyser(THERMAL_RESISTANCE, TANK_VOLUME, START_TEMPERATURE);
	    	Thermostat thermostat = new Thermostat(LOW_SETPOINT, HIGH_SETPOINT);
	    	
	    	while(true){
	    		
	    		input_points = InputPoint.importInputFromJSONFile(JSON_INPUT_FILEPATH);
		
	    		//Create iterators.
	        	ListIterator<InputPoint> input_iterator = input_points.listIterator();
	        	
	        	//Read first datapoint
	        	InputPoint point = input_iterator.next();
	    		
	    		
	    		//Iterate through usage points and step simulation
	        	while(input_iterator.hasNext()){
	        		
	        		InputPoint next_point = input_iterator.next();	
	        		ewh.setInletTemperature(point.t_inlet);
	        		ewh.setAmbientTemperature(point.t_ambient);
	        		ewh.stepUsage(point.water_usage);
	        		
	        		long time_delta = next_point.timestamp - point.timestamp;
	        		
	        		if(thermostat.elementState(ewh.getInsideTemperature())){
	        			ewh.stepTime(time_delta, ELEMENT_POWER_KW*1000);
	        		} else{
	        			ewh.stepTime(time_delta, 0);
	        		}
	        		
	        		point = next_point;
	        		
	        		//Create summary of data as JSON string
	        		String json_result = toJSON(point.timestamp, 
	        							ewh.getInsideTemperature(), 
	        							ewh.getInletTemperature(), 
	        							ewh.getAmbientTemperature(),
	        							point.water_usage,
	        							thermostat.elementState(ewh.getInsideTemperature()));
	        		
	        		
	        		ContentInstance contentInstance = new ContentInstance(APP_ID, DATA_CONTAINER_ID, json_result);
	        		contentInstanceManager.create(contentInstance);
	        		
	        		try {
						Thread.sleep(time_delta*1000);
					} catch (InterruptedException e) {
						logger.fatal("Error in main sleep: ", e);
					}
	        	}
	    		
	    		
	    	}
    	
		}catch (Exception e){
			logger.fatal("Fatal error. Ensure that ROUTE to xSCL and JSON input usage file is specified.", e);
			System.exit(0);
		}
    	
    	
		
		
		/*
		
		ContentInstance contentInstance = new ContentInstance(APP_ID, DATA_CONTAINER_ID, "60");
		contentInstanceManager.create(contentInstance);
		
		ContentInstancesResponse cir = contentInstanceManager.get(contentInstance);
		
		String content = null;
		try {
			content = new String(DatatypeConverter.parseBase64Binary(cir.getContent()), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 */

		

	}
	
	private static String toJSON(long timestamp, double t_inside, double t_inlet, double t_ambient, double water_usage, boolean element_state){
		
		DateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
		String stamp = df.format(new Date(timestamp*1000));
		
		
		String json = "{"
						+"\"timestamp\":" + stamp + ", "
						+"\"t_inside\":" + String.format("%.2f",t_inside)+ ", "
						+"\"t_inlet\":" + String.format("%.2f",t_inlet)+ ", "
						+"\"t_ambient\":" + String.format("%.2f",t_ambient)+ ", "
						+"\"water_usage\":" + String.format("%f",water_usage)+ ", "
						+"\"element\":" + String.format("%b",element_state)
						+ "}";
		
		return json;
	}
	

}
