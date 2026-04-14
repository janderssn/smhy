# SMHY
[![build](https://github.com/janderssn/smhy/actions/workflows/build.yml/badge.svg)](https://github.com/janderssn/smhy/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/janderssn/smhy/branch/master/graph/badge.svg?token=KXG8XFW84D)](https://codecov.io/gh/janderssn/smhy)
[![Better Uptime Badge](https://betteruptime.com/status-badges/v1/monitor/kwt8.svg)](https://betteruptime.com/?utm_source=status_badge)
[![License: CC BY 4.0](https://img.shields.io/badge/License-CC_BY_4.0-lightgrey.svg)](https://creativecommons.org/licenses/by/4.0/)

SMHY collects forecasts from YR.no and throws away all the crappy weather and shows you only the nice weather in an easy to digest format.

<p align="center">
  <kbd>
    <img src="https://github.com/janderssn/smhy/blob/master/smhy_screenshot.png"/>
  </kbd>
</p>

## Configuration

```properties
smhy.output.dir=/tmp/smhy
smhy.config.dir=classpath:config
smhy.assets.dir=classpath:assets/**

smhy.region=se/stockholm
smhy.provider=yr

smhy.provider.yr.useragent=MyWeatherApp/1.0 myemail@example.com
smhy.provider.yr.url=https://api.met.no/weatherapi/locationforecast/2.0/complete?altitude={altitude}&lat={latitude}&lon={longitude}

smhy.provider.smhi.useragent=MyWeatherApp/1.0 myemail@example.com
smhy.provider.smhi.url=https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/{longitude}/lat/{latitude}/data.json

spring.data.mongodb.uri=mongodb://127.0.0.1:27017/smhy
spring.data.mongodb.database=smhy
```

### Locations
The locations smhy should collect forecasts from are defined in the locations.yml file.
```yaml
- id: salanda
  name: Salanda
  region: se/stockholm
  latitude: '59.91993'
  longitude: '16.60655'
  altitude: '62'
  favorite: yes
```

### Conditions
The criterias smhy should use to score forecasts are defined in the conditions.yml file. 
```yaml
- id: salanda-perfect
  location: salanda
  score: 100.0
  windDirectionMin: 160
  windDirectionMax: 200
  windSpeedMax: 4.0
  windSpeedMin: 2.0
  windGustMax: 6.0
  daysOfWeek: [6, 7]
  monthsOfYear: [4, 5, 6, 7, 8, 9]
  hoursMin: 3
  timeOfDayMin: 8
  timeOfDayMax: 18
```

## Build & Deploy

SMHY is built and deployed to [Fly.io](https://fly.io) using GitHub Actions.
The following secrets need to be added in repository settings:

   | NAME | DESCRIPTION |
   | --- | --- |
   | FLY_API_TOKEN | Fly.io API token for deployment |
   | CODECOV_TOKEN | Codecov upload token |
   | BETTERSTACK_HEARTBEAT_URL | BetterStack monitoring heartbeat endpoint |
   | LOGTAIL_INGEST_KEY | Logtail log ingestion key |

## Run locally

```bash
docker run -dp 27017:27017 -v mongo:/data/db --name mongo --restart=always mongo
java -jar target/*.jar --forecast --locations salanda alleberg
```