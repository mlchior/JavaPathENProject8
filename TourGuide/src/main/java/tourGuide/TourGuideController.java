package tourGuide;

import java.util.*;
import java.util.stream.Collectors;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    RewardsService rewardsService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }
    
    //  TODO: Change this method to no longer return a List of Attractions.
 	//  Instead: Get the closest five tourist attractions to the user - no matter how far away they are.
 	//  Return a new JSON object that contains:
    	// Name of Tourist attraction, 
        // Tourist attractions lat/long, 
        // The user's location lat/long, 
        // The distance in miles between the user's location and each of the attractions.
        // The reward points for visiting each Attraction.
        //    Note: Attraction reward points can be gathered from RewardsCentral
    @RequestMapping("/getNearbyAttractions")
    public List<Map<String, Object>> getNearbyAttractions(@RequestParam String userName) {
        User user = tourGuideService.getUser(userName);
        VisitedLocation visitedLocation = tourGuideService.getUserLocation(user);

        List<Map<String, Object>> nearbyAttractions = new ArrayList<>();
        List<Attraction> attractions = tourGuideService.getNearByAttractions(visitedLocation);

        // Limit to the closest five attractions
        attractions = attractions.stream()
                .sorted(Comparator.comparing(attraction ->
                        rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location)))
                .limit(5)
                .collect(Collectors.toList());

        for (Attraction attraction : attractions) {
            Map<String, Object> attractionDetails = new HashMap<>();
            attractionDetails.put("attractionName", attraction.attractionName);
            // Create Location Map for Attraction
            Map<String, Double> attractionLocation = new HashMap<>();
            attractionLocation.put("latitude", attraction.latitude);
            attractionLocation.put("longitude", attraction.longitude);

            // Create Location Map for User
            Map<String, Double> userLocation = new HashMap<>();
            userLocation.put("latitude", visitedLocation.location.latitude);
            userLocation.put("longitude", visitedLocation.location.longitude);

            attractionDetails.put("attractionLocation", attractionLocation);
            attractionDetails.put("userLocation", userLocation);
            attractionDetails.put("distance", rewardsService.getDistance(new Location(attraction.latitude, attraction.longitude), visitedLocation.location));
            attractionDetails.put("rewardPoints", rewardsService.getRewardPoints(attraction, user));

            nearbyAttractions.add(attractionDetails);
        }

        return nearbyAttractions;
    }

    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }
    
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
    	// TODO: Get a list of every user's most recent location as JSON
    	//- Note: does not use gpsUtil to query for their current location, 
    	//        but rather gathers the user's current location from their stored location history.
    	//
    	// Return object should be the just a JSON mapping of userId to Locations similar to:
    	//     {
    	//        "019b04a9-067a-4c76-8817-ee75088c3822": {"longitude":-48.188821,"latitude":74.84371} 
    	//        ...
    	//     }

        // Get a list of every user
        List<User> users = tourGuideService.getAllUsers();

        // Create a map to store the user locations
        Map<String, Map<String, Double>> userLocations = new HashMap<>();

        // For each user, get the most recent location
        for(User user : users) {
            VisitedLocation lastLocation = user.getVisitedLocations().get(user.getVisitedLocations().size()-1);
            // Create a location map
            Map<String, Double> locationMap = new HashMap<>();
            locationMap.put("longitude", lastLocation.location.longitude);
            locationMap.put("latitude", lastLocation.location.latitude);
            // Put this in the main map
            userLocations.put(user.getUserId().toString(), locationMap);
        }

        // Serialize and return
        return JsonStream.serialize(userLocations);
    }
    
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
    	return JsonStream.serialize(providers);
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}