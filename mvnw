mvn "$@"

mvn -f "engine.io-java/pom.xml" $@
if [[ "$1" == "test" ]]; then
  mvn -f "engine.io-java/pom.xml" install -B
fi

mvn -f "engine.io-jetty/pom.xml" $@
if [[ "$1" == "test" ]]; then
  mvn -f "engine.io-jetty/pom.xml" install -B
fi

mvn -f "engine.io-test/pom.xml" $@
