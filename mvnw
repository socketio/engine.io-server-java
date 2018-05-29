mvn "$@"
mvn -f "engine.io-java/pom.xml" $@
mvn -f "engine.io-jetty/pom.xml" $@
mvn -f "engine.io-test/pom.xml" $@
