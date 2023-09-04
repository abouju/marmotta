# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Dockerfile for Apache Marmotta

FROM debian:bookworm
MAINTAINER Sergio Fernández <wikier@apache.org>

EXPOSE 8080

WORKDIR /src
ADD . /src

# configuration
ENV DEBIAN_FRONTEND noninteractive
ENV DB_NAME marmotta
ENV DB_USER marmotta
ENV DB_PASS s3cr3t
ENV PG_VERSION 15
ENV WAR_PATH /src/launchers/marmotta-webapp/target/marmotta.war
ENV CONF_PATH /var/lib/marmotta/system-config.properties

# prepare the environment
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y \
		default-jdk \
		maven \
		wget \
    || apt-get install -y -f

# build
RUN mvn clean
RUN mvn install -DskipTests -DskipITs -f ./parent/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./commons/marmotta-commons/pom.xml
RUN mvn install -DskipTests -DskipITs -f  ./build/plugins/buildinfo-maven-plugin/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldclient/ldclient-api/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldcache/ldcache-api/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldclient/ldclient-core/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldcache/ldcache-core/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldclient/ldclient-provider-rdf/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./commons/marmotta-sesame-tools/marmotta-model-vocabs/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldclient/ldclient-provider-facebook/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldclient/ldclient-provider-xml/pom.xml
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldclient/ldclient-provider-vimeo/pom.xml	
RUN mvn install -DskipTests -DskipITs -f ./libraries/ldcache/ldcache-backend-infinispan/pom.xml
RUN mvn install -DskipTests -DskipITs
RUN ls -l  /src/launchers/marmotta-webapp/target/
RUN test -e $WAR_PATH || exit

# install and configure postgres from the PGDG repo
RUN apt-get update && apt-get install -y locales apt-utils \
	&& localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.utf8
RUN apt-get install -y postgresql
RUN apt-get install -y postgis
USER postgres
RUN service postgresql start \
    && psql --command "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';" \
    && psql --command "CREATE DATABASE $DB_NAME WITH OWNER $DB_USER;" \
    && psql $DB_NAME --command "CREATE EXTENSION postgis;"	
ENV  PG_VERSIOM='postgres -V'
RUN echo $PG_VERSIOM
USER root
RUN service postgresql stop
RUN echo "host all  all    127.0.0.1/32  md5" >> /etc/postgresql/$PG_VERSION/main/pg_hba.conf
RUN echo "listen_addresses='*'" >> /etc/postgresql/$PG_VERSION/main/postgresql.conf

# install tomcat 9
RUN mkdir /opt/tomcat
RUN groupadd tomcat
RUN useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
RUN  wget https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.80/bin/apache-tomcat-9.0.80.tar.gz
RUN tar xzvf apache-tomcat-9*tar.gz -C /opt/tomcat --strip-components=1
RUN chgrp -R tomcat /opt/tomcat
RUN chown -R tomcat /opt/tomcat

# install the webapp
#RUN dpkg --debug=2000 --install target/marmotta_*_all.deb <-- we'd need to fix the postinst
RUN mkdir -p /usr/share/marmotta
RUN cp $WAR_PATH /usr/share/marmotta/
RUN chown tomcat:tomcat /usr/share/marmotta/marmotta.war
#RUN mkdir -p /var/lib/tomcat9/conf/Catalina/localhost/
RUN mkdir -p /opt/tomcat/conf/Catalina/localhost/
RUN cp /src/launchers/marmotta-webapp/src/deb/tomcat/marmotta.xml /opt/tomcat/conf/Catalina/localhost/
RUN chown tomcat:tomcat /opt/tomcat/conf/Catalina/localhost/marmotta.xml
RUN mkdir -p "$(dirname $CONF_PATH)"
RUN echo "security.enabled = false" > $CONF_PATH
RUN echo "database.type = postgres" >> $CONF_PATH
RUN echo "database.url = jdbc:postgresql://localhost:5432/$DB_NAME?prepareThreshold=3" >> $CONF_PATH
RUN echo "database.user = $DB_USER" >> $CONF_PATH
RUN echo "database.password = $DB_PASS" >> $CONF_PATH
RUN chown -R tomcat:tomcat	 "$(dirname $CONF_PATH)"

# cleanup
RUN mvn clean \
    && rm -rf ~/.m2 \
    && apt-get remove maven --purge -y \
    && apt-get autoremove -y \
    && apt-get clean -y \
    && apt-get autoclean -y \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# entrypoint
RUN cp /src/launchers/marmotta-webapp/src/docker/entrypoint.sh	 /usr/local/bin/marmotta.sh
ENTRYPOINT ["/usr/local/bin/marmotta.sh"]
