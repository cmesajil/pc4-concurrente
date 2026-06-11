```bash
mvn archetype:generate \
  -DgroupId=com.uni.chat \
  -DartifactId=chat-servidor \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

```bash
mvn exec:java -Dexec.mainClass="com.uni.service.ServidorChatTLS"

```


```bash
mvn exec:java -Dexec.mainClass="com.uni.chat.ClienteChatTLS"
```

