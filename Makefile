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
	cp target/makehandsons-*-jar-with-dependencies.jar ~/lib/makehandsons.jar
	cp scripts/* ~/bin/
clean:
	@rm -f makehandsons.log*
