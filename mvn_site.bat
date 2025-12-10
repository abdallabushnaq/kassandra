rem call mvn clean test
rem call mvn surefire-report:report-only
rem call mvn site
call mvn -B clean package --file pom.xml -DskipTests > package.txt 2>&1
call mvn test -Dselenium.headless=true > test.txt 2>&1
call mvn site > site.txt 2>&1
pause