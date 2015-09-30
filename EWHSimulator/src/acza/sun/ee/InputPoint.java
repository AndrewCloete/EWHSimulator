package acza.sun.ee;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class InputPoint{


	private static final Logger logger = LogManager.getLogger(InputPoint.class);

	public final static String CSV_HEADER = "timestamp, t_inlet, t_ambient, water_usage, power_usage";

	public final long timestamp;
	public final double t_inlet;
	public final double t_ambient;
	public final double water_usage;


	public InputPoint(long timestamp, double t_inlet, double t_ambient, double water_usage){

		this.timestamp = timestamp;
		this.t_inlet = t_inlet;
		this.t_ambient = t_ambient;
		this.water_usage = water_usage;
	}
	
	
	public static LinkedList<InputPoint> importInputFromJSONFile(String filepath){
		LinkedList<InputPoint> input_points = new LinkedList<InputPoint>();

		try {
			//Read file and decode JSON object.
			byte[] encoded = Files.readAllBytes(Paths.get(filepath.trim()));
			String json_object_str = new String(encoded, StandardCharsets.UTF_8);
			JSONObject json_object = new JSONObject(json_object_str);
			JSONArray json_usage_dataset = (JSONArray) json_object.get("dataset");
			logger.info("Read file with GeyserID: " + json_object.get("geyser_id") + " and dataset length: " + json_usage_dataset.length());

			//Traverse all data entries in JSON object and populate usage list
			SimpleDateFormat sdf  = new SimpleDateFormat("yy-MM-dd kk:mm:ss");
			for(int i = 0; i < json_usage_dataset.length(); i++){
				JSONObject json_datapoint = (JSONObject) json_usage_dataset.get(i);
				try {
					Date timestamp = sdf.parse((String)json_datapoint.get("timestamp"));
					double t_inlet = (Double)json_datapoint.get("t_inlet");
					double t_ambient = (Double)json_datapoint.get("t_ambient");			
					double water_usage = (Double)json_datapoint.get("water_usage");


					input_points.add(new InputPoint(timestamp.getTime()/1000L, t_inlet, t_ambient, water_usage));
				} catch (ParseException e) {
					logger.error("Unable to parse JSON.",e);
				}
			}

		} catch (IOException e) {
			logger.error("Unable to read/locate JSON file", e);
		} catch (JSONException e){
			logger.error("Corrupt JSON", e);
		}

		return input_points;
	}
}
