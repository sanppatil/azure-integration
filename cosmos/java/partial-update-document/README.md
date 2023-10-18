# Prerequisite 
## Set following environment variables in order to run this sample.
```
export COSMOS_HOST=""
export COSMOS_MASTER_KEY=""
export COSMOS_DATABASE_ID=""
export COSMOS_CONTAINER_ID=""
```

# Build Steps

### 1. Maven build
```bash
mvn clean package
```

### 2. Run application
```bash
java -jar target/partial-update-document-1.0.jar
```