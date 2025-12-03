#!/bin/bash

# 如果第一个参数是"build"或"true"，则执行第一行命令
if [ "$1" = "build" ] || [ "$1" = "true" ]; then
    echo "执行Maven完整构建（clean install）..."
    mvn -f ../pom.xml clean install -DskipTests=true
else
    echo "执行Maven打包（install）..."
    mvn -f ../pom.xml install -DskipTests=true
fi

# 使用第二个参数作为profile，如果没有提供则使用默认值"dev"
PROFILE=${2:-"dev"}
echo "使用profile: $PROFILE"
echo "启动Spring Boot应用..."
mvn spring-boot:run -Dmaven.test.skip=true -Dspring-boot.run.profiles=$PROFILE