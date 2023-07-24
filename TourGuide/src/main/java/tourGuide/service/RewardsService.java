package tourGuide.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.w3c.dom.Attr;
import rewardCentral.RewardCentral;
import tourGuide.user.Position;
import tourGuide.user.User;
import tourGuide.user.UserReward;
/**
 * Service de récompenses pour les utilisateurs basé sur leurs visites d'attractions.
 */
@Service
public class RewardsService {
	private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
	private Logger logger = LoggerFactory.getLogger(RewardsService.class);

	// proximity in miles
	private int proximityMilesBuffer = 10;

	private int attractionProximityRange = 200;

	private ExecutorService executor = Executors.newFixedThreadPool(10000);
	private final GpsUtilService gpsUtil;
	private final RewardCentral rewardsCentral;
	/**
	 * Constructeur de la classe RewardsService.
	 *
	 * @param gpsUtil Le service GPS utilisé pour obtenir les attractions et les emplacements des utilisateurs.
	 * @param rewardCentral Le service de récompenses utilisé pour obtenir le nombre de points de récompenses pour chaque attraction.
	 */
	public RewardsService(GpsUtilService gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	/**
	 * Définit la marge en miles pour la proximité des attractions.
	 *
	 * @param proximityMilesBuffer La marge en miles.
	 */
	public void setProximityMilesBuffer(int proximityMilesBuffer) {
		this.proximityMilesBuffer = proximityMilesBuffer;
	}
	/**
	 * Calcule les récompenses pour un utilisateur donné basées sur ses visites d'attractions.
	 *
	 * @param user L'utilisateur pour lequel calculer les récompenses.
	 */
	public void calculateRewards(User user) {
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<VisitedLocation> visitedLocationList = user.getVisitedLocations().stream().collect(Collectors.toList());

		// Create a list to hold futures
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for(VisitedLocation visitedLocation : visitedLocationList) {
			for(Attraction attraction : attractions) {
				if(user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
					// Submit a task for each attraction and add the future to the list
					futures.add(CompletableFuture.runAsync(() -> setRewardPoints(user, visitedLocation, attraction), executor));
				}
			}
		}

		// Wait for all futures to complete
		futures.forEach(CompletableFuture::join);
	}
	/**
	 * Définit les points de récompense pour une attraction spécifique visitée par l'utilisateur.
	 *
	 * @param user L'utilisateur qui a visité l'attraction.
	 * @param visitedLocation L'emplacement visité par l'utilisateur.
	 * @param attraction L'attraction visitée par l'utilisateur.
	 */
	public void setRewardPoints(User user, VisitedLocation visitedLocation, Attraction attraction) {
		Double distance = getDistance(attraction, visitedLocation.location);

		UserReward userReward = new UserReward(visitedLocation, attraction, distance.intValue());
		CompletableFuture.supplyAsync(() -> {
					return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
				}, executor)
				.thenAccept(points -> {
					userReward.setRewardPoints(points);
					user.addUserReward(userReward);
				})
				.exceptionally(ex -> {
					logger.warn("Failed to get or set reward points.", ex);
					return null;
				});
	}

	/**
	 * Obtient la distance entre une attraction et un emplacement.
	 *
	 * @param attraction L'attraction.
	 * @param location L'emplacement.
	 * @return La distance entre l'attraction et l'emplacement.
	 */
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	/**
	 * Vérifie si un emplacement est à proximité d'une attraction.
	 *
	 * @param attraction L'attraction à vérifier.
	 * @param location L'emplacement à vérifier.
	 * @return La distance entre l'attraction et l'emplacement de l'utilisateur.
	 */
	public double isWithinAttractionProximityDistance(Attraction attraction, Location location) {
		return getDistance(attraction, location);
	}

	public boolean isAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > 0 ? false : true;
	}

	public boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityMilesBuffer ? false : true;
	}
	/**
	 * Obtient les points de récompense pour une attraction pour un utilisateur donné.
	 *
	 * @param attraction L'attraction pour laquelle obtenir les points de récompense.
	 * @param user L'utilisateur pour lequel obtenir les points de récompense.
	 * @return Les points de récompense pour l'attraction pour l'utilisateur.
	 */
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	/**
	 * Calcule la distance entre deux emplacements géographiques.
	 *
	 * @param loc1 Le premier emplacement.
	 * @param loc2 Le deuxième emplacement.
	 * @return La distance en miles entre loc1 et loc2.
	 */
	public double getDistance(Location loc1, Location loc2) {
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);

		double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

		double nauticalMiles = 60 * Math.toDegrees(angle);
		double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
		return statuteMiles;
	}
}