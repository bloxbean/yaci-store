#To run this script, update the database information in the application.properties file. Check for the following properties in the application.properties file.
#spring.datasource.url, spring.datasource.username, spring.datasource.password

docker compose -f compose/admin-cli-compose.yml run --rm admin-cli $1
