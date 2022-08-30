@rem Make the Hands-Ons from the Solutions, which are "known" to work.

set HOME='C:/Users/student'

set JAR=%USERPROFILE%/lib/makehandsons.jar

java -jar %JAR% %*

set HOME=
