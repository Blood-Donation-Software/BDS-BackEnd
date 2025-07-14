# Profile Distance Management System

This system automatically calculates and stores the distance from each user's profile address to the medical facility using Google Maps Distance Matrix API.

## Features

- **Automatic Distance Calculation**: On application startup, calculates distances for any profiles missing this information
- **Google Maps Integration**: Uses Google Maps Distance Matrix API for accurate distance and duration calculations
- **Address Change Detection**: Automatically recalculates distance when profile address information is updated
- **Scheduled Updates**: Daily scheduled task to calculate missing distances
- **RESTful API**: Complete CRUD operations for distance management

## Components

### Entity
- `ProfileDistance`: Stores distance information for each profile
  - Distance in meters and kilometers
  - Travel duration in seconds and text format
  - Profile and medical facility addresses
  - Calculation timestamps

### Services
- `GoogleMapsService`: Handles Google Maps API integration
- `ProfileDistanceService`: Main business logic for distance operations
- `ProfileDistanceScheduledService`: Scheduled tasks for maintenance

### Repository
- `ProfileDistanceRepository`: Data access layer with custom queries

### Controller
- `ProfileDistanceController`: REST API endpoints

### Startup Component
- `ProfileDistanceStartupComponent`: Runs on application startup to calculate missing distances

## API Endpoints

### Get Distance
```
GET /api/profile-distances/{profileId}
```

### Calculate Distance
```
POST /api/profile-distances/calculate/{profileId}
```

### Recalculate Distance
```
POST /api/profile-distances/recalculate/{profileId}
```

### Get Profiles Within Distance
```
GET /api/profile-distances/within-distance?maxDistanceKm=10
```

### Get All Profiles Ordered by Distance
```
GET /api/profile-distances/all-ordered
```

### Calculate Missing Distances (Admin)
```
POST /api/profile-distances/calculate-missing
```

### Delete Distance (Admin)
```
DELETE /api/profile-distances/{profileId}
```

## Configuration

Ensure the following environment variables are set in your `.env` file:

```properties
# Google Maps API Key
SPRING_GOOGLE_MAPS_API_KEY=your_google_maps_api_key

# Medical Facility Address
STREET_ADDRESS=118 Đường Hồng Bàng
DISTRICT=Quận 5
CITY=Thành phố Hồ Chí Minh
STATE=Vietnam
```

## Automatic Features

1. **Startup Calculation**: When the application starts, it automatically finds profiles without distance calculations and calculates them
2. **Profile Update Integration**: When a profile's address is updated, the distance is automatically recalculated
3. **Scheduled Maintenance**: Daily at 2 AM, the system checks for and calculates any missing distances

## Error Handling

- Profiles with incomplete address information are skipped
- Failed distance calculations don't prevent other profiles from being processed
- Errors are logged but don't crash the application startup

## Frontend Integration

The frontend includes API functions for:
- Viewing profile distances
- Triggering manual calculations
- Getting profiles within specific distances
- Administrative functions

All API calls are properly configured in `src/apis/profileDistance.js` and endpoint mappings are in `src/utils/axios.js`.
