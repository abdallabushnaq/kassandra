call mvn clean install -DskipTests
call mvn test -Dselenium.headless=true
pause