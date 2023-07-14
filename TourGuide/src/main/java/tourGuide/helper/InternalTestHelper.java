package tourGuide.helper;

import tourGuide.service.RewardsService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InternalTestHelper {

	// Set this default up to 100,000 for testing
	private static int internalUserNumber = 100000;


	public static void setInternalUserNumber(int internalUserNumber) {
		InternalTestHelper.internalUserNumber = internalUserNumber;
	}
	
	public static int getInternalUserNumber() {
		return internalUserNumber;
	}

	// TODO: ajout dus preferences pour User et UserReward
}
