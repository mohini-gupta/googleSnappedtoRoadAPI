

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import eu.miaplatform.customplugin.springboot.model.*;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"endDate",
"startDate",
"locations"
})
public class TripData {

	@JsonProperty("endDate")
	public Double endDate;
	@JsonProperty("startDate")
	public Double startDate;
	@JsonProperty("locations")
	public List<Location> locations = null;

	@Override
    public String toString() {
    	String locationsString = "";
    	for(Location l : locations) {
    		locationsString = locationsString + l.toString() + "|";
    	}
    	locationsString = locationsString.substring(0, locationsString.length() - 1);

    	return locationsString;
    }

    public List<Map<String, Object>> getLocationsAsHashMap() {
		return locations
				.stream()
				.map(location -> location.coordinate)
				.map(coordinate -> {
					Map<String, Object> map = new HashMap<>();
					map.put("location", coordinate);
					return map;
				})
				.collect(Collectors.toList());
	}


}


