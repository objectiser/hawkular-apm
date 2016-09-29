docker run -it --rm -p 9042:9042 -e JVM_OPTS="-Dcassandra.custom_query_handler_class=org.apache.cassandra.cql3.CustomPayloadMirroringQueryHandler" hawkular/apm-example-polyglot-opentracing-cassandra

from java-cassandra folder


mvn clean install
mvn wildfly-swarm:run

from java-wildfly-swarm folder


curl -ivX POST -H 'Content-Type: application/json' 'http://localhost:8080/wildfly-swarm/users' -d '{"name": "jane"}'

to test the swarm+cassandra services




TODO: 

Try to enable services to run standalone and in docker:
- wildfly-swarm service would need cassandra host to be supplied as env variable or system property, and default to localhost

