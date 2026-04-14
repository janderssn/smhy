package smhy.locations;

import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ForecastLocation {
    String id;
    String name;
    String region;
    String station;
    String latitude;
    String longitude;
    String altitude;
    Boolean favorite;
    List<String> takeoffs;
}
