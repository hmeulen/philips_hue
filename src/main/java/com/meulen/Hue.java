package com.meulen;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;


class Hue {
	
	private static final Logger logger = LoggerFactory.getLogger(Hue.class);
    private static final GpioController gpio = GpioFactory.getInstance();
    private static final GpioPinDigitalOutput pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "GardenLight", PinState.HIGH);
    private static final GpioPinDigitalOutput pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Christmas", PinState.HIGH);
  
	private static boolean isMorning() {
		LocalDateTime today = LocalDateTime.now();
		ZonedDateTime dsttime = today.atZone(ZoneId.of("Europe/Amsterdam"));
		boolean isMorning = dsttime.getHour() < 12 ? true : false;
		return (isMorning);
	}

	private static JsonElement getTimesFromApi() throws Exception {
		URL url = new URL("https://api.sunrise-sunset.org/json?lat=51.733602&lng=5.317321&date=today");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		con.setFollowRedirects(false);
		int status = con.getResponseCode();
		System.out.format("Status code: %d%n",status);
		if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
    		String location = con.getHeaderField("Location");
    		URL newUrl = new URL(location);
    		con = (HttpURLConnection) newUrl.openConnection();
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
    		content.append(inputLine);
		}
		in.close();
		con.disconnect();
		JsonParser jsonParser = new JsonParser();
		JsonElement jsonTree = jsonParser.parse(content.toString());
		return(jsonTree);
	}

	private static boolean checkJson(JsonElement je1){
		if(je1.isJsonObject()) {
			JsonObject jo1 = je1.getAsJsonObject();
			JsonElement je2 = jo1.get("results");
			if(je2.isJsonObject()){
				return(true);
			} else {
				return(false);
			}
		} else {
			return(false);
		}
	}

	private static String getSunRiseSet(JsonElement je1, String s1) {
		JsonObject jo1 = je1.getAsJsonObject();
		JsonElement je2 = jo1.get("results");
		JsonObject jo2 = je2.getAsJsonObject();
		JsonElement je3 = jo2.get(s1);
		return(je3.toString());
	}

	private static JsonElement getStatusHueLights() throws Exception {
		URL url = new URL("http://10.0.1.6/api/5u2fOf-xNDOUJS4tuhacnhihv39KlxSbspDHlOzM/lights");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		con.setFollowRedirects(false);
		int status = con.getResponseCode();
		logger.info("Status code getStatusHueLights: " + status);
		if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
    		String location = con.getHeaderField("Location");
    		URL newUrl = new URL(location);
    		con = (HttpURLConnection) newUrl.openConnection();
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
    		content.append(inputLine);
		}
		in.close();
		con.disconnect();
		JsonParser jsonParser = new JsonParser();
		JsonElement jsonTree = jsonParser.parse(content.toString());
		return(jsonTree);
	}

	private static HashMap<Integer, String> getStatusHueLight(JsonElement je, Integer[] ar) {
		HashMap<Integer, String> statusHue = new HashMap<>();
		for (Integer i1 : ar) {
			JsonObject jo1 = je.getAsJsonObject();
			JsonElement je1 = jo1.get(i1.toString());
			JsonObject jo2 = je1.getAsJsonObject();
			JsonElement je2 = jo2.get("state");
			JsonObject jo3 = je2.getAsJsonObject();
			JsonElement je3 = jo3.get("on");
			statusHue.put(i1, je3.toString());
		}
	    return(statusHue);
	}

	private static ZonedDateTime getDate(String s1) throws Exception {
		LocalDateTime today = LocalDateTime.now();
		ZoneId systemZone = ZoneId.systemDefault();
        ZoneOffset currentOffsetForMyZone = systemZone.getRules().getOffset(today);
        LocalTime sun1 = LocalTime.parse(s1, DateTimeFormatter.ofPattern("K:mm:ss a"));
        LocalDateTime sun2 = LocalDateTime.of(LocalDate.now(), sun1);
        ZonedDateTime zdtSun1 = sun2.atZone(ZoneId.of("UTC"));
		ZonedDateTime zdtSun3 = zdtSun1.withZoneSameInstant(currentOffsetForMyZone);
		return (zdtSun3);
    }

	private static void putLight(JsonObject hueProperty, Integer lightNr) throws Exception {
		URL url1 = new URL("http://10.0.1.6/api/5u2fOf-xNDOUJS4tuhacnhihv39KlxSbspDHlOzM/lights/" + lightNr + "/state");
		HttpURLConnection con1 = (HttpURLConnection) url1.openConnection();
		con1.setRequestMethod("PUT");
		con1.setDoOutput(true);
		con1.setRequestProperty("Content-Type", "application/json");
		con1.setRequestProperty("Accept", "application/json");
		con1.setConnectTimeout(5000);
		con1.setReadTimeout(5000);
        OutputStreamWriter osw = new OutputStreamWriter(con1.getOutputStream());
		osw.write(hueProperty.toString());
        osw.flush();
        osw.close();
		logger.info("putLight status: " + con1.getResponseCode());
	}

	private static void sleepPeriod(int sleep) throws Exception {
		ZonedDateTime dsttime = LocalDateTime.now().atZone(ZoneId.of("Europe/Amsterdam"));
		ZonedDateTime zdt = dsttime.plusSeconds(sleep);
		Duration d1 = Duration.between(dsttime, zdt);
		Thread.sleep(d1.toMillis());
	}

	private static void sleepUntil(ZonedDateTime zdt, int delay, String s) throws Exception {
		ZonedDateTime dsttime = LocalDateTime.now().atZone(ZoneId.of("Europe/Amsterdam"));
		Duration d1 = null;
		if (s.equals("minus")) {
			d1 = Duration.between(dsttime, zdt.minusSeconds(delay));
		}
		if (s.equals("plus")) {
			d1 = Duration.between(dsttime, zdt.plusSeconds(delay));
		}
		if(!d1.isNegative()) {
			logger.info("Sleeping period: " + d1.toString());
			Thread.sleep(d1.toMillis());
		}
	}

	private static void configureHue(Integer[] a, Boolean on) throws Exception {
		JsonElement jeStatusHueLights = getStatusHueLights();
		HashMap<Integer, String> statusHueLight = getStatusHueLight(jeStatusHueLights, a);
		for (Integer i1 : a) {
			String status = statusHueLight.get(i1);
			logger.info("Status Hue light: " + i1 + " : " + status);
			if (status.equals(on.toString())) {
				logger.info("Status Hue light " + i1 + " already " + on.toString());
			} else {
				JsonObject hueSettings = setHueLightProperty(i1,on);
				logger.info("Property Hue light " + i1 + " : " + hueSettings.toString());
				putLight(hueSettings,i1);
			}
			Thread.sleep(1000);
		}
	}

	private static JsonObject setHueLightProperty(Integer lightNr, Boolean on) {
		JsonObject hueProperty = new JsonObject();
		switch(lightNr) {
			case 5:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",164);
				hueProperty.addProperty("on",on);
				break;
			case 6:
				hueProperty.addProperty("hue",7247);
				hueProperty.addProperty("bri",130);
				hueProperty.addProperty("sat",222);
				hueProperty.addProperty("on",on);
				break;
			case 7:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",164);
				hueProperty.addProperty("on",on);
				break;
			case 8:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",164);
				hueProperty.addProperty("on",on);
				break;
			case 9:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",128);
				hueProperty.addProperty("on",on);
				break;
			case 10:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",190);
				hueProperty.addProperty("on",on);
				break;
			case 11:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",254);
				hueProperty.addProperty("on",on);
				break;
			case 12:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",254);
				hueProperty.addProperty("on",on);
				break;
			case 13:
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("bri",254);
				hueProperty.addProperty("on",on);
				break;
			case 14 :
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("br",200);
				hueProperty.addProperty("on",on);
				break;
			case 15 :
				hueProperty.addProperty("ct",500);
				hueProperty.addProperty("br",200);
				hueProperty.addProperty("on",on);
				break;
		}
		return(hueProperty);
	}

	private static int getRandom(int min, int max) {
        int i1 = (int) (Math.random() * ((max - min) + 1)) + min;
        return (i1);
	}

	private static int getDelay(ZonedDateTime zdt) {
		int delay = 0;
		if(zdt.getDayOfWeek().getValue() == 5 | zdt.getDayOfWeek().getValue() == 6) {
			delay = getRandom(2700,3600);
		} else {
			delay = getRandom(900,1800);
		}
		logger.info("Delay is (" + zdt.getDayOfWeek().getValue() + "): " + delay + " seconds");
		return(delay);
	}
	
	private static void lightsGpio(boolean statePin) throws InterruptedException {
        if (statePin) {
            pin0.low();
            logger.info("GPIO pin0 state: ON");
            Thread.sleep(2000);
            pin2.low();
            logger.info("GPIO pin2 state: ON");
        } else {
            pin0.high();
            logger.info("GPIO pin0 state: OFF");
            Thread.sleep(2000);
            pin2.high();
            logger.info("GPIO pin2 state: OFF");
        }
	}
	
	private static boolean checkStart(ZonedDateTime sunRise) {
		ZonedDateTime dsttime = LocalDateTime.now().atZone(ZoneId.of("Europe/Amsterdam"));
		boolean b1 = Duration.between(dsttime, sunRise).getSeconds() > 600 ? true : false;
		return(b1);
	}
	
	public static void main(String [] args) throws Exception {
		logger.info("Hue started");
		JsonElement sunSetsunRise = getTimesFromApi();
		String sunRise;
		String sunSet;
		if(checkJson(sunSetsunRise)) {
			sunRise = getSunRiseSet(sunSetsunRise, "sunrise");
			sunSet = getSunRiseSet(sunSetsunRise, "sunset");
			sunRise = sunRise.replace("\"", "");
			sunSet = sunSet.replace("\"", "");
		} else {
			sunRise = "7:15:00 AM";
			sunSet = "4:00:00 PM";
		}
		ZonedDateTime zdtSunRise = getDate(sunRise);
		ZonedDateTime zdtSunSet = getDate(sunSet);
		logger.info("Sunrise: " + zdtSunRise);
		logger.info("Sunset: " + zdtSunSet);
				
		Integer[] hueLightsArray1 = {14,15};
		Integer[] hueLightsArray2 = {6,7,8,9,10,11,12,13};
		Integer[] hueLightsArray3 = {6,7,8,9,10,11,12,13,14,15};
		
		String plus = "plus";
		String minus = "minus";
		//boolean morning = isMorning();
		if(isMorning() && checkStart(zdtSunRise)) {
			logger.info("Is morning and lights on for more than 600 seconds");
			sleepPeriod(getRandom(1,600));
			configureHue(hueLightsArray1,true);
			lightsGpio(true);
			sleepPeriod(getRandom(600,1200));
			configureHue(hueLightsArray2,true);
			sleepUntil(zdtSunRise, 1, minus);
			configureHue(hueLightsArray3,false);
			lightsGpio(false);
		} else {
			logger.info("Is afternoon");
			sleepUntil(zdtSunSet,getRandom(900,1500), minus);
			configureHue(hueLightsArray2,true);
			sleepUntil(zdtSunSet,getRandom(900,1200), plus);
			configureHue(hueLightsArray1,true);
			lightsGpio(true);
			ZonedDateTime lightsOffInside = LocalDate.now().atTime(23, 00, 00).atZone(ZoneId.of("Europe/Amsterdam"));
			int delay = getDelay(lightsOffInside);
			sleepUntil(lightsOffInside,delay,plus);
			configureHue(hueLightsArray2,false);
			ZonedDateTime lightsOffOutside = LocalDateTime.now().atZone(ZoneId.of("Europe/Amsterdam"));
			sleepUntil(lightsOffOutside,delay,plus);
			configureHue(hueLightsArray1,false);
			lightsGpio(false);
		}
		logger.info("Hue finished");
	}
}
