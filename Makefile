test:
	java -jar target/makehandsons-*-jar-with-dependencies.jar inttestsolution
	diff -r inttestexpected inttest || echo FAIL
docs:
	asciidoctor README.adoc
	# On OpenBSD browsers don't have default access to source folders
	# to prevent numerous kinds of security breaches.
	mv README.html /tmp
install:
	mvn clean package install assembly:single
	@rm -f ~/lib/makehandsons*.jar
	cp target/makehandsons-*-jar-with-dependencies.jar ~/lib/
clean:
	@rm -f makehandsons.log.?
