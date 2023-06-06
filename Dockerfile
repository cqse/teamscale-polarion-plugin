FROM ubuntu:latest
RUN apt-get -y update
RUN apt-get -y install apache2 subversion libapache2-mod-svn libswt-gtk-4-java apache2-utils openjdk-17-jdk sudo curl vim locales
RUN locale-gen en_US.UTF-8

EXPOSE 80

WORKDIR /PolarionInstall
COPY . .
ENV JDK_HOME=/usr/lib/jvm/java-17-openjdk-arm64

ENV LC_ALL=en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US

ENV TZ=US/Pacific
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
