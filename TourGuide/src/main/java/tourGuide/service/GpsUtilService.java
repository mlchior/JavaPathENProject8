package tourGuide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.stereotype.Service;
import tourGuide.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GpsUtilService {
    private static final Logger logger = LoggerFactory.getLogger(GpsUtilService.class);


    private GpsUtil gpsUtil;

    private ExecutorService executor = Executors.newFixedThreadPool(10000);

    public GpsUtilService() {
        gpsUtil = new GpsUtil();
    }

    public List<Attraction> getAttractions() {
        return gpsUtil.getAttractions();
    }
    public VisitedLocation getUserLocation(UUID userId) {
        VisitedLocation visitedLocation;
        try {
            visitedLocation = gpsUtil.getUserLocation(userId);
        } catch (NumberFormatException nfe) {
            visitedLocation = null;
        }
        return visitedLocation;
    }


    public void trackUserLocationAsync(User user, TourGuideService tourGuideService) {
        CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), executor)
                .thenAccept(visitedLocation -> {
                    if (visitedLocation != null) {
                        tourGuideService.trackUserLocation(user);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error tracking user location", ex);
                    return null;
                });
    }

}