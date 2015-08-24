import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import org.json.*;
import arduino.Arduino;

public class LEDDemo {
	private static boolean LED_ON = false;
	public static void main(String[] args) throws UnknownHostException, IOException, JSONException {
		double threshold = 0.5;
		Arduino ard = new Arduino("/dev/cu.usbserial-AD01VFL0"); //change this
		//connect the Arduino
		ard.connect();
		//set the pinmode
		ard.pinMode(13, 'o'); // <--- new
		String params;
		String JSONResponse;
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

		//run API_Main for the EPOC server socket
		try {
			Thread t = new Thread(new API_Main());
			t.start();

			//connect to the EPOC server socket
			Socket clientSocket = new Socket("localhost", 2222);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

			//get user param and use that to turn on the LED and send to server
			System.out.println("Enter 2 EEG events to control LED (separated by commas): ");
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			params = inFromUser.readLine();
			outToServer.writeBytes(params + '\n');
			String[] tokens = params.split(", ");

			//fix this to terminate nicely, current setup causes stale lockfile
			if(ard.isConnected()) {
				while ((JSONResponse = inFromServer.readLine()) != null) {
					JSONObject obj = new JSONObject(JSONResponse);
					//System.out.println(obj); //debug
					for (String token : tokens) {
						//for expressiv and affectiv events, which are contained in JSONArrays
						if (API_Main.getAffectivMap().containsKey(token) || API_Main.getExpressivMap().containsKey(token)){
							JSONArray array = (API_Main.getAffectivMap().containsKey(token)) ? 
									obj.getJSONObject("EmoStateData").getJSONArray("Affectiv") : 
										obj.getJSONObject("EmoStateData").getJSONArray("Expressiv");
							for (int i = 0; i < array.length(); i++) {
								double param_val = array.getJSONObject(i).getDouble(token);
								if (param_val > threshold && token == tokens[0]) {
									//turn on LED
									LEDSwitch(ard);
								}
								else if (param_val > threshold && token == tokens[1]) {
									//turn off LED
									LEDSwitch(ard);
								}
							}
						}
						//for cognitiv events, which are contained in a JSONObject
						else if (API_Main.getCogntivMap().containsKey(token)) {
							String cog_action = obj.getJSONObject("EmoStateData").getString("Cognitiv");
							if (cog_action.equals(token)) {
								double param_val = obj.getJSONObject("EmoStateData").getDouble("Cognitiv");
								if (param_val > threshold && token == tokens[0]) {
									//turn on LED
									LEDSwitch(ard);
								}
								else if (param_val > threshold && token == tokens[1]) {
									//turn off LED
									LEDSwitch(ard);
								}
							}
						}
						else {
							System.out.println("Received bad input, enter a valid EEG event.");
							continue;
						}
					}
				}
			}
			//close all resources, disconnect Arduino
			clientSocket.close();
			inFromUser.close();
			inFromServer.close();
			outToServer.close();
			ard.disconnect(); //makes no sense to include this right now
		} catch (Exception e) {
			System.out.println("Could not start EPOC data server socket.");
			e.printStackTrace();
		}
	}

	public static void LEDSwitch(Arduino ard) {
		if (LED_ON) {
			System.out.println("Shutting off LED...");
			ard.digitalWrite(13, false);
		}
		else {
			System.out.println("Turning on LED...");
			ard.digitalWrite(13, true);
		}
		LED_ON = !LED_ON;
	}
}
