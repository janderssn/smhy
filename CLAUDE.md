# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SMHY is a Spring Boot weather forecast application that collects forecasts from weather APIs (YR.no, SMHI), filters them based on user-defined conditions, and generates reports showing only favorable weather periods. The application focuses on paragliding-friendly conditions but can be configured for any weather-dependent activities.

## Technology Stack

- **Java 25** with Spring Boot 3.5.6
- **Maven** for build management
- **Lombok 1.18.42** for reducing boilerplate
- **Thymeleaf** for HTML templating
- **MockServer** for integration testing

## Build and Test Commands

### Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Run Single Test
```bash
mvn test -Dtest=ForecastRatingServiceLocationTest
mvn test -Dtest=ForecastRatingServiceLocationTest#rateForecastSalanda
```

### Run Application Locally
```bash
java -jar target/*.jar --forecast --locations salanda alleberg
```

### Application Command-Line Options
- `--forecast` - Fetch and process weather forecasts
- `--reports` - Generate HTML reports (and JSON if enabled)
- `--all` - Run forecast and reports
- `--locationsFile <path>` - Load locations from custom YAML file
- `--conditionsFile <path>` - Load conditions from custom YAML file
- `--json` - Enable JSON report generation (overrides config)
- `--no-json` - Disable JSON report generation (overrides config)
- `--heartbeat` - Send heartbeat to monitoring service

## Architecture

### Core Domain Packages

- **forecasts/** - Weather forecast providers (YR, SMHI) with rate limiting and caching
- **conditions/** - Scoring criteria for weather forecasts (wind direction, speed, time of day, etc.)
- **locations/** - Geographic locations with coordinates and metadata
- **ratings/** - Scoring engine that matches forecasts against conditions
- **reports/** - HTML and JSON report generation using Thymeleaf templates
- **monitoring/** - BetterUptime heartbeat integration

### Data Flow

1. **ForecastService** fetches forecasts from weather APIs (YR/SMHI) via provider pattern
2. **RateLimitedWeatherForecastProvider** wraps providers with rate limiting
3. **ForecastRatingService** scores each forecast using conditions from `conditions.yml`
4. **ReportService** generates HTML reports showing only favorable weather periods
5. Reports are written to the configured output directory

### Configuration System

- **locations.yml** - Defines locations to monitor with coordinates, region, altitude, and associated paragliding takeoffs
- **conditions.yml** - Defines scoring rules per location (wind direction/speed ranges, time windows, minimum duration)
- **application.properties** - Spring configuration, API endpoints
- **application-prod.properties** - Production overrides

### Provider Pattern

Weather data providers implement the `WeatherForecastProvider` interface for forecast APIs (YR, SMHI). New providers can be added by implementing this interface and registering as Spring beans.

### Scoring System

Conditions in `conditions.yml` define scoring rules with these key fields:
- `windDirectionMin/Max` or `windDirections` - Acceptable wind angles/compass directions
- `windSpeedMin/Max` - Wind speed range in m/s
- `windGustMax` - Maximum gust speed
- `timeOfDayMin/Max` - Hour range (e.g., 8-18 for 8am-6pm)
- `daysOfWeek` - Array of weekday numbers (1=Monday, 7=Sunday)
- `monthsOfYear` - Array of month numbers
- `hoursMin` - Minimum consecutive hours of favorable conditions
- `score` - Points awarded when conditions match (0-100)

Multiple conditions can target the same location with different scores for different quality levels.

## Testing

- **MockServer** for mocking external weather APIs in integration tests
- Test resources in `src/test/resources/` include sample API responses
- Key test classes verify scoring logic against known conditions

## Deployment

- CI/CD via GitHub Actions (`.github/workflows/build.yml`)
- Builds on push to master, runs tests, generates code coverage with JaCoCo
- Deploys to Fly.io using `flyctl deploy`
- Runs as Docker container with Alpine JDK 25

## Configuration Properties

Key properties to configure:
- `smhy.output.dir` - Where generated HTML and JSON reports are written
- `smhy.json.enabled` - Enable/disable JSON report generation (default: true)
- `smhy.provider` - Weather provider to use (yr or smhi)
- `smhy.forecasts.yr.useragent` - Required user agent for YR.no API
- `betterstack.heartbeat.url` - Optional monitoring endpoint

<!-- BACKLOG.MD MCP GUIDELINES START -->

<CRITICAL_INSTRUCTION>

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses Backlog.md MCP for all task and project management activities.

**CRITICAL GUIDANCE**

- If your client supports MCP resources, read `backlog://workflow/overview` to understand when and how to use Backlog for this project.
- If your client only supports tools or the above request fails, call `backlog.get_workflow_overview()` tool to load the tool-oriented overview (it lists the matching guide tools).

- **First time working here?** Read the overview resource IMMEDIATELY to learn the workflow
- **Already familiar?** You should have the overview cached ("## Backlog.md Overview (MCP)")
- **When to read it**: BEFORE creating tasks, or when you're unsure whether to track work

These guides cover:
- Decision framework for when to create tasks
- Search-first workflow to avoid duplicates
- Links to detailed guides for task creation, execution, and completion
- MCP tools reference

You MUST read the overview resource to understand the complete workflow. The information is NOT summarized here.

</CRITICAL_INSTRUCTION>

<!-- BACKLOG.MD MCP GUIDELINES END -->