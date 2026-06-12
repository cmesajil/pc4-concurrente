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

```bash
sudo systemctl start docker.service  
```

```bash
sudo docker compose down -v  
```

```bash
sudo docker compose up --build -d 
```

```bash
sudo docker compose exec postgres psql -U postgres
```
