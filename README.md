# Kafka [Kraft模式]教程（一）

## 说明

Kafka版本：3.3.1

Kraft模式！

## 准备三台服务器
我是通过`vagrant`搭配`virtualbox`创建的虚拟机。
vagrant的Vagrantfile文件内容如下：
```text
Vagrant.configure("2") do |config|
   (1..3).each do |i|
        config.vm.define "kraft#{i}" do |node|
            # 设置虚拟机的Box。指定本地的box文件
            node.vm.box = "boxomatic/centos-stream-9"

            # 设置虚拟机的主机名
            node.vm.hostname="kraft#{i}"

            # 设置虚拟机的IP
            node.vm.network "private_network", ip: "192.168.10.1#{i}"

            # VirtualBox相关配置
            node.vm.provider "virtualbox" do |v|
                # 设置虚拟机的名称
                v.name = "kraft#{i}"
                # 设置虚拟机的内存大小
                v.memory = 2048
                # 设置虚拟机的CPU个数
                v.cpus = 1
            end
        end
   end
end
```
然后执行`vagrant up`执行创建虚拟机。
创建完成的虚拟机IP和HOSTNAME如下：

| IP            | 主机名 |
| ------------- | ------ |
| 192.168.10.11 | kraft1 |
| 192.168.10.12 | kraft2 |
| 192.168.10.13 | kraft3 |



## Host配置
修改`/etc/hosts`,配置hosts，保证服务器之间能够通过hostname通信。
```text
192.168.10.11 kraft1
192.168.10.12 kraft2
192.168.10.13 kraft3
```

## JDK安装
kafka的运行依赖JDK（要求JDK8+），三个服务器都安装JDK
```shell
[vagrant@kraft1 ~]$ sudo yum install java-17-openjdk -y
[vagrant@kraft2 ~]$ sudo yum install java-17-openjdk -y
[vagrant@kraft3 ~]$ sudo yum install java-17-openjdk -y
```
## 下载kafka
[kafka3.3.1下载地址](https://downloads.apache.org/kafka/3.3.1/kafka_2.13-3.3.1.tgz)
下载完毕后，上传到三台服务器上，然后解压。
## 配置修改
修改kafka目录下的`config/kraft/server.properties`文件。三个服务器都需要修改。
特别注意：每个服务器（broker）上的配置里的node.id必须是数字，并且不能重复。
```properties
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This configuration file is intended for use in KRaft mode, where
# Apache ZooKeeper is not present.  See config/kraft/README.md for details.
#

############################# Server Basics #############################

# The role of this server. Setting this puts us in KRaft mode
process.roles=broker,controller

# The node id associated with this instance's roles
node.id=1

# The connect string for the controller quorum
controller.quorum.voters=1@kraft1:9093,2@kraft2:9093,3@kraft3:9093

############################# Socket Server Settings #############################

# The address the socket server listens on.
# Combined nodes (i.e. those with `process.roles=broker,controller`) must list the controller listener here at a minimum.
# If the broker listener is not defined, the default listener will use a host name that is equal to the value of java.net.InetAddress.getCanonicalHostName(),
# with PLAINTEXT listener name, and port 9092.
#   FORMAT:
#     listeners = listener_name://host_name:port
#   EXAMPLE:
#     listeners = PLAINTEXT://your.host.name:9092
listeners=PLAINTEXT://:9092,CONTROLLER://:9093

# Name of listener used for communication between brokers.
inter.broker.listener.name=PLAINTEXT

# Listener name, hostname and port the broker will advertise to clients.
# If not set, it uses the value for "listeners".
advertised.listeners=PLAINTEXT://:9092

# A comma-separated list of the names of the listeners used by the controller.
# If no explicit mapping set in `listener.security.protocol.map`, default will be using PLAINTEXT protocol
# This is required if running in KRaft mode.
controller.listener.names=CONTROLLER

# Maps listener names to security protocols, the default is for them to be the same. See the config documentation for more details
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

# The number of threads that the server uses for receiving requests from the network and sending responses to the network
num.network.threads=3

# The number of threads that the server uses for processing requests, which may include disk I/O
num.io.threads=8

# The send buffer (SO_SNDBUF) used by the socket server
socket.send.buffer.bytes=102400

# The receive buffer (SO_RCVBUF) used by the socket server
socket.receive.buffer.bytes=102400

# The maximum size of a request that the socket server will accept (protection against OOM)
socket.request.max.bytes=104857600


############################# Log Basics #############################

# A comma separated list of directories under which to store log files
log.dirs=/home/vagrant/kraft-combined-logs

# The default number of log partitions per topic. More partitions allow greater
# parallelism for consumption, but this will also result in more files across
# the brokers.
num.partitions=1

# The number of threads per data directory to be used for log recovery at startup and flushing at shutdown.
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1

############################# Internal Topic Settings  #############################
# The replication factor for the group metadata internal topics "__consumer_offsets" and "__transaction_state"
# For anything other than development testing, a value greater than 1 is recommended to ensure availability such as 3.
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

############################# Log Flush Policy #############################

# Messages are immediately written to the filesystem but by default we only fsync() to sync
# the OS cache lazily. The following configurations control the flush of data to disk.
# There are a few important trade-offs here:
#    1. Durability: Unflushed data may be lost if you are not using replication.
#    2. Latency: Very large flush intervals may lead to latency spikes when the flush does occur as there will be a lot of data to flush.
#    3. Throughput: The flush is generally the most expensive operation, and a small flush interval may lead to excessive seeks.
# The settings below allow one to configure the flush policy to flush data after a period of time or
# every N messages (or both). This can be done globally and overridden on a per-topic basis.

# The number of messages to accept before forcing a flush of data to disk
#log.flush.interval.messages=10000

# The maximum amount of time a message can sit in a log before we force a flush
#log.flush.interval.ms=1000

############################# Log Retention Policy #############################

# The following configurations control the disposal of log segments. The policy can
# be set to delete segments after a period of time, or after a given size has accumulated.
# A segment will be deleted whenever *either* of these criteria are met. Deletion always happens
# from the end of the log.

# The minimum age of a log file to be eligible for deletion due to age
log.retention.hours=168

# A size-based retention policy for logs. Segments are pruned from the log unless the remaining
# segments drop below log.retention.bytes. Functions independently of log.retention.hours.
#log.retention.bytes=1073741824

# The maximum size of a log segment file. When this size is reached a new log segment will be created.
log.segment.bytes=1073741824

# The interval at which log segments are checked to see if they can be deleted according
# to the retention policies
log.retention.check.interval.ms=300000
```

**三个broker的配置基本都和上面的配置一样，不同的地方就是`node.id`.**

**kraft1**

```properties
node.id=1
```

**kraft2**

```properties
node.id=2
```

**kraft3**

```properties
node.id=3
```

另外还有两处需要修改。
* `controller.quorum.voters=1@kraft1:9093,2@kraft2:9093,3@kraft3:9093`【以逗号分隔的`{id}@{host}:{port}`投票者列表。例如：`1@localhost:9092,2@localhost:9093,3@localhost:9094`】
* `log.dirs=/home/vagrant/kraft-combined-logs`【日志路径，默认是`/temp`下的文件下，生产环境不要使用，因为`linux`会清理`/tmp`目录下的文件，会造成数据丢失】

## 生成集群ID
随便找一个服务器，进入kafka目录，使用`kafka-storage.sh`生成一个uuid，一个集群只能有一个uuid！！！
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
[vagrant@kraft1 kafka_2.13-3.3.1]$ echo $KAFKA_CLUSTER_ID
t6vWCV2iRneJB62NXxO19g
```
这个ID就可以作为集群的ID
## 格式化存储目录
三个机器上都需要执行
```shell
# kraft1服务器
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-storage.sh format -t t6vWCV2iRneJB62NXxO19g -c config/kraft/server.properties
Formatting /home/vagrant/kraft-combined-logs with metadata.version 3.3-IV3.
# kraft2服务器
[vagrant@kraft2 kafka_2.13-3.3.1]$ bin/kafka-storage.sh format -t t6vWCV2iRneJB62NXxO19g -c config/kraft/server.properties
Formatting /home/vagrant/kraft-combined-logs with metadata.version 3.3-IV3.
# kraft3服务器
[vagrant@kraft3 kafka_2.13-3.3.1]$ bin/kafka-storage.sh format -t t6vWCV2iRneJB62NXxO19g -c config/kraft/server.properties
Formatting /home/vagrant/kraft-combined-logs with metadata.version 3.3-IV3.
```
## 启动服务器
三个机器都需要执行
```shell
# kraft1服务器
[vagrant@kraft1 kafka_2.13-3.3.1]$  bin/kafka-server-start.sh -daemon  config/kraft/server.properties
# kraft2服务器
[vagrant@kraft2 kafka_2.13-3.3.1]$  bin/kafka-server-start.sh -daemon  config/kraft/server.properties
# kraft3服务器
[vagrant@kraft3 kafka_2.13-3.3.1]$  bin/kafka-server-start.sh -daemon  config/kraft/server.properties
```

## 查看元数据（Metadata）
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-metadata-shell.sh  --snapshot /home/vagrant/kraft-combined-logs/__cluster_metadata-0/00000000000000000000.log
Loading...
Starting...
[2022-12-28 11:12:45,455] WARN [snapshotReaderQueue] event handler thread exiting with exception (org.apache.kafka.queue.KafkaEventQueue)
java.nio.channels.NonWritableChannelException
        at java.base/sun.nio.ch.FileChannelImpl.truncate(FileChannelImpl.java:406)
        at org.apache.kafka.common.record.FileRecords.truncateTo(FileRecords.java:270)
        at org.apache.kafka.common.record.FileRecords.trim(FileRecords.java:231)
        at org.apache.kafka.common.record.FileRecords.close(FileRecords.java:205)
        at org.apache.kafka.metadata.util.SnapshotFileReader$3.run(SnapshotFileReader.java:182)
        at org.apache.kafka.queue.KafkaEventQueue$EventHandler.run(KafkaEventQueue.java:174)
        at java.base/java.lang.Thread.run(Thread.java:833)
[ Kafka Metadata Shell ]
>> ls /
brokers  features  local  metadataQuorum
>> ls brokers/
1  2  3
>> ls features/
metadata.version
>> ls local/
commitId  version
>> ls metadataQuorum/
leader  offset
```
集群搭建完毕后，metadata中的一级目录只有`brokers`,`features`,`local`,`metadataQuorum`。
创建主题，消费的时候，会增加一些其他的一级目录。比如`topics`，`topicIds`等。

> **这里报了个错，不知道具体原因，目前不影响使用，暂时忽略（之后确定下）！**

## 创建主题
我创建一个3副本、3分区的主题（itlab1024-topic1）。
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-topics.sh --create --topic itlab1024-topic1 --partitions 3 --replication-fa
ctor 3  --bootstrap-server kraft1:9092,kraft2:9092,kraft3:9092
Created topic itlab1024-topic1.
```
## 查看主题
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-topics.sh --describe --topic itlab1024-topic1 --bootstrap-server kraft1:909
2,kraft2:9092,kraft3:9092
Topic: itlab1024-topic1 TopicId: li_8Jn_USOeF-HIAZdDZKA PartitionCount: 3       ReplicationFactor: 3    Configs: segment.bytes=1073741824
        Topic: itlab1024-topic1 Partition: 0    Leader: 2       Replicas: 2,3,1 Isr: 2,3,1
        Topic: itlab1024-topic1 Partition: 1    Leader: 3       Replicas: 3,1,2 Isr: 3,1,2
        Topic: itlab1024-topic1 Partition: 2    Leader: 1       Replicas: 1,2,3 Isr: 1,2,3
```
## 再次查看元数据(Metadata)
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-metadata-shell.sh  --snapshot /home/vagrant/kraft-combined-logs/__cluster_m
etadata-0/00000000000000000000.log
Loading...
Starting...
[2022-12-28 11:24:58,958] WARN [snapshotReaderQueue] event handler thread exiting with exception (org.apache.kafka.queue.KafkaEventQueue)
java.nio.channels.NonWritableChannelException
        at java.base/sun.nio.ch.FileChannelImpl.truncate(FileChannelImpl.java:406)
        at org.apache.kafka.common.record.FileRecords.truncateTo(FileRecords.java:270)
        at org.apache.kafka.common.record.FileRecords.trim(FileRecords.java:231)
        at org.apache.kafka.common.record.FileRecords.close(FileRecords.java:205)
        at org.apache.kafka.metadata.util.SnapshotFileReader$3.run(SnapshotFileReader.java:182)
        at org.apache.kafka.queue.KafkaEventQueue$EventHandler.run(KafkaEventQueue.java:174)
        at java.base/java.lang.Thread.run(Thread.java:833)
[ Kafka Metadata Shell ]
>> ls /
brokers  features  local  metadataQuorum  topicIds  topics
>>
```
可以看到，一级目录多了`topicIds`和`topics`。

## 生产消息
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-console-producer.sh --topic itlab1024-topic1 --bootstrap-server kraft1:9092
,kraft2:9092,kraft3:9092
>1
>2
```
## 消费消息
上面发送了1和2两个消息，看看能否消费到。
```shell
[vagrant@kraft1 kafka_2.13-3.3.1]$ bin/kafka-console-consumer.sh --topic itlab1024-topic1 --bootstrap-server kraft1:9092,kraft2:9092,kraft3:9092 --from-beginning
1
2
```
没问题，正常消费。
## Api使用
使用Java程序，来生产和消费消息。
### 建立Maven项目并引入依赖
```xml
<dependency>
	<groupId>org.apache.kafka</groupId>
	<artifactId>kafka-clients</artifactId>
	<version>3.3.1</version>
</dependency>
```
### 创建消费者类
```shell
package com.itlab1024.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class ConsumerClient {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "kraft1:9092,kraft2:9092,kraft3:9092");
        props.setProperty("group.id", "test");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("itlab1024-topic1"));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records)
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
            }
        }
    }
}
```
### 创建生产者类
```shell
package com.itlab1024.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class ProducerClient {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "kraft1:9092,kraft2:9092,kraft3:9092");
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < 10; i++) {
                producer.send(new ProducerRecord<>("itlab1024-topic1", "itlab" + i, "itlab" + i));
            }
        }
    }
}
```
运行消费者类，再执行生产者类，观察消费者类的控制台会输出如下内容：
```text
offset = 0, key = itlab1, value = itlab1
offset = 1, key = itlab2, value = itlab2
offset = 2, key = itlab5, value = itlab5
offset = 3, key = itlab7, value = itlab7
offset = 4, key = itlab8, value = itlab8
offset = 0, key = itlab0, value = itlab0
offset = 1, key = itlab3, value = itlab3
offset = 2, key = itlab4, value = itlab4
offset = 3, key = itlab6, value = itlab6
offset = 4, key = itlab9, value = itlab9
```
说明也是能够正常生产和消费消息的。



> 上面基本介绍了Kafka Raft模式集群的搭建方式，并没有具体讲解配置含义（还有很多配置）。下一版会介绍！



---



# Kafka [Kraft模式]教程（二）

## 基本解释

在教程一中创建了一个基础的Kafka Raft模式集群，但是并没有细讲该模式的具体细节，本文章来讲解下，我尽可能讲解的很清晰。

在kafka中节点服务器主要有两种角色，一种是`controller`，一种是`broker`，`zookeeper`和`raft`模式下都是这两种角色，不同的是`zookeeper`模式下的`controller`强依赖于`zookeeper`，`zookeeper`中存储了集群的元数据信息。

但是依赖于`zookeeper`有很多问题：

* 首先使用`zookeeper`则多了一个组件，运维成本高
* `zookeeper`符合`CAP`悖论中的`CP`,也就是说`zookeeper`是强一致性的组件。那么如果集群中某个节点数据变更，就得通知其他节点同步，并且要超过半数完成才行，当节点较多的时候，性能下降明显。
* `zookeeper`的设计决定了它只适用于存储一些简单的配置或者是集群的元数据，数据量大的时候性能和稳定性就会下降，一些监听器也会延时甚至丢失。
* `zookeeper`本身也是分布式系统，主从结构，如果主节点挂掉，也会选举出来主节点，他的选举并不快，并且选举的时候是不能提供服务的。

那么`Raft`模式，弃用`zookeeper`后，controller中的信息就不会存储到`zookeeper`中了（`zookeeper`都没了）,而是存储到了kafka自己的服务器上。

通过一张图来看下变化前后的区别（图片来源网络）：

![](https://itlab1024-1256529903.cos.ap-beijing.myqcloud.com/202212301051680.png)

用`Quorum Controller`代替之前的`Controller`，`Quorum`中每个`Controller`节点都会保存所有元数据，通过`KRaft`协议保证副本的一致性。这样即使`Quorum Controller`节点出故障了，新的`Controller`迁移也会非常快。



在`Kraft`模式下，只有一小组专门选择的服务器可以充当控制器（设置`process.roles`包含`controller`）,controller服务器的作用是参与元数据的仲裁。

多个`controller`服务器只有一个是`active`状态的，其他的都是`standby`状态的（也就是备用服务器）。

`controler` 服务器的数量遵循Quorum原则（过半原则），也就是说要奇数个，比如3个服务器允许1个故障，5个服务器允许2个故障。

## 配置说明

`kraft`模式下的配置文件在`config/kraft`目录下。

```shell
[vagrant@kraft3 kraft]$ pwd
/home/vagrant/kafka_2.13-3.3.1/config/kraft
[vagrant@kraft3 kraft]$ ls
broker.properties  controller.properties  README.md  server.properties
```

这里有三个`properties`文件，三个文件中内容基本相同，唯一不同的是`process.roles`的配置。

* `broker.properties`：`process.roles=broker`，代表该服务器只是`broker`角色。
* `controller.properties`：`process.roles=controller`，代表该服务器只是`controller`角色。
* `server.properties`:`process.roles=broker,controller`，代表该服务器既是`broker`角色也是`controller`角色。

kafka只是给我们提供了三个不同角色的配置文件，方便我们使用而已。

文件中的具体配置内容，才是我们应该重视的，接下来一个一个说明，并尝试修改默认配置进行试验！

---



### process.roles

用于配置服务器的角色，可以有如下配置。

* `process.roles=broker`，代表该服务器只是`broker`角色。
* `process.roles=controller`，代表该服务器只是`controller`角色。
* `process.roles=broker,controller`，代表该服务器既是`broker`角色也是`controller`角色。

也可以不配置，如果不配置，则说明当前集群不是kraft模式，而是`zookeeper`模式。

说明：目前还不支持两种模式自由切换（以后是否支持也不清楚），如果要切换模式，比如重新使用`bin/kafka-storage.sh`重新格式化（重新格式化数据肯定会丢失的，特别注意！）

同时具有`broker`和`controller`两种角色的服务器（也叫组合服务器）在开发环境中是很好的（服务器可能较少，搭建方便），但是在生产环境中是不推荐的，因为这样做会导致`broker`和`controller`的隔离性差，不可能在组合模式下单独滚动或缩放`controller`与`broker`。

**试验**：

考虑我之前搭建的集群，三台机器都是配置的`process.roles=broker,controller`，这是不好的！



# 博主信息

个人主页：https://itlab1024.com

Github：https://github.com/itlab1024
