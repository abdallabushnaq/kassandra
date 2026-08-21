call mvn clean vaadin:prepare-frontend
call mvn package -DskipTests -Pwindows-npm
pause
