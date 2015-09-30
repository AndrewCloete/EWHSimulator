package acza.sun.ee;

public class Thermostat {
	
	private double high_setpoint;
	private double low_setpoint;
	private boolean element_state;
	
	public Thermostat(double low_setpoint, double high_setpoint){
		this.high_setpoint = high_setpoint;
		this.low_setpoint = low_setpoint;
		this.element_state = false;
	}

	
	public boolean elementState(double temperature){
		if(temperature > high_setpoint){
			this.element_state = false;
			return false;
		}
		else if(temperature < low_setpoint){
			this.element_state = true;
			return true;
		}
		else
			return this.element_state;
	}
	
	//----------------------------------------------------- GETTERS AND SETTER-----------------------------------------------
	public double getHighSetpiont() {
		return high_setpoint;
	}

	public void setHighSetpiont(double high_setpoint) {
		this.high_setpoint = high_setpoint;
	}

	public double getLowSetpoint() {
		return low_setpoint;
	}

	public void setLowSetpoint(double low_setpoint) {
		this.low_setpoint = low_setpoint;
	}
	
	

}