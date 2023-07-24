package tourGuide.dto;

public class UserLocationDto {

    private String userId;

    private LocationDto location;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public LocationDto getLocation() {
        return location;
    }

    public UserLocationDto() {
    }

    public UserLocationDto(String userId, LocationDto location) {
        this.userId = userId;
        this.location = location;
    }

    @Override
    public String toString() {
        return userId + ":" + location;
    }

}