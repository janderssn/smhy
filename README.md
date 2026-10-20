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
smhy.json.enabled=true

smhy.region=se/stockholm
smhy.provider=yr

smhy.forecasts.yr.useragent=smhy/1.0 github.com/janderssn/smhy
smhy.forecasts.yr.url=https://api.met.no/weatherapi/locationforecast/2.0/complete?altitude={altitude}&lat={latitude}&lon={longitude}

smhy.forecasts.smhi.useragent=smhy/1.0 github.com/janderssn/smhy
smhy.forecasts.smhi.url=https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/{longitude}/lat/{latitude}/data.json
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
java -jar target/*.jar --forecast --locations salanda alleberg
```

### View the UI

`--reports` or `--all` writes the HTML pages and `dashboard.json` to `smhy.output.dir` (`/tmp/smhy` locally). Serve that directory and open `index.html`:

```bash
java -jar target/*.jar --all
python3 -m http.server 8080 --directory /tmp/smhy
```

`index.html` is the week, `helg.html` the weekend, `favoriter.html` favourites, and `stockholm.html` the configured region. The page reads `dashboard.json` (schema 1) beside it. The dashboard lists only the provider that supplied the numbers (`yr.no` unless `smhy.provider=smhi`).

### Checks

GitHub Actions (`.github/workflows/build.yml`) runs on every pull request and on pushes to `master` and `env/*`:

- `test` — `mvn test`
- `ui` — syntax-checks the frontend, runs `java -jar … --all` against yr.no, then Playwright at 1440×900 and 390×844
- `preview` — pull requests from this repository publish that generated site to `https://smhy-pr-<number>.fly.dev`. Closing the pull request deletes the app.
- `deploy` — a push to `master` deploys the existing Fly app `smhy`. This does not change DNS for smhy.se.
- `deploy-env` — a push to `env/<name>` deploys a separate app `smhy-<name>`

`FLY_API_TOKEN` has to be allowed to create and destroy apps for previews and `env/*` deploys. A token scoped only to the app `smhy` can still deploy production. If Fly refuses `apps create`, the preview job records that and does not fail the unit or screenshot checks.
