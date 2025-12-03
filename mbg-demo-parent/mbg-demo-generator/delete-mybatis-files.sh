#!/bin/bash

# 用法: ./delete-mybatis-files.sh OldModelName

#PROJECT_NAME="global"

if [ $# -eq 0 ]; then
    echo "错误: 请提供要删除的表名作为参数"
    echo "用法: $0 OldModelName"
    exit 1
fi

#MODULE_NAME="$1"

OLD_MODEL_NAME="$1"

echo "开始删除Model类 $OLD_MODEL_NAME 对应的 MyBatis 文件..."

# 定义要搜索的目录
#JAVA_DIR="module-$MODULE_NAME-parent/module-$MODULE_NAME-model/src/main/java"
#RESOURCES_DIR="src/main/resources"

for JAVA_DIR in ../mbg-demo-model/src/main/java; do
    echo "搜索Model类目录: $JAVA_DIR"
    if [ -d "$JAVA_DIR" ]; then
        # 删除实体的Base类文件
        find "$JAVA_DIR" -name "Base*${OLD_MODEL_NAME}.java" -type f | while read file; do
            echo "删除实体的Base类: $file"
            rm -f "$file"
        done

        # 删除实体类文件
        find "$JAVA_DIR" -name "*${OLD_MODEL_NAME}.java" -type f | while read file; do
            echo "删除实体类: $file"
            rm -f "$file"
        done
        
        # 删除实体Example文件
        find "$JAVA_DIR" -name "Base*${OLD_MODEL_NAME}Example.java" -type f | while read file; do
            echo "删除实体类BaseExample: $file"
            rm -f "$file"
        done
        
        # 删除实体Example文件
        find "$JAVA_DIR" -name "*${OLD_MODEL_NAME}Example.java" -type f | while read file; do
            echo "删除实体类Example: $file"
            rm -f "$file"
        done
    fi
done

for RESOURCES_DIR in ../mbg-demo-mapper/src/main; do
    echo "搜索Mapper类与资源目录: $RESOURCES_DIR"

    # 删除Mapper接口文件
    find "$RESOURCES_DIR" -name "*${OLD_MODEL_NAME}Mapper.java" -type f | while read file; do
        echo "删除Mapper接口: $file"
        rm -f "$file"
    done

    # 删除XML映射文件
    find "$RESOURCES_DIR" -name "*${OLD_MODEL_NAME}Mapper.xml" -type f | while read file; do
        echo "删除XML文件: $file"
        rm -f "$file"
    done
done

echo "删除完成！"