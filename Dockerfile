# 指定基础镜像，这是分阶段构建的前期阶段
FROM openjdk:8u212-jdk-stretch
# 执行工作目录
WORKDIR application
# 配置参数
ARG JAR_FILE=target/*.jar
# 将编译构建得到的jar文件复制到镜像空间中
COPY ${JAR_FILE} application.jar
RUN ls 
RUN java -jar application.jar
