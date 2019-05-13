# Kafka with Kotlin

[We followed a tutorial online](https://aseigneurin.github.io/2018/08/01/kafka-tutorial-1-simple-producer-in-kotlin.html).

## Running a kafka cluster using docker-compose

[wurstmeister maintains a lovely docker-compose configuration](http://wurstmeister.github.io/kafka-docker/).  As mentioned, change the environment variable `KAFKA_ADVERTISED_HOST_NAME` in `docker-compose.yml`.

Connect to a kafka container, with an external IP of `192.168.86.240`:

```sh
./start-kafka-shell.sh 192.168.86.240 192.168.86.240:2181
```

Once inside a kafka container, we ran a producer via the following:

```sh
$KAFKA_HOME/bin/kafka-console-producer.sh --topic=topic \
--broker-list=`broker-list.sh`
```

Or a consumer:

```sh
$KAFKA_HOME/bin/kafka-console-consumer.sh --topic topic \
--bootstrap-server `broker-list.sh|head`
```

## Running this app

You need to know the address of your kafka broker, then:

```sh
gradle run --args='0.0.0.0:32784 happy_topic hello00'
```
