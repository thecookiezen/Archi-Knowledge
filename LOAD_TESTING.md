# Load Testing Guide

Dedicated load testing module and a script to run performance scenarios in isolated JVM environments.

## Running the Tests

The easiest way to run the full suite of load tests is using the provided bash script:

```bash
./run_load_tests.sh
```

This script will:
1. Build the project.
2. Run a sequence of scenarios ("Functional Check", "Small Load", "Medium Load").
3. Use different heap sizes for each scenario to simulate constraints.

## Manual Execution

You can run specific scenarios manually using the built JAR file.

### Prerequisites
Build the project:
```bash
mvn clean package -DskipTests
```

### Command
```bash
java -jar load-tests/target/load-tests-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=loadtest \
  --loadtest.scenario.name="My Custom Test" \
  --loadtest.entity-count=5000 \
  --loadtest.relations-per-entity=10 \
  --loadtest.batch-size=100
```

### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `loadtest.scenario.name` | "Manual Run" | Name of the test scenario for logging. |
| `loadtest.entity-count` | 1000 | Total number of entities to create. |
| `loadtest.relations-per-entity` | 10 | Number of relations to create for each entity. |
| `loadtest.batch-size` | 100 | Number of items to process in a single batch (saving memory). |

## JVM Memory Settings

To test the application limits, you can constrain the heap size using standard Java flags:

```bash
# Run with max 512MB heap
java -Xmx512m -jar ...
```
