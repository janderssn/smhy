package smhy.conditions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ForecastCondition {
    String id;
    String location;
    Double score;
    Double windDirectionMin;
    Double windDirectionMax;
    Double windSpeedMin;
    Double windSpeedMax;
    Double windGustMax;
    Integer timeOfDayMin;
    Integer timeOfDayMax;
    Integer hoursMin;
    List<WindDirection> windDirections;
    List<Integer> daysOfWeek;
    List<Integer> monthsOfYear;
}
