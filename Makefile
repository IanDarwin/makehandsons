test:
	mvn test
docs:
	asciidoctor README.adoc
	# On OpenBSD, browsers don't have default access to source folders
	# to prevent numerous kinds of security breaches.
	# This should work anywhere though:
	mv README.html /tmp
install:
	mvn -DskipTests clean package install assembly:single
	copy target\makehandsons-*-jar-with-dependencies.jar %USERPROFILE%\lib\makehandsons.jar
	copy scripts\* %USERPROFILE%/bin/
clean:
	@rm -f makehandsons.log*
