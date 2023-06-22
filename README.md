# JMeter Pika listener plugin

![](https://img.shields.io/badge/JMeter-green)
![](https://img.shields.io/badge/release-5.4.3-green)

It needs to be used with the backend listener used by  [Pika Server](https://github.com/kamalyes/pika)

## Packaging

Execute below mvn command. Make sure JAVA_HOME is set properly

```bash
mvn -B clean package -X "-Dmaven.test.skip=true -Dmaven.javadoc.skip=true"
```

## Upgrade

* Close JMeter if its started.
* Remove old jar.
* Put '`jmeter-pika-backend-listener-<version>.jar`' file from [Releases](https://github.com/kamalyes/jmeter-pika-backend-listener-plugins/releases) to `~<JmeterPath<\lib\ext`;
* Run JMeter again and got Listener.
* Select from the dropdown item with the name '`io.kamalyes.jmeter.PikaBackendListenerClient`'.
  
## 📌 Plugin configuration

Let’s explain the plugin fields:

* `pikaServerUrl` - pika server url.
* `projectName` - the name of the project.
* `envName` - the name of the env.
* `batchNo` - the batch number.
