# Set up kafka using Docker

### Step 1: Run Docker Compose
```bash
docker-compose up -d
```
### Step 2: Verify Kafka and ZooKeeper, check the status of the containers:
```bash
docker ps
```
### Step 3: Create a Kafka Topic (Optional)
If you want to manually create a Kafka topic, you can use the following command:
```bash
docker exec -it <kafka_container_id> kafka-topics.sh --create --topic test-topic --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092
```
Replace <kafka_container_id> with the actual container ID of your Kafka service.

