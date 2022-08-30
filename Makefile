test:
	mvn test
docs:
	asciidoctor README.adoc
	# On OpenBSD, browsers don't have default access to source folders
	# to prevent numerous kinds of security breaches.
	# This should work anywhere though:
	mv README.html /tmp
install:
	mvn clean package install assembly:single
	@-rm -f ~/lib/makehandsons*.jar
	# cp target/makehandsons-*-jar-with-dependencies.jar ~/lib/makehandsons.jar
	copy target/makehandsons-*-jar-with-dependencies.jar %USERPROFILE%\lib\makehandsons.jar
	copy scripts\* %USERPROFILE%/bin/
clean:
	@rm -f makehandsons.log*
