clean:
	@rm -f makehandsons.log.?
install:
	mvn clean package install assembly:single
	@rm -f ~/lib/makehandsons*.jar
	cp target/makehandsons-*-jar-with-dependencies.jar ~/lib/
