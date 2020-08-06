import eu.miaplatform.customplugin.springboot.model.*;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;


import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;

@Service
public class TripService {
   

    public List<Trip> getTrips() {

        logger.info("getting all the trips...");
        return tripRepository.findAll();

    }

    

    /**
     * 
     *<p>This function forwards the received data to the first part of the detection algorithm</p>
     * @param userId
     *          the id of the user who did the trip
     * @param ticketId
     *          the id of the ticket which was used to make the trip
     * @param tripData
     *          the data of the trip
     *
     */

    public void handleTripData(String userId, String ticketId, TripData tripData) {
            if(tripData.locations != null && !tripData.locations.isEmpty()){
                Map<String,Object> roadDataMap = null;

                if (tripData.locations.size() > 100)
                {
                    roadDataMap = processLongPath(tripData);
                }
                else {
                    roadDataMap = snappedToRoads(tripData);
                }

                TripRoad tripRoad = null;
                if (!roadDataMap.isEmpty()) {
                    if(roadDataMap.containsKey("snappedPoints")){
                        logger.info("successfully received snapped points from googleAPI");
                    }
                    else if(roadDataMap.containsKey("warningMessage")){
                        logger.info("warningMessage from googleAPI: " + roadDataMap.get("warningMessage").toString());
                    }
                    else if(roadDataMap.containsKey("error")){
                        logger.info("error from googleAPI: " + roadDataMap.get("error").toString());
                    }
                }
                else{
                    logger.info("received no data (empty map) from googleAPI");
                }
                tripRoad = new TripRoad(tripData.startDate, tripData.endDate, tripData.getLocationsAsHashMap(),roadDataMap);
                logger.info(tripRoad.toString());

                
                }
            else
            {
                logger.info("TripData data is empty, check the data in the request body");
            }



    }

    /**
     * Sends the trip data to the Google Road API to snap the GPS point
     *<p>This function sends raw GPS data to Google Road API to get an accurate road path</p>
     *
     * @param tripData
     *          the data of the trip
     *
     *
     */
    private static Map<String,Object> snappedToRoads(TripData tripData){

        String url="https://roads.googleapis.com/v1/snapToRoads?path="+tripData.toString()+"&interpolate=true&key=yourAPIKey";

        Map<String,Object> roadDataMap = null;
        JsonParser springParser = JsonParserFactory.getJsonParser();

        try{
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            roadDataMap = springParser.parseMap(response);
            // logger.info(roadDataMap.toString());
        }catch(HttpClientErrorException e){
            logger.info("StatusCode: " + e.getStatusCode());
            logger.info("ResponseBodyAsString: " + e.getResponseBodyAsString());
            roadDataMap = springParser.parseMap(e.getResponseBodyAsString());
        }catch(HttpServerErrorException e){
            logger.info("StatusCode: " + e.getStatusCode());
            logger.info("ResponseBodyAsString" + e.getResponseBodyAsString());
            roadDataMap = springParser.parseMap(e.getResponseBodyAsString());
        }catch(UnknownHttpStatusCodeException e){
            logger.info("RawStatusCode: " + e.getRawStatusCode());
            logger.info("getResponseBodyAsString: " + e.getResponseBodyAsString());
            roadDataMap = springParser.parseMap(e.getResponseBodyAsString());
        }
        return roadDataMap;
    }


    /**
     * The google Road api has a limit to take only 100 coordinates.
     * To process long paths we send multiple request to the api.
     * And append each response value and create a json map that is similar to the response of each request to the api.
     * @param tripData
     *              has the coordinates of the user
     *<p>
     *  Send a temporary sublist to snappedToRoads function and appends each request in a StringBuilder.
     *  If for any one response we don't get snappedPoints then we return that message.
     * </p>
     *
     * @return a JSON object map that has snappedPoints, or errorMessage, or a warningMessage
     */

    private static Map<String,Object> processLongPath(TripData tripData){

        Map<String,Object> eachDataMap = null;
        Map< String,Object> roadDataMap = new HashMap< String,Object>();
        int flag = 0;

        StringBuilder locationString = new StringBuilder();
        int size = tripData.locations.size();

        logger.info("inside processLongPath ");

        for(int i= 0; i<= size; i = i+100)
        {
            TripData tempTripData = new TripData();
            tempTripData.locations = tripData.locations.subList(i,Math.min(i+100,size));

            eachDataMap = snappedToRoads(tempTripData);
            if(eachDataMap.containsKey("snappedPoints"))
            {
                locationString.append(eachDataMap.get("snappedPoints").toString().replaceAll("\\[", "").replaceAll("\\]","")+", ");
            }
            else
            {
                //error found; exit the for loop and send this instance of eachDataMap
                flag = 1;
                break;
            }
        }
        if (flag == 0)
        {
            locationString.delete(locationString.length() - 2, locationString.length());
            roadDataMap.put("snappedPoints","["+locationString+"]");
            // logger.info("final roadDataMap "+ roadDataMap);
        }
        else
        {
            roadDataMap = new HashMap<String,Object>(eachDataMap);
        }

        return roadDataMap;
    }

    
}
