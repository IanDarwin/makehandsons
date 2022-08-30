	mvn -DskipTests clean package install assembly:single
	copy target\makehandsons-*-jar-with-dependencies.jar %USERPROFILE%\lib\makehandsons.jar
	copy scripts\* %USERPROFILE%/bin/
