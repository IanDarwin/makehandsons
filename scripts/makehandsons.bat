@rem Make the Hands-Ons from the Solutions, which are "known" to work.

set HOME='C:/Users/student'

set JAR=%USERPROFILE%/.m2/repository/com/darwinsys/makehandsons/1.0-SNAPSHOT/makehandsons-1.0-SNAPSHOT.jar

java -jar %JAR% *solution
